# Emit Compose compiler reports on demand

- **Status:** Proposed — deliberately **not implemented blind**
- **Capability:** none (build tooling; no observable behaviour changes)
- **Roadmap:** "Se puede hacer ahora" — *Métricas del compilador de Compose*

## Why

Without a device, nothing in this repository says anything real about recomposition. The Compose
compiler's own reports do: they list unstable parameters and non-skippable composables, which is the
input to any performance work that is not guesswork.

This matters here more than in most projects. The scan screen composes over a live camera preview,
and RNF-01/RNF-02 (detection under 500 ms, camera under 1 s) have **never been measured**. The
reports do not measure them either — but an unstable parameter on a composable that redraws every
frame is a finding you can act on without hardware.

## What changes

`reportsDestination` and `metricsDestination` on the Compose compiler extension, **gated behind a
Gradle property** so the default build path is untouched:

```
./gradlew :composeApp:assembleDebug -Pwhyscan.composeReports=true
```

Plus a manual workflow, like `iOS (manual)` and `Baseline profile (manual)`, that runs it and uploads
the reports as an artefact. Not in `Verify`: generating them costs build time on every job and the
output is not an acceptance criterion.

## Why this is a proposal and not a commit

**I could not verify it, and the failure mode is total.**

The configuration goes in `build-logic/src/main/kotlin/whyscan.kmp.compose.gradle.kts`, a
precompiled script plugin. The `composeCompiler { }` type-safe accessor exists only if Gradle
generates it for that script — which depends on `libs.gradlePlugin.composeCompiler` being on the
build-logic classpath. It is, as `implementation`, and the KDoc in `build-logic/build.gradle.kts`
explains at length why (`compileOnly` already broke this project's first CI run).

So it will *probably* work. But if the accessor is not generated, **`build-logic` fails to compile
and every job in `Verify` dies** — not just Android. In an environment where nothing compiles
locally, pushing that on "probably" trades a real risk for a report nobody reads until they go
looking.

This is the same judgement the repository already applies to publishing configuration in
`federate-design-system`, and the same one that would have caught the first version of
`cover-android-database-builder`.

## Tasks

- [ ] Add the gated `composeCompiler { }` block to `whyscan.kmp.compose.gradle.kts`. Gate it with
      `providers.gradleProperty("whyscan.composeReports").isPresent` so an ordinary build is
      byte-for-byte unaffected.
- [ ] **First push: nothing else.** Confirm `Verify` is green with the property unset before
      touching anything more. That single push is the whole risk of this change; isolate it.
- [ ] If the type-safe accessor turns out not to be generated, fall back to configuring the
      extension by type from the root build rather than fighting the accessor.
- [ ] Add a manual workflow that builds with the property set and uploads
      `**/build/compose-reports/**` and `**/build/compose-metrics/**`.
- [ ] Document in `docs/SDD.md` §13 how to read the output, and what it does **not** say — it
      reports stability and skippability, not time. Nothing here measures milliseconds.

## Verification

| Claim | Proof | Runs on every PR |
|---|---|---|
| The gated block does not affect ordinary builds | `Verify` green with the property unset | Yes |
| The reports are produced when asked | The manual workflow's artefact | No — manual, by design |
| The findings mean anything for real performance | **Nothing here.** Stability is a compile-time property; whether it costs the user anything needs a device | No |
