import pytest
from jose import JWTError
from starlette.requests import Request

from app.core.security import (
    create_access_token,
    decode_access_token,
    generate_pin,
    get_client_ip,
    hash_pin,
    verify_pin,
)


def _make_request(headers: dict[str, str], client_host: str | None = "10.0.0.1") -> Request:
    scope = {
        "type": "http",
        "headers": [(k.lower().encode(), v.encode()) for k, v in headers.items()],
        "client": (client_host, 12345) if client_host else None,
    }
    return Request(scope)


def test_generate_pin_is_six_digits():
    for _ in range(20):
        pin = generate_pin()
        assert len(pin) == 6
        assert pin.isdigit()


def test_hash_pin_roundtrip():
    pin_hash = hash_pin("123456")
    assert verify_pin("123456", pin_hash) is True
    assert verify_pin("654321", pin_hash) is False


def test_hash_pin_is_salted():
    assert hash_pin("123456") != hash_pin("123456")


def test_access_token_roundtrip():
    token = create_access_token(subject=42)
    assert decode_access_token(token) == 42


def test_decode_rejects_garbage_token():
    with pytest.raises(JWTError):
        decode_access_token("not-a-real-token")


def test_get_client_ip_falls_back_to_socket_when_no_proxy_headers():
    request = _make_request({}, client_host="203.0.113.5")
    assert get_client_ip(request) == "203.0.113.5"


def test_get_client_ip_prefers_cloudflare_header():
    request = _make_request(
        {"CF-Connecting-IP": "198.51.100.7", "X-Forwarded-For": "1.2.3.4, 10.0.0.1"},
        client_host="10.0.0.1",
    )
    assert get_client_ip(request) == "198.51.100.7"


def test_get_client_ip_uses_x_real_ip():
    request = _make_request({"X-Real-IP": "198.51.100.9"}, client_host="10.0.0.1")
    assert get_client_ip(request) == "198.51.100.9"


def test_get_client_ip_uses_last_hop_of_x_forwarded_for():
    # AWS ALB / Azure App Gateway append the real client IP as the last hop;
    # earlier entries may be client-supplied and spoofable.
    request = _make_request(
        {"X-Forwarded-For": "9.9.9.9, 203.0.113.42"}, client_host="10.0.0.1"
    )
    assert get_client_ip(request) == "203.0.113.42"


def test_get_client_ip_returns_unknown_without_client_or_headers():
    request = _make_request({}, client_host=None)
    assert get_client_ip(request) == "unknown"
