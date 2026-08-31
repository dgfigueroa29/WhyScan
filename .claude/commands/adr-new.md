---
description: Draft an architecture decision record from the template, numbered and indexed
argument-hint: [what was decided, in one line]
allowed-tools: Bash(ls docs/adr:*), Read, Write, Edit, Grep, Glob
---

Write a new ADR for: **$ARGUMENTS**

## Before writing

1. `ls docs/adr/` — the next number is the highest existing one plus one, zero-padded to four
   digits. Never reuse a number, even for an ADR that was abandoned.
2. Read [`docs/adr/TEMPLATE.md`](../../docs/adr/TEMPLATE.md) and at least two recent ADRs
   (`ADR-0014`, `ADR-0015`) to match the voice.
3. Check whether an existing ADR already covers this. If one does and this changes it, the answer is
   a **new ADR that supersedes it**, plus a `Superseded by` line in the old one. Never rewrite the
   old one's reasoning.

## Rules that make an ADR worth keeping

**ADRs are in Spanish**, like the rest of `docs/`. File name:
`ADR-NNNN-titulo-en-minusculas-con-guiones.md`.

- The title states the **decision**, not the topic. "Probar un motor abre un diálogo a pantalla
  completa, no un destino nuevo" — not "Sobre el diálogo de prueba".
- **Contexto** describes the forces, including the constraint that made the obvious option
  impossible. If there was no tension, there was no decision and this does not need an ADR.
- **Decisión** is written in the present indicative, as a rule that now holds.
- **Consecuencias** must include at least one cost. An ADR with only benefits is advertising, and it
  is the section future readers actually need.
- **Alternativas descartadas** is a table: the alternative, and the specific reason it lost. "Peor"
  is not a reason. Discarded options with no reason recorded get re-proposed every six months.
- Prefer the concrete defect over the abstract principle: *what broke* is what makes the record
  useful later.

## After writing

- Add the row to the table in [`docs/adr/README.md`](../../docs/adr/README.md) — `tools/checks.py`
  verifies every ADR is indexed and every indexed ADR exists.
- Link it from wherever it is now binding: the SDD section, `docs/ENGINES.md`, the ROADMAP round, or
  the KDoc of the type it governs.
- Run `python3 tools/checks.py`.
