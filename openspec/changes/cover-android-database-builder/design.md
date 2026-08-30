# Design

One question here has a real trade-off; the rest follows from the answer.

## Where does the test live?

**Chosen: `:core:database`, in an Android unit-test source set, with Robolectric.**

| Option | Why not |
|---|---|
| `:composeApp`, beside `AndroidKoinGraphTest` | The graph test already lives there because the graph *is* assembled there. The database builder is not: putting its test in `:composeApp` means a change to `:core:database` is validated by a test in another module, and whoever edits the factory does not see it |
| An instrumented test in `androidTest` | Hard stop. No emulator in CI, so it would never run (debt D6). A test that never runs is worse than no test, because it reads as coverage |
| A common test with a fake `Context` | Faking `Context` is faking exactly the thing under test. The value of this test is that `applicationContext` and `getDatabasePath` behave as Android behaves |

Robolectric is already a dependency of this repository and already runs on the JVM in the `checks`
job, so this adds a source set, not a capability.

## What is actually asserted

The assertion has to be **a query that returns data**, not a non-null builder.

This is the lesson of debt D19 stated as a test design rule. When `buildBundled()` was shadowed by
Room's own `build()`, every structural assertion still passed: the builder existed, the database
object was created, the schema was right. On Android it even worked — by falling back to the
framework's SQLite, which is the driver this code exists to avoid. Only *using* the database through
the configured driver distinguishes the two.

So: insert, read back, assert. If the bundled driver is ever lost again, this fails.

## What this deliberately does not do

- **It does not test Room.** Migrations are already covered by `MigrationTest`, which opens a real
  v1 database. Duplicating that on Android would add runtime and no information.
- **It does not touch iOS.** The iOS `actual` stays uncovered. Saying so is the honest outcome:
  the platform is deprioritized and has no device, and inventing a Kotlin/Native test that only
  proves compilation would restate what the `iOS (manual)` workflow already proves.
- **It does not assert on SQLite's version string.** The guarantee is "the same SQLite everywhere",
  and pinning a version string in a test makes the next dependency bump fail for no user-visible
  reason. The driver being the bundled one is what matters, and the query proves that.
