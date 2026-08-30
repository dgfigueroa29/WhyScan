# How WhyScan is built with AI

Most of this repository was written by an AI agent working under a human's direction. That is not a
disclaimer at the bottom of the page — it is the reason the repository is shaped the way it is, and
this directory documents the operating model that makes it work.

The claim being made here is narrow and testable: **an agent produces good software when the
environment makes bad work visible cheaply.** Not when the model is clever, not when the prompt is
long. This project is an unusually harsh test of that claim, because the development environment
**cannot compile anything** — no network to `dl.google.com`, so no Gradle task runs. Every safety net
had to be built deliberately.

| Document | Question it answers |
|---|---|
| [`workflow.md`](workflow.md) | The loop: how a request becomes a merged, documented change |
| [`harness.md`](harness.md) | The tooling: checks, hooks, commands, subagents, skills, CI |
| [`planning.md`](planning.md) | How work is planned with an agent — and why the ROADMAP reads as it does |
| [`prompt-library.md`](prompt-library.md) | Prompts that reliably produce good work here, and why |
| [`provenance.md`](provenance.md) | Who wrote what, how it is disclosed, and who is accountable |
| [`state-of-the-art.md`](state-of-the-art.md) | Where this setup actually stands, and the four gaps that are open |

## The four ideas the rest of this directory elaborates

### 1. The feedback loop is the product

Without a compiler, the shortest path from "I edited a file" to "I know it was wrong" was a full CI
round trip: five to fifteen minutes. `tools/checks.py` shortened it to seconds for a specific set of
mistakes, and it earned every one of those checks — each was written **after** CI rejected that exact
case. It lives in version control, so CI runs it too, which is what stops it from drifting out of
sync with what detekt actually enforces (debt D23).

A `PostToolUse` hook now runs it after every source edit. The agent gets told about a broken resource
catalog before it has moved on to the next file.

### 2. Truth is checked, not asserted

`docs/ENGINES.md` and `ScannerEngineCatalog` cannot diverge, because `check_engine_catalog()` compares identifier, phase and platforms between them. The
privacy guarantee is checked in the manifest, not trusted in the code. Coverage floors are measured,
not promised. The ADR index is verified against the files.

The pattern generalises: **when a rule can be checked mechanically, checking it beats writing it
down.** A documented rule depends on someone remembering; a check does not. This applies to agents
exactly as it applies to people, with one difference — an agent will follow a written rule more
consistently and forget it more completely between sessions, so the checked version is worth even
more.

### 3. Saying what you did not verify is a first-class output

The most expensive failure available to an agent in this repository is not a bug. It is reporting
that something works when it was never run. A bug gets found; a false verification claim spends the
trust that makes every other report useful.

So the contract is explicit about three levels — executed here, established by reading, decided by
`Verify` — and every pull request states which claims fall where. `AGENTS.md` requires it, the
`whyscan-verification` skill teaches it, and the pull request template asks for it.

### 4. Decisions get written down while they are still known

Seventeen ADRs, a design document, a roadmap that records defects alongside progress, and an
OpenSpec directory that states what must become true before the code is written. This is more
documentation than a project this size usually carries, and it is deliberate: an agent has no memory
between sessions, so **written context is the only context**. The documentation is not overhead
around the work — for an agent, it *is* the working memory.

The side effect is that a human reading this repository in a year gets the same benefit.

## What this is not

It is not a claim that the agent worked unsupervised. Every decision here was directed by the
project owner, several were reversed by them, and the most valuable defects in the project's history
were found by a human putting the app on a real phone — something no amount of CI could substitute
for. See [`provenance.md`](provenance.md).

It is also not a claim that this is finished. The ROADMAP names what is still missing, including the
things that only a device can answer.
