from fastapi import APIRouter, Depends, Query
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.deps import get_current_household, get_db, get_household_membership
from app.controllers import dashboard_controller
from app.models.household import Household, HouseholdMember
from app.schemas.common import Page
from app.schemas.dashboard import ActivityFeedItem, DashboardResponse

router = APIRouter(prefix="/households/{household_id}", tags=["Dashboard"])


@router.get("/dashboard", response_model=DashboardResponse)
async def get_dashboard(
    household: Household = Depends(get_current_household),
    db: AsyncSession = Depends(get_db),
):
    """Budget gauge + category snapshots for the current cycle (B1/B3)."""
    return await dashboard_controller.get_dashboard(db, household)


@router.get("/activity-feed", response_model=Page[ActivityFeedItem])
async def get_activity_feed(
    household_id: int,
    limit: int = Query(default=20, ge=1, le=100),
    offset: int = Query(default=0, ge=0),
    _membership: HouseholdMember = Depends(get_household_membership),
    db: AsyncSession = Depends(get_db),
):
    """Reverse-chronological household activity feed (B4). Polled, not pushed — see
    AGENTS.md for the deferred real-time/WebSocket follow-up."""
    return await dashboard_controller.get_activity_feed(db, household_id, limit=limit, offset=offset)
