import itertools

FIXED_PIN = "123456"
_token_counter = itertools.count(1)
_state = {"invite_token": None}


def patch_deterministic_secrets(monkeypatch):
    """Invite-token generation and forgot-PIN's server-generated PIN are intentionally not
    echoed back in API responses (see AGENTS.md "Known gaps" — real email delivery is still
    stubbed). Tests patch the generators so they can drive those flows without reading logs.
    Signup no longer needs this: the PIN is user-chosen (see `signup()` below), not generated.
    """
    monkeypatch.setattr("app.services.auth_service.generate_pin", lambda: FIXED_PIN)

    def _generate_invite_token():
        _state["invite_token"] = f"test-invite-token-{next(_token_counter)}"
        return _state["invite_token"]

    monkeypatch.setattr("app.services.household_service.generate_invite_token", _generate_invite_token)


def last_invite_token():
    return _state["invite_token"]


async def signup(client, monkeypatch, email, full_name="Test User", nickname="Tester", pin=FIXED_PIN):
    patch_deterministic_secrets(monkeypatch)
    resp = await client.post(
        "/auth/signup",
        json={"email": email, "full_name": full_name, "nickname": nickname, "pin": pin},
    )
    return resp


async def signup_and_login(client, monkeypatch, email, full_name="Test User", nickname="Tester"):
    await signup(client, monkeypatch, email, full_name, nickname)
    resp = await client.post("/auth/login", json={"email": email, "pin": FIXED_PIN})
    return resp.json()["access_token"], resp.json()["user"]


def auth_headers(token):
    return {"Authorization": f"Bearer {token}"}


async def create_household(client, token, name="Casa", **overrides):
    payload = {"name": name, "currency": "USD", "language": "en", "cycle_start_day": 1}
    payload.update(overrides)
    resp = await client.post("/households", json=payload, headers=auth_headers(token))
    return resp.json()


async def create_invite(client, admin_token, household_id, email=None):
    resp = await client.post(
        f"/households/{household_id}/invites",
        json={"email": email},
        headers=auth_headers(admin_token),
    )
    return resp, last_invite_token()
