import pytest
from jose import JWTError

from app.core.security import (
    create_access_token,
    decode_access_token,
    generate_pin,
    hash_pin,
    verify_pin,
)


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
