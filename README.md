# Trainwise Android

Trainwise is a native Android endurance-training companion built with Kotlin and Jetpack Compose. It is designed around conservative, rule-based training planning, recovery-aware load adjustment, race planning, and workout analysis. It is not a WebView or an HTML wrapper.

## Current direction

The Android app is replacing the earlier browser prototype while preserving the same product rules and user data model.

Planned and migrated capabilities include:

- Today / readiness overview
- Monday-to-Sunday weekly training plan
- Adjustable weekly running-volume percentage
- Configurable minimum run duration
- Long-run progression with a 60-minute minimum for a scheduled Sunday long run
- Recovery check-in
- Strength training as a protected part of the week rather than the first session to be removed
- Race calendar
- Workout analysis with next-day recovery response
- Local persistence of user settings
- Garmin Connect™ Personal API synchronization

## Garmin Connect Personal API

Garmin Connect Personal API integration is planned but not yet implemented in this build.

Authentication flow:

1. The user creates a Personal API key in Garmin Connect Web.
2. Trainwise exchanges the key for a short-lived bearer token using Garmin's official Personal API token endpoint.
3. The Personal API key and bearer token are stored only on the Android device and encrypted using Android Keystore.
4. Bearer tokens are reused until close to expiry instead of requesting a new token on every sync.
5. Trainwise uses the bearer token to pull read-only Garmin data.

The planned sync layer will support:

- Activities by date range
- Daily summaries
- Sleep summaries
- HRV summaries
- Body Battery
- Daily stress
- Individual activity details
- Original activity ZIP/FIT downloads for the workout-analysis pipeline

Garmin data is pull-only. Trainwise does not write workouts or modify Garmin Connect data through the Personal API.

## Privacy and security

This public repository must contain source code and synthetic/demo data only.

It must never contain:

- Garmin Personal API keys
- bearer tokens or OAuth credentials
- Android signing keystores or passwords
- personal Garmin activity/health exports
- personal workout, recovery, or health history

Garmin credentials must be encrypted locally through Android Keystore when integration is implemented. Synced Garmin payloads must stay inside the app's private Android storage and must never be committed to GitHub.

See [SECURITY.md](SECURITY.md) for repository rules.

## GitHub Actions and APK releases

Every push to `main` builds a debug APK as a CI artifact.

Tags matching `v*` (for example `v0.15.0`) are intended for user-facing releases. Release APKs use one persistent Android signing key stored only in GitHub Actions Secrets. Keeping the same signing identity allows a newer Trainwise APK to update an existing installation.

Required GitHub Actions Secrets:

- `TRAINWISE_KEYSTORE_BASE64`
- `TRAINWISE_KEYSTORE_PASSWORD`
- `TRAINWISE_KEY_ALIAS`
- `TRAINWISE_KEY_PASSWORD`

Until these secrets are configured, normal debug builds can still run. Tagged release builds intentionally fail rather than publish an APK with the wrong signing identity.

## Open locally

1. Open the repository in Android Studio.
2. Let Gradle sync finish.
3. Run the app on an Android device or emulator.

## Data architecture

User settings and synced training data belong on the device or in a future protected backend. They are not part of the source tree or Git history.
