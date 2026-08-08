import logging

import httpx

from app.core.config import settings

logger = logging.getLogger("budge_yet.email")
logger.setLevel(logging.INFO)
if not logger.handlers:
    # Own handler so stub-mode output is visible regardless of the app-wide logging config —
    # this is the only place a PIN/invite token surfaces when RESEND_API_KEY is unset.
    _handler = logging.StreamHandler()
    _handler.setFormatter(logging.Formatter("%(asctime)s %(name)s %(message)s"))
    logger.addHandler(_handler)
    logger.propagate = False

RESEND_API_URL = "https://api.resend.com/emails"


async def _send(*, to: str, subject: str, html: str) -> None:
    async with httpx.AsyncClient() as client:
        response = await client.post(
            RESEND_API_URL,
            headers={"Authorization": f"Bearer {settings.RESEND_API_KEY}"},
            json={
                "from": f"{settings.EMAIL_FROM_NAME} <{settings.EMAIL_FROM_ADDRESS}>",
                "to": [to],
                "subject": subject,
                "html": html,
            },
        )
    if response.is_error:
        # A failed send must not break the signup/forgot-PIN/invite request itself —
        # same "no-op on failure" spirit as AuthService.forgot_pin's unknown-email case.
        logger.error(
            "Resend send failed to=%s status=%s body=%s", to, response.status_code, response.text
        )


async def send_pin_email(email: str, pin: str) -> None:
    if not settings.RESEND_API_KEY:
        logger.info("STUB EMAIL to=%s subject='Your BudgeYet PIN' body='Your PIN is %s'", email, pin)
        return
    await _send(
        to=email,
        subject="Your BudgeYet PIN",
        html=f"<p>Your BudgeYet PIN is <strong>{pin}</strong>.</p>",
    )


async def send_invite_email(email: str, household_name: str, invite_token: str) -> None:
    if not settings.RESEND_API_KEY:
        logger.info(
            "STUB EMAIL to=%s subject='You are invited to join %s on BudgeYet' body='Invite token: %s'",
            email,
            household_name,
            invite_token,
        )
        return
    await _send(
        to=email,
        subject=f"You are invited to join {household_name} on BudgeYet",
        html=(
            f"<p>You've been invited to join <strong>{household_name}</strong> on BudgeYet.</p>"
            f"<p>Invite token: <strong>{invite_token}</strong></p>"
        ),
    )
