"""convert money columns to numeric

Revision ID: f65692d4add0
Revises: 74dd4ff7da5b
Create Date: 2026-07-29 20:31:14.524423

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = 'f65692d4add0'
down_revision: Union[str, None] = 'c97b8f0e262b'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # batch_alter_table so this works on both SQLite (table rebuild) and Postgres
    # (plain ALTER COLUMN) — a bare op.alter_column type change isn't supported on SQLite.
    with op.batch_alter_table('budgets') as batch_op:
        batch_op.alter_column(
            'monthly_goal_amount',
            existing_type=sa.Float(),
            type_=sa.Numeric(precision=12, scale=2),
            existing_nullable=False,
        )
    with op.batch_alter_table('categories') as batch_op:
        batch_op.alter_column(
            'monthly_limit',
            existing_type=sa.Float(),
            type_=sa.Numeric(precision=12, scale=2),
            existing_nullable=False,
        )
    with op.batch_alter_table('transactions') as batch_op:
        batch_op.alter_column(
            'amount',
            existing_type=sa.Float(),
            type_=sa.Numeric(precision=12, scale=2),
            existing_nullable=False,
        )


def downgrade() -> None:
    with op.batch_alter_table('transactions') as batch_op:
        batch_op.alter_column(
            'amount',
            existing_type=sa.Numeric(precision=12, scale=2),
            type_=sa.Float(),
            existing_nullable=False,
        )
    with op.batch_alter_table('categories') as batch_op:
        batch_op.alter_column(
            'monthly_limit',
            existing_type=sa.Numeric(precision=12, scale=2),
            type_=sa.Float(),
            existing_nullable=False,
        )
    with op.batch_alter_table('budgets') as batch_op:
        batch_op.alter_column(
            'monthly_goal_amount',
            existing_type=sa.Numeric(precision=12, scale=2),
            type_=sa.Float(),
            existing_nullable=False,
        )
