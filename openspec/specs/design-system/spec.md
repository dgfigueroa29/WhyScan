# Design system

## Purpose

One visual language across four platforms, defined so that its guarantees are **arithmetic rather
than intention**: colour roles declared exhaustively, contrast measured by a test with no device,
and a reading mode that scales the whole type system rather than one screen.

The reusable part of this is not the colours — it is the rules. That distinction is what
`docs/adr/ADR-0018-federar-la-base-y-no-la-marca.md` acts on.

Design: `docs/SDD.md` §9, `docs/adr/ADR-0011-idioma-de-la-app-por-encima-del-sistema.md`,
`docs/adr/ADR-0014-la-marca-sale-del-objeto.md`, RNF-05.

## Requirements

### Requirement: Every Material colour role is declared in both themes

The light and dark colour schemes SHALL each set every Material 3 role the application uses, and
SHALL set the same set of roles as one another.

A role left undeclared SHALL be treated as a defect, because `lightColorScheme()` and
`darkColorScheme()` fill omissions from Material's factory palette rather than failing.

#### Scenario: A role is added to one scheme only

- **WHEN** a colour role is set in the light scheme and not in the dark one
- **THEN** `check_design_system()` in `tools/checks.py` reports it
- **AND** the pull request fails before any build runs

#### Scenario: An undeclared container role

- **WHEN** `secondaryContainer` is not declared
- **THEN** the selected `FilterChip`, the navigation indicator and the card surface render in
  Material's factory purple, in an application whose brand is not purple
- **AND** nothing fails at build or run time, which is why the check exists

### Requirement: The palette is data, not Compose

Colour values SHALL be declared as ARGB integers independent of Compose, so that contrast can be
computed and asserted in `commonTest` without rendering anything.

#### Scenario: Contrast is asserted without a device

- **WHEN** the accessibility requirement RNF-05 is checked
- **THEN** `ContrastTest` computes WCAG 2.1 ratios arithmetically over the palette
- **AND** the assertion runs on every pull request, on the JVM

### Requirement: Colour pairs meet WCAG AA in both themes

Every colour pair the interface actually uses SHALL meet 4.5:1 for text, and 3.0:1 for non-textual
components such as `outline`.

The measured set SHALL include pairs no Material convention guarantees — accent colours used as
**text on a surface** — and SHALL be declared once and applied to both themes.

#### Scenario: An accent used as text on a card

- **WHEN** `primary`, `tertiary` or `error` is used as the text colour on a card surface
- **THEN** that pair is in the measured set
- **AND** it is asserted at 4.5:1 in both the light and the dark theme

#### Scenario: The two themes drift apart

- **WHEN** one theme's measured pair list gains an entry the other does not have
- **THEN** `los_dos_temas_miden_los_mismos_pares` fails

### Requirement: What is painted over video lives outside the theme

Colours drawn on top of the camera preview SHALL be declared separately from the light and dark
schemes, and SHALL be identical in both.

Legibility over video SHALL NOT depend on colour alone: overlay elements SHALL carry shape — a
stroked reticle and a closed detection outline.

#### Scenario: The theme changes

- **WHEN** the user switches between light and dark
- **THEN** the reticle and detection outline colours do not change, because the background is the
  filmed scene and not a themed surface

#### Scenario: A colour is written inline in a screen

- **WHEN** a literal `Color(0x…)` appears anywhere outside the palette
- **THEN** `check_design_system()` reports it, because a colour outside the palette is measured by
  nothing and does not follow the theme

### Requirement: The reading mode scales the whole type system

When the reading mode is enabled, every typographic role SHALL grow in size, letter spacing and
line height; no role SHALL retain negative tracking; and the scale SHALL remain sans-serif.

The value of a scanned code SHALL remain monospaced in every mode.

#### Scenario: The mode is off

- **WHEN** the reading mode is disabled
- **THEN** the type scale is byte-for-byte the ordinary one

#### Scenario: A new typographic role is added

- **WHEN** a role is added to the scale
- **THEN** `ReadingTypographyTest` requires it to grow, gain tracking and gain leading like the rest

### Requirement: Application language and theme override the system

The application's language and theme SHALL be selectable inside the application, SHALL persist, and
SHALL take precedence over the system setting.

System bars SHALL follow the application's theme, not the system's.

#### Scenario: System is dark, application is set to light

- **WHEN** the device is in dark mode and the user selects Light in the application
- **THEN** the application renders light
- **AND** the system bars follow the application's choice

### Requirement: Brand-agnostic foundation carries no brand dependency

The parts of the design system declared as reusable foundation SHALL NOT reference the brand
palette or the brand mark.

#### Scenario: Brand leaks into the foundation

- **WHEN** a foundation file starts referencing `ScannerPalette` or `BrandMark`
- **THEN** `check_design_system()` reports it, because that file has stopped being shareable
  without shipping WhyScan's identity with it
