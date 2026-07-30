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
    """Raising this commits the session instead of rolling it back.

    `get_async_db` (core/database.py) special-cases this type so login-failure
    bookkeeping (e.g. the failed-attempt counter in `AuthService.login`) survives
    the very error it triggers. That carve-out is keyed on the exception type, not
    the call site — if you add a new place that raises `AuthenticationError` after
    some unrelated flush, that write will get committed too. Check
    `get_async_db`'s docstring before doing that.
    """

    pass
