from sqlalchemy.ext.asyncio import AsyncSession

from app.models.household import Household
from app.schemas.common import Page
from app.schemas.dashboard import ActivityFeedItem, DashboardResponse
from app.services.dashboard_service import DashboardService


async def get_dashboard(db: AsyncSession, household: Household) -> DashboardResponse:
    return await DashboardService(db).get_dashboard(household)


def _to_activity_item(transaction) -> ActivityFeedItem:
    return ActivityFeedItem(
        id=transaction.id,
        type=transaction.type,
        amount=transaction.amount,
        merchant=transaction.merchant,
        category_name=transaction.category.name if transaction.category else None,
        user=transaction.created_by_user,
        transaction_date=transaction.transaction_date,
        created_at=transaction.created_at,
    )


async def get_activity_feed(
    db: AsyncSession, household_id: int, *, limit: int, offset: int
) -> Page[ActivityFeedItem]:
    items, total = await DashboardService(db).get_activity_feed(
        household_id, limit=limit, offset=offset
    )
    return Page[ActivityFeedItem](
        items=[_to_activity_item(t) for t in items],
        total=total,
        limit=limit,
        offset=offset,
    )
