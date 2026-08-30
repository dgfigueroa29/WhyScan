---
name: whyscan-verification
description: How to verify work on WhyScan when nothing compiles locally, and how to state what was and was not proven. Use before claiming a change works, when interpreting tools/checks.py output, when deciding whether a test belongs in this project, or when writing the verification section of a pull request.
---

# Verifying work on WhyScan

The development environment cannot reach `dl.google.com` or `api.foojay.io`, so **no Gradle task
runs here**. Every claim you make about a change is either something you actually executed, something
you established by reading, or something only CI can decide — and saying which is which is the whole
skill.

Claiming verification you did not perform is the worst failure available in this repository. It is
worse than the bug it hides, because it spends the trust that makes the rest of the reporting useful.

## The three levels of certainty

**Executed here.** Exactly one command: `python3 tools/checks.py`. Plus the two measurement scripts,
`tools/coverage.py` and `tools/binary_size.py`, which only mean anything against artefacts CI already
produced. Say "ran here" and quote the result.

**Established by reading.** Structural facts you can prove from the source: a Koin module declares
the type a consumer asks for; an engine's descriptor matches its behaviour; a document contradicts
the code. Say "verified by reading — not executed".

**Decided by `Verify`.** Compilation, detekt, unit tests, Android lint, R8, Desktop and Web builds,
coverage floors, and resolved-versus-declared dependency versions. Say **"not run here — `Verify`
will decide"**.

Nothing at all covers: that the app starts, renders, or reads a code. That needs a device, and this
project has had exactly one real boot — which found a defect months old that CI never saw.

## What `tools/checks.py` actually covers

Two groups, and the distinction matters when you read a finding:

*Duplicated from detekt, for speed* — line length (120) and import order. detekt remains the
authority; if these two diverge, CI says so, which is the point of running the script in CI too.

*Covered by nothing else* —
- Resource-catalog parity between `values/` and `values-es/`, including per-quantity `<plurals>`.
- `Res.string.X` used without its import; keys used but undeclared; orphan keys nobody uses.
- `package` matching the directory under `kotlin/`.
- `return@label` pointing at a lambda that does not exist — a compile error, catchable offline.
- Unused imports, with the operator-convention exclusions (`by`, `a[b] = c`, `a(b)`).
- The **privacy guarantee** in the Android manifest: no `INTERNET`, `allowBackup="false"`, and
  `dataExtractionRules` present.
- Well-formed XML everywhere.
- Repository structure: ADR headers and index parity, `AGENTS.md` ↔ `CLAUDE.md` cross-links, and
  the shape of every in-flight OpenSpec change.

It deliberately does **not** reimplement detekt. Rules that need a syntax tree — complexity,
`MagicNumber`, unused functions — need a real analyser, and a regex approximation that fails where
detekt passes is the worst kind of check.

## Does this test belong in this project?

The rule is *anything that is checked must be executable on every PR*. Applied:

| Kind of test | Verdict |
|---|---|
| JVM unit test in `commonTest` / `jvmTest` / `desktopTest` | Yes — the default |
| Robolectric test on the JVM (Android graph, Android `Context`) | Yes. `AndroidKoinGraphTest` is exactly this |
| Compose runtime test with a no-op `Applier` | Yes. `ComposeKoinContextTest` proves things that look like they need a device and do not |
| Composition test that mounts `App()` with the real graph | Yes. `AppCompositionTest` already does |
| Instrumented test needing a device or emulator | **No.** Debt D6. A test that never runs is worse than no test |
| The `:baselineprofile` module | Not a test — a *recording*. It asserts nothing, cannot fail on app behaviour, and its output is a file. Hence a manual workflow, not `Verify` |

Before concluding something "can only be tested on a device", check the third and fourth rows again.
Debt D20 stayed open for months on the belief that removing `KoinContext { }` could not be verified
without installing the app. It could: `koinInject` is not UI.

## Writing the verification section

Two sentences, always both:

> Ran here: `python3 tools/checks.py` — no findings. Verified by reading: the Android
> `platformModule` declares `X` with the type `Y` consumes.
>
> Not run here — `Verify` decides: compilation, detekt, unit tests, Android lint and R8. Not covered
> by anything: that the app starts and reads a code, which needs a device.

If a claim does not fit in one of the three levels, it is not a claim you have earned.
