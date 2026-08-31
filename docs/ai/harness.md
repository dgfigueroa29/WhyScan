# The harness

Everything that turns the rules into something an agent cannot quietly ignore. Four layers, ordered
by how fast they answer.

| Layer | Answers in | Catches |
|---|---|---|
| The edit hook | seconds, per edit | Resource parity, imports, line length, `package` vs. path, unresolved labels, the privacy guarantee, repository structure |
| Slash commands and skills | one turn | The procedure for recurring work — nothing forgotten, nothing improvised |
| Subagents | one turn, fresh context | What the agent that wrote the diff cannot see about it |
| `Verify` in CI | 5–15 minutes | Compilation, detekt, tests, lint, R8, coverage, resolved versions, binary size |

## Layer 1 — `tools/checks.py` and the edit hook

`tools/checks.py` runs in seconds, with no network and no Gradle. It exists because the development
environment cannot reach `dl.google.com` or `api.foojay.io`, so for a long time the only way to find
out whether a change was sound was to push it and wait.

It started as loose scripts, each written **after** CI rejected that exact case, and each living
outside version control — so they were lost between sessions and rewritten. That was debt D23. Being
in the repository fixes both halves: anyone can run them before pushing, and **CI runs them too**,
which is what stops them from silently drifting out of sync with what detekt actually enforces.

What it checks, in two groups:

- **Duplicated from detekt for speed** — line length (120) and import order. detekt stays the
  authority; the duplication is the point, and CI reveals any divergence.
- **Covered by nothing else** — resource-catalog parity between `values/` and `values-es/` including
  per-quantity plurals; `Res.string.X` used without its import; orphan keys; `package` matching the
  directory; `return@label` pointing at a lambda that does not exist; unused imports with the
  operator-convention exclusions; well-formed XML; the **privacy guarantee** in the manifest; and the
  repository's own structure — ADR headers and index parity, the `AGENTS.md` ↔ `CLAUDE.md`
  cross-link, and the shape of every in-flight OpenSpec change.

It deliberately does **not** reimplement detekt. Rules that need a syntax tree — complexity,
`MagicNumber`, unused functions — need a real analyser, and a regex approximation that fails where
detekt passes is the worst kind of check.

The `PostToolUse` hook in [`.claude/settings.json`](../../.claude/settings.json) runs it after every
`Edit`, `Write` or `NotebookEdit` on a `.kt`, `.kts` or `.xml` file, and feeds findings back to the
agent. The hook is deliberately forgiving about everything else: a missing `python3`, a crash in the
checker, or an edit to an unrelated file all exit zero. A hook that fails noisily on unrelated work
gets disabled, and a disabled hook checks nothing.

### Two lessons from writing these checks

**Fifteen findings, fifteen false positives.** The first version of the unused-import check flagged
`getValue`, `setValue` and `set` — all used by Kotlin *syntax* (`by`, `a[b] = c`) without ever naming
them. Hence the `OPERATORS` exclusion list, and hence the rule that syntax-tree rules stay out of
this file: a check that fails where detekt passes is worse than no check.

**Order of stripping matters.** Comments are removed *after* string literals, and interpolations are
preserved, because removing `//…` first eats the rest of any line containing `"https://example.com"`
— and this project is full of test URLs.

## Layer 2 — commands and skills

Slash commands in [`.claude/commands/`](../../.claude/commands/) are deterministic entry points to
recurring work: `/preflight`, `/pr-ready`, `/adr-new`, `/spec-propose`, `/spec-apply`, `/docs-sync`,
`/engine-add`. Each carries the procedure *and* the reasons behind it, so the agent following it can
tell when a step does not apply.

Skills in [`.claude/skills/`](../../.claude/skills/) are procedural knowledge loaded on demand:

- **`whyscan-verification`** — the three levels of certainty and how to state them. The most
  load-bearing skill in the repository, because the failure it prevents is the most expensive one.
- **`whyscan-engine-authoring`** — the SPI contract, the four session guarantees, the decorator trap,
  and why two decoders that do the same job stay two engines.
- **`whyscan-adr`** — when a decision deserves a record, and what separates a useful ADR from a
  decorative one.

Add a command when the same instructions get typed twice. Add a skill when the knowledge is
procedural and reusable. **If the rule can be checked mechanically, it belongs in `tools/checks.py`
instead of either** — a rule enforced by a script does not depend on anyone remembering it.

## Layer 3 — subagents

Subagents in [`.claude/agents/`](../../.claude/agents/) get their own context window, and that is the
entire point: an agent asked to review its own work in the same conversation is defending a position.
A fresh reviewer is not.

- **`kotlin-reviewer`** — reviews a diff against this project's conventions and, first, against the
  defects that produced them: exact-type Koin resolution, member-shadows-extension, `capability<T>()`
  instead of a cast, the four session guarantees.
- **`docs-auditor`** — finds divergence between code and `docs/`, and labels each finding as
  mechanical, substantive, or the owner's call. It never proposes rewriting the owner's prose or
  editing an ADR.
- **`spec-reviewer`** — checks an OpenSpec change is well-formed, scoped and *verifiable in this
  project* before implementation, which is where proposals here go wrong.

## Layer 4 — CI

`Verify` runs on every pull request and is the authority. Four jobs: `checks` (offline checks →
detekt → resolved-versus-declared versions → multiplatform tests → the Android graph on Robolectric →
coverage), then `android` (debug, lint, release with R8, binary size), `desktop` and `web`.

`iOS (manual)` and `Baseline profile (manual)` are separate and manual on purpose: what they do is
not an acceptance criterion, and a check nobody can satisfy would leave `main` permanently red.

Two CI details worth copying:

- **Coverage and binary size are published to the run summary**, not just logged. The `checks` job
  log runs past two thousand lines; a number buried in there is a number nobody reads, and the whole
  point of measuring is to look.
- **`testLogging.exceptionFormat = FULL` at the root.** Before that, a test failing in CI reported an
  exception type and a line with no message and no cause. Finding debt D18 required fixing *that*
  first — a real cost of poor error reporting, paid in an area nobody thinks of as a feature.

## What the harness does not cover

That the app starts, renders, and reads a code. That needs a device, and it always will. This project
has had one real boot; it found a defect months old that CI never saw. Nothing in this directory
replaces putting the app on a phone — it just makes sure that when someone does, the defects they
find are the interesting ones.
