from fastapi import APIRouter
from app.api.v1.endpoints import (
    auth,
    budgets,
    categories,
    dashboard,
    health,
    households,
    transactions,
    users,
)

api_router = APIRouter()
api_router.include_router(health.router, tags=["Health & Status"])
api_router.include_router(auth.router)
api_router.include_router(users.router)
api_router.include_router(households.router)
api_router.include_router(budgets.router)
api_router.include_router(categories.router)
api_router.include_router(transactions.router)
api_router.include_router(dashboard.router)
