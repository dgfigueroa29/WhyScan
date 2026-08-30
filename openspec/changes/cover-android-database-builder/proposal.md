# Cover the Android database builder with a test that runs on every pull request

- **Status:** Proposed
- **Capability:** `scan-history`
- **Roadmap:** "Pendiente para publicar" — *El `actual` de Android de `DatabaseBuilderFactory`, que
  es lo único de la cadena de Room que no ejecuta ningún test*

## Why

`DatabaseBuilderFactory` is an `expect` class with three `actual` implementations. Two of them are
exercised today; the Android one is not.

| Platform | What runs it |
|---|---|
| JVM / Desktop | `MigrationTest` opens a real v1 database and asserts the rows survive |
| iOS | Nothing — the platform is deprioritized and has no device |
| **Android** | **Nothing.** `AndroidKoinGraphTest` resolves the graph, which proves the factory can be *constructed*, not that the database it builds opens |

Android is the platform that ships, and it is the one whose failure mode here is silent. This exact
shape of defect has already cost this project twice:

- **Debt D19.** `buildBundled()` was called `build()`, and in Kotlin a member always beats an
  extension, so the bundled driver was never configured. Desktop and iOS crashed on the first
  screen; **Android kept working** by silently falling back to the framework's SQLite — the very
  driver the code exists to avoid. The guarantee that all platforms run the same SQLite version had
  been false since the day it was written, and the compiler had been warning about it in every
  build.
- **Debt D18.** The first real device boot found a Koin registration defect that CI could not see.

Both were found by a test that ran on the JVM. This proposal closes the last piece of the same chain
the same way.

## What changes

A Robolectric test in `:core:database` that exercises the Android `actual`:

1. `create().buildBundled()` returns a database that **opens and answers a query**, so a regression
   that loses the bundled driver fails here rather than on a user's phone.
2. The database file resolves under the application's own database directory.
3. The factory holds the *application* context, not the `Context` it was handed — the database
   outlives any Activity, and the current implementation calls `applicationContext` precisely for
   that reason. Nothing checks that it keeps doing so.

## What does not change

- No production code, unless the test finds a defect — in which case that fix is its own change.
- No new dependency beyond Robolectric, already used by `:composeApp` for `AndroidKoinGraphTest`.
- Nothing about iOS. Its `actual` stays uncovered, and that is recorded as blocked on hardware,
  not fixed here.

## Verification

| Claim | Proof | Runs on every PR |
|---|---|---|
| The Android builder produces an openable database | New Robolectric test in `:core:database` | Yes — `./gradlew :core:database:testDebugUnitTest`, JVM, no emulator |
| The bundled driver is configured | The same test, by querying through the built instance | Yes |
| The application context is used | The same test, passing a wrapper context | Yes |
| The database behaves identically on a real device | **Nothing.** Requires hardware | No — and this proposal does not claim otherwise |

`Verify` must be extended to run the new task, or the test joins the set of things that exist and
never execute — which is the failure this change is about.
