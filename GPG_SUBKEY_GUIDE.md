Creating a dedicated GPG signing subkey (recommended)

Author: WorkerRobotics <ricky.van.rijn@worker-robotics.com>

It is recommended to create a dedicated signing subkey for CI use instead of exposing your primary key. This guide shows how to create a signing subkey, export only that secret subkey (so the primary secret key material is not leaked), and prepare it for safe storage (base64) for CI.

1) Create a new primary key (if you don't already have one)

# interactive recommended for setting user details
gpg --full-generate-key

# or quick (non-interactive) example for an RSA 3072 key
gpg --batch --quick-generate-key "WorkerRobotics <ricky.van.rijn@worker-robotics.com>" rsa3072 sign 0

2) Add a signing subkey

# Use --edit-key to add a new subkey (signing)
gpg --edit-key YOUR_PRIMARY_KEY_ID

# inside gpg prompt:
#   Command: addkey
#   Choose (1) RSA (sign only) or rsa3072
#   Set expiry if desired
#   save

# Example sequence (manual):
# gpg> addkey
# (choose RSA sign only, 3072)
# gpg> save

3) Export only the secret subkeys (recommended for CI)

# This exports secret subkeys but removes primary secret key material from the exported file.
# Replace <KEYID> with your primary key id (the export will include the subkeys only).
gpg --armor --export-secret-subkeys <KEYID> > subkeys.asc

# Inspect subkeys.asc to confirm it only contains subkey material and not the primary secret key.

4) Base64-encode the exported subkeys for storing in CI secrets

base64 subkeys.asc > subkeys.asc.b64

# Now copy the contents of subkeys.asc.b64 into your CI secret (e.g., GPG_PRIVATE_KEY_B64)

5) Importing the secret subkey in CI or another machine

# On CI (GitHub Actions), decode and import:
echo "$GPG_PRIVATE_KEY_B64" | base64 --decode > /tmp/subkeys.asc
gpg --batch --import /tmp/subkeys.asc
rm -f /tmp/subkeys.asc

# Configure loopback pinentry so the passphrase can be supplied programmatically
mkdir -p ~/.gnupg
echo "allow-loopback-pinentry" >> ~/.gnupg/gpg-agent.conf
gpgconf --kill gpg-agent || true

# Provide the passphrase to Maven during build:
# mvn -Dgpg.passphrase="$GPG_PASSPHRASE" -Dgpg.keyname="$GPG_KEY_ID" deploy

Security best practices
- Use a subkey dedicated to CI and revoke it if compromised.
- Prefer storing the base64 secret in the CI provider's secret manager, not in plain repo files or .env committed to source control.
- Limit who can edit/deploy workflows that access the secret.
- Rotate keys periodically.
