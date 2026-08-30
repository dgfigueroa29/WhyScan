# Scan history — delta

## ADDED Requirements

### Requirement: Every shipping platform's database builder is exercised on every pull request

For each platform the application ships on, a test that runs in CI without a device SHALL build the
database through that platform's `DatabaseBuilderFactory` and perform a real read and write through
it.

Asserting that the builder returns an instance SHALL NOT be sufficient: the assertion must exercise
the configured driver.

Where a platform's builder cannot be covered without hardware, the gap SHALL be recorded in
`docs/ROADMAP.md` rather than left unstated.

#### Scenario: The Android builder loses its bundled driver

- **WHEN** a change causes the Android builder to stop configuring the bundled SQLite driver
- **THEN** the Android database test fails, because its read-back query no longer runs through that
  driver
- **AND** the failure appears on the pull request rather than on a user's device

#### Scenario: A structural-only assertion would pass

- **WHEN** the driver is silently lost but the builder still returns a database instance
- **THEN** the test still fails, because it asserts on data read back rather than on the instance

#### Scenario: The iOS builder remains uncovered

- **WHEN** the iOS `actual` has no test, because the platform is deprioritized and no device exists
- **THEN** `docs/ROADMAP.md` names that gap explicitly
- **AND** no test is added that would only prove Kotlin/Native compiles

### Requirement: The database outlives the component that created it

The Android `DatabaseBuilderFactory` SHALL resolve its context to the application context, and SHALL
place the database file inside the application's own database directory under
`ScanDatabase.FILE_NAME`.

#### Scenario: The factory is handed an Activity context

- **WHEN** the factory is constructed with a context that is not the application context
- **THEN** the database is still built against the application context
- **AND** the database survives the destruction of that component
