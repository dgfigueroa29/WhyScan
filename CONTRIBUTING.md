# Contributing to WhyScan

**English · [Castellano](CONTRIBUTING.es.md)**

Thanks for looking. WhyScan is a barcode and QR reader with no account, no tracking and no network,
and underneath it a test bench comparing nine scanning engines across four platforms.

If you are an AI agent, read [`AGENTS.md`](AGENTS.md) instead — it is the normative contract, and it
is more specific than this file.

## Before you write code

**Open an issue first** for anything that changes behaviour. Not as bureaucracy: the design is
written down in `docs/SDD.md` and seventeen ADRs, and a change that contradicts a recorded decision
needs a new decision, not a patch. Finding that out after you have written the code is nobody's idea
of a good afternoon.

Good first contributions: a defect you can reproduce, a missing test for existing behaviour, a
documentation error, a translation fix.

## What you should know about this project

**Android is the priority.** It is the platform that ships and the only one anyone has run
end to end. Desktop and Web are maintained because they compile in CI and cost no attention.
**iOS is deprioritized** — not abandoned, but without Apple hardware nothing there can be tested,
and linking the framework only proves that Kotlin/Native compiles.

**There are no instrumented tests, and there will not be.** With no emulator in CI, a test that
needs a device never runs and gives a false sense of safety. The rule, stated precisely, is that
**anything checked must be executable on every pull request** — which is why the Android dependency
graph does have a test, with Robolectric, on the JVM.

**The app has no network access at all.** No `INTERNET` permission, no HTTP client, no analytics, no
crash reporting, no system backup of app data. This is a promise made to users in the README, in
Settings and in the published privacy policy, and part of it is enforced by a check in CI. A pull
request that weakens it will not be merged.

**`docs/` is source of truth, not a summary.** `docs/ENGINES.md` and `ScannerEngineCatalog` cannot
diverge — a test compares them.

## Setting up

```bash
git clone https://github.com/dgfigueroa29/WhyScan.git
cd WhyScan
./gradlew :androidApp:assembleDebug     # JDK 17, Android SDK 36
```

Open in Android Studio (or IntelliJ with the Kotlin Multiplatform plugin). The `:composeApp` module
holds the shared shell; `:androidApp` is the Android entry point.

Other targets: `./gradlew :composeApp:desktopJar` and
`./gradlew :composeApp:wasmJsBrowserDistribution`.

## Before every commit

```bash
python3 tools/checks.py
```

It runs in seconds, needs no network, and catches what nothing else does: resource-catalog parity
between English and Spanish, orphan keys, `Res.string.X` used without its import, `package` not
matching its directory, unresolved `return@` labels, the privacy guarantee in the manifest, and the
repository's own structure. It runs first in CI too.

Then, if you can build locally:

```bash
./gradlew detekt jvmTest desktopTest
./gradlew :composeApp:testDebugUnitTest    # the Android graph, on Robolectric
```

## Conventions

- **Code, comments and KDoc are in Spanish**, and so is everything under `docs/`. Agent-facing files
  (`AGENTS.md`, `.claude/`, `openspec/`, `docs/ai/`) are in English. Human guides are in both. The
  full table is in [`AGENTS.md`](AGENTS.md#language-policy).
- Comments explain **why**, not what. A comment repeating the code is noise; one preserving the
  reason for a decision earns its place.
- Lines under 120 characters. Import order is ktlint's: everything else, then `java.**`, `javax.**`,
  `kotlin.**`, then aliased imports.
- Every user-visible string exists in both `values/` and `values-es/`. The unqualified catalog is the
  fallback for **any** language, so a Spanish-only key breaks every other locale.
- Dependencies point inward. An engine depends only on `:core:scanner-api`, `:core:model` and its
  SDK — never on a feature or on `:core:domain`.

## Adding a scanning engine

The nine steps are in [`docs/ENGINES.md`](docs/ENGINES.md#cómo-añadir-un-motor). The two that trip
people up:

- **Declare an honest `ScannerEngineDescriptor`.** The selector and the whole UI branch on declared
  capabilities, and `BarcodeScannerEngineContractTest` checks the declaration against real behaviour.
  Inheriting that suite is not optional.
- **Koin resolves by exact type equality and does not walk supertypes.** Declare each dependency with
  the type its consumer asks for, not the one the factory returns. This killed the app on its first
  real device boot with CI green throughout.

If adding an engine forces you to touch `:feature:scanner` or `:core:domain`, stop and say so in the
pull request: the SPI came up short, and extending it is a decision with its own ADR.

## Pull requests

Fill in the template. The section that matters most is the last one: **what you verified and what you
did not**. Claiming a test passed when you did not run it is the one thing that will get a pull
request closed rather than reviewed.

`Verify` runs on every pull request — detekt, core tests, Android (debug, lint, release with R8),
Desktop and Web — and it is the authority. `iOS (manual)` and `Baseline profile (manual)` are
separate and manual on purpose; they are not acceptance criteria.

## Reporting a security or privacy issue

See [`SECURITY.md`](SECURITY.md). A privacy defect — anything that could let scanned data leave the
device — is treated as a security issue, because that is what it is.

## Licence

By contributing you agree that your contribution is licensed under the
[Apache License 2.0](LICENSE), the same as the rest of the project.
