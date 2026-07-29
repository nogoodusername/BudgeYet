import pytest

from tests.helpers import auth_headers, create_household, create_invite, signup_and_login

pytestmark = pytest.mark.asyncio


async def test_create_household_makes_creator_admin(client, monkeypatch):
    token, _ = await signup_and_login(client, monkeypatch, "ada@example.com")
    household = await create_household(client, token)
    assert household["members"][0]["role"] == "admin"


async def test_v1_caps_a_user_to_one_household(client, monkeypatch):
    token, _ = await signup_and_login(client, monkeypatch, "ada@example.com")
    await create_household(client, token, "First")
    resp = await client.post(
        "/households",
        json={"name": "Second", "currency": "USD", "language": "en", "cycle_start_day": 1},
        headers=auth_headers(token),
    )
    assert resp.status_code == 409


async def test_concurrent_household_creation_hits_unique_constraint_cleanly(client, monkeypatch):
    """Simulates a race: another request's membership insert lands in between this
    request's existing_membership check and its own insert. The check alone can't
    catch that — household_members.user_id's unique constraint is the real guard,
    so this exercises the IntegrityError -> ConflictError translation path rather
    than the check-then-act fast path.
    """
    from app.models.household import HouseholdMember, MemberRole
    from app.repositories.household_member_repository import HouseholdMemberRepository
    from tests.conftest import TestSessionLocal

    other_token, _ = await signup_and_login(client, monkeypatch, "other-admin@example.com")
    other_household = await create_household(client, other_token, "Other House")

    token, user = await signup_and_login(client, monkeypatch, "racer@example.com")

    async with TestSessionLocal() as session:
        session.add(
            HouseholdMember(
                household_id=other_household["id"], user_id=user["id"], role=MemberRole.MEMBER
            )
        )
        await session.commit()

    async def _fake_get_by_user(self, user_id):
        return None

    monkeypatch.setattr(HouseholdMemberRepository, "get_by_user", _fake_get_by_user)

    resp = await client.post(
        "/households",
        json={"name": "Second", "currency": "USD", "language": "en", "cycle_start_day": 1},
        headers=auth_headers(token),
    )
    assert resp.status_code == 409


async def test_concurrent_join_hits_unique_constraint_cleanly(client, monkeypatch):
    from app.models.household import HouseholdMember, MemberRole
    from app.repositories.household_member_repository import HouseholdMemberRepository
    from tests.conftest import TestSessionLocal

    admin_token, _ = await signup_and_login(client, monkeypatch, "admin2@example.com")
    household = await create_household(client, admin_token, "Racer House")
    _, token_str = await create_invite(client, admin_token, household["id"])

    other_admin_token, _ = await signup_and_login(client, monkeypatch, "other-admin2@example.com")
    other_household = await create_household(client, other_admin_token, "Other House 2")

    member_token, member_user = await signup_and_login(client, monkeypatch, "racer2@example.com")

    async with TestSessionLocal() as session:
        session.add(
            HouseholdMember(
                household_id=other_household["id"], user_id=member_user["id"], role=MemberRole.MEMBER
            )
        )
        await session.commit()

    async def _fake_get_by_user(self, user_id):
        return None

    monkeypatch.setattr(HouseholdMemberRepository, "get_by_user", _fake_get_by_user)

    resp = await client.post(
        "/households/join", json={"token": token_str}, headers=auth_headers(member_token)
    )
    assert resp.status_code == 409


async def test_join_via_invite_adds_member(client, monkeypatch):
    admin_token, _ = await signup_and_login(client, monkeypatch, "admin@example.com")
    household = await create_household(client, admin_token)

    _, token_str = await create_invite(client, admin_token, household["id"])

    member_token, _ = await signup_and_login(client, monkeypatch, "member@example.com")
    resp = await client.post(
        "/households/join", json={"token": token_str}, headers=auth_headers(member_token)
    )
    assert resp.status_code == 201
    assert resp.json()["role"] == "member"


