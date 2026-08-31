# Make manual entry actually reachable, and stop the empty chain from erroring

- **Status:** Accepted 2026-08-30 — rewritten after reading the full path; the first version was
  wrong about severity
- **Capability:** `engine-selection`
- **Found by:** the documentation audit, then a second pass through the UI that corrected it

## Correction to the first version

The first version of this proposal claimed a Desktop user hits `ScanError.EngineUnavailable`. **They
do not.** `ScannerViewfinder.kt:126-131` branches before any session starts:

```
needsCameraPermission   → PermissionRequest   (explanation + button)
!hasLiveCameraEngine    → NoCameraAvailable   (explanation + "scan from image")
```

and the start/stop control only renders when both are fine (`:418`), as does auto-start
(`ScannerViewModel.startIfPending():175`). The empty chain exists in the domain and the UI rarely
asks for it.

Overstating a finding costs the same trust as missing one. What the second pass found instead is
worse, and it is below.

## The real problem: manual entry is unreachable where it matters most

`ScannerState.isManualEntryActive` is `activeEngineId == ManualInput`. That requires a **running
session** with the manual engine. On Desktop:

- auto-start is gated off (`!hasLiveCameraEngine`),
- the start button is not rendered,
- and the engine bench, the only other way to select an engine, is **advanced mode only**
  (ADR-0010, ADR-0015).

So **on Desktop, in basic mode, there is no way to reach manual entry at all.** The user gets "no
camera here" and a "scan from image" button, and nothing else. The safety net that G4 calls *always
available* is unreachable on the one platform that has no camera.

That is the change worth making. The rest follows from it.

## Three separable changes

### (b) Manual entry reachable from the no-camera screen — the priority

`NoCameraAvailable` gains a second action that starts a manual-entry session directly.

It must **not** go through `selectEngine()` or `tryEngine()`: both call `settings.preferEngine(id)`,
which persists the choice. Someone typing one code by hand on Desktop should not have their
preferred engine pinned to manual forever — and on a platform that later gains a camera engine, that
pin would be actively wrong. A new action starts the session for this one time without touching
preferences.

### (a) An empty live-camera chain falls back to manual instead of erroring

In `SelectScannerEngineUseCase.select()`, as an explicit last step: if the chain comes out empty
**and** `request.source == ScanSource.LiveCamera`, re-select with `ScanSource.ManualInput`.

Only `LiveCamera`. A static-image request must keep erroring: the user picked a photograph, and
offering them a keyboard instead is not a fallback, it is a non-sequitur. `DecodeImageUseCase` calls
the same `select()`, which is exactly why the condition is on the source and not on emptiness alone.

The rejection list from the first pass is preserved, so the interface can still explain *why*
nothing else was viable.

Reachable today only in advanced mode — a format and continuous/multiple combination no available
engine satisfies — which is why it is (a) and not the priority. It is nearly free alongside (b).

### (c) The thirteen places that assert G4

G4 is asserted in 13 files. They do not all say the same thing, and the distinction matters:

- **`docs/SDD.md` states it correctly**: *"Si el motor preferido no está disponible, se usa el
  siguiente sin error visible."* That is degradation between engines, and `FallbackScannerEngine`
  **does** implement it. This wording needs no change.
- **`docs/ENGINES.md` states the strong version**: *"`MANUAL_INPUT` cierra siempre la cadena"* and
  *"en escritorio, una petición de cámara en vivo cae directamente a `MANUAL_INPUT`"*. Neither is
  true today, and the second is wrong in a third way nobody had noticed: the Desktop user sees a
  "no camera" screen, not a fallback and not an error.
- **`ScannerEngineCatalogTest`** carries a comment claiming it *"garantiza que la cadena de fallback
  nunca se quede vacía (G4)"*. It does not check that. That comment goes regardless of everything
  else here — a comment claiming a guarantee no test enforces is how this survived two years.

## Also, regardless of the above

`StartScanSessionUseCase.noEngineError()` builds:

```
"Motores descartados: gms_code_scanner, mlkit_camerax, zxing_cpp"
```

Raw engine identifiers, shown to a user, in an application whose exit criterion is that someone
reads a code **without ever seeing the word "engine"**. It needs to be a translated string; the
identifiers belong in a log, not on screen.

## What does not change

- `FallbackScannerEngine` and degradation between engines. That part works.
- The permission screen and the no-camera screen keep their existing behaviour and their
  "scan from image" action.
- Image decoding: no fallback to manual, deliberately.
- Nothing about engine descriptors. Making `MANUAL_INPUT` declare `LiveCamera` would be the wrong
  fix — it consumes no frames, and declaring a source it cannot serve is the dishonest descriptor
  ADR-0002 exists to prevent.

## Verification

| Claim | Proof | Runs on every PR |
|---|---|---|
| An empty live-camera chain returns a manual chain | New cases in `SelectScannerEngineUseCaseTest`, replacing the two that pin the empty chain | Yes — JVM |
| A static-image request still errors when nothing can decode | New case in the same suite | Yes |
| Preferences are untouched when manual entry is used from the no-camera screen | New case in `ScannerViewModelTest` asserting `preferEngine` was not called | Yes |
| The session emits `SessionStarted(MANUAL_INPUT)` rather than `Failed` | `ScanSessionsTest` | Yes |
| The error string is translated and carries no engine identifier | `tools/checks.py` resource parity, plus reading | Partly |
| **The no-camera screen is understandable with the new button on it** | **Nothing here.** Layout and wording need eyes | No |

The last row is the honest limit: the mechanism can be built and tested, and whether the screen
reads well cannot. The button's label is the project owner's call, not a detail to invent silently.

The two tests that currently pin the empty chain must be **changed, not deleted** — they are the
record of the old behaviour, and their replacements should say why it changed.
