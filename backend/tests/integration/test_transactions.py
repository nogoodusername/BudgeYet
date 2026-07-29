import pytest

from tests.helpers import auth_headers, create_household, create_invite, signup_and_login

pytestmark = pytest.mark.asyncio


async def _setup_household_with_two_members(client, monkeypatch):
    admin_token, _ = await signup_and_login(client, monkeypatch, "admin@example.com")
    household = await create_household(client, admin_token)
    category = (
        await client.post(
            f"/households/{household['id']}/categories",
            json={"name": "Groceries", "icon": "cart", "monthly_limit": 500},
            headers=auth_headers(admin_token),
        )
    ).json()

    _, token_str = await create_invite(client, admin_token, household["id"])
    member_token, _ = await signup_and_login(client, monkeypatch, "member@example.com")
    await client.post(
        "/households/join", json={"token": token_str}, headers=auth_headers(member_token)
    )
    return admin_token, member_token, household, category


async def test_create_expense_requires_category(client, monkeypatch):
    admin_token, _ = await signup_and_login(client, monkeypatch, "admin@example.com")
    household = await create_household(client, admin_token)
    resp = await client.post(
        f"/households/{household['id']}/transactions",
        json={"amount": 20, "merchant": "Store"},
        headers=auth_headers(admin_token),
    )
    assert resp.status_code == 422


async def test_future_dated_transaction_rejected(client, monkeypatch):
    admin_token, _, household, category = await _setup_household_with_two_members(
        client, monkeypatch
    )
    resp = await client.post(
        f"/households/{household['id']}/transactions",
        json={
            "amount": 20,
            "merchant": "Store",
            "category_id": category["id"],
            "transaction_date": "2099-01-01T00:00:00",
        },
        headers=auth_headers(admin_token),
    )
    assert resp.status_code == 422


async def test_member_can_only_edit_own_transaction(client, monkeypatch):
    admin_token, member_token, household, category = await _setup_household_with_two_members(
        client, monkeypatch
    )

    admin_txn = (
        await client.post(
            f"/households/{household['id']}/transactions",
            json={"amount": 20, "merchant": "Admin Store", "category_id": category["id"]},
            headers=auth_headers(admin_token),
        )
    ).json()

    member_txn = (
        await client.post(
            f"/households/{household['id']}/transactions",
            json={"amount": 15, "merchant": "Member Store", "category_id": category["id"]},
            headers=auth_headers(member_token),
        )
    ).json()

    # Member editing their own transaction succeeds
    resp = await client.patch(
        f"/households/{household['id']}/transactions/{member_txn['id']}",
        json={"amount": 30},
        headers=auth_headers(member_token),
    )
    assert resp.status_code == 200

    # Member editing the admin's transaction is forbidden
    resp = await client.patch(
        f"/households/{household['id']}/transactions/{admin_txn['id']}",
        json={"amount": 30},
        headers=auth_headers(member_token),
    )
    assert resp.status_code == 403

    # Admin editing the member's transaction succeeds
    resp = await client.patch(
        f"/households/{household['id']}/transactions/{member_txn['id']}",
        json={"amount": 99},
        headers=auth_headers(admin_token),
    )
    assert resp.status_code == 200


async def test_member_can_only_delete_own_transaction(client, monkeypatch):
    admin_token, member_token, household, category = await _setup_household_with_two_members(
        client, monkeypatch
    )
    admin_txn = (
        await client.post(
            f"/households/{household['id']}/transactions",
            json={"amount": 20, "merchant": "Admin Store", "category_id": category["id"]},
            headers=auth_headers(admin_token),
        )
    ).json()

    resp = await client.delete(
        f"/households/{household['id']}/transactions/{admin_txn['id']}",
        headers=auth_headers(member_token),
    )
    assert resp.status_code == 403

    resp = await client.delete(
        f"/households/{household['id']}/transactions/{admin_txn['id']}",
        headers=auth_headers(admin_token),
    )
    assert resp.status_code == 204


async def test_category_must_belong_to_household(client, monkeypatch):
    admin_token, _ = await signup_and_login(client, monkeypatch, "admin@example.com")
    household_a = await create_household(client, admin_token, "House A")

    other_admin_token, _ = await signup_and_login(client, monkeypatch, "other@example.com")
    household_b = await create_household(client, other_admin_token, "House B")
    foreign_category = (
        await client.post(
            f"/households/{household_b['id']}/categories",
            json={"name": "Foreign", "icon": "box", "monthly_limit": 100},
            headers=auth_headers(other_admin_token),
        )
    ).json()

    resp = await client.post(
        f"/households/{household_a['id']}/transactions",
        json={"amount": 10, "merchant": "Store", "category_id": foreign_category["id"]},
        headers=auth_headers(admin_token),
    )
    assert resp.status_code == 422


async def test_paid_by_must_be_a_household_member(client, monkeypatch):
    admin_token, _ = await signup_and_login(client, monkeypatch, "admin@example.com")
    household = await create_household(client, admin_token)
    category = (
        await client.post(
            f"/households/{household['id']}/categories",
            json={"name": "Groceries", "icon": "cart", "monthly_limit": 500},
            headers=auth_headers(admin_token),
        )
    ).json()

    outsider_token, outsider_user = await signup_and_login(client, monkeypatch, "outsider@example.com")

    resp = await client.post(
        f"/households/{household['id']}/transactions",
        json={
            "amount": 10,
            "merchant": "Store",
            "category_id": category["id"],
            "paid_by_id": outsider_user["id"],
        },
        headers=auth_headers(admin_token),
    )
    assert resp.status_code == 422


async def test_list_transactions_filters_by_category(client, monkeypatch):
    admin_token, _, household, category = await _setup_household_with_two_members(
        client, monkeypatch
    )
    other_category = (
        await client.post(
            f"/households/{household['id']}/categories",
            json={"name": "Fun", "icon": "star", "monthly_limit": 100},
            headers=auth_headers(admin_token),
        )
    ).json()

    await client.post(
        f"/households/{household['id']}/transactions",
        json={"amount": 10, "merchant": "A", "category_id": category["id"]},
        headers=auth_headers(admin_token),
    )
    await client.post(
        f"/households/{household['id']}/transactions",
        json={"amount": 10, "merchant": "B", "category_id": other_category["id"]},
        headers=auth_headers(admin_token),
    )

    resp = await client.get(
        f"/households/{household['id']}/transactions",
        params={"category_id": category["id"]},
        headers=auth_headers(admin_token),
    )
    body = resp.json()
    assert body["total"] == 1
    assert body["items"][0]["merchant"] == "A"
