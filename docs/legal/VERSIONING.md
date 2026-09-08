# Semantic Versioning Policy for WhyScan Foundation

The `:core:foundation` module follows [Semantic Versioning 2.0.0](https://semver.org/).

## Version Scheme

- **MAJOR** version when you make incompatible API changes.
- **MINOR** version when you add functionality in a backwards compatible manner.
- **PATCH** version when you make backwards compatible bug fixes.

## Binary Compatibility in Compose

In Compose Multiplatform, source-compatibility does NOT always mean binary-compatibility. The following rules must be observed to avoid breaking consumers:

### Binary Breaking Changes (requires MAJOR version bump)

1.  **Changing a default parameter value**: This is source-compatible but changes the signature at the bytecode level in Kotlin.
2.  **Adding a parameter to a public `@Composable` function**: Even if it has a default value, it breaks binary compatibility for the same reasons as above.
3.  **Renaming or removing** a public declaration.
4.  **Changing the visibility** of a declaration from `public` to `internal` or `private`.
5.  **Moving** a declaration to a different package.

### Non-Breaking Changes (MINOR or PATCH)

1.  **Adding a new public declaration** (MINOR).
2.  **Internal logic changes** that do not affect the public API surface or its behavior (PATCH).
3.  **Adding internal or private members** (PATCH).

## API Tracking

The public API surface of `:core:foundation` is tracked via the [Kotlin Binary Compatibility Validator](https://github.com/Kotlin/binary-compatibility-validator). Any change to the generated `.api` dump must be reviewed and justified with the corresponding version bump.
