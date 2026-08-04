import pytest

pytestmark = pytest.mark.asyncio


async def test_ping_is_reachable_without_auth(client):
    resp = await client.get("/ping")
    assert resp.status_code == 200
    body = resp.json()
    assert body["status"] == "online"
    assert "service" in body


async def test_health_reports_database_status(client):
    resp = await client.get("/health")
    assert resp.status_code == 200
    body = resp.json()
    assert body["status"] == "online"
    assert body["database_status"] == "healthy"
