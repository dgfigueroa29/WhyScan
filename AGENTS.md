# AGENTS.md — the contract for coding agents working on WhyScan

WhyScan is a barcode and QR reader built with Compose Multiplatform (Android, iOS, Desktop, Web)
with **no account, no tracking and no network**. Underneath the reader there is a **test bench for
scanning engines**: nine alternatives behind one SPI, compared side by side and degraded gracefully
when one is unavailable.

This file is the **canonical, normative contract** for any agent — Claude Code, Copilot, Cursor,
Codex or a human following the same rules. It is written in English because that is what agent
tooling reads most reliably. Everything a human is expected to read is available in Spanish too;
see [Language policy](#language-policy).

| If you read one thing | Read this |
|---|---|
| Before your first edit | [Golden rules](#golden-rules) |
| Before you claim something works | [What you can and cannot verify](#what-you-can-and-cannot-verify-here) |
| Before you open a PR | [Definition of done](#definition-of-done) |
| When you don't know where a change belongs | [Where things go](#where-things-go) |

---

## Golden rules

These are non-negotiable. A change that breaks one of them is wrong even if it compiles, even if CI
is green, and even if the user asked for it in passing — say so and propose the alternative.

1. **Android first, and that's the end of it.** Android is the platform that ships, the only one
   anyone has actually run, and the only one whose work can be validated end to end from this
   repository.

2. **iOS is deprioritized until the project owner says otherwise.** Do not touch it on your own
   initiative: no new engines, no refactors, no closing Phase 3 leftovers, no running the
   `iOS (manual)` workflow, no proposing it as a next step. That an iOS task is the one you *could*
   do without a device does not make it a priority. If an Android or shared-code change forces an
   iOS edit to keep it compiling, make the minimum edit and say so — that is not "working on iOS".
   The reason is in [`docs/ROADMAP.md`](docs/ROADMAP.md): without Apple hardware nothing on that
   platform can be *tested*. Linking the framework proves Kotlin/Native compiles and nothing more.

3. **Nothing compiles in this environment.** The development sandbox cannot reach `dl.google.com`
   or `api.foojay.io`, so no Gradle task runs locally. Do not report a Gradle command as executed.

4. **Run `python3 tools/checks.py` before every commit.** It runs in seconds, needs no network, and
   covers what nothing else does: resource-catalog parity between languages, orphan keys, line
   length, import order, `package` vs. directory, and the privacy guarantee in the manifest.

5. **`Verify` is the authority.** It runs on every pull request across the three platforms this
   project can execute: detekt, core tests, Android (debug, lint and release with R8), Desktop and
   Web. `iOS (manual)` and `Baseline profile (manual)` are deliberately separate and manual — what
   they do is not an acceptance criterion for a change.

6. **No instrumented tests, ever (debt D6).** With no emulator in CI, a test that needs a device is
   a test that never runs and gives a false sense of safety. The rule stated precisely is *anything
   that is checked must be executable on every PR* — which is why Android's Koin graph does have a
   test, with Robolectric, on the same JVM as everything else.

7. **`docs/` is source of truth, not a summary.** [`docs/ENGINES.md`](docs/ENGINES.md) and
   `ScannerEngineCatalog` cannot diverge — a test enforces it. A behaviour change that never reaches
   the ROADMAP is a change left half done.

8. **ADRs are records of decisions already taken. They are never rewritten** to agree with today.
   A superseded ADR gets a new ADR that supersedes it, and a `Superseded by` line in the old one.

9. **Never weaken the privacy guarantee.** No `INTERNET` permission, no HTTP client, no analytics,
   no crash reporting, no system backup of app data. `check_privacy_guarantee()` in
   `tools/checks.py` enforces the parts that live in the manifest; the rest is on you.

---

## Language policy

The repository is bilingual by role, not by accident:

| Surface | Language | Why |
|---|---|---|
| Kotlin code, KDoc, code comments | Spanish | The whole codebase is written that way; a mixed file is worse than a consistent one |
| `docs/ENGINES.md`, `docs/ROADMAP.md`, `docs/SDD.md`, `docs/adr/**` | Spanish | Existing source of truth, written by and for the project owner |
| `AGENTS.md`, `.claude/**`, `openspec/**`, `docs/ai/**` | English | Agent-facing surfaces; English is what tooling and models parse most reliably |
| `CONTRIBUTING`, `SECURITY`, `CODE_OF_CONDUCT`, `docs/guides/**` | English **and** Spanish | Human-facing, public repository, two audiences |
| `docs/legal/**` | Spanish and English | Shipped to users from Settings → About |

Comments explain **why**, never what. A comment that repeats the code is noise; one that preserves
the reason for a decision — or the defect that forced it — earns its place.

---

## Repository map

```
androidApp/           Android application module (manifest, R8 rules, launcher)
composeApp/           Shared application shell: App(), navigation root, platformModule() per target
core/model/           Pure domain types (Barcode, ScanRequest, ScannerEngineId, …)
core/scanner-api/     THE SPI: BarcodeScannerEngine + segregated optional capabilities
core/domain/          Use cases, engine selection policy, engine decorators
core/data/            Repositories, persistence-facing implementations
core/database/        Room database, migrations, driver wiring
core/designsystem/    Material 3 theme, typography, shapes, colour roles
core/permissions/     Camera permission handling per platform
core/platform/        Small platform abstractions (sharing, opening, file access)
core/scanner-ui/      The CameraPreviewEngine capability: the engine's own video surface (ADR-0007)
core/scanner-testing/ BarcodeScannerEngineContractTest — the suite every engine must pass
engines/<name>/       One module per alternative — 8 modules for 9 engines: the two OCR
                      engines share engines/ocr/ (see docs/ENGINES.md)
feature/scanner/      Scan screen + engine bench + comparator
feature/history/      Scan history, notes, export
feature/settings/     Preferences, theme, language, about
baselineprofile/      Baseline profile generator — a recording, not a test; manual workflow only
build-logic/          Gradle convention plugins
config/detekt/        Static analysis configuration
tools/                Compiler-free checks and CI measurements (Python, no network)
docs/                 Source of truth: SDD, ROADMAP, ENGINES, ADRs, legal, AI operating model
openspec/             Spec-driven change proposals: current specs + in-flight deltas
.claude/              The harness: settings, hooks, slash commands, subagents, skills
```

---

## What you can and cannot verify here

Being explicit about this is the single most useful thing this file does. Claiming verification you
did not perform is the failure mode that costs the most trust.

| Can be verified in this environment | Cannot be verified here |
|---|---|
| `python3 tools/checks.py` — resource parity, orphan keys, unused imports, import order, line length, `package` vs. path, unresolved `return@` labels, manifest privacy guarantee, well-formed XML | Anything requiring Gradle: compilation, detekt, unit tests, lint, R8 |
| Reading code and docs for consistency (catalog ↔ `ENGINES.md`, ADR references, ROADMAP checkboxes) | That the app starts, renders, or reads a code |
| Structural review of a diff against the rules in this file | Startup time, frame timing, binary size deltas |
| `python3 tools/coverage.py` and `tools/binary_size.py` **against artefacts CI produced**, not locally built ones | iOS anything — no device, and it is deprioritized regardless |

When you have not run something, write "not run here — `Verify` will decide". Never write "tests
pass" unless you saw them pass.

---

## The working loop

Every non-trivial change follows the same five steps. Steps 2 and 5 are the ones agents skip; they
are the ones that keep this repository coherent.

1. **Understand.** Read the relevant section of `docs/SDD.md`, the ADRs it references, and the
   ROADMAP entry that covers the work. The design is written down; do not re-derive it.
2. **Propose.** Anything that changes observable behaviour, adds a capability, or crosses a module
   boundary gets an OpenSpec change under `openspec/changes/<change-id>/` **before** the code.
   See [`openspec/AGENTS.md`](openspec/AGENTS.md). Small, local fixes skip this.
3. **Implement.** Smallest change that satisfies the proposal. Respect module boundaries: adding an
   engine must not touch `:feature:scanner` or `:core:domain` — if it does, the SPI is short and
   extending it is its own decision.
4. **Verify.** `python3 tools/checks.py`, then re-read your own diff adversarially: what would make
   `Verify` reject this? Fix that before pushing.
5. **Document.** Update the ROADMAP checkbox, the SDD section, `ENGINES.md` if the catalog moved,
   and write an ADR if you *decided* something rather than merely implemented it.

---

## Where things go

| What you have | Where it belongs |
|---|---|
| A decision between real alternatives, with consequences | New ADR in `docs/adr/`, using [`docs/adr/TEMPLATE.md`](docs/adr/TEMPLATE.md) |
| A change to observable behaviour, not yet implemented | `openspec/changes/<change-id>/` (proposal + tasks + spec delta) |
| The current, implemented truth about a capability | `openspec/specs/<capability>/spec.md` |
| Design detail: structure, types, contracts, quality strategy | `docs/SDD.md`, in the numbered section that already covers it |
| Progress, pending work, accepted debt | `docs/ROADMAP.md` |
| A new engine, or a change to an engine's capabilities | `docs/ENGINES.md` **and** `ScannerEngineCatalog` — `check_engine_catalog()` compares them |
| A rule agents must follow | This file. Then mirror the summary in `CLAUDE.md` |
| Something a human contributor needs | `CONTRIBUTING.md` / `CONTRIBUTING.es.md` |

---

## Conventions that have already cost something

Each of these exists because it broke once. They are cheaper to read than to rediscover.

- **Koin resolves by exact type equality and does not walk supertypes.** Declare every dependency
  with the type the consumer *asks for*, not the one the factory returns. This killed the app on
  its first real device boot with CI green throughout (debt D18, ADR-0003). `KoinGraphTest` and
  `AndroidKoinGraphTest` now cover every graph.
- **A member always wins over an extension in Kotlin.** `:core:database` declared a `build()`
  extension on `RoomDatabase.Builder`; every call site got Room's member instead, so the bundled
  driver was never configured — for the entire life of the project (debt D19).
- **Declaring a version does not impose it.** Gradle resolves the highest request across the
  classpath. `tools/check_resolved_versions.py` runs in CI for exactly this (debt D24).
- **Compose resources are per module and imported one key at a time.** A string added to one
  catalog and not the other compiles fine and breaks the screen for the other language.
  `values/` (unqualified) is the fallback for *any* language, so a Spanish-only key breaks everyone
  who does not speak Spanish.
- **Engine descriptors are contracts, not documentation.** The selector and the entire UI branch on
  declared capabilities. `BarcodeScannerEngineContractTest` verifies declared behaviour against
  actual behaviour. Inheriting it is mandatory for every engine that can be instantiated without a
  device; camera engines deliberately do not, because constructing them needs an emulator (D6).
- **Repeated detections are suppressed in the domain, not in engines** — a two-second window on
  `(format, value)`. The comparator deliberately does **not** carry that decorator: its whole point
  is that every engine reports the same code.

---

## Definition of done

A change is done when all of these are true. If one cannot be satisfied, say which and why in the
PR body rather than quietly dropping it.

- [ ] `python3 tools/checks.py` reports no findings.
- [ ] The diff is the smallest change that satisfies the request; nothing widened on your own.
- [ ] Module boundaries respected; no new dependency from an engine into a feature or the domain.
- [ ] New or changed behaviour is covered by a test that runs **on every PR** (JVM, no device).
- [ ] `docs/ROADMAP.md` reflects the new state — a checkbox ticked, a round updated, or debt named.
- [ ] `docs/SDD.md` updated if the design moved; `docs/ENGINES.md` if the catalog did.
- [ ] An ADR exists if a decision was made, and no existing ADR was rewritten.
- [ ] The OpenSpec change, if there was one, is archived under `openspec/changes/archive/`.
- [ ] Nothing in the diff adds network access, analytics, backup, or an instrumented test.
- [ ] The PR body says explicitly what was verified here and what only `Verify` can confirm.

---

## Hard stops

Do not do any of these without an explicit instruction from the project owner in the current
conversation:

- Add the `INTERNET` permission, an HTTP client, analytics, or crash reporting.
- Set `allowBackup="true"` or remove `dataExtractionRules`.
- Add an instrumented (device or emulator) test, or add an emulator step to `Verify`.
- Start iOS work, or run the `iOS (manual)` workflow.
- Rewrite or delete an existing ADR.
- Push to `main`, force-push a shared branch, or open a PR that was not asked for.
- Disable, skip or quarantine a failing test to get CI green.

---

## Pointers

- Human-facing contribution guide: [`CONTRIBUTING.md`](CONTRIBUTING.md) ·
  [`CONTRIBUTING.es.md`](CONTRIBUTING.es.md)
- How this project uses AI, end to end: [`docs/ai/README.md`](docs/ai/README.md)
- Harness details — commands, subagents, skills, hooks: [`.claude/README.md`](.claude/README.md)
- Spec-driven change workflow: [`openspec/AGENTS.md`](openspec/AGENTS.md)
- Spanish mirror of this contract's headline rules: [`CLAUDE.md`](CLAUDE.md) (non-normative)
