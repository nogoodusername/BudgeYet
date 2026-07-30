"""merge login-lockout and missing-indexes heads

Revision ID: 91bd6c67df47
Revises: 04b5fb1d758f, 7e4675cad365
Create Date: 2026-07-30 12:38:16.505412

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = '91bd6c67df47'
down_revision: Union[str, None] = ('04b5fb1d758f', '7e4675cad365')
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    pass


def downgrade() -> None:
    pass
