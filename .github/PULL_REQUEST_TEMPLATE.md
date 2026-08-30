<!--
Fill in every section. The last one is the one reviewers read first.
Full contract: AGENTS.md · Human guide: CONTRIBUTING.md / CONTRIBUTING.es.md
-->

## What changes / Qué cambia

<!-- One paragraph. What is different for a user, or for the next person to read this code. -->

## Why / Por qué

<!--
The problem, with evidence. Link the issue, the ROADMAP round, the OpenSpec change
(`openspec/changes/<id>/`) or the ADR that requires this.
-->

## Verification / Verificación

**This is the section that matters most.** Say what you actually did, in three levels.

- **Executed here / Ejecutado aquí:**
  <!-- e.g. `python3 tools/checks.py` — no findings. `./gradlew detekt jvmTest` — passed. -->
- **Established by reading / Comprobado leyendo:**
  <!-- Structural claims you verified by reading the source, not by running anything. -->
- **Not run here — `Verify` decides / No ejecutado aquí — lo decide `Verify`:**
  <!-- Compilation, detekt, unit tests, Android lint, R8, Desktop, Web — whichever you did not run. -->
- **Not covered by anything / Sin cubrir:**
  <!-- Typically: that the app starts and reads a code. That needs a device. -->

> Claiming a check passed when you did not run it is the one thing that gets a pull request closed
> rather than reviewed. "Not run here" is always an acceptable answer.

## Checklist

- [ ] `python3 tools/checks.py` reports no findings
- [ ] The diff is the smallest change that does the job — nothing widened on my own
- [ ] Module boundaries respected (no engine depending on a feature or on `:core:domain`)
- [ ] New or changed behaviour has a test that runs on **every** pull request (JVM, no device)
- [ ] `docs/ROADMAP.md` reflects the new state
- [ ] `docs/SDD.md` updated if the design moved; `docs/ENGINES.md` if the catalog did
- [ ] An ADR was added if a decision was made — and no existing ADR was rewritten
- [ ] The OpenSpec change, if there was one, is applied and archived
- [ ] Nothing here adds network access, analytics, backup of app data, or an instrumented test
- [ ] No unrequested iOS work (iOS is deprioritized — see `AGENTS.md`)

## Notes for the reviewer / Notas para quien revisa

<!-- Anything you are unsure about, deliberately left out, or would like a second opinion on. -->
