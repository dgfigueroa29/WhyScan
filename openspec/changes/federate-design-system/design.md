# Design

ADR-0018 decided *what* is federated and *from where*. Three questions remain about *how*, and each
has a real trade-off.

## Why `explicitApi()` comes before the split, not after

The obvious order is: split the module, then lock down its API. It is wrong, for a reason that only
shows up once you try it.

`explicitApi()` in strict mode fails compilation on every public declaration missing a visibility
modifier or an explicit return type. That first pass is not mechanical cleanup — it is **the act of
deciding what the API is**, declaration by declaration. Doing it after the split means doing it
twice, once per module, with the added confusion of having just moved every file.

There is a worse version of getting this wrong. If the binary-compatibility baseline is generated
before that pass, it records as public API everything that was public by accident. From that moment
on, making it `internal` is a breaking change against a baseline nobody meant to promise.

So: lock down the API while it is one module, then split, then baseline.

## Why the sample module is not optional

Four of the five guarantees in ADR-0018 are properties of the code. The fifth — a consumer that is
not WhyScan — is the only one that tests the *assumption*, and assumptions are what break
reusability.

The failure it catches is specific: a foundation API that compiles perfectly and cannot be used
without also depending on something it did not declare. A `CompositionLocal` with no public default.
A theme function that needs a type only the brand module constructs. A resource key that lives in
WhyScan's catalog. None of these fail any test in this repository, and all of them fail on the first
morning of the first team that tries to adopt it.

A `samples/` module compiled in `Verify` is the cheap version of that morning. It is not a real
second consumer — nothing here can be — but it turns "reusable" from an opinion into something with
at least one witness.

The rule that makes it work: **whatever the sample cannot do without reaching into
`:core:designsystem` is a gap in the foundation's API.** Record it. Do not fix it by widening the
sample's dependencies, which converts the test into a formality.

## `0.x` and what the version actually promises

The design system is not stable and pretending otherwise would be the expensive kind of dishonesty.
It was born in Round 1 and changed materially in Rounds 8 (brand), 10 (accessibility) and 15
(components that fit). A `1.0` published today promises a stability nobody can hold for six months.

`0.x` says "may break" in a convention every consumer already understands, and it costs nothing that
matters: an internal consumer pinning `0.3.0` is exactly as pinned as one pinning `1.3.0`.

`1.0` waits for a second real consumer — not for a date, and not for a feeling that it is ready.
Until someone outside this repository has integrated it, there is no evidence about which parts of
the API are load-bearing, and stabilising an API without that evidence just freezes the guesses.

## What this deliberately does not do

- **It does not create a second repository.** ADR-0018 covers why: today there is exactly one
  consumer, and a separate repo charges the coordination toll on every change before any benefit
  arrives. Revisit when two applications actually pull the artefact.
- **It does not federate `:core:model`, `:core:scanner-api` or the engines.** Those are the product.
  Barcode formats and engine descriptors are not a company-wide concern, and sharing them would
  export WhyScan's domain into applications that have no barcodes in them.
- **It does not touch `:core:platform` or `:core:permissions` yet**, although both are plausible
  candidates — `FileSaver`, `ImagePicker`, `OpenableUri` and camera permission are generic. They are
  a second change, after the first one has an actual consumer. Federating three modules at once,
  with zero consumers, is how a shared-library effort produces a lot of infrastructure and no users.
- **It does not package a brand font.** That decision is already recorded in `Typography.kt` and
  belongs to the Play listing, not to this change.
