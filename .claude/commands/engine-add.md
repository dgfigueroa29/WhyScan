---
description: Add a scanning engine end to end, following the SPI contract
argument-hint: [engine name and platform]
allowed-tools: Read, Write, Edit, Grep, Glob, Bash(python3 tools/checks.py), Bash(ls:*)
---

Add the scanning engine: **$ARGUMENTS**

Load the `whyscan-engine-authoring` skill for the SPI details, and read `docs/ENGINES.md`
§"Cómo añadir un motor" and `docs/adr/ADR-0002-scanner-engine-spi.md` before writing anything.

## Check the premises first

- **Is this Android?** If the engine is iOS-only, stop and say so: iOS is deprioritized until the
  project owner says otherwise, and "it is the one I could do without a device" is not a reason.
- **Is it a new engine or a new implementation of an existing one?** Two engines that produce the
  same reading through different decoders stay two engines — attributing one's reading to the other
  would falsify the comparison the app exists to make (debt D13, the `ZXING_JAVA` / `ZXING_CPP`
  criterion, and the same reason the two OCRs are two entries).
- **Does anything here need a decision?** If yes, write the ADR before the code.

## The nine steps

1. `engines/<name>/` with only the targets the engine actually supports.
2. Depend on `:core:scanner-api`, `:core:model` and the platform SDK. Nothing else — **never** a
   feature module or `:core:domain`.
3. Implement `BarcodeScannerEngine`, plus the optional capabilities that genuinely apply:
   `ImageDecodingEngine`, `CameraControlEngine`, `TextInputEngine`, `CameraPreviewEngine`
   (ADR-0007). Implementing one you cannot honour is the failure ADR-0002 exists to prevent.
4. Declare an **honest** `ScannerEngineDescriptor`. Declared capabilities are what the selector and
   the whole UI branch on, and the contract suite checks them against real behaviour.
5. Add the ID to `ScannerEngineId`, the row to `docs/ENGINES.md`, and the entry to
   `ScannerEngineCatalog` — a test compares the last two.
6. Register it in the target's `platformModule()` in `:composeApp`. **Koin resolves by exact type
   equality and does not walk supertypes**: declare each dependency with the type its consumer asks
   for, not the type the factory returns. This is what killed the app on its first real device boot
   with CI green throughout (debt D18).
7. Inherit `BarcodeScannerEngineContractTest`, supplying the engine factory. Not optional.
8. Add the module to `settings.gradle.kts`.
9. Update `docs/ROADMAP.md`, and `docs/ENGINES.md` §"Prioridad de selección automática" if the
   engine belongs in a default chain.

## The four session guarantees

The contract suite verifies these; know them before implementing `scan()`:

1. The first event is `ScanEvent.SessionStarted`.
2. If the session ends by itself, the last event is `ScanEvent.SessionEnded`. If the consumer
   cancels, the `Flow` cancels with no further emissions — standard coroutine semantics.
3. Cancelling the collecting coroutine releases the camera (`awaitClose` / `finally`).
4. Every format reported in `ScanEvent.Detected` is in `descriptor.capabilities.supportedFormats`.

## Finish

If any step forces a change to `:feature:scanner` or `:core:domain`, **stop and say so**: the SPI
came up short, and extending it is an explicit decision, not a patch to the UI.

Run `python3 tools/checks.py`. Report what you verified here and what only `Verify` can confirm —
nothing in this repository compiles locally.
