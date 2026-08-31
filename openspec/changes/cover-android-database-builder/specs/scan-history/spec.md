# Scan history — delta

## ADDED Requirements

### Requirement: The database outlives the component that created it

Each platform's `DatabaseBuilderFactory` SHALL resolve its context to the long-lived application
context, and SHALL place the database file inside the application's own storage under
`ScanDatabase.FILE_NAME`.

On Android this SHALL be asserted by a test that runs on every pull request without a device.

#### Scenario: The factory is handed a short-lived context

- **WHEN** the factory is constructed with a `ContextWrapper` that is not the application context
- **THEN** the builder is still configured against the application context
- **AND** the database survives the destruction of that component

#### Scenario: The database file is relocated

- **WHEN** a change alters the path the builder resolves
- **THEN** the Android test fails, because relocating the file silently orphans every existing
  user's history

### Requirement: What the bundled driver does on Android is not covered without a device

The system SHALL NOT claim that the Android persistence chain is verified.

The bundled SQLite driver ships native binaries built for Android ABIs and cannot be loaded by a
JVM-hosted test, so no check on any pull request proves that the Android database opens or answers a
query. This gap SHALL remain named in `docs/ROADMAP.md` rather than being closed by a narrower test
that appears to cover it.

#### Scenario: A test attempts to open the database under Robolectric

- **WHEN** a test calls `buildBundled()` in a JVM-hosted Android test
- **THEN** it fails while loading the native driver, not on any assertion about behaviour
- **AND** that failure must not be mistaken for a wiring defect

#### Scenario: The roadmap item is reviewed

- **WHEN** the Android builder's context and path are covered
- **THEN** the roadmap entry is narrowed rather than ticked
- **AND** it continues to state that the driver working on Android needs a device
