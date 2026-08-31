# Tasks

Rewritten after the audit. **Nothing here can be executed in this environment.**

## 1. Wiring

- [ ] Add the Android unit-test source set to `:core:database` and the Robolectric dependency,
      matching how `:composeApp` already declares it. Reuse the version catalog entry.
- [ ] Pin the Robolectric SDK level explicitly rather than inheriting `targetSdk`, as
      `AndroidKoinGraphTest` does and for the same reason: raising `targetSdk` should not break the
      tests before Robolectric supports that level.

## 2. The test — and only what it can actually assert

- [ ] `DatabaseBuilderFactoryAndroidTest` in
      `core/database/src/androidUnitTest/kotlin/com/whyscan/core/database/`.
- [ ] **Application context.** Construct the factory with a `ContextWrapper` around the Robolectric
      application and assert the builder is configured against the application context.
- [ ] **File location.** Assert the path resolves under the application's database directory and
      uses `ScanDatabase.FILE_NAME`.
- [ ] **Do not call `buildBundled()`**, do not open the database, do not query it. `BundledSQLiteDriver`
      loads native binaries built for Android ABIs, and Robolectric runs on a desktop JVM. A test that
      tries will fail at class-load time, and the failure will read like a wiring bug rather than what
      it is.
- [ ] Put that constraint in the test's own KDoc, next to the assertions, so the next person does not
      re-propose the version this change already withdrew.

## 3. Make it run

- [ ] Add `:core:database:testDebugUnitTest` to the `checks` job in `.github/workflows/verify.yml`,
      beside the existing `:composeApp:testDebugUnitTest` step, with a comment saying it is a JVM
      test so nobody later reads it as an emulator step.

## 4. Close the loop honestly

- [ ] **Do not tick the ROADMAP entry.** Rewrite it: two of the Android `actual`'s decisions are now
      covered; that the bundled driver works on Android still needs a device, and that is where the
      item stays.
- [ ] Update `docs/SDD.md` §11 and §13 only if the coverage claim there changes.
- [ ] `python3 tools/checks.py`.
- [ ] `/spec-apply cover-android-database-builder`.
