# Scanner engine SPI

## Purpose

Every scanning alternative — nine of them today, across four platforms — is reachable through one
interface, so that the domain and the user interface branch on **declared capabilities** and never
on an engine's identity. Adding an engine adds no conditional anywhere.

Design: `docs/adr/ADR-0002-scanner-engine-spi.md`, `docs/adr/ADR-0004-flow-como-api-de-sesion.md`,
`docs/adr/ADR-0007-preview-como-capacidad-del-motor.md`, `docs/SDD.md` §7.

## Requirements

### Requirement: Every engine implements one minimal interface

Every scanning alternative SHALL implement `BarcodeScannerEngine`, exposing an `id`, a `descriptor`,
a suspending `availability()` and a cold `scan(request): Flow<ScanEvent>`.

The interface SHALL NOT contain any operation that some engine cannot honour. Capabilities that not
every engine has SHALL live in segregated interfaces.

#### Scenario: An engine that opens its own UI

- **WHEN** `GMS_CODE_SCANNER`, which opens a system screen and never exposes the camera, is used
- **THEN** it implements `BarcodeScannerEngine` in full
- **AND** it does not implement `CameraControlEngine`
- **AND** no call path requires it to throw `UnsupportedOperationException`

### Requirement: A scan session obeys four guarantees

Every implementation SHALL guarantee, for the `Flow` returned by `scan()`:

1. The first emitted event is `ScanEvent.SessionStarted`.
2. If the session ends by itself, the last emitted event is `ScanEvent.SessionEnded`.
3. Cancelling the collecting coroutine releases the camera.
4. Every format reported in `ScanEvent.Detected` is contained in
   `descriptor.capabilities.supportedFormats`.

`BarcodeScannerEngineContractTest` SHALL be inherited by every engine that can be instantiated
without a device — today `MANUAL_INPUT` and `ZXING_JAVA` — and by the domain decorators and the
full chain.

Camera engines SHALL NOT inherit it: constructing them requires an emulator, and a test that never
runs is worse than no test (debt D6). Their guarantees are covered by their declared capabilities
and by the decorators that wrap them.

Guarantee 3 SHALL be reviewed by reading. The suite asserts that cancelling the session raises
nothing; that the camera is actually released cannot be observed without hardware.

#### Scenario: The consumer cancels the session

- **WHEN** the coroutine collecting `scan()` is cancelled
- **THEN** the flow terminates without emitting further events, per coroutine semantics
- **AND** no `SessionEnded` event is required, because the consumer ended it
- **AND** the release of the camera is a review obligation, not an assertion

#### Scenario: An engine reports an undeclared format

- **WHEN** an engine emits `ScanEvent.Detected` carrying a format absent from its declared
  `supportedFormats`
- **THEN** `BarcodeScannerEngineContractTest` fails for that engine

### Requirement: Capabilities are declared as data

Each engine SHALL declare a `ScannerCapabilities` value describing supported formats, sources,
multiple codes, continuous scanning, own UI, torch, zoom, corner points, confidence, camera
permission, network need and runtime model download.

The user interface and the selection policy SHALL derive every optional behaviour — torch, zoom,
image decoding, own preview surface — from these values, never from an engine's identity.

The single exception is `MANUAL_INPUT`, which the interface names in order to render its text field
and which the domain names in order to set the request source. Any other branch on a specific
`ScannerEngineId` SHALL be treated as a defect.

#### Scenario: The engine bench renders a new engine

- **WHEN** a new engine is added to `ScannerEngineCatalog` with an honest descriptor
- **THEN** the engine bench screen renders its card by walking the declared fields
- **AND** no change to `:feature:scanner` is required

### Requirement: Optional capabilities are found through decorators

Domain decorators SHALL copy the descriptor of the engine they wrap, and SHALL expose that engine
through `DecoratingScannerEngine.delegate`.

Consumers SHALL locate an optional capability with `capability<T>()`, which walks the delegate chain,
rather than with a direct cast.

#### Scenario: Torch control on a decorated engine

- **WHEN** an engine that implements `CameraControlEngine` is wrapped by the domain decorators
- **AND** the UI asks for torch control
- **THEN** `capability<CameraControlEngine>()` returns the wrapping chain's real implementor
- **AND** the torch control remains available to the user

#### Scenario: The comparator exposes no delegate

- **WHEN** `ComparingScannerEngine` wraps several engines at once
- **THEN** it does not implement `DecoratingScannerEngine`
- **AND** `capability<T>()` over it returns `null` rather than an arbitrary engine's capability

### Requirement: The catalog and its documentation cannot diverge

`ScannerEngineCatalog` SHALL contain one entry per engine, and `docs/ENGINES.md` SHALL contain one
master-table row per engine, agreeing on identifier and phase.

`check_engine_catalog()` in `tools/checks.py` SHALL compare identifier, phase and platforms between
the two, and SHALL run before any Gradle task in CI.

Display names and dependency strings SHALL NOT be compared: they are product prose that changes
wording without changing meaning.

#### Scenario: An engine is added to code but not to the documentation

- **WHEN** an entry is added to `ScannerEngineCatalog` with no matching row in `docs/ENGINES.md`
- **THEN** `check_engine_catalog()` reports it
- **AND** `Verify` rejects the pull request in its first step

#### Scenario: A platform or phase drifts

- **WHEN** the master table lists a platform the descriptor does not declare
- **THEN** the check names both sides of the disagreement

#### Scenario: The check itself is the guarantee

- **WHEN** a document claims this parity is enforced
- **THEN** the claim refers to `tools/checks.py`, not to a Kotlin test
- **AND** it cannot be a Kotlin test, because a KMP `commonTest` has no filesystem
