# Close an empty selection chain with manual entry, so "cannot scan" stops being reachable

- **Status:** Proposed — **needs the project owner's decision**, because it changes what a user sees
- **Capability:** `engine-selection`
- **Found by:** the full documentation audit, 2026-08-30

## Why

Goal G4 is stated as a product guarantee in five places:

- `docs/ENGINES.md`: *"`MANUAL_INPUT` cierra **siempre** la cadena: garantiza que nunca hay un estado
  'no se puede escanear'"*, and *"en escritorio, una petición de cámara en vivo **cae directamente a
  `MANUAL_INPUT`**"*.
- `openspec/project.md`, under non-negotiable behaviours.
- The KDoc of `EnginePriorityPolicy`.
- The comment in `ScannerEngineCatalogTest`: *"garantiza que la cadena de fallback nunca se quede
  vacía (G4)"* — on a test that does not check that.
- `ScannerEngineCatalog.manualInput.strength`: *"Siempre disponible: cierra la cadena de fallback en
  las cuatro plataformas"*.

**The code does not do this.** `manualInput` declares `sources = setOf(ScanSource.ManualInput)`, so
`ScannerCapabilities.satisfies()` returns false for any `LiveCamera` request. On Desktop, a live
camera request produces an **empty chain**, and `StartScanSessionUseCase` emits
`ScanError.EngineUnavailable`. `SelectScannerEngineUseCaseTest:234` asserts that empty chain, so the
current behaviour is pinned by a test.

Manual entry is reachable only when the user picks it in the bench: `ScanSessions.sourceFor()` then
flips the request source. That is a real mechanism and it is not the guarantee — it requires the
user to already know that an engine catalogue exists, in an application whose exit criterion is that
someone reads a code **without ever seeing the word "engine"**.

The gap is not theoretical. On Desktop there is no webcam capture at all, so *every* live-camera
request lands there.

## What changes

When the selector produces an empty chain for a request the manual engine could serve by source
substitution, the domain re-selects with `ScanSource.ManualInput` and returns that chain, instead of
emitting `EngineUnavailable`.

The user sees the typed-entry field with an explanation, rather than an error.

## What does not change

- No change when the chain is non-empty; the fallback order is untouched.
- `EngineUnavailable` still exists and is still emitted when an engine fails **mid-session** — this
  is only about a chain that was empty from the start.
- No engine descriptor changes. Making `manualInput` declare `LiveCamera` would be the wrong fix: it
  does not consume frames, and declaring a source it cannot serve is exactly the dishonest
  descriptor that ADR-0002 exists to prevent.
- No change to image decoding, which has its own chain.

## The alternative, if the owner prefers it

Accept today's behaviour and **correct the five documents** to say "manual entry closes the chain
when the user selects it". That is cheaper and it is a real option — but it gives up G4, and the
`ScannerEngineCatalogTest` comment must go either way, because that test does not check what it
claims.

This proposal recommends fixing the code, on the grounds that a guarantee written in five places
over two years is the intended behaviour and the empty chain is the accident.

## Verification

| Claim | Proof | Runs on every PR |
|---|---|---|
| An empty camera chain falls back to manual entry | New cases in `SelectScannerEngineUseCaseTest`, replacing the two that pin the empty chain | Yes — JVM |
| The session emits `SessionStarted` for `MANUAL_INPUT` rather than `Failed` | New case in `ScanSessionsTest` | Yes — JVM |
| Descriptors stay honest — no engine declares a source it cannot serve | `ScannerEngineCatalogTest`, unchanged | Yes |
| The user actually understands what happened on that screen | **Nothing here.** It is a wording and layout question, and it needs a person looking at it | No |

The two tests that currently assert the empty chain must be **changed, not deleted**: they are the
record of the old behaviour, and their replacements should say why it changed.
