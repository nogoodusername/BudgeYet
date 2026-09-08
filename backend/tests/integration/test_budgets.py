import pytest

from tests.helpers import (
    auth_headers,
    create_household,
    create_invite,
    signup_and_login,
)

pytestmark = pytest.mark.asyncio


async def _move_budget_to_previous_cycle(household_id: int, budget_id: int):
    """Backdate a budget row by one month so the current cycle has no budget,
    simulating a month rollover."""
    from app.models.budget import Budget
    from tests.conftest import TestSessionLocal

    async with TestSessionLocal() as session:
        budget = await session.get(Budget, budget_id)
        if budget.month == 1:
            budget.month, budget.year = 12, budget.year - 1
        else:
            budget.month -= 1
        await session.commit()


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


async def test_rollover_carries_previous_budget_into_current_cycle(client, monkeypatch):
    from app.services.cycle_utils import get_current_cycle_bounds, month_label

    token, _ = await signup_and_login(client, monkeypatch, "admin@example.com")
    household = await create_household(client, token)
    budget = (
        await client.post(
            f"/households/{household['id']}/budgets",
            json={"name": "Last month", "monthly_goal_amount": 1234},
            headers=auth_headers(token),
        )
    ).json()
    await _move_budget_to_previous_cycle(household["id"], budget["id"])

    resp = await client.post(
        f"/households/{household['id']}/budgets/rollover", headers=auth_headers(token)
    )
    assert resp.status_code == 200
    body = resp.json()
    bounds = get_current_cycle_bounds(household["cycle_start_day"])
    assert body["id"] != budget["id"]
    assert body["monthly_goal_amount"] == "1234.00"
    assert body["name"] == month_label(bounds.label_month, bounds.label_year)
    assert (body["month"], body["year"]) == (bounds.label_month, bounds.label_year)

    # Dashboard now reports a budget for the current cycle.
    dash = await client.get(
        f"/households/{household['id']}/dashboard", headers=auth_headers(token)
    )
    assert dash.json()["has_budget"] is True


async def test_rollover_is_noop_when_current_cycle_budget_exists(client, monkeypatch):
    token, _ = await signup_and_login(client, monkeypatch, "admin@example.com")
    household = await create_household(client, token)
    budget = (
        await client.post(
            f"/households/{household['id']}/budgets",
            json={"name": "This month", "monthly_goal_amount": 2000},
            headers=auth_headers(token),
        )
    ).json()

    resp = await client.post(
        f"/households/{household['id']}/budgets/rollover", headers=auth_headers(token)
    )
    assert resp.status_code == 200
    assert resp.json()["id"] == budget["id"]
    assert resp.json()["monthly_goal_amount"] == "2000.00"

    history = await client.get(
        f"/households/{household['id']}/budgets", headers=auth_headers(token)
    )
    assert len(history.json()) == 1


async def test_rollover_returns_null_when_household_never_had_a_budget(client, monkeypatch):
    token, _ = await signup_and_login(client, monkeypatch, "admin@example.com")
    household = await create_household(client, token)

    resp = await client.post(
        f"/households/{household['id']}/budgets/rollover", headers=auth_headers(token)
    )
    assert resp.status_code == 200
    assert resp.json() is None

    history = await client.get(
        f"/households/{household['id']}/budgets", headers=auth_headers(token)
    )
    assert history.json() == []


async def test_rollover_is_idempotent(client, monkeypatch):
    token, _ = await signup_and_login(client, monkeypatch, "admin@example.com")
    household = await create_household(client, token)
    budget = (
        await client.post(
            f"/households/{household['id']}/budgets",
            json={"name": "Last month", "monthly_goal_amount": 900},
            headers=auth_headers(token),
        )
    ).json()
    await _move_budget_to_previous_cycle(household["id"], budget["id"])

    first = await client.post(
        f"/households/{household['id']}/budgets/rollover", headers=auth_headers(token)
    )
    second = await client.post(
        f"/households/{household['id']}/budgets/rollover", headers=auth_headers(token)
    )
    assert first.status_code == second.status_code == 200
    assert first.json()["id"] == second.json()["id"]

    history = await client.get(
        f"/households/{household['id']}/budgets", headers=auth_headers(token)
    )
    assert len(history.json()) == 2  # the backdated one + the carried-over one


async def test_rollover_can_be_triggered_by_a_plain_member(client, monkeypatch):
    admin_token, _ = await signup_and_login(client, monkeypatch, "admin@example.com")
    household = await create_household(client, admin_token)
    budget = (
        await client.post(
            f"/households/{household['id']}/budgets",
            json={"name": "Last month", "monthly_goal_amount": 750},
            headers=auth_headers(admin_token),
        )
    ).json()
    await _move_budget_to_previous_cycle(household["id"], budget["id"])

    _, token_str = await create_invite(client, admin_token, household["id"])
    member_token, _ = await signup_and_login(client, monkeypatch, "member@example.com")
    await client.post(
        "/households/join", json={"token": token_str}, headers=auth_headers(member_token)
    )

    resp = await client.post(
        f"/households/{household['id']}/budgets/rollover", headers=auth_headers(member_token)
    )
    assert resp.status_code == 200
    assert resp.json()["monthly_goal_amount"] == "750.00"


async def test_rollover_targets_previous_month_label_for_custom_cycle_start_day(
    client, monkeypatch
):
    """cycle_start_day=15 with a reference before the 15th belongs to the prior
    month's cycle — the carried budget must be labelled for that cycle."""
    from datetime import date

    from app.models.household import Household
    from app.services.budget_service import BudgetService
    from app.services.cycle_utils import month_label
    from tests.conftest import TestSessionLocal

    token, _ = await signup_and_login(client, monkeypatch, "admin@example.com")
    household = await create_household(client, token, cycle_start_day=15)
    budget = (
        await client.post(
            f"/households/{household['id']}/budgets",
            json={"name": "Older", "monthly_goal_amount": 500},
            headers=auth_headers(token),
        )
    ).json()
    await _move_budget_to_previous_cycle(household["id"], budget["id"])
    await _move_budget_to_previous_cycle(household["id"], budget["id"])

    async with TestSessionLocal() as session:
        household_row = await session.get(Household, household["id"])
        carried = await BudgetService(session).rollover_current_cycle_budget(
            household_row, reference=date(2026, 5, 5)
        )
        await session.commit()
        assert (carried.month, carried.year) == (4, 2026)
        assert carried.name == month_label(4, 2026)
        assert carried.monthly_goal_amount == 500
