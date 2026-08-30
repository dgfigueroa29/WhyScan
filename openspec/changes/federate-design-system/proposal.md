# Federate the design-system foundation so other applications can depend on it

- **Status:** Proposed — **blocked on one decision the project owner must take** (see Blockers)
- **Capability:** `design-system`
- **Decision:** [ADR-0018](../../../docs/adr/ADR-0018-federar-la-base-y-no-la-marca.md)

## Why

Other applications in the company want to reuse this design system. Today they cannot depend on it,
they can only copy it — and copying is not cheap, it is expensive later: in six months there are
four divergent versions, and the contrast fix made in one never reaches the others.

Three things stand in the way, and they are different problems:

1. **`:core:designsystem` is not a design system, it is WhyScan's theme.** Of its 930 lines,
   `ScannerPalette` and `BrandMark` *are* the brand. Sharing them is not sharing a system — it is
   making every application in the company look like WhyScan.
2. **There is no API surface.** No `explicitApi()`, so anything not marked `internal` is public API
   by accident. No binary-compatibility validation, so breaking a consumer is invisible in review.
   No publication, no versioning, no generated documentation.
3. **The package is `com.whyscan`.** Shared code under a product's namespace is a promise that
   breaks the day the product is renamed or archived.

## What changes

**The split.** A new `:core:foundation`, brand-free and publishable, takes what is reusable; the
existing `:core:designsystem` keeps the brand and is never published.

| To `:core:foundation` | Stays in `:core:designsystem` |
|---|---|
| `Contrast` — WCAG arithmetic over ARGB, no Compose | `ScannerPalette`, `BrandMark` |
| `AppLanguage` + its four `actual`s (ADR-0011) | WhyScan's radius and type-scale **values** |
| `LocalSnackbarHostState` | `WhyScanTheme` |
| Scale *mechanics*: a type that takes values and produces `Shapes` and `Typography` | |
| `themeFrom(palette)`: declaring all ~34 Material roles from a palette | |

**The five guarantees**, without which nothing is published:

- `explicitApi()` in strict mode on `:core:foundation`.
- Binary-compatibility validation with the `.api` dump committed, so an API break is a reviewable
  diff.
- Semantic versioning with the policy written down — in Compose it is not obvious: changing a
  default parameter value is source-compatible and **binary-breaking**.
- Generated API documentation.
- **A consumer that is not WhyScan**: a `samples/` module depending only on the public API of
  `:core:foundation`, compiled in `Verify`. This is the only one of the five that detects real
  coupling.

Published at `0.x`, where the contract is explicitly "may break". `1.0` waits for a second real
consumer.

## What does not change

- No visual change to WhyScan. Every value moves; none is re-chosen.
- `:core:designsystem` keeps its name, its brand and its API to the features.
- `docs/legal/`, the privacy guarantee, the engines and the scanning behaviour are untouched.
- iOS gets no new work: the `AppLanguage.ios.kt` `actual` moves modules and nothing more.

## Blockers

**The Maven group and the foundation package name are not decided, and this change cannot start
without them.** They must not contain `whyscan`. This is a naming decision for the project owner,
not a technical one, and inventing a company name here would be worse than waiting.

## Verification

The honest table. **Nothing in this change can be executed in the development environment** — no
network to `dl.google.com`, so no Gradle task runs. That is why the tasks are sequenced smallest
first and why `explicitApi()` comes before publication rather than after.

| Claim | Proof | Runs on every PR |
|---|---|---|
| The foundation carries no brand dependency | `check_design_system()` in `tools/checks.py` | Yes — and it already passes today for the three files that qualify |
| The split compiles | `Verify`: detekt, `jvmTest`, `desktopTest`, Android, Desktop, Web | Yes, but **not verifiable here** |
| `explicitApi()` is satisfied | Compilation. It will fail loudly the first time, on every public declaration missing a visibility modifier or an explicit return type | Yes — expect a large, mechanical first pass |
| The public API did not break | The committed `.api` dump versus the generated one | Yes, once the validator is wired |
| The foundation is usable without WhyScan | The `samples/` module compiling against the published API only | Yes |
| Contrast and typography guarantees survive the move | `ContrastTest` and `ReadingTypographyTest`, moved with the code | Yes |
| Another team can actually adopt it | **Nothing here.** It needs a second application and a person integrating it | No — and no test will ever substitute for it |

The last row is the one that matters most and the one this repository cannot close. Until a second
application consumes the artefact, "it is reusable" is a design opinion, not a verified property.
