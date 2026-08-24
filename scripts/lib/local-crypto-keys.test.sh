#!/usr/bin/env bash
# =============================================================================
# scripts/lib/local-crypto-keys.test.sh — lease-demo
#
# Lightweight, dependency-free shell tests for scripts/lib/local-crypto-keys.sh.
# Run manually:
#   bash scripts/lib/local-crypto-keys.test.sh
#
# Each test runs in an isolated temporary "repo root" directory so it never
# touches the real .env.local. No key material is printed.
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
HELPER="$SCRIPT_DIR/local-crypto-keys.sh"

PASS=0
FAIL=0

fail() {
    echo "FAIL: $1" >&2
    FAIL=$((FAIL + 1))
}

pass() {
    echo "PASS: $1"
    PASS=$((PASS + 1))
}

fp() {
    # Safe fingerprint only — never the raw key.
    printf '%s' "$1" | sha256sum | cut -d' ' -f1
}

new_tmp_root() {
    mktemp -d
}

# ---------------------------------------------------------------------------
# 1. First-run generation
# ---------------------------------------------------------------------------
t1_root="$(new_tmp_root)"
(
    source "$HELPER"
    if load_local_crypto_keys "$t1_root" >/tmp/out1.$$ 2>&1; then
        if [ -f "$t1_root/.env.local" ] && [ -n "${CRYPTO_AES_KEY:-}" ] && [ -n "${CRYPTO_HMAC_KEY:-}" ]; then
            exit 0
        fi
    fi
    exit 1
) && pass "first-run generation creates .env.local and exports keys" || fail "first-run generation"
rm -f /tmp/out1.$$

# ---------------------------------------------------------------------------
# 2. Subsequent-run reuse (no regeneration, same fingerprints)
# ---------------------------------------------------------------------------
t2_root="$(new_tmp_root)"
(
    source "$HELPER"
    load_local_crypto_keys "$t2_root" >/dev/null 2>&1
    fp1_aes="$(fp "$CRYPTO_AES_KEY")"
    fp1_hmac="$(fp "$CRYPTO_HMAC_KEY")"
    unset CRYPTO_AES_KEY CRYPTO_HMAC_KEY
    load_local_crypto_keys "$t2_root" >/dev/null 2>&1
    fp2_aes="$(fp "$CRYPTO_AES_KEY")"
    fp2_hmac="$(fp "$CRYPTO_HMAC_KEY")"
    [ "$fp1_aes" = "$fp2_aes" ] && [ "$fp1_hmac" = "$fp2_hmac" ]
) && pass "subsequent-run reuses persisted keys (fingerprints match)" || fail "subsequent-run reuse"

# ---------------------------------------------------------------------------
# 3. Missing AES entry -> fail
# ---------------------------------------------------------------------------
t3_root="$(new_tmp_root)"
printf 'CRYPTO_HMAC_KEY=%s\n' "$(openssl rand -base64 32)" > "$t3_root/.env.local"
(
    source "$HELPER"
    ! load_local_crypto_keys "$t3_root" >/dev/null 2>&1
) && pass "missing CRYPTO_AES_KEY fails fast" || fail "missing CRYPTO_AES_KEY should fail"

# ---------------------------------------------------------------------------
# 4. Missing HMAC entry -> fail
# ---------------------------------------------------------------------------
t4_root="$(new_tmp_root)"
printf 'CRYPTO_AES_KEY=%s\n' "$(openssl rand -base64 32)" > "$t4_root/.env.local"
(
    source "$HELPER"
    ! load_local_crypto_keys "$t4_root" >/dev/null 2>&1
) && pass "missing CRYPTO_HMAC_KEY fails fast" || fail "missing CRYPTO_HMAC_KEY should fail"

# ---------------------------------------------------------------------------
# 5. Invalid Base64 -> fail
# ---------------------------------------------------------------------------
t5_root="$(new_tmp_root)"
{
    echo "CRYPTO_AES_KEY=not-valid-base64!!!"
    echo "CRYPTO_HMAC_KEY=$(openssl rand -base64 32)"
} > "$t5_root/.env.local"
(
    source "$HELPER"
    ! load_local_crypto_keys "$t5_root" >/dev/null 2>&1
) && pass "invalid Base64 fails fast" || fail "invalid Base64 should fail"

# ---------------------------------------------------------------------------
# 6. Invalid AES key length -> fail (16 bytes instead of 32)
# ---------------------------------------------------------------------------
t6_root="$(new_tmp_root)"
{
    echo "CRYPTO_AES_KEY=$(openssl rand -base64 16)"
    echo "CRYPTO_HMAC_KEY=$(openssl rand -base64 32)"
} > "$t6_root/.env.local"
(
    source "$HELPER"
    ! load_local_crypto_keys "$t6_root" >/dev/null 2>&1
) && pass "short AES key length fails fast" || fail "short AES key should fail"

# ---------------------------------------------------------------------------
# 7. No key values printed on first-run generation
# ---------------------------------------------------------------------------
t7_root="$(new_tmp_root)"
(
    source "$HELPER"
    load_local_crypto_keys "$t7_root"
) > /tmp/out7.$$ 2>&1
generated_aes_val="$(grep '^CRYPTO_AES_KEY=' "$t7_root/.env.local" | cut -d= -f2-)"
if grep -qF "$generated_aes_val" /tmp/out7.$$; then
    fail "key value leaked into stdout/stderr"
else
    pass "no key values printed to stdout/stderr"
fi
rm -f /tmp/out7.$$

# ---------------------------------------------------------------------------
# Summary
# ---------------------------------------------------------------------------
echo
echo "PASS=$PASS FAIL=$FAIL"
if [ "$FAIL" -ne 0 ]; then
    exit 1
fi
