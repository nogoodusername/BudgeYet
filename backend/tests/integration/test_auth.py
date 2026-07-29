import pytest

from tests.helpers import FIXED_PIN, signup

pytestmark = pytest.mark.asyncio


async def test_signup_creates_user_without_leaking_pin(client, monkeypatch):
    resp = await signup(client, monkeypatch, "ada@example.com", "Ada Admin", "Ada")
    assert resp.status_code == 201
    body = resp.json()
    assert body["email"] == "ada@example.com"
    assert "pin" not in body
    assert "pin_hash" not in body


async def test_signup_duplicate_email_conflicts(client, monkeypatch):
    await signup(client, monkeypatch, "dup@example.com")
    resp = await signup(client, monkeypatch, "dup@example.com")
    assert resp.status_code == 409


async def test_login_with_correct_pin_succeeds(client, monkeypatch):
    await signup(client, monkeypatch, "ada@example.com")
    resp = await client.post("/auth/login", json={"email": "ada@example.com", "pin": FIXED_PIN})
    assert resp.status_code == 200
    assert "access_token" in resp.json()


async def test_login_with_wrong_pin_fails(client, monkeypatch):
    await signup(client, monkeypatch, "ada@example.com")
    resp = await client.post("/auth/login", json={"email": "ada@example.com", "pin": "000000"})
    assert resp.status_code == 401


async def test_login_unknown_email_fails(client, monkeypatch):
    resp = await client.post(
        "/auth/login", json={"email": "nobody@example.com", "pin": FIXED_PIN}
    )
    assert resp.status_code == 401


async def test_protected_endpoint_requires_token(client):
    resp = await client.get("/users/me")
    assert resp.status_code == 401


async def test_forgot_pin_issues_new_pin_and_invalidates_old_one(client, monkeypatch):
    await signup(client, monkeypatch, "ada@example.com")

    monkeypatch.setattr("app.services.auth_service.generate_pin", lambda: "654321")
    resp = await client.post("/auth/forgot-pin", json={"email": "ada@example.com"})
    assert resp.status_code == 204

    old_pin_resp = await client.post(
        "/auth/login", json={"email": "ada@example.com", "pin": FIXED_PIN}
    )
    assert old_pin_resp.status_code == 401

    new_pin_resp = await client.post(
        "/auth/login", json={"email": "ada@example.com", "pin": "654321"}
    )
    assert new_pin_resp.status_code == 200


async def test_forgot_pin_unknown_email_does_not_leak_existence(client):
    resp = await client.post("/auth/forgot-pin", json={"email": "nobody@example.com"})
    assert resp.status_code == 204
