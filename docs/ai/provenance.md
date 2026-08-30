# Provenance and accountability

Who wrote what, how it is disclosed, and who answers for it.

## The short version

Most of the code, and most of the documentation, was written by an AI agent working under the
direction of the project owner, who reviews and accepts every change. **The owner is accountable for
all of it.** "The agent wrote it" is an explanation of how a defect got in, never an excuse for it
being there.

## Why disclose at all

Three reasons, in order of weight:

1. **It is true, and this is a public repository.** Someone evaluating this code — as a contributor,
   an employer, or a user deciding whether to trust an app with camera access — is entitled to know
   how it was produced.
2. **It changes how the code should be read.** A codebase with this density of documentation and
   this many recorded defects is unusual, and the reason is that an agent has no memory between
   sessions. Knowing that explains the shape.
3. **Hiding it would contradict the project's own standard.** This repository fails a change for
   claiming verification it did not perform. Concealing its own authorship would be the same
   category of dishonesty, at a larger scale.

## How it is disclosed

- **Here**, and in [`docs/ai/README.md`](README.md), in full.
- **In the commit history.** Commits authored in an agent session carry a `Co-Authored-By` trailer.
  The history is the record; it is not rewritten to tidy this up.
- **In the ROADMAP**, implicitly but usefully: rounds name the defects that were introduced and the
  reason nobody saw them, including the ones an agent introduced.

What is deliberately **not** done: no per-file banners, no comment headers announcing authorship, no
badge. They add noise to every file to state something that belongs in one place, and they age badly
as files are edited by different hands.

## What the human did that the agent could not

This matters more than the disclosure, because it is the honest limit of the claim being made.

**Ran the app.** This project has had one real device boot, and it found a defect months old that
CI never saw: a Koin registration with the wrong type (debt D18). No amount of static analysis
substitutes for putting the app on a phone. The camera-trap bug of Round 20 — grant the permission,
open the camera, and then be unable to leave the screen by any means — was also reported from a
phone, by a person.

**Decided what mattered.** iOS is deprioritized, Play Feature Delivery is postponed, there will be
no instrumented tests. Each is a judgement about cost and value that an agent, given the option,
consistently gets wrong in the same direction: toward whatever it can complete without external
input.

**Rejected work.** Including work that was correct but not wanted. That does not appear in the
history, and it is a large part of why the history looks coherent.

**Set the standard.** The rule that anything checked must run on every pull request, the refusal to
claim unverified work, the requirement that documentation be source of truth — these are the
constraints that make the rest work, and they came from the owner.

## What the agent did that a person would not have

Stated fairly, in both directions:

- **Wrote the documentation as it went**, at a level of detail few humans sustain. Seventeen ADRs and
  a design document that stays current exist because the agent needed them as working memory. The
  side effect is a repository a stranger can understand.
- **Built the checks after each CI rejection.** `tools/checks.py` grew one rule at a time, each after
  a specific failure. A person would more likely have remembered the rule than automated it.
- **Was consistent about conventions** — import order, comment style, module boundaries — across
  hundreds of files, in a way that is genuinely tedious for a person.
- **And needed to be stopped**, repeatedly, from working on the deprioritized platform, from widening
  a change beyond what was asked, and from reporting verification it had not performed. Those are the
  three rules that appear most emphatically in `AGENTS.md`, and they are there because each was
  broken.

## Licensing and third-party code

The project is licensed under the file at [`LICENSE`](../../LICENSE). AI-generated contributions are
covered by the same terms, and the project owner holds the same responsibility for them as for
hand-written code — including the responsibility not to introduce code whose licence conflicts.

Where a dependency does substantive work, it is named: the engine catalog in `docs/ENGINES.md` lists
each engine's third-party dependency, and `docs/legal/privacidad.md` discloses the one engine that
hands frames to a component outside the application.

## If you are evaluating this repository

The interesting question is not "did an AI write this". It is **what was put in place so that AI
work could be trusted** — and the answer is in [`harness.md`](harness.md): a check that runs in
seconds, tests that compare documentation against code, a CI pipeline that is the sole authority, and
a written rule that unverified claims are the worst available failure.

Then read `docs/ROADMAP.md` for what is still missing. That list is honest, and it is the strongest
evidence for everything else on this page.
