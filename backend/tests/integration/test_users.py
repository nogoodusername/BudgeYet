import pytest

from tests.helpers import auth_headers, create_household, create_invite, signup_and_login

pytestmark = pytest.mark.asyncio


async def test_get_my_household_is_null_before_creating_or_joining(client, monkeypatch):
    token, _ = await signup_and_login(client, monkeypatch, "solo@example.com")
    resp = await client.get("/users/me/household", headers=auth_headers(token))
    assert resp.status_code == 200
    assert resp.json() is None


async def test_get_my_household_resolves_after_create(client, monkeypatch):
    token, _ = await signup_and_login(client, monkeypatch, "creator@example.com")
    household = await create_household(client, token, "Casa")
    resp = await client.get("/users/me/household", headers=auth_headers(token))
    assert resp.status_code == 200
    assert resp.json()["id"] == household["id"]


async def test_get_my_household_resolves_after_join(client, monkeypatch):
    admin_token, _ = await signup_and_login(client, monkeypatch, "admin2@example.com")
    household = await create_household(client, admin_token, "Casa")
    _, token_str = await create_invite(client, admin_token, household["id"])

    member_token, _ = await signup_and_login(client, monkeypatch, "member2@example.com")
    join_resp = await client.post(
        "/households/join", json={"token": token_str}, headers=auth_headers(member_token)
    )
    assert join_resp.status_code == 201
    assert join_resp.json()["household_id"] == household["id"]

    resp = await client.get("/users/me/household", headers=auth_headers(member_token))
    assert resp.status_code == 200
    assert resp.json()["id"] == household["id"]
