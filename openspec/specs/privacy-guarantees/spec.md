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

Not declaring it is **not sufficient**. The Google engine dependencies contribute `INTERNET` through
their own manifests, so the application manifest SHALL remove it explicitly with
`tools:node="remove"`, and `tools/checks.py` SHALL fail if that removal is absent (ADR-0020).

`tools/checks.py` SHALL fail if the **source** manifest declares `INTERNET`, and it SHALL run in CI
before any Gradle task. A `uses-permission` entry carrying `tools:node="remove"` is a removal, not a
declaration, and SHALL NOT be treated as one.

#### Scenario: The permission is added to the application's own manifest

- **WHEN** `android.permission.INTERNET` is added to `androidApp/src/main/AndroidManifest.xml`
- **THEN** `check_privacy_guarantee()` fails
- **AND** the pull request is rejected before the build runs

The **merged** manifest SHALL be checked as well, after `assembleDebug`, by
`tools/merged_manifest.py` in the `android` job.

#### Scenario: A dependency introduces the permission by manifest merge

- **WHEN** a library contributes `android.permission.INTERNET` through its own manifest
- **THEN** the merged-manifest check fails and the pull request is rejected
- **AND** the source manifest stays clean, which is exactly why checking only the source was not
  enough

#### Scenario: The removal is deleted from the application manifest

- **WHEN** the `tools:node="remove"` entry is deleted from `androidApp/src/main/AndroidManifest.xml`
- **THEN** `check_privacy_guarantee()` fails on the source manifest, before any build runs
- **AND** the failure names ADR-0020, because without that line the Google engines put the
  permission back into the APK and nothing in the source would show it

#### Scenario: The merged manifest cannot be found

- **WHEN** the build tool changes where it writes the merged manifest and the glob matches nothing
- **THEN** the check fails rather than passing
- **AND** the failure says to fix the pattern, because a check that silently inspects nothing is
  worse than no check — detekt analysed zero files and passed green for exactly this reason

#### Scenario: A dependency adds a permission that is not INTERNET

- **WHEN** a library contributes a permission that does not break the guarantee
- **THEN** the check prints every permission reaching the APK to the run summary and does not fail
- **AND** the new permission is visible in review, rather than turning a legitimate change red

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

There are three explicit actions: sharing a reading through the system share sheet, **opening a
scanned value in another application**, and exporting the history to a file the user chooses.

#### Scenario: The user shares a reading

- **WHEN** the user taps share on a reading
- **THEN** the value is handed to the system share sheet
- **AND** the destination is chosen by the user, not by the application

#### Scenario: The user opens a scanned value

- **WHEN** the user taps open on a reading whose value is a link
- **THEN** the raw scanned value is handed to the platform, which passes it to a browser or another
  application
- **AND** this is the action that carries scanned content furthest from the application, so it is
  named here rather than treated as navigation

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
