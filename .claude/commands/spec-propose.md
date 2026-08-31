---
description: Open an OpenSpec change — proposal, tasks and spec delta — before writing code
argument-hint: [what should change, in one line]
allowed-tools: Bash(ls openspec:*), Read, Write, Edit, Grep, Glob
---

Propose a change for: **$ARGUMENTS**

Read [`openspec/AGENTS.md`](../../openspec/AGENTS.md) first — it holds the exact delta syntax and
the validation rules. What follows is the procedure.

## 1. Decide whether this needs a change at all

A change proposal is required when the work alters **observable behaviour**, adds or removes a
capability, or crosses a module boundary. It is not required for a local bug fix, a rename, a doc
correction or a test that covers existing behaviour. Proposing a change for a one-line fix is
bureaucracy; skipping one for a new capability is how specs go stale.

If it does not need a change, say so and get on with the work.

## 2. Read the current truth

`openspec/specs/<capability>/spec.md` is what the system does **today**. If the capability has no
spec yet, that is itself worth saying: the change then adds one.

Cross-read the design in `docs/SDD.md` and any ADR it references. The proposal must not contradict a
standing decision — if it does, it needs an ADR that supersedes that decision, and you should say so
before writing anything else.

## 3. Write the change

Create `openspec/changes/<change-id>/`, where `<change-id>` is a short kebab-case verb phrase
(`verify-android-database-builder`, not `db-fix`).

- **`proposal.md`** — Why (the problem, with evidence), What changes, What does not change, and
  Verification: name exactly which of these prove it, and admit what cannot be proven here.
- **`tasks.md`** — Ordered, checkable, small enough that each one is a coherent commit.
- **`design.md`** — Only when there is a real trade-off. If you write one and it turns out to record
  a decision with consequences, it wants to become an ADR instead.
- **`specs/<capability>/spec.md`** — the delta, using `## ADDED Requirements`,
  `## MODIFIED Requirements` or `## REMOVED Requirements`. Every `### Requirement:` needs at least
  one `#### Scenario:`; `tools/checks.py` enforces that.

## 4. Requirements are testable or they are wishes

Write `SHALL` statements about observable behaviour. Each scenario is `WHEN` / `THEN`, concrete
enough that a reader can tell whether it holds. "The scanner SHALL be fast" is not a requirement.
"The domain SHALL suppress a repeated `(format, value)` pair detected within two seconds" is.

## 5. Stop

Do not implement. Present the proposal, and let the project owner accept it. If they ask you to
continue in the same turn, that is your acceptance — start from task 1 of `tasks.md`.
