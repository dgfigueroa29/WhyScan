---
description: Fold an implemented OpenSpec change into the current specs and archive it
argument-hint: [change-id]
allowed-tools: Bash(ls openspec:*), Bash(git mv:*), Bash(python3 tools/checks.py), Read, Write, Edit, Grep, Glob
---

Apply the implemented change: **$ARGUMENTS**

A change that shipped but was never folded back leaves `openspec/specs/` describing a system that no
longer exists. That is worse than having no specs: it is a source of truth that lies.

## Procedure

1. **Verify it is actually done.** Every box in `openspec/changes/$ARGUMENTS/tasks.md` is ticked, and
   the code to back each one is in the diff. An unticked box means stop and finish the work.

2. **Merge each delta into the current spec**, per capability:
   - `## ADDED Requirements` → append the requirements to
     `openspec/specs/<capability>/spec.md`, keeping the file's existing order and heading levels.
   - `## MODIFIED Requirements` → replace the matching `### Requirement:` block in full. Match by
     the requirement's exact heading text.
   - `## REMOVED Requirements` → delete the block, and make sure nothing else in the spec still
     refers to it.
   - If the capability had no spec, create `openspec/specs/<capability>/spec.md` with a `## Purpose`
     section and the requirements.
   The result must read as current truth: no `ADDED`/`MODIFIED` headers survive into `specs/`, and
   no "will" or "should" — the merged text describes what the system **does**.

3. **Archive the change.** `git mv openspec/changes/$ARGUMENTS openspec/changes/archive/$ARGUMENTS`.
   The proposal is kept verbatim; it is the record of why the spec says what it says.

4. **Close the loop in `docs/`.** Tick the ROADMAP entry, update the SDD section if the design moved,
   and `docs/ENGINES.md` if the catalog did.

5. **Run `python3 tools/checks.py`** and report what it said.

## Output

State which requirements were added, modified or removed, in one line each, and where the change now
lives. If any part of the change could not be verified in this environment, say which and leave it to
`Verify`.
