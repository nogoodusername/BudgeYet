import re

import pytest

from app.core.config import settings
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


async def test_login_locks_account_after_max_failed_attempts(client, monkeypatch):
    await signup(client, monkeypatch, "ada@example.com")

    for _ in range(5):
        resp = await client.post(
            "/auth/login", json={"email": "ada@example.com", "pin": "000000"}
        )
        assert resp.status_code == 401

    # Correct PIN is rejected once the account is locked.
    resp = await client.post("/auth/login", json={"email": "ada@example.com", "pin": FIXED_PIN})
    assert resp.status_code == 401
    assert "too many" in resp.json()["detail"].lower()


async def test_login_resets_attempt_counter_after_success(client, monkeypatch):
    await signup(client, monkeypatch, "ada@example.com")

    for _ in range(4):
        resp = await client.post(
            "/auth/login", json={"email": "ada@example.com", "pin": "000000"}
        )
        assert resp.status_code == 401

    resp = await client.post("/auth/login", json={"email": "ada@example.com", "pin": FIXED_PIN})
    assert resp.status_code == 200

    # Counter should have reset, so another 4 bad attempts don't trigger a lockout yet.
    for _ in range(4):
        resp = await client.post(
            "/auth/login", json={"email": "ada@example.com", "pin": "000000"}
        )
        assert resp.status_code == 401

    resp = await client.post("/auth/login", json={"email": "ada@example.com", "pin": FIXED_PIN})
    assert resp.status_code == 200


async def test_login_lockout_message_reports_multi_day_wait_correctly(client, monkeypatch):
    # Regression test: timedelta.seconds is the sub-day remainder, not the total
    # duration, so a lockout window past 1440 minutes would previously be
    # under-reported (e.g. ~1500 minutes silently became ~60).
    monkeypatch.setattr(settings, "LOGIN_LOCKOUT_MINUTES", 1500)
    await signup(client, monkeypatch, "ada@example.com")

    for _ in range(5):
        resp = await client.post(
            "/auth/login", json={"email": "ada@example.com", "pin": "000000"}
        )
        assert resp.status_code == 401

    resp = await client.post("/auth/login", json={"email": "ada@example.com", "pin": FIXED_PIN})
    assert resp.status_code == 401
    minutes_reported = int(re.search(r"(\d+) minute", resp.json()["detail"]).group(1))
    assert minutes_reported > 1440


async def test_login_throttles_by_ip_across_distinct_emails(client, monkeypatch):
    # An attacker spraying guesses across many emails (or many unregistered
    # emails) never trips the per-account lockout, since that counter lives on
    # the targeted account. The IP-level throttle catches this instead.
    monkeypatch.setattr(settings, "MAX_LOGIN_FAILURES_PER_IP", 3)

    for _ in range(3):
        resp = await client.post(
            "/auth/login", json={"email": "nobody@example.com", "pin": "000000"}
        )
        assert resp.status_code == 401

    resp = await client.post(
        "/auth/login", json={"email": "somebody-else@example.com", "pin": "000000"}
    )
    assert resp.status_code == 429


async def test_forgot_pin_clears_existing_lockout(client, monkeypatch):
    await signup(client, monkeypatch, "ada@example.com")

    for _ in range(5):
        resp = await client.post(
            "/auth/login", json={"email": "ada@example.com", "pin": "000000"}
        )
        assert resp.status_code == 401

    monkeypatch.setattr("app.services.auth_service.generate_pin", lambda: "654321")
    resp = await client.post("/auth/forgot-pin", json={"email": "ada@example.com"})
    assert resp.status_code == 204

    resp = await client.post("/auth/login", json={"email": "ada@example.com", "pin": "654321"})
    assert resp.status_code == 200
