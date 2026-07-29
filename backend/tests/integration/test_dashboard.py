import pytest

from tests.helpers import auth_headers, create_household, signup_and_login

pytestmark = pytest.mark.asyncio


async def test_dashboard_empty_state(client, monkeypatch):
    token, _ = await signup_and_login(client, monkeypatch, "admin@example.com")
    household = await create_household(client, token)
    resp = await client.get(f"/households/{household['id']}/dashboard", headers=auth_headers(token))
    assert resp.status_code == 200
    body = resp.json()
    assert body["has_budget"] is False
    assert body["has_transactions"] is False
    assert body["budget"] is None
    assert body["categories"] == []


async def test_dashboard_over_budget_status(client, monkeypatch):
    token, _ = await signup_and_login(client, monkeypatch, "admin@example.com")
    household = await create_household(client, token)
    category = (
        await client.post(
            f"/households/{household['id']}/categories",
            json={"name": "Groceries", "icon": "cart", "monthly_limit": 100},
            headers=auth_headers(token),
        )
    ).json()
    await client.post(
        f"/households/{household['id']}/budgets",
        json={"name": "This month", "monthly_goal_amount": 100},
        headers=auth_headers(token),
    )
    await client.post(
        f"/households/{household['id']}/transactions",
        json={"amount": 150, "merchant": "Big Store", "category_id": category["id"]},
        headers=auth_headers(token),
    )

    resp = await client.get(f"/households/{household['id']}/dashboard", headers=auth_headers(token))
    body = resp.json()
    assert body["budget"]["status"] == "over_budget"
    assert body["categories"][0]["status"] == "over_budget"


async def test_activity_feed_reverse_chronological(client, monkeypatch):
    token, _ = await signup_and_login(client, monkeypatch, "admin@example.com")
    household = await create_household(client, token)
    category = (
        await client.post(
            f"/households/{household['id']}/categories",
            json={"name": "Groceries", "icon": "cart", "monthly_limit": 500},
            headers=auth_headers(token),
        )
    ).json()

    await client.post(
        f"/households/{household['id']}/transactions",
        json={"amount": 10, "merchant": "First", "category_id": category["id"]},
        headers=auth_headers(token),
    )
    await client.post(
        f"/households/{household['id']}/transactions",
        json={"amount": 20, "merchant": "Second", "category_id": category["id"]},
        headers=auth_headers(token),
    )

    resp = await client.get(
        f"/households/{household['id']}/activity-feed", headers=auth_headers(token)
    )
    body = resp.json()
    assert body["total"] == 2
    assert body["items"][0]["merchant"] == "Second"
    assert body["items"][1]["merchant"] == "First"
