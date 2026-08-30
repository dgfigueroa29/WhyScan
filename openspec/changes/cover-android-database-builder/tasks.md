# Tasks

Work in order. Each task is a coherent commit.

## 1. Wiring

- [ ] Add the Android unit-test source set to `:core:database` and the Robolectric dependency,
      matching how `:composeApp` already declares it. Reuse the version catalog entry; do not add a
      second one.
- [ ] Confirm the module's convention plugin already enables `testOptions.unitTests` with
      `isIncludeAndroidResources`, and add it if not.

## 2. The test

- [ ] `DatabaseBuilderFactoryAndroidTest` in
      `core/database/src/androidUnitTest/kotlin/com/whyscan/core/database/`.
- [ ] **Opens and answers.** Build through `create().buildBundled()`, insert one entry, read it back
      through the repository, assert it is there. This is the assertion that catches a lost driver;
      asserting only that the builder returns non-null would have passed throughout debt D19.
- [ ] **File location.** Assert the database path resolves under the application's database
      directory and uses `ScanDatabase.FILE_NAME`.
- [ ] **Application context.** Construct the factory with a `ContextWrapper` around the Robolectric
      application, and assert the built database still resolves against the application context.
- [ ] Close the database in a `@AfterTest`; a leaked open handle makes the next test's failure
      unreadable.

## 3. Make it run

- [ ] Add `:core:database:testDebugUnitTest` to the `checks` job in `.github/workflows/verify.yml`,
      next to the existing `:composeApp:testDebugUnitTest` step, with a comment saying why it is a
      separate task and not part of `jvmTest`.
- [ ] Verify the step name states it is a JVM test, so nobody later reads it as an emulator step.

## 4. Close the loop

- [ ] Tick the ROADMAP entry under "Pendiente para publicar" and state what it now covers and what
      it still does not — the iOS `actual` stays uncovered and blocked on hardware.
- [ ] Update `docs/SDD.md` §11 and §13 if the coverage claim there changes.
- [ ] `python3 tools/checks.py`.
- [ ] `/spec-apply cover-android-database-builder` — fold the delta into
      `openspec/specs/scan-history/spec.md` and archive this change.
