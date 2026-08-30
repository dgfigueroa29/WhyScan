# Project context

Read this before proposing a change. It is the minimum an agent needs in order to write a proposal
that is not immediately wrong.

## What WhyScan is

A barcode and QR reader in Compose Multiplatform — Android, iOS, Desktop, Web — with no account, no
tracking and no network. Underneath it is a **test bench for scanning engines**: nine alternatives
behind one SPI, selected automatically, compared side by side, and degraded gracefully when one is
unavailable.

Both things are the same app in two modes. By default it is a reader: point and scan. **Advanced
mode** (Settings → Advanced) exposes the engine catalog, the parallel comparator and per-read
latencies.

## Constraints that shape every proposal

- **Android is the priority.** Desktop and Web are maintained because they cost nothing. **iOS is
  deprioritized** until the project owner says otherwise — a proposal that targets iOS needs their
  explicit instruction.
- **Nothing compiles in the development environment.** No network to `dl.google.com` or
  `api.foojay.io`. `python3 tools/checks.py` is the only thing that runs locally; `Verify` decides.
- **No instrumented tests, ever** (debt D6). Anything a requirement asserts must be provable on the
  JVM, on every pull request.
- **No network at runtime, at all.** No `INTERNET` permission, no HTTP client, no analytics, no
  crash reporting, no system backup of app data. This is a product promise stated in the README, in
  Settings, in `docs/legal/` and in the manifest, and part of it is enforced by a check in CI.
- **Two languages, always in parity.** Every user-visible string exists in `values/` (English, the
  fallback for *any* locale) and `values-es/`.

## Architecture in one screen

```
androidApp / composeApp        App shell, navigation root, platformModule() per target
    ↓
feature/{scanner,history,settings}     ViewModels + Compose UI
    ↓
core/domain                    Use cases, engine selection, engine decorators
    ↓
core/scanner-api               THE SPI — BarcodeScannerEngine + segregated capabilities
    ↑
engines/<nine of them>         One module per alternative; depend only on scanner-api + model
```

Dependencies point inward. An engine never depends on a feature or on the domain; a feature never
depends on another feature. Dependency injection is Koin (ADR-0003), and **it resolves by exact type
equality without walking supertypes** — the trap that killed the app on its only real device boot.

## Non-negotiable behaviours

These are already true and a proposal must not break them:

- `MANUAL_INPUT` closes every selection chain, so there is never a "cannot scan" state.
- Declared capabilities are contracts: the selector and the UI branch on them, and
  `BarcodeScannerEngineContractTest` checks declaration against behaviour.
- Repeated detections are suppressed **in the domain**, over a two-second `(format, value)` window —
  and deliberately **not** in the comparator, whose purpose is that every engine reports the same
  code.
- History is persistent, notes belong to the history entry rather than the detection (ADR-0012), and
  deletion of a single entry is undoable.
- The app's language and theme override the system's (ADR-0011), and both persist.

## Conventions

- Kotlin, Compose Multiplatform, coroutines and `Flow`. Code and KDoc in **Spanish**; this directory
  and `.claude/` in **English**.
- Lines under 120 characters. Import order: everything else, then `java.**`, `javax.**`,
  `kotlin.**`, then aliased imports. `package` matches the directory.
- detekt with the configuration in `config/detekt/`. Coverage floors: 80 % lines in `:core:domain`
  and `:core:data`, enforced by `tools/coverage.py` in CI; the three features are measured without a
  floor until there is a number to set one from.

## Where the current truth lives

| Question | File |
|---|---|
| What the system does today, as requirements | `openspec/specs/` |
| How it is designed | `docs/SDD.md` |
| Which engines exist and what they can do | `docs/ENGINES.md` |
| Why a structural decision was taken | `docs/adr/` |
| What is done, pending, or accepted debt | `docs/ROADMAP.md` |
| The rules for agents | `AGENTS.md` (repository root) |
