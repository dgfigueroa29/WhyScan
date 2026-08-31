# Planning with an agent

How work gets decided here, and why `docs/ROADMAP.md` reads the way it does.

## Three horizons, three documents

| Horizon | Document | Unit |
|---|---|---|
| The whole migration | `docs/ROADMAP.md` — Phases 1–6 | A phase, with an exit criterion |
| A few days of work | `docs/ROADMAP.md` — Rounds | A round, named after what it fixed |
| One change | `openspec/changes/<id>/` | A proposal with tasks |

Phases were planned once, up front, and have barely moved. Rounds are planned one at a time, because
**the interesting work is almost never what was planned** — it is what the previous round uncovered.
Rounds 9 to 14 are an audit by pillar (security and privacy, accessibility, internationalisation,
resilience, verification, performance), and each of them found something that was not on any list.

## Why the ROADMAP records defects, not just progress

The unusual thing about this roadmap is that it names what broke, in the round where it broke, with
the reason nobody saw it. Round 20 is titled "the camera you could not get out of". Round 16 records
that `allowBackup` was `true` while the Settings screen told the user the opposite.

That is not confession for its own sake. It is the highest-value context an agent can be given,
because **an agent has no memory between sessions**: the only thing standing between it and the same
mistake is a written record of the previous one. Every entry in the "Conventions that have already
cost something" section of `AGENTS.md` traces back to a ROADMAP round.

It also disciplines the planning. A round that only lists wins is a round that did not look hard
enough.

## The four buckets

The ROADMAP's "Por dónde seguir" section sorts pending work into four buckets, and this is the part
worth copying:

1. **Can be done now, with nothing to wait for.**
2. **Blocked by a number only CI can produce** — for example the binary-size baseline, which has to
   be recorded from an artefact CI built, because nothing compiles locally.
3. **Needs an Android device.** Touch targets, `enableEdgeToEdge` seen with human eyes, cold-start
   measurement.
4. **Out of scope by decision, not by oversight** — with the decision linked.

The point of bucket 3 is that it exists at all. Work blocked on hardware, dragged from round to
round, starts to look like work nobody is doing. Naming the blocker once, in its own list, is more
honest and stops it from distorting the plan.

Bucket 4 is what keeps an agent from helpfully re-proposing something already decided against. iOS
lives near there: **deprioritized, not abandoned**, and the reason is written down — without Apple
hardware nothing on that platform can be tested, and linking the framework proves only that
Kotlin/Native compiles.

## How a round gets planned with an agent

1. **Ask for the audit, not the fix.** "Audit accessibility against RNF-05 and report what fails"
   produces a round's worth of real work. "Improve accessibility" produces a plausible diff.
2. **Make it argue against itself.** The `exigente` posture — or simply asking "what in this plan is
   wrong, and what did you assume?" — is worth more than another planning pass. An agent asked to
   critique its own plan will find real problems; an agent asked to extend it will find work.
3. **Split by what can be verified.** Anything provable on the JVM goes in the round. Anything
   needing a device goes to bucket 3, immediately, before it gets started and abandoned.
4. **Write the exit criterion in user terms.** Phase 6's is: *someone who does not know what a
   scanning engine is opens the app, reads a code and shares it, without ever seeing the word
   "engine".* An exit criterion stated in components is one you can satisfy without shipping
   anything.
5. **Turn each item into an OpenSpec change** if it alters behaviour. Otherwise it is just a task.

## What planning with an agent gets wrong

Three failure modes seen repeatedly in this project, and the counter for each:

**It plans what is easy to do rather than what matters.** An agent offered a list will gravitate to
the item it can complete without external input. That is exactly how iOS work kept surfacing:
possible without a device, and therefore attractive, and therefore explicitly forbidden in
`AGENTS.md` — *being able to do it does not make it a priority*.

**It over-decomposes.** Twelve tasks where three would do reads as thoroughness and costs review
time. The test is whether each task is a coherent commit.

**It plans in components rather than in outcomes.** "Add a ViewModel for X" is not a plan item; it
is an implementation detail predicted in advance, and predicting them in advance is how a plan
becomes wrong on contact with the code.

## The measurement rule

Two rounds in this project's history are the same lesson from different angles, and together they
form the rule that governs planning here.

**Round 5** promised a coverage floor in SDD §13.1 that nothing measured. Fifty-one test files, and
nobody knew the number. When it was finally measured, `:core:data` came in at 60.8 % — and the cause
was **dead code**, two classes no Koin module had declared since Room replaced them. The right answer
was deleting them, not writing tests for them.

**Round 13** measured the three feature modules **with no floor at all**, deliberately. They already
had tests, so the problem was never a missing safety net; it was that nobody knew the number.
Inventing a threshold before the first measurement has two endings and both are bad: it breaks CI on
day one, or it is set so low it demands nothing.

So: **measure first, set the floor with the number in front of you.** A goal nobody measures does not
tell you when it is missed — it is missed silently, which is how SDD §13.1 was untrue for months
while every check was green.
