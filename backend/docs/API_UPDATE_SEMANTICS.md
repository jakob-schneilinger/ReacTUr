# API Update Semantics (PUT vs. PATCH)

This document is the single source of truth for **how "update an existing resource" endpoints
behave** in the ReacTUr backend. If you touch `ExperimentEndpoint#updateExperiment`,
`ReactantEndpoint#updateReactant`, `UpdateReactantDto`, `SaveExperimentDto`, or either validator's
update path, update this file in the same change.

Last verified against code: 2026-07-30 (commit `9ac77c6`, branch `feature/security_conventions_unify`).

## Why two verbs, and why we're not unifying them

There are exactly **two** genuine "update an existing resource" endpoints in the API (as opposed to
create/archive/restore/delete, which are separate state-transition actions, not covered here):

- `PUT /api/experiment/{uuid}` (`ExperimentEndpoint#updateExperiment`)
- `PATCH /api/reactant/{uuid}` (`ReactantEndpoint#updateReactant`)

They were built independently, by different developers, using different (and individually
consistent) semantics: PUT does full-replacement, PATCH does partial/merge updates. The Angular
frontend is already implemented against **both** of these contracts as they exist today. Unifying
the verb/semantics would be a breaking frontend change, so this is intentionally **not** being done
as part of this review — this document exists so the inconsistency is a documented, deliberate
choice rather than an accidental one.

All other `@PutMapping`s in the codebase — `.../restore` on Experiment, Reactant, User, and the
options catalog (`AbstractOptionsEndpoint`) — are no-body idempotent state-transition actions, not
resource updates, and are out of scope here.

## `PUT /api/experiment/{uuid}` — full replacement

- DTO: `SaveExperimentDto` (the **same** DTO used by `POST /api/experiment` for creation).
- `reactantA`, `reactantB`, `metadata`, `measurementData` are all `@NotNull`/`@Valid` — a client
  cannot send a partial body; bean validation rejects it with 400 before the service is even
  called.
- `dois` is the only optional field; its compact constructor defaults a missing/`null` value to an
  empty list. **Omitting `dois` clears it** — this is correct PUT behavior (full replacement, no
  silent preservation of old values), not a bug.
- `ExperimentServiceImpl#updateExperiment` overwrites **every** field present in the DTO
  unconditionally (verified field-by-field against `SaveMetadataDto`/`MeasurementDataDto` and their
  corresponding `Metadata`/`ExperimentResult` entity columns — nothing in the DTO is skipped or
  conditionally preserved).
- Fields **not** present in the DTO are correctly left untouched, by design:
  - `Experiment.createdBy` — ownership is immutable after creation.
  - `Experiment.archivedAt` — archive state is a separate action (`.../archive`, `.../restore`);
    attempting to update an archived experiment is rejected with 409 before any field is touched.
  - `Metadata.archivedAt` — an unused/legacy column on the `metadata` table with no corresponding
    DTO field or endpoint; not touched by create or update. Out of scope for this review (flagged
    only for awareness, not a defect in the update logic itself).
  - `Experiment.files` (attachments) — managed by a separate endpoint/service.
- **Bottom line: there is no partial-update path for experiments, and no field is silently
  preserved that the client would have expected to be replaced.** Regression coverage: see
  `updateExperiment_ShouldFullyReplaceEveryDtoField_WhenUpdating` in `ExperimentServiceTest`, which
  asserts every DTO field lands on the saved entity and that `createdBy`/both `archivedAt` fields
  are left alone.

## `PATCH /api/reactant/{uuid}` — partial update with an explicit clear convention

- DTO: `UpdateReactantDto`, distinct from `CreateReactantDto`. Every field is nullable.
- Convention (see the `// PATCH: Update if not null. "" clears strings, [] empties lists.` comment
  in `ReactantServiceImpl#updateReactant`):
  - **Field omitted / `null`** → not touched, existing value is preserved.
  - **Field present with an empty value** (`""` for strings, `[]` for lists) → explicitly clears
    the field.
  - **Field present with a non-empty value** → replaces the existing value.

