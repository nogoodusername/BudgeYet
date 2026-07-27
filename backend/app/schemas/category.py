from pydantic import BaseModel, ConfigDict

class CategoryBase(BaseModel):
    name: str
    icon: str = "default"
    monthly_limit: float = 0.0

class CategoryCreate(CategoryBase):
    household_id: int

class CategoryResponse(CategoryBase):
    id: int
    household_id: int

    model_config = ConfigDict(from_attributes=True)
