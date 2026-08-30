# Archive

Changes that have been implemented and folded into `openspec/specs/`.

They are kept **verbatim**, including the reasoning that turned out to be wrong. The current spec
says what the system does; the archive says why it says that, and what was considered instead. A
proposal edited after the fact to match what was eventually built is worth nothing to the person who
reopens the question in a year.

A change arrives here through `/spec-apply`, and only after every box in its `tasks.md` is ticked:

```
git mv openspec/changes/<change-id> openspec/changes/archive/<change-id>
```

Nothing else about the directory changes.
