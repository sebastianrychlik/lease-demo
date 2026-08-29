#!/usr/bin/env bash
# =============================================================================
# scripts/lib/local-crypto-keys.sh — lease-demo
#
# LOCAL-DEVELOPMENT-ONLY helper that provides persistent CRYPTO_AES_KEY /
# CRYPTO_HMAC_KEY values shared by scripts/deploy-local.sh and
# scripts/postgres-seed-local-data.sh.
#
# Rationale (M4.1.3 correction):
# The local PostgreSQL database is persistent across backend restarts.
# Customer.pesel_encrypted (AES-256-GCM) and Customer.pesel_lookup
# (HMAC-SHA-256) are derived from CRYPTO_AES_KEY/CRYPTO_HMAC_KEY. If these
# keys were regenerated on every script invocation (as they previously
# were), existing encrypted/hashed data would become undecryptable and
# PESEL duplicate-detection would silently break. This helper generates the
# keys exactly ONCE and persists them in a gitignored root-level file,
# ".env.local", so that every local process (backend, seeder) uses the
# same key material until a developer deliberately resets it.
#
# This mechanism is STRICTLY local development tooling. Production secrets
# must continue to come from a managed secret store / deployment
# infrastructure (e.g. GCP Secret Manager, KMS) — that is explicitly out of
# scope here and is not implemented by this helper.
#
# Usage (source, do not execute):
#   source "$ROOT_DIR/scripts/lib/local-crypto-keys.sh"
#   load_local_crypto_keys "$ROOT_DIR"
#
# After a successful call, CRYPTO_AES_KEY and CRYPTO_HMAC_KEY are exported
# into the calling shell. Key values are NEVER printed/logged by this
# helper.
# =============================================================================

# Resolve the repository root robustly regardless of caller cwd.
_local_crypto_keys_repo_root() {
    local script_dir
    script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
    (cd "$script_dir/../.." && pwd)
}

# Validate that $1 is well-formed Base64 and decodes to at least $2 bytes
# (and, if $3 = "exact", exactly $2 bytes). Prints nothing sensitive.
_local_crypto_keys_validate() {
    local value="$1"
    local min_len="$2"
    local mode="${3:-min}"
    local label="$4"
    local decoded_len

    if [ -z "$value" ]; then
        echo "ERROR: $label is missing or empty in .env.local" >&2
        return 1
    fi

    if ! decoded_len="$(printf '%s' "$value" | base64 -d 2>/dev/null | wc -c | tr -d ' ')"; then
        echo "ERROR: $label is not valid Base64 in .env.local" >&2
        return 1
    fi

    if [ -z "$decoded_len" ]; then
        echo "ERROR: $label is not valid Base64 in .env.local" >&2
        return 1
    fi

    if [ "$mode" = "exact" ]; then
        if [ "$decoded_len" -ne "$min_len" ]; then
            echo "ERROR: $label must decode to exactly $min_len bytes, but was $decoded_len bytes" >&2
            return 1
        fi
    else
        if [ "$decoded_len" -lt "$min_len" ]; then
            echo "ERROR: $label must decode to at least $min_len bytes, but was $decoded_len bytes" >&2
            return 1
        fi
    fi

    return 0
}

# load_local_crypto_keys [repo_root]
#
# Ensures $repo_root/.env.local exists (generating it on first run only),
# loads CRYPTO_AES_KEY/CRYPTO_HMAC_KEY from it, validates them, and exports
# them into the calling shell. Fails fast (non-zero return / exit under
# `set -e`) on any malformed existing secret file WITHOUT regenerating it.
load_local_crypto_keys() {
    local repo_root="${1:-}"
    if [ -z "$repo_root" ]; then
        repo_root="$(_local_crypto_keys_repo_root)"
    fi

    local env_file="$repo_root/.env.local"

    if [ ! -f "$env_file" ]; then
        command -v openssl >/dev/null 2>&1 || {
            echo "ERROR: openssl is required to generate local crypto keys but was not found on PATH." >&2
            return 1
        }

        local generated_aes generated_hmac
        generated_aes="$(openssl rand -base64 32)"
        generated_hmac="$(openssl rand -base64 32)"

        {
            echo "# LOCAL DEVELOPMENT SECRET MATERIAL — DO NOT COMMIT."
            echo "# Generated once by scripts/lib/local-crypto-keys.sh."
            echo "# Deleting this file will make existing encrypted/hashed local"
            echo "# Customer data (pesel_encrypted / pesel_lookup) unreadable."
            echo "CRYPTO_AES_KEY=$generated_aes"
            echo "CRYPTO_HMAC_KEY=$generated_hmac"
        } > "$env_file"

        # Best-effort restrict permissions; ignore failures on platforms
        # (e.g. some Windows shells) where chmod has no effect.
        chmod 600 "$env_file" 2>/dev/null || true

        unset generated_aes generated_hmac

        echo "Created persistent local crypto keys in .env.local"
    fi

    # Parse only the two expected KEY=VALUE lines (no blind sourcing/eval).
    local loaded_aes="" loaded_hmac=""
    local line key value
    while IFS= read -r line || [ -n "$line" ]; do
        case "$line" in
            \#*|"") continue ;;
        esac
        key="${line%%=*}"
        value="${line#*=}"
        case "$key" in
            CRYPTO_AES_KEY) loaded_aes="$value" ;;
            CRYPTO_HMAC_KEY) loaded_hmac="$value" ;;
        esac
    done < "$env_file"

    if [ -z "$loaded_aes" ] || [ -z "$loaded_hmac" ]; then
        echo "ERROR: .env.local exists but is missing CRYPTO_AES_KEY and/or CRYPTO_HMAC_KEY." >&2
        echo "       Refusing to regenerate keys automatically, as this would make" >&2
        echo "       existing encrypted local Customer data unreadable." >&2
        echo "       Fix or recover $env_file manually (e.g. restore from backup," >&2
        echo "       or delete it ONLY if you accept losing access to existing" >&2
        echo "       local pesel_encrypted/pesel_lookup data)." >&2
        return 1
    fi

    _local_crypto_keys_validate "$loaded_aes" 32 "exact" "CRYPTO_AES_KEY" || return 1
    _local_crypto_keys_validate "$loaded_hmac" 32 "min" "CRYPTO_HMAC_KEY" || return 1

    export CRYPTO_AES_KEY="$loaded_aes"
    export CRYPTO_HMAC_KEY="$loaded_hmac"

    unset loaded_aes loaded_hmac

    return 0
}
