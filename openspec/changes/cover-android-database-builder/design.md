# Design

## What the first version of this change got wrong

It planned a Robolectric test that would build the Android database through `buildBundled()`, insert
a row and read it back — and it argued, correctly in the abstract, that only a real read-back would
catch a lost driver, because debt D19 passed every structural assertion.

The argument was right. The test is impossible. `sqlite-bundled` ships native binaries for Android
ABIs; Robolectric runs on a desktop JVM and cannot load them. `AndroidKoinGraphTest`'s KDoc says so
explicitly, in the same module, and it is the reason that test excludes the whole history chain.

Two lessons, and the second is the reusable one:

1. **Read the KDoc of the test you are about to imitate.** The constraint was written down, in
   Spanish, forty lines above where anyone would have looked.
2. **"What proves it, and does that proof run on every PR?" is not enough on its own.** The first
   version answered it — a JVM test — and was still wrong, because the answer assumed a JVM test
   *could exist* for that assertion. The question needs a second half: **can the proof physically
   run here?**

That second half belongs in `openspec/AGENTS.md`, and the audit that found this is the reason it is
there now.

## Where the test lives

**`:core:database`, in an Android unit-test source set, with Robolectric.**

Putting it in `:composeApp` beside `AndroidKoinGraphTest` would mean a change to
`:core:database` is validated by a test in another module, where whoever edits the factory will not
see it. An instrumented test in `androidTest` is a hard stop: no emulator in CI, so it would never
run (debt D6).

## What it asserts, and why that is worth anything

Two decisions, neither of which needs the driver:

- **`applicationContext`.** The database outlives any Activity. This is exactly the line a future
  refactor deletes as redundant, and nothing else would notice.
- **The path.** Relocating the file silently orphans every user's history — no error, no migration,
  the old file is just never read again.

It deliberately asserts nothing about the driver. The honest consequence is that this change
**narrows** the roadmap item instead of closing it, and the roadmap must say so. A test that looks
like it covers the Android database and does not is worse than the gap it appears to fill.

## What this deliberately does not do

- **It does not test Room.** Migrations are covered by `MigrationTest` on the JVM.
- **It does not touch iOS.** That `actual` stays uncovered; the platform is deprioritized and has no
  device.
- **It does not pin a SQLite version string.** The guarantee is "the same SQLite everywhere", and a
  version string in a test makes the next dependency bump fail for no user-visible reason — and
  here it could not be asserted anyway.
