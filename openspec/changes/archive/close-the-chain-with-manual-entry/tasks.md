# Tasks

Order matters: (a) first because it is pure domain and its tests are the safety net for (b).
**Nothing here can be executed in this environment** beyond `tools/checks.py`.

## 1. (a) The empty live-camera chain falls back to manual

- [ ] In `SelectScannerEngineUseCase.select()`, after `promote()`: if the chain is empty **and**
      `request.source == ScanSource.LiveCamera`, re-select with `ScanSource.ManualInput` and return
      that chain, preserving the `rejected` list from the first pass.
- [ ] Condition on the **source**, not on emptiness alone. `DecodeImageUseCase` calls the same
      `select()`, and a static-image request that nothing can decode must keep erroring: the user
      picked a photograph, and a keyboard is not a fallback for that.
- [ ] Keep `select()` a pure function of catalog + request. It is what lets the whole policy be
      tested in `commonTest` with no repository, and that property is worth more than brevity.

## 2. (a) Tests — change the two that pin the old behaviour, do not delete them

- [ ] `SelectScannerEngineUseCaseTest:234` (`fromCamera.chain.isEmpty()`) and
      `sin motores elegibles la cadena queda vacia`: rewrite to assert the manual chain, and name
      them so the new guarantee is what the test says out loud.
- [ ] New case: a **static-image** request with nothing able to decode still returns an empty chain.
      This is the one that stops a future refactor from "simplifying" the source condition away.
- [ ] New case: the `rejected` list survives the fallback, so the bench can still explain itself.
- [ ] `ScanSessionsTest`: a Desktop live-camera request with no camera engine emits
      `SessionStarted(MANUAL_INPUT)`, not `Failed`.

## 3. (b) Manual entry reachable from the no-camera screen — the priority

- [ ] New `ScannerAction.UseManualEntry`.
- [ ] In `ScannerViewModel`, handle it by starting a manual session **without** calling
      `settings.preferEngine`. Neither `selectEngine()` nor `tryEngine()` can be reused: both
      persist the choice, and typing one code by hand must not pin the preferred engine forever.
- [ ] Add the button to `NoCameraAvailable`, beside the existing "scan from image".
- [ ] Two new strings in `values/` **and** `values-es/`. `tools/checks.py` fails on a catalog that
      only has one of them.
- [ ] **The wording is the project owner's call.** Put a placeholder that says what it does, and
      flag it in the pull request rather than inventing final copy.
- [ ] Test: `UseManualEntry` starts a session with `MANUAL_INPUT` and leaves preferences untouched.

## 4. The error message, regardless of the rest

- [ ] `StartScanSessionUseCase.noEngineError()` stops putting engine identifiers in the user-facing
      reason. A translated string on the UI side; the identifiers stay available for a log.
- [ ] Check no other user-facing string carries a raw `ScannerEngineId`.

## 5. (c) The thirteen places that assert G4

- [ ] `docs/ENGINES.md`: rewrite both claims — "cierra siempre la cadena" and the Desktop line — to
      what the code does after (a) and (b).
- [ ] `ScannerEngineCatalogTest`: remove the comment claiming it guarantees a non-empty chain.
      It never checked that. **Do this even if everything else is dropped.**
- [ ] `ScannerEngineCatalog.manualInput.strength`, `EnginePriorityPolicy` KDoc, and the
      `ScannerContract` KDoc: check each against the new behaviour.
- [ ] **Do not touch `docs/SDD.md`'s G4 row.** It states degradation between engines without a
      visible error, which is true and was always true.

## 6. Close the loop

- [ ] `openspec/specs/engine-selection/spec.md`: the requirement currently documents the gap.
- [ ] `docs/ROADMAP.md`: a round entry, including that the screen was never seen by anyone.
- [ ] `python3 tools/checks.py`.
- [ ] `/spec-apply close-the-chain-with-manual-entry`.
