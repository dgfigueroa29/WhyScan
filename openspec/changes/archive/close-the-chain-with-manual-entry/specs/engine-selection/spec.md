# Engine selection — delta

## MODIFIED Requirements

### Requirement: Manual entry is reachable on every platform, and closes the chain once chosen

`MANUAL_INPUT` SHALL be available on all four platforms, and SHALL be the engine of last resort:
when engine selection produces an empty chain for a request, the domain SHALL re-select with
`ScanSource.ManualInput` and return that chain.

The system SHALL NOT present a state in which scanning is impossible.

`MANUAL_INPUT` SHALL continue to declare only `ScanSource.ManualInput`. The substitution SHALL
happen in the domain, not by widening the descriptor: the engine does not consume frames, and
declaring a source it cannot serve is the dishonest descriptor ADR-0002 exists to prevent.

Selection SHALL preserve the rejection reasons from the first pass, so the interface can explain why
no other engine was viable.

#### Scenario: The user selects manual entry

- **WHEN** the user chooses `MANUAL_INPUT` in the engine bench
- **THEN** the request source is set to `ScanSource.ManualInput`
- **AND** the selector returns a chain containing it, and the text field is rendered

#### Scenario: A desktop live-camera request

- **WHEN** a live camera scan is requested on Desktop, where `ZXING_JAVA` declares only static-image
  input and there is no webcam capture
- **THEN** the first selection pass produces an empty chain
- **AND** the domain re-selects with `ScanSource.ManualInput` and the session emits
  `SessionStarted(MANUAL_INPUT)`
- **AND** the user is offered typed entry rather than `ScanError.EngineUnavailable`

#### Scenario: Web beyond the browser detector

- **WHEN** the browser does not expose `BarcodeDetector` and a live camera is requested
- **THEN** the chain resolves to `MANUAL_INPUT`, since `zxing-cpp` publishes no wasmJs artefact
  (ADR-0008)

#### Scenario: An engine fails mid-session

- **WHEN** an engine fails after the session has started and no fallback remains
- **THEN** `ScanError.EngineUnavailable` is still emitted
- **AND** the substitution does not apply, because it concerns a chain that was empty from the start
