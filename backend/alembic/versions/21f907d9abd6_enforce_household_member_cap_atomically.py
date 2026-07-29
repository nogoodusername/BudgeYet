"""enforce household member cap atomically

Revision ID: 21f907d9abd6
Revises: 6ec7c455842b
Create Date: 2026-07-29 21:18:37.530400

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = '21f907d9abd6'
down_revision: Union[str, None] = '6ec7c455842b'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.add_column(
        'households',
        sa.Column('member_count', sa.Integer(), nullable=False, server_default='0'),
    )
    # Backfill from the actual membership rows — existing households didn't have this
    # counter before, so it can't just default to 0.
    op.execute(
        "UPDATE households SET member_count = ("
        "SELECT COUNT(*) FROM household_members WHERE household_members.household_id = households.id"
        ")"
    )
    # batch_alter_table: SQLite can only add a CHECK constraint via table rebuild.
    # 3 here mirrors app.core.constants.HOUSEHOLD_MEMBER_CAP (migrations stay
    # self-contained rather than importing application code).
    with op.batch_alter_table('households') as batch_op:
        batch_op.create_check_constraint(
            'ck_households_member_count_within_cap',
            'member_count >= 0 AND member_count <= 3',
        )


def downgrade() -> None:
    with op.batch_alter_table('households') as batch_op:
        batch_op.drop_constraint('ck_households_member_count_within_cap', type_='check')
        batch_op.drop_column('member_count')
