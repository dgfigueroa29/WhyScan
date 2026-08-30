# Tasks

**Blocked on the owner's decision** in `proposal.md`: fix the code, or accept today's behaviour and
correct the five documents instead. Do not start until that is answered — the two paths share only
task 4.

## If the decision is to fix the code

- [ ] In `SelectScannerEngineUseCase`, when the chain comes out empty and the request source is not
      already `ManualInput`, re-select with `ScanSource.ManualInput`. Keep the `rejected` list from
      the first pass: it is what the bench shows to explain *why* nothing else was viable.
- [ ] Do **not** add `LiveCamera` to `manualInput.sources`. The engine does not consume frames, and
      a descriptor that declares a source it cannot serve is the dishonesty ADR-0002 prevents.
- [ ] Decide where the substitution lives — the use case or `ScanSessions`. `ScanSessions.sourceFor()`
      already does the same substitution for the user-chosen case, so putting both in one place is
      probably right; say which and why in the commit.
- [ ] Change the two tests that pin the empty chain (`SelectScannerEngineUseCaseTest:234` and
      `sin motores elegibles la cadena queda vacia`) rather than deleting them, and have the new
      names state the new guarantee.
- [ ] Add a `ScanSessionsTest` case: a Desktop live-camera request with no camera engine emits
      `SessionStarted(ManualInput)`, not `Failed`.
- [ ] The screen must explain the substitution — the user asked for a camera and got a text field.
      The wording is the owner's call; do not invent it silently.

## If the decision is to accept today's behaviour

- [ ] Correct `docs/ENGINES.md` (both the "cierra siempre" line and the Desktop line),
      `openspec/project.md`, the `EnginePriorityPolicy` KDoc, and
      `ScannerEngineCatalog.manualInput.strength`.
- [ ] Record the decision as an ADR: giving up a goal stated for two years is a decision with a
      cost, and the cost is that a Desktop user meets an error where the documentation promised a
      fallback.

## Both paths

- [ ] Remove or correct the `ScannerEngineCatalogTest` comment claiming it *"garantiza que la cadena
      de fallback nunca se quede vacía (G4)"*. It does not check that, and a comment claiming a
      guarantee no test enforces is how this gap survived two years.
- [ ] Update `openspec/specs/engine-selection/spec.md`, whose requirement currently documents the
      gap explicitly.
- [ ] `python3 tools/checks.py`, then `/spec-apply close-the-chain-with-manual-entry`.