| Field | No value (`null`) | Empty value (`""` / `[]`) | New value |
|---|---|---|---|
| `smiles` | preserved | clears to `""` (see open question below) | replaces |
| `chemicalType` | preserved (association untouched, not re-resolved) | *(no empty-UUID convention — cannot be cleared this way)* | replaces (validated to exist) |
| `shortName` | preserved | clears to `""` (see open question below) | replaces (checked for conflicts) |
| `additionalShortNames` | preserved | clears to `[]` | replaces (checked for conflicts, dedup/blank-entry validated) |
| `chemicalName` | preserved | clears to `""` | replaces |
| `casNumber` | preserved | clears to `""` | replaces (checked for conflicts, blank never checked) |
| `inChiKey` | preserved | clears to `""` | replaces (checked for conflicts, blank never checked) |
| `comment` | preserved | clears to `""` | replaces |
| `references` | preserved | clears to `[]` | replaces |

Test coverage for all nine fields × the "preserved when omitted" and "cleared via empty value"
cases (plus "settable to a genuinely new value" for `smiles`/`shortName` specifically) now lives in
`ReactantServiceTest` (`updateReactant_ShouldOnlyUpdateProvidedFields_WhenPatching`,
`updateReactant_ShouldClearFields_WhenEmptyStringOrListProvided`,
`updateReactant_ShouldSetSmilesAndShortNameToNewValue_WhenPatching`).

`chemicalType` is the one field with no "clear" sentinel: since it's a UUID reference (not a string
or list), there's no empty-value equivalent, so a reactant can never have its chemical type cleared
via PATCH — only preserved or replaced with another existing type. This matches the business
requirement that every reactant must have a type, so it is **not** treated as a gap.

## Cross-team contract note

The PATCH null-vs-omitted-vs-empty convention above is a cross-team API contract, not just an
internal implementation detail: the frontend (Angular, Jakob) must send `null`/omit a field to
preserve it and send `""`/`[]` to explicitly clear it — sending nothing when the intent was "clear
this field" will silently do nothing. **If this hasn't already been communicated to Jakob, it
should be, ideally by linking this document.**

## Open questions / genuine gaps found during this review (not fixed — flagged for a decision)

1. **`shortName` and `smiles` can be blanked via PATCH despite being documented as
   mandatory/required, unlike `CreateReactantDto`.**
   - `CreateReactantDto.shortName` and `CreateReactantDto.smiles` both carry `@NotBlank`.
   - `UpdateReactantDto.shortName` and `UpdateReactantDto.smiles` have **no** blank-rejecting
     constraint (`shortName` only has `@Size(max = 100)`; `smiles` has no constraint at all).
   - Because of this, sending `PATCH` with `shortName: ""` or `smiles: ""` currently succeeds and
     persists a blank value for a field the Reactant entity treats as mandatory
     (`shortName` is `nullable = false, unique = true`; `smiles` is `nullable = false`).
   - Compounding this: `ReactantValidator#collectNames` filters out blank names before checking for
     conflicts, so a blanked-out `shortName` is **never checked for uniqueness** — meaning multiple
     reactants could end up with `shortName = ""` without ever tripping the conflict check.
   - This is pinned down (not fixed) by two new regression tests:
     `updateReactant_ShouldCurrentlyAllowBlankingMandatoryShortName_WhenEmptyStringProvided` and
     `updateReactant_ShouldCurrentlyAllowBlankingRequiredSmiles_WhenEmptyStringProvided` in
     `ReactantServiceTest`. If this is intentionally fixed later (e.g. by adding `@NotBlank`-style
     validation to `UpdateReactantDto` for these two fields, which would be a DTO/contract change
     requiring frontend sign-off), these two tests' expectations must be revisited.
   - **This needs a team decision**: either (a) accept that PATCH can never blank these two fields
     and add validation to `UpdateReactantDto` accordingly, or (b) explicitly decide blanking them
     is acceptable and drop the "mandatory" wording from the Javadoc/entity comments.

2. **No field was found where a real-world use case would need a true `null` distinct from the
   empty-string/empty-list "clear" convention.** All nine `UpdateReactantDto` fields are strings,
   lists, or a single required-association UUID, and none have a business meaning for "unknown" vs.
   "explicitly empty" — so the existing null/empty convention is sufficient as-is. No action needed
   here, called out only because it was explicitly checked.

3. **No other DTO in the codebase mixes both conventions or hides a third update-style endpoint.**
   A project-wide grep for `@PutMapping`/`@PatchMapping` turns up exactly six matches: the two
   update endpoints described above, plus four no-body `.../restore` idempotent state-transition
   endpoints (Experiment, Reactant, User, and the shared `AbstractOptionsEndpoint` used by
   Instrument/Solvent/Modality/ChemicalType). `ExperimentFolderEndpoint` and `StructureEndpoint` have
   no `PUT`/`PATCH` mappings at all. The two-endpoint picture in this document is confirmed
   complete.
