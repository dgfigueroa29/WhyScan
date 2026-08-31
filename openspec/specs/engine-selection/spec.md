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

### Requirement: Manual entry closes every camera chain

`MANUAL_INPUT` SHALL be available on all four platforms and SHALL declare only
`ScanSource.ManualInput`.

When selection produces an empty chain for a request whose source is `LiveCamera`, the selector
SHALL re-select with `ScanSource.ManualInput` and return that chain, preserving the rejection
reasons from the first pass.

The substitution SHALL happen in the selector, never by widening the engine's declared sources: it
consumes no frames, and declaring a source it cannot serve is the dishonest descriptor ADR-0002
exists to prevent.

The system SHALL NOT present a state in which scanning is impossible.

#### Scenario: The user selects manual entry

- **WHEN** the user chooses `MANUAL_INPUT` in the engine bench
- **THEN** `sourceFor()` sets the request source to `ScanSource.ManualInput`
- **AND** the selector returns a chain containing it, and the text field is rendered

#### Scenario: A desktop live-camera request

- **WHEN** a live camera scan is requested on Desktop, where `ZXING_JAVA` declares only static-image
  input and there is no webcam capture
- **THEN** the first pass produces an empty chain and the selector re-selects with manual input
- **AND** the returned chain is `[MANUAL_INPUT]`
- **AND** the rejection list still names `ZXING_JAVA`, so the bench can explain why there was no
  camera

#### Scenario: A static image nothing can decode

- **WHEN** a static-image request finds no engine able to decode it
- **THEN** the chain stays empty and the session fails
- **AND** no substitution happens, because the user chose a photograph and a keyboard is not a
  fallback for that

#### Scenario: Not even manual entry is available

- **WHEN** the manual engine itself is unavailable rather than merely unsuited to the source
- **THEN** the chain is empty and the session fails
- **AND** this is the only case in which it should

### Requirement: Manual entry is reachable without the engine bench

Every screen that replaces the viewfinder because no camera is usable SHALL offer a way to type a
code by hand.

That action SHALL NOT persist the user's preferred engine: it starts one manual session and leaves
preferences untouched.

#### Scenario: Desktop in basic mode

- **WHEN** a Desktop user in basic mode opens the scanner and no live-camera engine exists
- **THEN** the screen offers both scanning from an image and typing a code by hand
- **AND** neither requires advanced mode, where the engine bench lives

#### Scenario: Typing one code does not change the preference

- **WHEN** the user types a code from that screen
- **THEN** `preferEngine` is not called
- **AND** a platform that later gains a camera engine is not left pinned to manual input

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
