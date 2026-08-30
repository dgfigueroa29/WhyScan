# Tasks

The task list is in `proposal.md`, because this change is small enough that splitting it across two
files would say the same thing twice — and the same fact written twice is how debt D20 stayed open
for months.

The one rule worth repeating on its own:

- [ ] **The first push contains only the gated `composeCompiler { }` block, and nothing else.**
      That push is the entire risk of this change: if the type-safe accessor is not generated for a
      precompiled script plugin, `build-logic` fails to compile and every job in `Verify` dies.
      Isolated, the failure is one revert. Bundled with the workflow and the documentation, it is a
      revert that takes three other things with it.
