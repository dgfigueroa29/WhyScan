---
name: whyscan-adr
description: How architecture decision records work in WhyScan — when one is required, how to write it, and how to supersede an old one. Use when a decision is being made between real alternatives, when writing or reviewing a file under docs/adr/, or when a change contradicts a decision already recorded.
---

# Architecture decision records in WhyScan

There are sixteen of them, in Spanish, under `docs/adr/`, and they are the reason someone reading
this repository in a year will understand *why* rather than only *what*. They are not design
documents — `docs/SDD.md` is that — and they are not a changelog. They record a decision that had a
real alternative, and the cost that was accepted.

**ADRs are never rewritten to agree with today.** Superseding one means writing a new ADR and adding
a `Superseded by` line to the old one. Editing the old reasoning destroys the only thing it was for.

## When one is required

Write an ADR when **all three** hold:

1. There was a genuine alternative — something a competent person would have chosen instead.
2. The decision constrains future work: it is a rule, not an implementation detail.
3. It has a cost worth naming.

Do not write one for: a bug fix, a rename, adopting the obvious library, or a change nobody could
reasonably have made differently. An ADR for a non-decision dilutes the fifteen that matter.

Signals you *are* looking at one: two options each with real merit; a constraint that made the
obvious choice impossible; a defect that forced a structural change; "we deliberately did not do X".

## Format

File name: `ADR-NNNN-titulo-en-minusculas-con-guiones.md`, the number being the next unused one.
Numbers are never reused, not even for an abandoned ADR. Start from `docs/adr/TEMPLATE.md`.

```
# ADR-NNNN — <the decision, as a sentence>

- **Estado:** Aceptada | Propuesta | Superada por ADR-MMMM
- **Fecha:** YYYY-MM-DD

## Contexto
## Decisión
## Consecuencias
## Alternativas descartadas
```

`tools/checks.py` verifies the header fields exist and that the ADR is listed in
`docs/adr/README.md`.

## What makes them good here

Read `ADR-0014` and `ADR-0015` for the voice. The pattern that recurs:

- **The title states the decision, not the topic.** "Probar un motor abre un diálogo a pantalla
  completa, no un destino nuevo" tells you the outcome before you read a word of the body.
- **Contexto names the constraint that closed the obvious door.** ADR-0015's real content is that
  the scan screen lives inside the app's `Scaffold` and therefore *cannot* go full-screen. Without
  that, the decision looks arbitrary.
- **Decisión is present indicative.** A rule that holds now, not a plan.
- **Consecuencias includes at least one cost.** ADR-0015 admits that any feature can now paint over
  the whole app and only judgement prevents it. An ADR with only benefits is advertising.
- **Alternativas descartadas is a table with a specific reason per row.** "Worse" is not a reason.
  Options discarded without a recorded reason get proposed again every six months.
- **The concrete defect beats the abstract principle.** ADR-0003 is memorable because the app died on
  a real device with CI green; the principle alone would not have been.

## After writing

1. Add the row to the table in `docs/adr/README.md`.
2. Link it from wherever it now binds: the SDD section, `docs/ENGINES.md`, the ROADMAP round, or the
   KDoc of the type it governs. An unlinked ADR is one nobody will find at the moment it matters.
3. If it supersedes another, edit **only** the old one's `Estado` line to
   `Superada por ADR-MMMM`. Nothing else in it changes.
4. Run `python3 tools/checks.py`.
