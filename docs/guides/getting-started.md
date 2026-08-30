# Getting started

**English · [Castellano](primeros-pasos.md)**

A tour of WhyScan for someone who has just cloned it. Twenty minutes, and by the end you will know
where everything is and why it is there.

## What you are looking at

Two things that happen to be one application:

- **A barcode and QR reader.** No account, no tracking, no network. Point and scan.
- **A test bench for scanning engines.** Nine alternatives behind one interface, selected
  automatically, compared side by side, degrading gracefully when one is unavailable — across
  Android, iOS, Desktop and Web from a single codebase.

The second one is only visible in **advanced mode** (Settings → Advanced). Someone who installs the
app to read a QR code never sees the word "engine", and that is deliberate: it is the exit criterion
for the current phase.

## Build it

You need **JDK 17** and the **Android SDK (API 36)**. Android Studio brings both.

```bash
git clone https://github.com/dgfigueroa29/WhyScan.git
cd WhyScan

./gradlew :androidApp:assembleDebug                  # Android
./gradlew :composeApp:desktopJar                     # Desktop
./gradlew :composeApp:wasmJsBrowserDistribution      # Web
```

iOS links from the `iOS (manual)` workflow on GitHub Actions. There is no `iosApp.xcodeproj` in the
repository — it can only be created from Xcode — and the platform is deprioritized, so do not start
there.

## The one command to remember

```bash
python3 tools/checks.py
```

Seconds, no network, no Gradle. It catches resource-catalog drift between English and Spanish, orphan
keys, import order, `package` not matching its directory, unresolved `return@` labels, the privacy
guarantee in the manifest, and the repository's own structure. Run it before every commit; CI runs it
first too.

## Find your way around

Start with the SPI, because everything else orbits it:

```
core/scanner-api/       BarcodeScannerEngine — the whole design lives here
    ↑
engines/<nine>/         One module per alternative
    ↓
core/domain/            Selection policy + the decorators that wrap every engine
    ↓
feature/{scanner,history,settings}      ViewModels and Compose UI
    ↓
composeApp/             App(), navigation, platformModule() per target
```

Then read, in this order:

1. **[`docs/ENGINES.md`](../ENGINES.md)** — the nine engines, what each can do, and the default
   selection chain per platform. Short, and it makes the rest make sense.
2. **[`docs/adr/README.md`](../adr/README.md)** — eighteen decisions with their costs. Start with
   [ADR-0002](../adr/ADR-0002-scanner-engine-spi.md) (the SPI) and
   [ADR-0003](../adr/ADR-0003-koin-como-di.md) (dependency injection, and the defect that killed the
   app on its first real device boot).
3. **[`docs/SDD.md`](../SDD.md)** — the design document. About twenty-five thousand words; read the
   section you need, not the whole thing. §7 is the SPI, §10 dependency injection, §11 persistence, §13 the
   quality strategy.
4. **[`docs/ROADMAP.md`](../ROADMAP.md)** — what is done, what is pending, and what is blocked on not
   having a device. It also records the defects, in the round where they were found.

## Three things that will surprise you

**There are no instrumented tests.** With no emulator in CI, a test that needs a device never runs
and gives a false sense of safety. Instead: the Koin graph is tested on the JVM with Robolectric, the
entire `App()` is composed in a test with no window, and `koinInject` is verified using the Compose
runtime alone. The rule is that anything checked must be executable on every pull request.

**The documentation is source of truth, not a summary.** `docs/ENGINES.md` and `ScannerEngineCatalog`
cannot diverge, because a check in `tools/checks.py` compares them. Coverage floors are measured, not promised.

**Most of this was written by an AI agent**, under the project owner's direction. That is documented
in [`docs/ai/`](../ai/README.md), including what the agent could not do — the two most valuable
defects in this project's history were found by a human putting the app on a real phone.

## Where to go next

| You want to | Go to |
|---|---|
| Fix something or add a feature | [`CONTRIBUTING.md`](../../CONTRIBUTING.md) |
| Add a scanning engine | [`docs/ENGINES.md`](../ENGINES.md#cómo-añadir-un-motor) |
| Work on this with an AI agent | [`AGENTS.md`](../../AGENTS.md), then [`.claude/README.md`](../../.claude/README.md) |
| Propose a change to behaviour | [`openspec/README.md`](../../openspec/README.md) |
| Understand a decision | [`docs/adr/README.md`](../adr/README.md) |
| Report a privacy or security issue | [`SECURITY.md`](../../SECURITY.md) — privately |
