# Scan history

## Purpose

Every reading is kept on the device so the user can find it again, annotate it, act on it and take
it elsewhere. History is the only user data this application holds, which is why its persistence,
its pruning and its export are specified rather than left to implementation.

Design: `docs/SDD.md` §11, `docs/adr/ADR-0012-la-nota-es-del-historial-no-de-la-deteccion.md`,
RF-11.

## Requirements

### Requirement: History survives the process and schema changes

The history SHALL persist across process death and application updates, on all four platforms —
Room on Android, iOS and Desktop; JSON in browser storage on Web.

Schema changes SHALL be migrated, not dropped. A test SHALL open a database written by the previous
schema version and assert that its rows are still present.

#### Scenario: The schema gains a column

- **WHEN** the entity gains a field and the schema version is raised
- **THEN** an `@AutoMigration` carries existing rows forward
- **AND** the migration test opens a v1 database with data and finds that data intact

### Requirement: A note belongs to the history entry, not to the detection

A note SHALL be attached to a stored history entry, and SHALL be writable both from the history
screen and from the scanner immediately after a reading.

A note SHALL NOT be a property of the detection event, so that annotating never alters what an
engine reported.

#### Scenario: Annotating from the scanner

- **WHEN** the user writes a note on a reading that has just appeared in the scanner
- **THEN** the note is stored against that history entry
- **AND** the same note is visible from the history screen

#### Scenario: Annotating an entry that no longer exists

- **WHEN** the user submits a note for an entry deleted or pruned while the field was open
- **THEN** the operation reports that the entry is gone rather than recreating it

### Requirement: Search covers value and note

The history search SHALL match against both the stored value and the note.

#### Scenario: Finding a reading by what the user wrote

- **WHEN** the user searches for a word that appears only in a note
- **THEN** the entry carrying that note is returned

### Requirement: Pruning never removes an annotated entry

When the history exceeds its maximum size, the system SHALL remove the oldest entries **that have no
note**, and SHALL keep every annotated entry regardless of age.

This rule SHALL hold identically in all three implementations: the two stores in `:core:data` and the
`WHERE note IS NULL` clause in Room.

#### Scenario: A long continuous session with one annotated reading

- **WHEN** a continuous session pushes the history past its maximum, and one older entry has a note
- **THEN** unannotated entries are removed oldest-first until the size fits
- **AND** the annotated entry remains

### Requirement: Deletion is reversible for a single entry and confirmed in bulk

Deleting one entry SHALL be undoable.

Deleting the whole history SHALL require a confirmation that states how many readings will be lost.

#### Scenario: Undoing a single deletion

- **WHEN** the user deletes one reading and chooses undo
- **THEN** the reading is restored with its note intact

#### Scenario: Clearing everything

- **WHEN** the user asks to clear the history
- **THEN** a confirmation naming the exact number of readings is shown before anything is removed

### Requirement: History is grouped by day

The history SHALL be presented grouped by day, with headers that read "Today" and "Yesterday" for
those two days and a date otherwise.

#### Scenario: Readings from three different days

- **WHEN** the history holds readings from today, yesterday and an earlier date
- **THEN** three groups are rendered, headed "Today", "Yesterday" and that date

### Requirement: Export produces three formats with stable field names

The history SHALL be exportable as CSV, JSON and plain text, and the resulting file SHALL be
saveable on all four platforms.

CSV column names and JSON keys SHALL be stable `snake_case` identifiers in English, independent of
the application's display language. New columns SHALL be appended last.

#### Scenario: The application language changes

- **WHEN** the user switches the application to Spanish and exports to CSV
- **THEN** the column names are unchanged
- **AND** a script written against a previous export keeps working

#### Scenario: Plain text export

- **WHEN** the user exports as plain text
- **THEN** the file contains one reading per line, with no header and no quoting, so it can be
  pasted into a message or a spreadsheet cell
