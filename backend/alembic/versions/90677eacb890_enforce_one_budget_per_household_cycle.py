"""enforce one budget per household cycle

Revision ID: 90677eacb890
Revises: 6ec7c455842b
Create Date: 2026-07-29 21:35:20.574649

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = '90677eacb890'
down_revision: Union[str, None] = '6ec7c455842b'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # batch_alter_table: SQLite can only add a table-level constraint via table rebuild.
    with op.batch_alter_table('budgets') as batch_op:
        batch_op.create_unique_constraint(
            'uq_budgets_household_cycle', ['household_id', 'month', 'year']
        )


def downgrade() -> None:
    with op.batch_alter_table('budgets') as batch_op:
        batch_op.drop_constraint('uq_budgets_household_cycle', type_='unique')
