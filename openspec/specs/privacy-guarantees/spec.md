# Privacy guarantees

## Purpose

The application promises that what it scans never leaves the device. That promise is made in the
README, on the Settings → About screen, and in the published privacy policy, so it is a
**specification**, not an intention — and the parts of it that can be checked mechanically are
checked on every pull request.

Design: `docs/SDD.md` §12, `docs/legal/privacidad.md`, `docs/legal/privacy.md`, RNF-03.

## Requirements

### Requirement: The application has no network capability

The Android manifest SHALL NOT declare the `INTERNET` permission. The application SHALL NOT contain
an HTTP client, analytics, crash reporting or telemetry of any kind.

`tools/checks.py` SHALL fail if the manifest declares `INTERNET`, and it SHALL run in CI before any
Gradle task.

#### Scenario: A dependency introduces the permission by manifest merge

- **WHEN** a dependency contributes `android.permission.INTERNET` through manifest merging
- **THEN** the check over the merged application manifest fails
- **AND** the pull request is rejected before the build runs

### Requirement: The system does not back up application data

The Android manifest SHALL declare `allowBackup="false"` **and** a `dataExtractionRules` resource.

Both are required: since Android 12 the attribute governs cloud backup only, and device-to-device
transfer is configured separately.

`tools/checks.py` SHALL fail if either is missing.

#### Scenario: Backup would carry scanned values off the device

- **WHEN** Auto Backup is enabled for the application
- **THEN** `databases/` and `shared_prefs/` are copied to the user's cloud account by a system
  process that does not need the `INTERNET` permission the application withholds
- **AND** the raw value of every scanned code, including the password inside a Wi-Fi QR, leaves the
  device
- **THEREFORE** both settings are mandatory, and the guarantee is checked at the manifest rather
  than in application code

### Requirement: Scanned values stay on the device

No scanned value SHALL be transmitted, uploaded, or written anywhere outside application-private
storage, except where the user explicitly acts.

The two explicit actions are: sharing a reading through the system share sheet, and exporting the
history to a file the user chooses.

#### Scenario: The user shares a reading

- **WHEN** the user taps share on a reading
- **THEN** the value is handed to the system share sheet
- **AND** the destination is chosen by the user, not by the application

### Requirement: The one third-party component is named

Where a scan is performed by a system component belonging to a third party — the Google Code
Scanner on Android — the privacy policy SHALL say so explicitly, in both languages.

#### Scenario: The policy is read against the code

- **WHEN** `docs/legal/privacidad.md` and `docs/legal/privacy.md` are compared against the engine
  catalog
- **THEN** every engine that hands frames to a component outside the application is disclosed
- **AND** both language versions disclose the same set

### Requirement: The guarantee is reachable from inside the application

Settings → About SHALL link to the privacy policy and the terms of use, in the application's
language.

#### Scenario: A user checks the claim

- **WHEN** the user opens Settings → About
- **THEN** the privacy policy and terms are reachable
- **AND** the text shown matches the language selected in the application, not the system's
