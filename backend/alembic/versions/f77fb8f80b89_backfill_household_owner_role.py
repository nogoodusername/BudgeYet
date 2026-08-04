"""backfill household owner role

Revision ID: f77fb8f80b89
Revises: a1b2c3d4e5f6
Create Date: 2026-08-03 00:00:00.000000

"""
from typing import Sequence, Union

from alembic import op


# revision identifiers, used by Alembic.
revision: str = 'f77fb8f80b89'
down_revision: Union[str, None] = 'a1b2c3d4e5f6'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # MemberRole gained a third value, 'owner' (single-holder per household, see
    # app.models.household.MemberRole). No column change is needed — role is a
    # plain String(20), not a native enum — but existing rows predate the Owner
    # concept, so every household needs exactly one. Backfill picks the
    # longest-standing Admin (lowest id) per household as its Owner; anyone can
    # transfer it onward afterwards via update_member_role.
    op.execute(
        "UPDATE household_members SET role = 'owner' WHERE id IN ("
        "SELECT MIN(id) FROM household_members WHERE role = 'admin' GROUP BY household_id"
        ")"
    )


def downgrade() -> None:
    op.execute("UPDATE household_members SET role = 'admin' WHERE role = 'owner'")
