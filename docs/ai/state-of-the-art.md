# Where this repository actually stands

An honest assessment of the AI engineering setup here, written to be uncomfortable rather than
flattering. It exists because "are we state of the art?" is a question worth answering with a list
rather than a feeling — and because the answer changes, so it needs somewhere to be revised.

**Short version: the setup is solid current practice, well executed and — unusually — enforced. It
is not frontier, and the gap between those two is measured below.**

## Present, and current practice

| Piece | Where | Note |
|---|---|---|
| Cross-tool agent contract | `AGENTS.md` | Now the common convention. Ours is normative and mirrored in Spanish (ADR-0016) |
| Project skills | `.claude/skills/` | Three, procedural, loaded on demand |
| Subagents with isolated context | `.claude/agents/` | Three. The isolation is the point: a reviewer that did not write the diff |
| Slash commands | `.claude/commands/` | Seven, each carrying the reasons, not just the steps |
| Edit-time hook | `.claude/hooks/preflight.sh` | Runs the offline checks after every source edit |
| Spec-driven change | `openspec/` | Proposals before code (ADR-0017) |
| Decision records | `docs/adr/` | Eighteen, with costs stated |
| Provenance disclosure | `docs/ai/provenance.md` | Including what the agent could not do |

None of this is novel. All of it is what a well-run repository looks like today, and most
repositories that have some of it do not have the rest.

## Uncommon, and worth naming

Three things here are rarer than the file layout, and they are what actually make the setup work:

**The structure is enforced by a script that runs in CI.** `tools/checks.py` validates ADR headers
and index parity, the `AGENTS.md` ↔ `CLAUDE.md` cross-link, the shape of every OpenSpec change —
including that every requirement carries a scenario — the light/dark parity of every colour role,
and every relative link between documents. Most repositories with an `AGENTS.md` have a document.
This one has a document plus something that fails the build when the document stops being true.

**Unverified claims are treated as the worst available failure.** Three levels of certainty —
executed here, established by reading, decided by CI — are required in every pull request, taught by
a skill, and asked for by the template. In an environment where nothing compiles locally, this is
the difference between a report and a guess.

**Defects are recorded where they happened.** The roadmap names what broke, in the round where it
broke, with the reason nobody saw it. That is the highest-value context an agent can be handed,
because an agent has no memory between sessions.

## Missing, ranked by what it would actually buy

### 1. Nothing measures whether any of this works

`docs/ai/README.md` states a thesis — *an agent produces good software when the environment makes
bad work visible cheaply* — and this repository contains **zero evidence for it**. The skills have
no evaluation suite: nothing checks that `whyscan-verification` triggers when it should, or that
`kotlin-reviewer` finds the defect classes it claims to. The commands have never been run against a
fixture to see whether they produce the same result twice.

This is the largest gap, and it is the one that separates "we wrote a harness" from "we know the
harness helps". Tooling for skill evaluation exists; a first suite would be three or four fixtures
per skill.

### 2. The harness only helps someone running an agent locally

`Verify` runs detekt, tests and builds. Nothing in CI reviews a diff the way `kotlin-reviewer` does.
A contributor who does not run an agent gets none of the benefit, and neither does a pull request
opened at midnight. An automated review at pull-request time would close that loop — and would be
the first thing here that makes the harness useful to someone other than its author.

### 3. The specs were written by reading, not by verifying

The 30 requirements in `openspec/specs/` were derived from the source in a single pass. That is a
reasonable way to start and a bad place to stop: a requirement that is wrong is a source of truth
that lies, which is worse than having no specs. An audit is the correction, and it needs to be
repeatable — the `docs-auditor` subagent exists for exactly this, and running it should become
routine rather than a one-off.

### 4. One consumer is not federation

`:core:foundation` (ADR-0018) will be publishable, documented and API-checked, and until a second
application actually integrates it, "reusable" remains a design opinion. The `samples/` module is a
cheap witness, not a substitute. This is stated in the change's own verification table because the
temptation to declare victory at publication is strong.

## Deliberately absent, and staying that way

Not every current technique belongs here. Naming the ones we reject is part of the assessment:

- **No MCP server.** MCP connects an agent to external services. This application has no network by
  design and talks to nothing. Adding one would be adopting a technology to have adopted it.
- **No multi-agent orchestration.** Three focused subagents at one repository's scale is right.
  Fan-out architectures pay off across many parallel workstreams; here they would add coordination
  cost and non-determinism to a codebase one person reviews.
- **No autonomous merging.** Every change is reviewed by the project owner. The two most expensive
  defects in this project's history — a Koin type registered wrong, and a camera screen with no way
  out — both passed CI green. Removing the human from that loop would remove the only step that
  caught them.
- **No generated code without the verification discipline.** The rule that anything checked must run
  on every pull request is what makes AI-written code trustworthy here. A faster pipeline that
  weakened it would be a worse pipeline.

## The thing this page cannot say

Being current is not the goal. A repository can hold every technique on this list and still ship an
application that crashes on the first phone it meets — which is roughly what happened here in
August 2026, with CI green throughout.

The goal is that the work is **trustworthy**: that what the documentation says is true, that what a
pull request claims was verified actually was, and that the parts nobody can verify are named
instead of glossed. Measured against that, the honest score is good and improving, with the four
gaps above open.

What is still blocked, and on what, lives in `docs/ROADMAP.md` — including the items that need a
device and the ones waiting on a decision that is not technical. It is not repeated here: the same
fact written in two places is how debt D20 stayed open for months after the work was done.
