---
name: whyscan-engine-authoring
description: The scanner engine SPI contract — how to add, change or review a scanning engine in WhyScan. Use when working on anything under engines/, on core/scanner-api, on ScannerEngineCatalog, on engine selection, or when reviewing a change that touches engine capabilities or descriptors.
---

# Authoring a scanning engine

Nine engines implement one SPI. The SPI is the whole design (ADR-0002): the UI and the domain branch
on **declared capabilities**, never on an engine's identity, which is why adding an engine adds no
conditional anywhere.

That only holds while descriptors stay honest. An engine that declares what it cannot do does not
break itself — it breaks the selector and every screen that trusted the declaration.

## The contract

`BarcodeScannerEngine` is deliberately minimal:

```kotlin
val id: ScannerEngineId
val descriptor: ScannerEngineDescriptor
suspend fun availability(): EngineAvailability   // idempotent, no observable side effects
fun scan(request: ScanRequest): Flow<ScanEvent>  // cold: camera opens on collect, closes on cancel
```

Capabilities that not every engine has live in **segregated interfaces**, so nobody has to throw
`UnsupportedOperationException` to satisfy a contract:

| Interface | For | Requirement |
|---|---|---|
| `ImageDecodingEngine` | Decoding an already-captured image (RF-07) | `decode(image, request): Result<List<Barcode>>` |
| `CameraControlEngine` | Torch and zoom (RF-14) | The UI shows the controls only if the engine exposes it |
| `TextInputEngine` | Typed input rather than frames | Implemented by the manual engine, which closes every fallback chain |
| `CameraPreviewEngine` | The engine supplies its own video surface | See ADR-0007 |

### The four session guarantees

`BarcodeScannerEngineContractTest` verifies guarantees 1, 2 and 4 for every engine it covers.
Guarantee 3 — that cancelling releases the camera — it cannot: the suite only asserts that
cancelling raises nothing, and observing the release needs hardware. Treat it as a review
obligation. Know all four before writing
`scan()`:

1. The first emitted event is `ScanEvent.SessionStarted`.
2. If the session ends by itself, the last event is `ScanEvent.SessionEnded`. If the **consumer**
   cancels, the flow simply cancels with no further emissions — standard coroutine semantics.
3. Cancelling the collecting coroutine releases the camera. `awaitClose` or `finally`, always.
4. Every format reported in `ScanEvent.Detected` is in `descriptor.capabilities.supportedFormats`.

### Decorators and `capability<T>()`

Domain decorators wrap engines to filter formats, apply request limits, interpret values, impose a
timeout and suppress repeated detections. They copy the wrapped engine's descriptor — correct, since
they remove no capability — which leaves a trap: the descriptor says "I can control the torch" while
`as? CameraControlEngine` on the decorator returns null, because the implementor is inside.

**Always use `capability<T>()`**, which walks `DecoratingScannerEngine.delegate` until it finds the
real implementor. On an undecorated engine it does exactly what the cast did.

`ComparingScannerEngine` deliberately does not implement `DecoratingScannerEngine`: it wraps several
engines at once and there is no single delegate to inherit capabilities from.

## Adding an engine: the nine steps

1. `engines/<name>/`, with only the targets the engine supports.
2. Depend on `:core:scanner-api`, `:core:model` and the platform SDK. Nothing else.
3. Implement `BarcodeScannerEngine` plus the optional capabilities that genuinely apply.
4. Declare an honest `ScannerEngineDescriptor`.
5. Add the ID to `ScannerEngineId`, the row to `docs/ENGINES.md`, the entry to
   `ScannerEngineCatalog` — `check_engine_catalog()` compares the last two.
6. Register it in the target's `platformModule()` in `:composeApp`.
7. Inherit `BarcodeScannerEngineContractTest`, supplying the factory. **Mandatory for any engine
   that can be instantiated without a device** — today `MANUAL_INPUT` and `ZXING_JAVA`, plus every
   domain decorator. Camera engines do not inherit it, by decision (D6).
8. Add the module to `settings.gradle.kts`.
9. Update `docs/ROADMAP.md`, and the default chain in `docs/ENGINES.md` if it belongs in one.

No step touches `:feature:scanner` or `:core:domain`. If one has to, the SPI came up short — that is
an explicit extension with its own ADR, not a patch to the UI.

### Step 6 is where the app died once

**Koin resolves by exact type equality and does not walk supertypes.** Declare every dependency with
the type the *consumer* asks for, not the type the factory returns. This was debt D18: the first real
device boot crashed on an `Executor` registered under the wrong type, with CI green the whole time.
`KoinGraphTest` (common + desktop) and `AndroidKoinGraphTest` (Robolectric, on the JVM) now cover
every graph.

## Selection, and what it does not do

The selector walks the platform's default chain, drops engines that are not `Available` and those
that cannot satisfy the `ScanRequest` — `ScannerCapabilities.satisfies()` decides viability, not
quality — and returns preferred plus fallbacks. `MANUAL_INPUT` always closes the chain, so there is
never a "cannot scan" state.

Two things engines are **not** responsible for:

- **Repeated readings.** No engine avoids repeating a code, and that is not a defect: to ML Kit or
  Vision, a code still in frame is a code still there — thirty a second.
  `DistinctDetectionsScannerEngine` suppresses the same `(format, value)` within two seconds, in the
  domain. The comparator deliberately does **not** carry it: its entire purpose is that every engine
  reports the same code. Neither does image decoding, where codes appear once.
- **Format filtering.** A decorator applies the request's formats over whatever the engine reports.

## Two engines or one?

Two decoders that do the same job stay **two engines**. `ZXING_JAVA` and `ZXING_CPP` are separate
(debt D13), and so are the two OCRs — they share `OcrCodeInterpreter`, which validates the check
digit and holds the tests, but not the recogniser. Attributing one engine's reading to another would
falsify exactly the comparison this app exists to make.
