# The working loop

How a request becomes a merged, documented change. Five steps; the two that agents skip are the two
that keep this repository coherent.

```
    ┌─ 1. Understand ──────── read SDD § + ADRs + ROADMAP entry
    │
    ├─ 2. Propose ─────────── openspec/changes/<id>/   ← skipped most often
    │                          ↳ spec-reviewer, then the owner accepts
    │
    ├─ 3. Implement ───────── smallest change; hook runs checks.py on every edit
    │
    ├─ 4. Verify ─────────── checks.py + adversarial re-read + kotlin-reviewer
    │
    └─ 5. Document ────────── ROADMAP, SDD, ENGINES, ADR, /spec-apply  ← skipped second most
```

## 1. Understand

The design is written down. Re-deriving it from the code produces a change that is locally sensible
and globally wrong.

Read, in this order: the `docs/SDD.md` section that covers the area, every ADR it references, and the
ROADMAP round where this work belongs. If `openspec/specs/` has a spec for the capability, that is
the current contract.

The failure mode this prevents is real and has a name in this repository: **the guarantee was checked
in the wrong place.** Debt D18 (Koin resolving by exact type), debt D19 (a member shadowing an
extension) and the `allowBackup` defect were all changes that looked correct against the code in
front of them and wrong against the design.

## 2. Propose

Anything that changes observable behaviour, adds a capability, or crosses a module boundary gets an
OpenSpec change **before** the code — proposal, tasks, and a spec delta. See
[`openspec/AGENTS.md`](../../openspec/AGENTS.md), or run `/spec-propose`.

Why this step exists at all: when an agent writes the code, the cheap checkpoint moves earlier. A
one-line request produces something plausible in minutes, and reviewing that costs more than agreeing
beforehand on what had to become true. The review has to happen **while changing your mind is still
free**.

The question that carries the most weight in a proposal is not "what will you build" but **"what
proves it, and does that proof run on every pull request?"** In this project, a device test is not an
answer (debt D6) — the requirement gets restated so the JVM can prove it, or it is declared blocked
on hardware and moved to the ROADMAP.

Skip this step for a bug fix that restores documented behaviour, a rename, a refactor, a test over
existing behaviour, or a documentation correction.

## 3. Implement

Smallest change that satisfies the proposal. Nothing widened on the agent's own initiative.

The `PostToolUse` hook runs `tools/checks.py` after every Kotlin, Gradle or XML edit and feeds
findings straight back. In an environment with no compiler this is the only sub-minute feedback
available, and it catches the specific mistakes that CI would otherwise reject ten minutes later:
resource parity, import order, orphan keys, unresolved labels, `package` versus path.

Boundaries hold during implementation, not after: an engine depends only on `:core:scanner-api`,
`:core:model` and its SDK. If a change to an engine requires touching `:feature:scanner` or
`:core:domain`, **stop** — the SPI came up short, and extending it is a decision with its own ADR.

## 4. Verify

Three passes, in increasing cost:

1. **`python3 tools/checks.py`.** The only thing that executes here.
2. **Adversarial re-read of your own diff.** One question: what would make `Verify` reject this?
   Compilation errors an offline check cannot see, a detekt rule, an unused parameter, a test that
   assumed the old behaviour. Fix what you find before pushing.
3. **`kotlin-reviewer`**, in its own context window. A reviewer that has not read the conversation
   defending the diff gives a more honest answer — that is the whole reason it is a subagent and not
   another turn.
4. **`Verify` itself, after pushing.** The only pass that compiles anything. It is not optional and
   it is not someone else's job: read the run on the pushed head before calling the work finished.

Then write the two sentences the pull request needs: what was executed here, and what only `Verify`
can decide. `/pr-ready` walks the full definition of done.

## 5. Document

A behaviour change that never reaches the documentation is half done — in this repository literally,
because `docs/` is source of truth and two tests enforce parts of it.

- **`docs/ROADMAP.md`** — tick the box, update the round, or name the debt. Both directions of error
  happen; a box ticked for work that is not done is the worse one.
- **`docs/SDD.md`** — if the design moved, in the numbered section that already covers it.
- **`docs/ENGINES.md`** — if the catalog moved. A test compares it to `ScannerEngineCatalog`.
- **An ADR** — if something was *decided* rather than merely implemented. Never edit an existing one
  to agree with today.
- **`/spec-apply`** — fold the delta into `openspec/specs/` and archive the change. A change
  implemented but never applied leaves the specs describing a system that no longer exists.

## What happens when the loop is skipped

Two entries from the ROADMAP, both instructive:

**Debt D20 stayed open for months after the work was finished.** It was tracked in two places, one
of them got ticked and the other did not. The fix was not more diligence; it was noticing that the
same fact was written twice.

**The coverage floor in SDD §13.1 was promised long before anything measured it.** Fifty-one test
files and nobody knew the number. When it was finally measured, `:core:data` came in at 60.8 % — and
the cause was not missing tests but **dead code**: two classes no Koin module had declared since Room
replaced them. The right answer was deleting them, not writing tests for them. A goal nobody measures
does not tell you when it is missed; it is missed silently.

And one from the writing of this very document:

**An agent pushed a round, declared it finished, and `Verify` had been red the whole time.** The
failure was a single assertion it had written itself: a selection test expecting one rejected engine
where the first pass rejects two. Nothing in the loop was wrong except its last step — the agent had
the rule "`Verify` is the authority" and never opened it. Pass 4 above and the last box of the
definition of done exist because of that afternoon. The lesson generalises past this repository:
**a verification step nobody is required to read is decoration**, and an agent will skip it in
exactly the way a human does — not by deciding to, but by feeling done.
