import pytest

from tests.helpers import auth_headers, create_household, create_invite, signup_and_login

pytestmark = pytest.mark.asyncio


async def _setup_household_with_category(client, monkeypatch):
    token, _ = await signup_and_login(client, monkeypatch, "admin@example.com")
    household = await create_household(client, token)
    resp = await client.post(
        f"/households/{household['id']}/categories",
        json={"name": "Groceries", "icon": "cart", "monthly_limit": 500},
        headers=auth_headers(token),
    )
    return token, household, resp.json()


async def test_create_and_list_categories_with_stats(client, monkeypatch):
    token, household, category = await _setup_household_with_category(client, monkeypatch)

    resp = await client.get(
        f"/households/{household['id']}/categories", headers=auth_headers(token)
    )
    assert resp.status_code == 200
    [snapshot] = resp.json()
    assert snapshot["id"] == category["id"]
    assert snapshot["spent"] == 0.0
    assert snapshot["status"] == "on_track"


async def test_non_admin_cannot_create_category(client, monkeypatch):
    admin_token, household, _ = await _setup_household_with_category(client, monkeypatch)
    _, token_str = await create_invite(client, admin_token, household["id"])
    member_token, _ = await signup_and_login(client, monkeypatch, "member@example.com")
    await client.post(
        "/households/join", json={"token": token_str}, headers=auth_headers(member_token)
    )

    resp = await client.post(
        f"/households/{household['id']}/categories",
        json={"name": "Fun", "icon": "star", "monthly_limit": 100},
        headers=auth_headers(member_token),
    )
    assert resp.status_code == 403


async def test_delete_category_with_transactions_requires_reassignment(client, monkeypatch):
    token, household, category = await _setup_household_with_category(client, monkeypatch)

    await client.post(
        f"/households/{household['id']}/transactions",
        json={"amount": 20, "merchant": "Store", "category_id": category["id"]},
        headers=auth_headers(token),
    )

    resp = await client.delete(
        f"/households/{household['id']}/categories/{category['id']}",
        headers=auth_headers(token),
    )
    assert resp.status_code == 409

    other = await client.post(
        f"/households/{household['id']}/categories",
        json={"name": "Misc", "icon": "box", "monthly_limit": 100},
        headers=auth_headers(token),
    )
    other_id = other.json()["id"]

    resp = await client.delete(
        f"/households/{household['id']}/categories/{category['id']}",
        params={"reassign_to_category_id": other_id},
        headers=auth_headers(token),
    )
    assert resp.status_code == 204

    txns = await client.get(
        f"/households/{household['id']}/transactions", headers=auth_headers(token)
    )
    assert txns.json()["items"][0]["category"]["id"] == other_id


async def test_delete_category_without_transactions_succeeds_directly(client, monkeypatch):
    token, household, category = await _setup_household_with_category(client, monkeypatch)
    resp = await client.delete(
        f"/households/{household['id']}/categories/{category['id']}",
        headers=auth_headers(token),
    )
    assert resp.status_code == 204


async def test_update_category_limit(client, monkeypatch):
    token, household, category = await _setup_household_with_category(client, monkeypatch)
    resp = await client.patch(
        f"/households/{household['id']}/categories/{category['id']}",
        json={"monthly_limit": 750},
        headers=auth_headers(token),
    )
    assert resp.status_code == 200
    assert resp.json()["monthly_limit"] == 750
