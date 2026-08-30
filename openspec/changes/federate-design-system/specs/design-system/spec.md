# Design system — delta

## ADDED Requirements

### Requirement: The reusable foundation is a separate, publishable module

The brand-agnostic parts of the design system SHALL live in a module that does not depend on the
brand palette, the brand mark, the barcode domain, or any feature.

That module SHALL be publishable; the module carrying the brand SHALL NOT be.

#### Scenario: The foundation is asked to depend on the brand

- **WHEN** a change makes the foundation module depend on `:core:designsystem`
- **THEN** the dependency direction is inverted and the change is rejected, because the foundation
  would no longer be usable without shipping WhyScan's identity

#### Scenario: Another application applies its own palette

- **WHEN** an application supplies a palette that is not WhyScan's
- **THEN** it obtains a Material theme with every role declared, and the contrast arithmetic to
  assert its own palette meets AA
- **AND** it inherits none of WhyScan's colour values

### Requirement: The published API is explicit and its breaks are visible

The publishable module SHALL compile under explicit API mode, and SHALL carry a committed dump of
its public API surface.

A change to that surface SHALL appear as a diff in that dump within the same pull request.

#### Scenario: A helper becomes public by omission

- **WHEN** a new declaration is added without a visibility modifier
- **THEN** compilation fails under explicit API mode
- **AND** the author decides its visibility rather than publishing it by accident

#### Scenario: A public signature changes

- **WHEN** a public function's signature changes
- **THEN** the committed API dump no longer matches the generated one and the build fails
- **AND** the reviewer sees exactly what would break for a consumer

### Requirement: The foundation is proven usable without the application that wrote it

A sample consumer SHALL exist that depends only on the foundation's public API, and it SHALL be
built on every pull request.

It SHALL NOT depend on the brand module, on the barcode domain, or on any feature.

#### Scenario: The foundation hides a dependency it did not declare

- **WHEN** the foundation's API cannot be used without reaching into the brand module
- **THEN** the sample fails to build
- **AND** the missing surface is recorded as a gap in the foundation's API, rather than fixed by
  widening the sample's dependencies

### Requirement: Version numbers state the real stability

The published module SHALL carry a version whose contract is documented, and SHALL remain below
`1.0` until a second consumer outside this repository has integrated it.

The versioning policy SHALL state explicitly which Compose changes are source-compatible and which
are binary-breaking.

#### Scenario: A default parameter value changes

- **WHEN** the default value of a parameter on a public function changes
- **THEN** the policy classifies it as source-compatible and binary-breaking
- **AND** the release is versioned accordingly rather than by how large the diff looked
