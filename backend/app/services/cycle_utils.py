import calendar
from datetime import date, datetime
from typing import NamedTuple

from app.core.constants import BUDGET_OVER_THRESHOLD, BUDGET_WARNING_THRESHOLD


class CycleBounds(NamedTuple):
    start: datetime
    end: datetime  # exclusive
    label_month: int
    label_year: int


def _clamp_day(year: int, month: int, day: int) -> int:
    return min(day, calendar.monthrange(year, month)[1])


def get_current_cycle_bounds(
    cycle_start_day: int, reference: date | None = None
) -> CycleBounds:
    """Compute the [start, end) window and (month, year) label of the budget cycle
    containing `reference` (default: today), given a household's configured
    cycle_start_day (1-31, clamped to the shorter month when needed).
    """
    reference = reference or date.today()

    if reference.day >= cycle_start_day:
        label_month, label_year = reference.month, reference.year
    else:
        label_year = reference.year if reference.month > 1 else reference.year - 1
        label_month = reference.month - 1 if reference.month > 1 else 12

    start_day = _clamp_day(label_year, label_month, cycle_start_day)
    start = datetime(label_year, label_month, start_day)

    if label_month == 12:
        next_month, next_year = 1, label_year + 1
    else:
        next_month, next_year = label_month + 1, label_year
    end_day = _clamp_day(next_year, next_month, cycle_start_day)
    end = datetime(next_year, next_month, end_day)

    return CycleBounds(start=start, end=end, label_month=label_month, label_year=label_year)


def budget_status(percent_used: float) -> str:
    if percent_used >= BUDGET_OVER_THRESHOLD:
        return "over_budget"
    if percent_used >= BUDGET_WARNING_THRESHOLD:
        return "warning"
    return "on_track"
