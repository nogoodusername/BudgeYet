"""add login lockout fields to users

Revision ID: 04b5fb1d758f
Revises: f65692d4add0
Create Date: 2026-07-29 21:05:00.000000

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = '04b5fb1d758f'
down_revision: Union[str, None] = '21f907d9abd6'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    with op.batch_alter_table('users') as batch_op:
        batch_op.add_column(
            sa.Column('failed_login_attempts', sa.Integer(), nullable=False, server_default='0')
        )
        batch_op.add_column(sa.Column('locked_until', sa.DateTime(), nullable=True))


def downgrade() -> None:
    with op.batch_alter_table('users') as batch_op:
        batch_op.drop_column('locked_until')
        batch_op.drop_column('failed_login_attempts')
