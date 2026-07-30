"""merge heads

Revision ID: 448438686134
Revises: 21f907d9abd6, 90677eacb890
Create Date: 2026-07-30 11:02:22.413014

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = '448438686134'
down_revision: Union[str, None] = ('21f907d9abd6', '90677eacb890')
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    pass


def downgrade() -> None:
    pass
