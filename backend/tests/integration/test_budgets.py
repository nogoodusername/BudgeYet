import pytest

from tests.helpers import auth_headers, create_household, signup_and_login

pytestmark = pytest.mark.asyncio


async def test_create_budget_defaults_to_current_cycle(client, monkeypatch):
    token, _ = await signup_and_login(client, monkeypatch, "admin@example.com")
    household = await create_household(client, token)

    resp = await client.post(
        f"/households/{household['id']}/budgets",
        json={"name": "This month", "monthly_goal_amount": 2000},
        headers=auth_headers(token),
    )
    assert resp.status_code == 201
    body = resp.json()
    assert body["month"] and body["year"]


async def test_duplicate_budget_for_same_cycle_conflicts(client, monkeypatch):
    token, _ = await signup_and_login(client, monkeypatch, "admin@example.com")
    household = await create_household(client, token)

    payload = {"name": "This month", "monthly_goal_amount": 2000}
    first = await client.post(
        f"/households/{household['id']}/budgets", json=payload, headers=auth_headers(token)
    )
    assert first.status_code == 201

    second = await client.post(
        f"/households/{household['id']}/budgets", json=payload, headers=auth_headers(token)
    )
    assert second.status_code == 409


async def test_concurrent_budget_creation_hits_unique_constraint_cleanly(client, monkeypatch):
    """Simulates a race: another request's budget insert lands in between this
    request's get_for_cycle check and its own insert. The check alone can't catch
    that -- the (household_id, month, year) unique constraint is the real guard,
    so this exercises the IntegrityError -> ConflictError translation path rather
    than the check-then-act fast path.
    """
    from app.models.budget import Budget
    from app.repositories.budget_repository import BudgetRepository
    from app.services.cycle_utils import get_current_cycle_bounds
    from tests.conftest import TestSessionLocal

    token, _ = await signup_and_login(client, monkeypatch, "admin@example.com")
    household = await create_household(client, token)
    bounds = get_current_cycle_bounds(household["cycle_start_day"])

    async with TestSessionLocal() as session:
        session.add(
            Budget(
                household_id=household["id"],
                name="Racing budget",
                monthly_goal_amount=500,
                month=bounds.label_month,
                year=bounds.label_year,
            )
        )
        await session.commit()

    async def _fake_get_for_cycle(self, household_id, month, year):
        return None

    monkeypatch.setattr(BudgetRepository, "get_for_cycle", _fake_get_for_cycle)

    resp = await client.post(
        f"/households/{household['id']}/budgets",
        json={"name": "This month", "monthly_goal_amount": 2000},
        headers=auth_headers(token),
    )
    assert resp.status_code == 409


async def test_current_budget_reflects_spend(client, monkeypatch):
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
        f"/households/{household['id']}/budgets",
        json={"name": "This month", "monthly_goal_amount": 1000},
        headers=auth_headers(token),
    )
    await client.post(
        f"/households/{household['id']}/transactions",
        json={"amount": 250, "merchant": "Store", "category_id": category["id"]},
        headers=auth_headers(token),
    )

    resp = await client.get(
        f"/households/{household['id']}/budgets/current", headers=auth_headers(token)
    )
    body = resp.json()
    assert body["spent"] == "250.00"
    assert body["remaining"] == "750.00"
    assert body["percent_used"] == 25.0
    assert body["status"] == "on_track"


async def test_current_budget_is_null_when_none_configured(client, monkeypatch):
    token, _ = await signup_and_login(client, monkeypatch, "admin@example.com")
    household = await create_household(client, token)
    resp = await client.get(
        f"/households/{household['id']}/budgets/current", headers=auth_headers(token)
    )
    assert resp.status_code == 200
    assert resp.json() is None


async def test_update_budget_goal_amount(client, monkeypatch):
    token, _ = await signup_and_login(client, monkeypatch, "admin@example.com")
    household = await create_household(client, token)
    budget = (
        await client.post(
            f"/households/{household['id']}/budgets",
            json={"name": "This month", "monthly_goal_amount": 1000},
            headers=auth_headers(token),
        )
    ).json()

    resp = await client.patch(
        f"/households/{household['id']}/budgets/{budget['id']}",
        json={"monthly_goal_amount": 1500},
        headers=auth_headers(token),
    )
    assert resp.status_code == 200
    assert resp.json()["monthly_goal_amount"] == "1500.00"
