# Cover what can be covered of the Android database builder, and name what cannot

- **Status:** Proposed — **rewritten on 2026-08-30 after an audit found the first version
  unimplementable**
- **Capability:** `scan-history`
- **Roadmap:** "Pendiente para publicar" — *El `actual` de Android de `DatabaseBuilderFactory`, que
  es lo único de la cadena de Room que no ejecuta ningún test*

## Correction, stated first because it is the point

The first version of this proposal planned a Robolectric test that would call
`create().buildBundled()`, insert a row and read it back. **That cannot work, and the repository
already said so.**

`AndroidKoinGraphTest`'s own KDoc explains it: `sqlite-bundled` ships native binaries compiled for
Android ABIs, and under Robolectric the process is a desktop JVM that cannot load them. That is
precisely why the Android graph test excludes `ScanHistoryRepository`, `ScanHistory` and
`ScanSessions`. The same KDoc ends by naming this exact gap: *"Lo único que no comprueba nadie es el
`actual` de Android de `DatabaseBuilderFactory`, que son cuatro líneas y **sigue necesitando un
dispositivo**."*

The first version also credited `AndroidKoinGraphTest` with proving the factory "can be
constructed". It does not — that test never resolves the history chain.

Writing a proposal that contradicts a KDoc in the module it targets is the failure the review step
exists to catch. It was caught before anyone wrote code, which is the process working; it should have
been caught while writing, which is the process being used properly.

## Why

`DatabaseBuilderFactory` is an `expect` class with three `actual`s. The JVM one is exercised by
`MigrationTest`, which opens a real v1 database and asserts the rows survive. The iOS one is
uncovered and the platform is deprioritized. The Android one — the platform that ships — is
uncovered.

Its four lines carry two decisions that can break silently:

- **`applicationContext`, not the context it is handed.** The database outlives any Activity. If
  someone "simplifies" this away, nothing fails until a leak or a crash on a real device.
- **The path comes from `getDatabasePath(ScanDatabase.FILE_NAME)`.** A change here relocates every
  user's history, silently, and the old file is simply never read again.

Neither needs the driver to load. Both are reachable under Robolectric.

## What changes

A Robolectric test in `:core:database` covering the part that does not need native libraries:

1. `create()` returns a builder configured against the **application** context, even when the
   factory is constructed with a `ContextWrapper` around it.
2. The database path resolves under the application's own database directory and uses
   `ScanDatabase.FILE_NAME`.

**It stops there.** It does not call `buildBundled()`, does not open the database and does not query
it — `BundledSQLiteDriver` is exactly the native dependency Robolectric cannot load.

## What does not change

- No production code.
- No claim that the Android persistence chain is verified. It is not, and the ROADMAP must keep
  saying so.
- Nothing about iOS.

## Verification

| Claim | Proof | Runs on every PR |
|---|---|---|
| The builder uses the application context | New Robolectric test in `:core:database` | Yes — JVM, no emulator |
| The database file resolves under the app's database directory | The same test | Yes |
| The bundled driver is configured and works on Android | **Nothing.** `BundledSQLiteDriver` cannot load under Robolectric | No — needs a device |
| The Android database opens and answers a query | **Nothing** | No — needs a device |

The bottom two rows are why this change **does not close** the ROADMAP item. It narrows it: from
"nothing tests the Android `actual`" to "two of its decisions are tested; that the driver works on
Android still needs a phone". The ROADMAP entry must be rewritten to say that, not ticked.

Whether even the narrowed test compiles and runs is **not verifiable in this environment** — nothing
compiles here. `Verify` decides.

## The alternative worth considering

Do nothing, and move the item to the device-blocked list as it stands. That is a defensible call: the
two decisions are four lines of straightforward code, and a test for them buys less than a test that
actually opened the database would have.

The argument for doing it anyway is that `applicationContext` is exactly the kind of line a future
refactor removes as redundant, and this is the cheapest thing that would notice.
