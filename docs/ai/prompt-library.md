# Prompt library

Prompts that reliably produce good work in this repository, with the reason each one is shaped the
way it is. They are here rather than in someone's notes for the same reason `tools/checks.py` is:
anything kept outside version control is rebuilt from scratch every session and drifts.

Most recurring work already has a slash command — `/preflight`, `/pr-ready`, `/adr-new`,
`/spec-propose`, `/spec-apply`, `/docs-sync`, `/engine-add`. **Prefer the command.** What follows is
for the work that is not yet a command, and for understanding *why* the commands are worded as they
are.

## The four shapes that work here

### 1. Audit, not improve

> Audit `:feature:history` against RNF-05 (accessibility) and report what fails, ranked by user
> impact. Do not fix anything yet. For each finding, say how it would be verified — and if that
> needs a device, say so.

"Improve accessibility" produces a plausible diff. "Audit against RNF-05 and report what fails"
produces a round's worth of real work, and the verification clause stops the report from including
things nobody can check. This shape produced rounds 9 through 13.

### 2. Name the constraint before the task

> Nothing compiles in this environment. Working only from reading the source, tell me whether the
> Android `platformModule` registers every type `App()` resolves — and which of those you can prove
> and which you are inferring.

An agent that has not been told a constraint will assume the normal case and report accordingly. The
"which you can prove" clause is what turns a confident wrong answer into a useful uncertain one.

### 3. Ask for the failure scenario, not the opinion

> For each finding, give the concrete scenario: what inputs or state, and what goes wrong. If you
> cannot construct one, drop the finding.

This single clause removes most of the noise from an AI review. A finding that cannot be attached to
a failure is usually a style preference wearing a bug's clothes.

### 4. Make it argue against itself

> Before implementing: what in this plan is wrong? What did you assume that I did not tell you? What
> would a reviewer who dislikes this approach say first?

Worth more than another planning pass. An agent asked to extend a plan finds work; an agent asked to
attack one finds problems.

## Prompts for specific recurring work

### Investigating a defect found on a real device

> This was reported from a phone: <symptom>. The app boots and CI is green, so this is in the gap
> CI cannot see. Work backwards from the symptom through the layers — UI, ViewModel, domain,
> DI graph, platform module — and tell me which layer it must be in before you tell me the fix.
> Check the Koin registration types explicitly: exact-type resolution has hidden this class of
> defect here before.

The layer-first ordering matters. Asked for a fix, an agent produces one for the first plausible
cause; asked to localise first, it eliminates.

### Deciding whether something needs an ADR

> Does <decision> need an ADR? Test it against three conditions: was there a real alternative, does
> it constrain future work, and does it have a cost worth naming? If any is no, say no — an ADR for
> a non-decision dilutes the ones that matter.

Agents write ADRs eagerly. The negative case has to be made easy to reach.

### Reviewing a diff you just wrote

Don't. Use the `kotlin-reviewer` subagent, which gets a fresh context window. An agent reviewing its
own work in the same conversation is defending a position, and it will find the problems it already
knows about rather than the ones it does not.

### Writing the verification section of a pull request

> Write the verification section. Three levels, explicitly: what you executed here, what you
> established by reading, and what only `Verify` can decide. If a claim does not fit one of the
> three, remove it.

The forced trichotomy is what prevents "tested and working" from appearing after a session in which
nothing was executed.

### Closing a documentation gap

> `docs/<file>` claims <X>. Check that against the code and tell me which one is wrong. If the
> document is right and the code regressed, say that — do not update the document to match the code.

The default assumption runs the other way, and it is often wrong here: the documentation is the
source of truth, and a mismatch can mean the code drifted.

## Anti-patterns

| Prompt | What you get |
|---|---|
| "Make this better" | A refactor nobody asked for, spread across files nobody wanted touched |
| "Fix all the warnings" | Suppressions. Debt D19 was a warning printed in every build for months; the fix was reading it, not silencing it |
| "Add tests to raise coverage" | Tests over dead code. When `:core:data` measured 60.8 %, the right answer was deleting two unused classes, not testing them |
| "Is this correct?" | Agreement. Ask what would have to be true for it to be wrong |
| "Do whatever you think is best for iOS" | Work on a deprioritized platform that nobody can test |

## One rule that matters more than any prompt

**Tell it what it cannot do, and what it must not claim.** Every prompt in this file that works does
so because it fences the answer: no fixing yet, no unverifiable findings, no claim without a level.
Capability is rarely the limiting factor here. Knowing the edges of what has actually been
established is.
