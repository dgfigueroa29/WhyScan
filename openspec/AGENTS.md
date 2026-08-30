# OpenSpec conventions for WhyScan

Spec-driven development: **the change is agreed before the code is written**. An agent that starts
typing from a one-line request produces something plausible; an agent that first writes down what
must become true produces something reviewable. This directory is where that agreement lives.

```
openspec/
├── AGENTS.md          This file — the rules
├── project.md         Project context an agent needs before proposing anything
├── specs/             Current truth: what the system does today, per capability
│   └── <capability>/spec.md
└── changes/           In flight: what should become true, and why
    ├── <change-id>/
    │   ├── proposal.md          Why, what changes, what does not, how it is verified
    │   ├── tasks.md             Ordered, checkable implementation steps
    │   ├── design.md            Optional — only when there is a real trade-off
    │   └── specs/<capability>/spec.md   The delta against the current spec
    └── archive/       Changes already folded into specs/, kept as the record of why
```

The [OpenSpec](https://github.com/Fission-AI/OpenSpec) CLI (`npx openspec validate`) understands this
layout, but this repository does not depend on it: the development environment has no network, so
`tools/checks.py` validates the structure offline and CI runs the same script.

## When a change is required

| Situation | Change proposal? |
|---|---|
| New capability, or a new engine | **Yes** |
| Observable behaviour changes — what the user sees, gets or can do | **Yes** |
| A module boundary moves, or the SPI is extended | **Yes** |
| Persistence schema or export format changes | **Yes** |
| Bug fix restoring documented behaviour | No |
| Rename, refactor, comment, formatting | No |
| A test covering behaviour that already exists | No |
| Documentation correction | No |

Proposing a change for a one-line fix is bureaucracy. Skipping one for a new capability is how a
spec directory becomes decoration.

## Writing a delta

The delta file mirrors the path of the spec it changes:
`changes/<id>/specs/<capability>/spec.md`. It contains only what moves, under exactly one of three
headings:

```markdown
## ADDED Requirements

### Requirement: Manual entry closes every fallback chain

The system SHALL always offer `MANUAL_INPUT` as the last engine of any selection chain.

#### Scenario: No camera engine is available

- **WHEN** a scan is requested on a platform where no camera engine reports `Available`
- **THEN** the selector returns a chain whose only entry is `MANUAL_INPUT`
- **AND** the UI never presents a "cannot scan" state
```

Rules, all enforced by `tools/checks.py`:

- Only `## ADDED Requirements`, `## MODIFIED Requirements`, `## REMOVED Requirements`.
- Every `### Requirement:` has at least one `#### Scenario:`.
- A `MODIFIED` or `REMOVED` heading must match an existing requirement heading in `specs/`
  **character for character**. A near-miss silently creates a duplicate when the change is applied.

## Writing a requirement

- One `SHALL` statement about **observable behaviour**. Not implementation, not intent.
- Scenarios are `WHEN` / `THEN` / optional `AND`, concrete enough that a reader can decide whether
  the requirement holds.
- Unfalsifiable words are defects: fast, robust, user-friendly, properly, appropriately, seamless.
  "The scanner SHALL be fast" says nothing. "The domain SHALL suppress a repeated `(format, value)`
  pair detected within two seconds" is a requirement.

## The verification question

This is where proposals in *this* project go wrong. For every requirement, answer: **what proves it,
and does that proof run on every pull request?**

- A JVM test — good, and name it.
- A device or emulator test — **impossible here** (debt D6, no emulator in CI). Either restate the
  requirement so something on the JVM can prove it, or say plainly that it is blocked on hardware —
  in which case it belongs in the ROADMAP's device-blocked list, not in an in-flight change.
- "Verified by reading" — acceptable for structural claims only, and it must say so.

`proposal.md` must contain that answer. A change whose requirements nothing can check is a wish.

## Lifecycle

1. **Propose** — `/spec-propose`. Create the change. Do not write code.
2. **Review** — the `spec-reviewer` subagent, then the project owner. Verdict: ready, ready with
   fixes, or not ready.
3. **Implement** — work `tasks.md` in order, ticking boxes as they land.
4. **Apply** — `/spec-apply`. Merge each delta into `specs/`, so the spec reads as current truth
   with no `ADDED`/`MODIFIED` headings and no future tense.
5. **Archive** — `git mv openspec/changes/<id> openspec/changes/archive/<id>`. The proposal is kept
   verbatim: it is the record of *why* the spec says what it says.

A change is not done until step 5. `AGENTS.md`'s definition of done says so.

## Relationship to the rest of the documentation

| Directory | Answers |
|---|---|
| `openspec/specs/` | What the system does **now**, as checkable requirements |
| `openspec/changes/` | What should become true, and why — before it is true |
| `docs/SDD.md` | **How** it is built: structure, types, contracts, quality strategy |
| `docs/adr/` | **Why** a structural decision was taken, and what it cost |
| `docs/ROADMAP.md` | **When**: progress, pending work, accepted debt |

A change proposal that contradicts an ADR is blocked until an ADR supersedes it. A change that
merely adds detail to the SDD is not a change proposal — it is an SDD edit.
