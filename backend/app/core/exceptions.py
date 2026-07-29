class AppError(Exception):
    """Base class for domain-level errors raised by services/repositories.

    Kept HTTP-agnostic on purpose — the FastAPI exception handlers registered in
    main.py translate these into responses, so services/repositories never import
    fastapi.HTTPException.
    """

    def __init__(self, message: str):
        self.message = message
        super().__init__(message)


class NotFoundError(AppError):
    pass


class PermissionDeniedError(AppError):
    pass


class ConflictError(AppError):
    pass


class ValidationAppError(AppError):
    pass


class AuthenticationError(AppError):
    pass
