# Tasks

Ordered smallest-risk first. **Nothing here can be executed in this environment** — every task ends
at `Verify`. Do not batch them: each of the first three will produce a large mechanical diff, and
mixing two of them makes the failure unreadable.

## 0. Unblock

- [ ] **The project owner decides the Maven group and the foundation package name.** Must not
      contain `whyscan`. Everything below is blocked on this; do not invent a placeholder and
      rename later — the rename is the expensive part.

## 1. Draw the line before moving anything

- [ ] Add `explicitApi()` to `:core:designsystem` **first**, while it is still a single module. The
      first compilation will fail on every public declaration lacking an explicit visibility
      modifier or return type; that pass is the actual work of deciding what the API is, and doing
      it before the split means deciding it once instead of twice.
- [ ] For each public declaration, decide: does a consumer need this, or was it public because
      nobody wrote `internal`? Default to `internal`. Widening later is cheap; narrowing is a
      breaking change.
- [ ] `Verify` must be green before the next task starts.

## 2. Split the module

- [ ] Create `:core:foundation` with the same targets as `:core:designsystem`, add it to
      `settings.gradle.kts`, and give it the package decided in task 0.
- [ ] Move, without changing a value: `Contrast.kt`, `AppLanguage.kt` and its four `actual`s,
      `LocalSnackbarHostState.kt`, plus `ContrastTest`. `check_design_system()` already proves these
      three carry no brand dependency, which is why they move first and alone.
- [ ] Extract the *mechanics* from `Radius.kt` and `Typography.kt`: a type that receives values and
      produces `Shapes` and `Typography`. WhyScan's **values** stay in `:core:designsystem`.
- [ ] Extract `themeFrom(palette)` from `Theme.kt` — declaring the ~34 Material roles from a palette
      — leaving `WhyScanTheme` as the thin call that passes `ScannerPalette`.
- [ ] `:core:designsystem` depends on `:core:foundation`. Never the reverse: a dependency from the
      foundation to the brand is the failure this whole change exists to prevent.
- [ ] Update `FOUNDATION` in `tools/checks.py` to point at the new module, and extend the brand-leak
      check to the whole of `:core:foundation` rather than a file list.
- [ ] No visual change is intended. Any diff in a rendered value is a defect in this task.

## 3. Make the API a contract

- [ ] Wire the binary-compatibility validator on `:core:foundation` and commit the generated `.api`
      dump. Do this **after** task 1, or the baseline records everything that was public by
      accident.
- [ ] Write the versioning policy in `docs/` and link it from the ADR. State the Compose-specific
      cases explicitly: changing a default parameter value is source-compatible and binary-breaking;
      adding a parameter to a public `@Composable` breaks both.
- [ ] Wire Dokka and publish the output as a CI artefact.

## 4. Prove it is usable without WhyScan

- [ ] A `samples/` module that depends **only** on `:core:foundation` and renders a small screen
      with a palette that is not WhyScan's. It must not depend on `:core:designsystem`,
      `:core:model` or any feature.
- [ ] Add it to `Verify`. A sample that does not build in CI is a sample that will not build for the
      first team that tries it.
- [ ] Whatever the sample cannot express without reaching into `:core:designsystem` is a real gap in
      the foundation's API. Record it; do not work around it by widening the dependency.

## 5. Publish

- [ ] A publishing convention plugin in `build-logic`, applied only to `:core:foundation`. Applying
      it to a module that carries the brand is the mistake this ADR exists to prevent, so make it
      opt-in per module, never a repo-wide `allprojects` block.
- [ ] Version `0.1.0`. The contract at `0.x` is "may break"; `1.0` waits for a second real consumer.
- [ ] Publish credentials come from CI secrets, never from a file in the repository. `local.properties`
      is git-ignored and is not a secret store.
- [ ] A release workflow, manual like `iOS (manual)` — publishing is not an acceptance criterion for
      a pull request and must not run on every merge.

## 6. Close the loop

- [ ] `docs/SDD.md` §9: the split and where each concern now lives.
- [ ] `docs/ROADMAP.md`: a round entry with what shipped and what is still unproven.
- [ ] `README.md`: a section for consumers of the foundation — coordinates, version, and the honest
      stability warning.
- [ ] `python3 tools/checks.py`.
- [ ] `/spec-apply federate-design-system`.
