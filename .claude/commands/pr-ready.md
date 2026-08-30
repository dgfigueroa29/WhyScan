---
description: Walk the AGENTS.md definition of done over the current diff before pushing
allowed-tools: Bash(git diff:*), Bash(git status:*), Bash(git log:*), Bash(python3 tools/checks.py), Read, Grep, Glob
---

Audit the current branch against the definition of done in `AGENTS.md`. Report, do not fix — unless
a finding is a one-line omission (a missing ROADMAP checkbox, a stale link), in which case fix it and
say you did.

Start from `git status --short` and `git diff main...HEAD`.

## Walk these in order

1. **`python3 tools/checks.py` reports no findings.** Run it. If it fails, stop here: nothing else
   matters until it passes.
2. **Smallest change.** Is anything in the diff not required by the request? Drive-by refactors,
   reformatting, renamed things nobody asked to rename — name them.
3. **Module boundaries.** No engine module may depend on a feature or on `:core:domain`. Adding an
   engine must not touch `:feature:scanner`. If it did, the SPI came up short and that is its own
   decision, with its own ADR.
4. **Test that runs on every PR.** New or changed behaviour needs a JVM test — no device, no
   emulator. If the diff adds an instrumented test, that is a hard stop (debt D6).
5. **`docs/ROADMAP.md`.** A behaviour change that never reaches the ROADMAP is half done. Look for
   the round or checkbox this belongs to.
6. **`docs/SDD.md`** if the design moved, **`docs/ENGINES.md`** if the catalog did. `ENGINES.md` and
   `ScannerEngineCatalog` cannot diverge — `check_engine_catalog()` compares them, so a mismatch fails `Verify`.
7. **ADR.** Was a decision made between real alternatives, with consequences? Then it needs one.
   Was an existing ADR edited? That is a hard stop — supersede, never rewrite.
8. **OpenSpec.** If the work started from `openspec/changes/<id>/`, is every task ticked, and is the
   change ready to fold into `openspec/specs/` and archive? Use `/spec-apply`.
9. **Hard stops.** Grep the diff for: `INTERNET`, `allowBackup`, `dataExtractionRules`, any HTTP
   client, analytics, crash reporting, an emulator step in a workflow, iOS work that was not forced
   by a shared-code change.
10. **Honest reporting.** Draft the two lines the PR body must contain: what was verified here, and
    what only `Verify` can confirm.

## Output

A checklist with ✅ / ⚠️ / ❌ per item, then the single most important thing to fix before pushing.
If everything passes, say so without hedging.
