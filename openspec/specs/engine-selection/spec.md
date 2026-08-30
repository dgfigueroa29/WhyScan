# Engine selection and the fallback chain

## Purpose

The user asks to scan; the system decides which of the available engines answers, and what happens
when it cannot. Selection is derived from declared capabilities and current availability, so that a
new engine changes behaviour without changing code outside its own module.

Design: `docs/ENGINES.md` §"Prioridad de selección automática", `docs/SDD.md` §7, RF-04.

## Requirements

### Requirement: Selection produces a chain, not a single engine

The selector SHALL return an ordered chain of engines: a preferred engine followed by its fallbacks.

It SHALL discard engines that do not report `EngineAvailability.Available`, and engines whose
`ScannerCapabilities.satisfies(request)` is false.

#### Scenario: The preferred engine becomes unavailable mid-session

- **WHEN** the preferred engine fails or reports itself unavailable during a session
- **THEN** the next engine in the chain takes over
- **AND** the session continues without the user restarting it

### Requirement: Manual entry is reachable on every platform, and closes the chain once chosen

`MANUAL_INPUT` SHALL be available on all four platforms, SHALL declare `ScanSource.ManualInput`, and
SHALL therefore be selected for any request whose source is manual input.

When the user selects the manual engine, the domain SHALL set the request source to
`ScanSource.ManualInput` so that the selector does not discard it — `sourceFor()` in `ScanSessions`.

> **Known gap, stated rather than glossed.** Because `MANUAL_INPUT` declares only
> `ScanSource.ManualInput`, `ScannerCapabilities.satisfies()` rejects it for any live-camera
> request. It therefore does **not** close a camera chain automatically, and the goal G4 — "there is
> never a state in which scanning is impossible" — is not currently met. See the change
> `close-the-chain-with-manual-entry`.

#### Scenario: The user selects manual entry

- **WHEN** the user chooses `MANUAL_INPUT` in the engine bench
- **THEN** `sourceFor()` sets the request source to `ScanSource.ManualInput`
- **AND** the selector returns a chain containing it, and the text field is rendered

#### Scenario: A desktop live-camera request — today's behaviour

- **WHEN** a live camera scan is requested on Desktop, where `ZXING_JAVA` declares only static-image
  input and there is no webcam capture
- **THEN** the chain is **empty** and the session emits `ScanError.EngineUnavailable`
- **AND** `SelectScannerEngineUseCaseTest` asserts exactly that empty chain
- **AND** the user sees an error rather than the typed-entry fallback the goal promises

#### Scenario: Web beyond the browser detector — today's behaviour

- **WHEN** the browser does not expose `BarcodeDetector` and the request is for a live camera
- **THEN** the chain is empty, since `zxing-cpp` publishes no wasmJs artefact (ADR-0008) and manual
  entry does not satisfy a camera source

### Requirement: The request constrains the chain

Selection SHALL honour the `ScanRequest`: source, requested formats, continuous scanning, multiple
codes and required torch control.

#### Scenario: Continuous scanning on Android

- **WHEN** a request asks for continuous scanning
- **THEN** `GMS_CODE_SCANNER` is discarded, because it does not declare `supportsContinuousScan`
- **AND** `MLKIT_CAMERAX` heads the Android chain

#### Scenario: Static image input

- **WHEN** a request declares `ScanSource.StaticImage`
- **THEN** only engines declaring that source enter the chain

### Requirement: The domain suppresses repeated readings

Over the live session chain — not per engine — the domain SHALL suppress a detection whose
`(format, value)` pair was already emitted within a two-second window.

A suppressed detection SHALL NOT refresh the window, and an event left with no fresh detections
SHALL NOT be emitted at all.

#### Scenario: A code held in front of the camera

- **WHEN** a QR code stays in frame for three seconds at thirty frames per second
- **THEN** at most one detection reaches the consumer per two-second window
- **AND** the history receives one entry, not ninety

#### Scenario: A code removed and presented again

- **WHEN** the same code is scanned again more than two seconds after the previous reading
- **THEN** the detection is emitted, because reading the same code twice is a real use case

#### Scenario: The comparator is exempt

- **WHEN** the engine comparator runs several engines against the same code
- **THEN** no suppression is applied, because every engine reporting the same code is the point of
  the comparison

#### Scenario: Image decoding is exempt

- **WHEN** a static image is decoded
- **THEN** no suppression is applied, because codes appear once in a photograph

### Requirement: Fallback across engines is one reading, not two

Suppression of repeated readings SHALL be applied outside the fallback chain.

#### Scenario: A fallback takes over on the same physical code

- **WHEN** the preferred engine fails while a code is in frame and the next engine reads that same
  code
- **THEN** the consumer sees one reading, not two

### Requirement: The user's chosen engine overrides automatic selection

When the user selects an engine in the bench, the system SHALL use it as the preferred engine for
subsequent sessions, and SHALL persist that choice.

Fallbacks SHALL still apply if the chosen engine is unavailable.

#### Scenario: A chosen engine that stops being available

- **WHEN** the user has chosen an engine that later reports itself unavailable
- **THEN** the session proceeds with the next viable engine
- **AND** the user's stored preference is not silently overwritten
