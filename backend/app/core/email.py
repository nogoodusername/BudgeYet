import logging

logger = logging.getLogger("budge_yet.email")
logger.setLevel(logging.INFO)
if not logger.handlers:
    # Own handler so the stub is visible regardless of the app-wide logging config —
    # this is the only place a PIN/invite token surfaces until real delivery exists.
    _handler = logging.StreamHandler()
    _handler.setFormatter(logging.Formatter("%(asctime)s %(name)s %(message)s"))
    logger.addHandler(_handler)
    logger.propagate = False

# TODO(budge-yet): This is a stub — it logs instead of actually sending email.
# Real delivery (SMTP/SES/etc.) still needs to be implemented before signup/invite
# flows work outside of local development. See AGENTS.md "Known gaps".


def send_pin_email(email: str, pin: str) -> None:
    logger.info("STUB EMAIL to=%s subject='Your budge-yet PIN' body='Your PIN is %s'", email, pin)


def send_invite_email(email: str, household_name: str, invite_token: str) -> None:
    logger.info(
        "STUB EMAIL to=%s subject='You are invited to join %s on budge-yet' body='Invite token: %s'",
        email,
        household_name,
        invite_token,
    )
