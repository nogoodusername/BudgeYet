from datetime import date, datetime

from app.services.cycle_utils import budget_status, get_current_cycle_bounds


def test_cycle_bounds_when_reference_after_start_day():
    bounds = get_current_cycle_bounds(cycle_start_day=1, reference=date(2026, 7, 15))
    assert (bounds.label_month, bounds.label_year) == (7, 2026)
    assert bounds.start == datetime(2026, 7, 1)
    assert bounds.end == datetime(2026, 8, 1)


def test_cycle_bounds_when_reference_before_start_day_rolls_back_a_month():
    # cycle starts on the 20th; the 5th of August belongs to the cycle that
    # started July 20th, not the August 20th cycle.
    bounds = get_current_cycle_bounds(cycle_start_day=20, reference=date(2026, 8, 5))
    assert (bounds.label_month, bounds.label_year) == (7, 2026)


def test_cycle_bounds_rolls_back_across_year_boundary():
    bounds = get_current_cycle_bounds(cycle_start_day=15, reference=date(2026, 1, 5))
    assert (bounds.label_month, bounds.label_year) == (12, 2025)


def test_cycle_bounds_clamps_start_day_to_shorter_month():
    # March 5th (day < 31) belongs to the cycle that "started" Jan 31st's cycle
    # boundary the month before — February — whose start day clamps to the 28th
    # since cycle_start_day=31 doesn't exist in a non-leap February.
    bounds = get_current_cycle_bounds(cycle_start_day=31, reference=date(2026, 3, 5))
    assert (bounds.label_month, bounds.label_year) == (2, 2026)
    assert bounds.start.day == 28  # 2026 is not a leap year


def test_budget_status_thresholds():
    assert budget_status(0) == "on_track"
    assert budget_status(74.9) == "on_track"
    assert budget_status(75) == "warning"
    assert budget_status(99.9) == "warning"
    assert budget_status(100) == "over_budget"
    assert budget_status(150) == "over_budget"
