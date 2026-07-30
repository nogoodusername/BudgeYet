"""add login_failures table for IP-based throttling

Revision ID: a1b2c3d4e5f6
Revises: 91bd6c67df47
Create Date: 2026-07-30 00:00:00.000000

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = 'a1b2c3d4e5f6'
down_revision: Union[str, None] = '91bd6c67df47'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_table(
        'login_failures',
        sa.Column('id', sa.Integer(), primary_key=True, autoincrement=True),
        sa.Column('ip_address', sa.String(length=45), nullable=False),
        sa.Column('created_at', sa.DateTime(), server_default=sa.func.now(), nullable=False),
    )
    op.create_index('ix_login_failures_ip_address', 'login_failures', ['ip_address'])
    op.create_index('ix_login_failures_created_at', 'login_failures', ['created_at'])


def downgrade() -> None:
    op.drop_index('ix_login_failures_created_at', table_name='login_failures')
    op.drop_index('ix_login_failures_ip_address', table_name='login_failures')
    op.drop_table('login_failures')
