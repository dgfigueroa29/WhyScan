---
description: Run the offline checks and interpret the findings against project rules
allowed-tools: Bash(python3 tools/checks.py), Read, Grep, Glob, Edit
---

Run `python3 tools/checks.py` and act on the result.

This is the only verification that actually executes in this environment. Gradle does not run here —
the sandbox cannot reach `dl.google.com` or `api.foojay.io` — so treat this as a fast pre-filter for
`Verify`, never as proof that the build is green.

## What to do with the output

- **No findings.** Say so, and say plainly what remains unverified: compilation, detekt, unit tests,
  lint and R8 are decided by `Verify` on the pull request.
- **Findings.** Fix them at the source, then re-run. Do not silence a check to make it pass.

## Reading specific findings

| Finding | What it usually means |
|---|---|
| `falta la cadena 'X', que sí está en inglés` | A string was added to one catalog only. The unqualified `values/` catalog is the fallback for **every** language, so a Spanish-only key breaks all other locales |
| `se usa Res.string.X sin importarla` | Compose resources are imported one key at a time, per module |
| `clave huérfana 'X'` | Dead text that someone will eventually translate. Delete it |
| `orden de imports` | ktlint's order is not alphabetical: everything else first, then `java.**`, `javax.**`, `kotlin.**`, then aliased imports |
| `import sin usar` | Check it is not an operator convention (`by`, `a[b] = c`, `a(b)`) before deleting — that false positive cost fifteen bogus findings once |
| `package X no coincide con la ruta` | Left behind by a half-finished rename; it makes path-based search lie |
| `la etiqueta 'X' de un return@` | A mechanical replacement left a label pointing at a lambda that no longer exists. This is a compile error |
| anything from `check_privacy_guarantee` | Stop. The privacy guarantee is a product promise stated in the README, in Settings and in the manifest. Do not "fix" it by relaxing the check |

Finish by reporting, in one line each: what ran, what it found, and what only `Verify` can confirm.
