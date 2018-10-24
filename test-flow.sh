#!/bin/bash
set -e
set -x

USERNAME="${1:-cafe}"
HOST="${2:-localhost:9000}"
http --check-status POST "$HOST/api/signup" username="$USERNAME"
http --check-status GET "$HOST/make-keybase-proof" keybase-username==t_alice sig-hash==8514ae2f9083a3c867318437845855f702a4154d1671a19cf274fb2e6b7dec7c0f
http --check-status POST "$HOST/api/prove-keybase-identity" username="$USERNAME" keybase-username=t_alice sig-hash=8514ae2f9083a3c867318437845855f702a4154d1671a19cf274fb2e6b7dec7c0f
http --check-status GET "$HOST/api/keybase-proofs" username=="$USERNAME"
http --check-status GET "$HOST/api/user" username=="$USERNAME"