async def test_household_member_cap_is_three(client, monkeypatch):
    admin_token, _ = await signup_and_login(client, monkeypatch, "admin@example.com")
    household = await create_household(client, admin_token)

    for i in range(2):
        _, token_str = await create_invite(client, admin_token, household["id"])
        member_token, _ = await signup_and_login(client, monkeypatch, f"member{i}@example.com")
        resp = await client.post(
            "/households/join", json={"token": token_str}, headers=auth_headers(member_token)
        )
        assert resp.status_code == 201

    # household now has 3 members (1 admin + 2 members) — a 4th invite must be refused
    resp = await client.post(
        f"/households/{household['id']}/invites", json={}, headers=auth_headers(admin_token)
    )
    assert resp.status_code == 409


async def test_non_admin_cannot_create_invite(client, monkeypatch):
    admin_token, _ = await signup_and_login(client, monkeypatch, "admin@example.com")
    household = await create_household(client, admin_token)
    _, token_str = await create_invite(client, admin_token, household["id"])

    member_token, _ = await signup_and_login(client, monkeypatch, "member@example.com")
    await client.post(
        "/households/join", json={"token": token_str}, headers=auth_headers(member_token)
    )

    resp = await client.post(
        f"/households/{household['id']}/invites", json={}, headers=auth_headers(member_token)
    )
    assert resp.status_code == 403


async def test_expired_invite_cannot_be_joined(client, monkeypatch):
    import datetime

    from app.repositories.invite_repository import InviteRepository
    from tests.conftest import TestSessionLocal

    admin_token, _ = await signup_and_login(client, monkeypatch, "admin@example.com")
    household = await create_household(client, admin_token)
    _, token_str = await create_invite(client, admin_token, household["id"])

    async with TestSessionLocal() as session:
        repo = InviteRepository(session)
        invite = await repo.get_by_token(token_str)
        invite.expires_at = datetime.datetime.utcnow() - datetime.timedelta(days=1)
        await session.commit()

    member_token, _ = await signup_and_login(client, monkeypatch, "member@example.com")
    resp = await client.post(
        "/households/join", json={"token": token_str}, headers=auth_headers(member_token)
    )
    assert resp.status_code == 422


async def test_sole_admin_cannot_leave(client, monkeypatch):
    admin_token, _ = await signup_and_login(client, monkeypatch, "admin@example.com")
    household = await create_household(client, admin_token)

    resp = await client.post(
        f"/households/{household['id']}/leave", headers=auth_headers(admin_token)
    )
    assert resp.status_code == 409


async def test_sole_admin_cannot_be_demoted(client, monkeypatch):
    admin_token, _ = await signup_and_login(client, monkeypatch, "admin@example.com")
    household = await create_household(client, admin_token)
    admin_member_id = household["members"][0]["id"]

    resp = await client.patch(
        f"/households/{household['id']}/members/{admin_member_id}/role",
        json={"role": "member"},
        headers=auth_headers(admin_token),
    )
    assert resp.status_code == 409


async def test_promote_member_then_original_admin_can_leave(client, monkeypatch):
    admin_token, _ = await signup_and_login(client, monkeypatch, "admin@example.com")
    household = await create_household(client, admin_token)
    _, token_str = await create_invite(client, admin_token, household["id"])

    member_token, member_user = await signup_and_login(client, monkeypatch, "member@example.com")
    join_resp = await client.post(
        "/households/join", json={"token": token_str}, headers=auth_headers(member_token)
    )
    new_member_id = join_resp.json()["id"]

    promote_resp = await client.patch(
        f"/households/{household['id']}/members/{new_member_id}/role",
        json={"role": "admin"},
        headers=auth_headers(admin_token),
    )
    assert promote_resp.status_code == 200

    leave_resp = await client.post(
        f"/households/{household['id']}/leave", headers=auth_headers(admin_token)
    )
    assert leave_resp.status_code == 204


async def test_revoked_invite_cannot_be_joined(client, monkeypatch):
    admin_token, _ = await signup_and_login(client, monkeypatch, "admin@example.com")
    household = await create_household(client, admin_token)
    invite_resp, token_str = await create_invite(client, admin_token, household["id"])
    invite_id = invite_resp.json()["id"]

    revoke_resp = await client.delete(
        f"/households/{household['id']}/invites/{invite_id}", headers=auth_headers(admin_token)
    )
    assert revoke_resp.status_code == 204

    member_token, _ = await signup_and_login(client, monkeypatch, "member@example.com")
    resp = await client.post(
        "/households/join", json={"token": token_str}, headers=auth_headers(member_token)
    )
    assert resp.status_code == 422
