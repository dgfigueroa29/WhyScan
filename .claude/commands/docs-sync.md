---
description: Find divergence between the code and docs/ after a behaviour change
allowed-tools: Bash(git diff:*), Bash(git log:*), Bash(python3 tools/checks.py), Read, Grep, Glob, Edit
---

Audit `docs/` against the code. In this project documentation is **source of truth, not a summary**,
so divergence is a defect, not untidiness.

Scope: the current diff (`git diff main...HEAD`) unless the user named something else.

## Pairs that must agree

| Code | Document | How it is enforced |
|---|---|---|
| `ScannerEngineCatalog` | `docs/ENGINES.md` master table | A test compares IDs and phases — divergence fails `Verify` |
| Declared `ScannerCapabilities` per engine | `docs/ENGINES.md` capability and format matrices | By reading; nothing automated |
| Engine selection policy in `:core:domain` | `docs/ENGINES.md` §"Prioridad de selección automática" | By reading |
| Domain decorators (formats, limits, timeout, distinct detections) | `docs/SDD.md` §7 | By reading |
| Koin modules per platform | `docs/SDD.md` §10 | `KoinGraphTest` / `AndroidKoinGraphTest` prove the graph resolves, not that the doc is right |
| Room entities and migrations | `docs/SDD.md` §11 | A migration test opens a v1 database |
| Manifest permissions and backup flags | `docs/SDD.md` §12, `docs/legal/privacidad.md`, README | `check_privacy_guarantee()` in `tools/checks.py` |
| Coverage floors in CI | `docs/SDD.md` §13.1 | `tools/coverage.py` fails the build below the floor |
| What shipped | `docs/ROADMAP.md` round and checkboxes | By reading |

## Also check

- **ADR references resolve.** Every `ADR-NNNN` mentioned in code, README or docs points at a file
  that exists, and the claim made about it matches what it decided.
- **The README status table** still matches reality. It is the first thing a stranger reads.
- **`docs/legal/`** — the privacy policy and terms are checkable against the code. If the diff
  changed what data is stored, exported or shared, both language versions move together.
- **`openspec/specs/`** — if the diff changed observable behaviour, either a change is in flight
  under `openspec/changes/` or the spec is now stale.

## Output

A table of divergences: document, what it claims, what the code does. Fix the mechanical ones
(a stale ID, a wrong path, an unticked checkbox) and list them as fixed. For anything that needs a
judgement call — a claim that is now arguably false, a design that moved — describe it and ask,
rather than rewriting the owner's prose on your own.
