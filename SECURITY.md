# Security and privacy

Trainwise is intentionally structured so the public repository contains source code only.

## Never commit

- Android signing keystores (`.jks`, `.keystore`, `.p12`, `.pfx`)
- keystore passwords or aliases
- OAuth client secrets, access tokens, refresh tokens, API keys
- Garmin/health exports (`.fit`, `.tcx`, `.gpx`, exported JSON/CSV)
- personal workout, recovery or health data
- `local.properties`, `.env`, `secrets.properties`, `google-services.json`

These paths and file types are covered by `.gitignore`, but `.gitignore` is not a substitute for reviewing changes before committing.

## Android release signing

Release signing is configured only from environment variables supplied by GitHub Actions Secrets:

- `TRAINWISE_KEYSTORE_BASE64`
- `TRAINWISE_KEYSTORE_PASSWORD`
- `TRAINWISE_KEY_ALIAS`
- `TRAINWISE_KEY_PASSWORD`

The workflow decodes the keystore into the runner's temporary directory. The keystore file is never committed to the repository.

## Personal data

Repository source must contain only synthetic/demo fitness data. Real Garmin data belongs on the user's device or a future protected backend and must not be committed.

If a secret is ever committed, treat it as compromised: revoke/rotate it first, then remove it from Git history.
