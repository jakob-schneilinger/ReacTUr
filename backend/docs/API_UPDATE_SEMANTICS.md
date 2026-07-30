# API Update Semantics (PUT vs. PATCH)

This document is the single source of truth for **how "update an existing resource" endpoints
behave** in the ReacTUr backend. If you touch `ExperimentEndpoint#updateExperiment`,
`ReactantEndpoint#updateReactant`, `UpdateReactantDto`, `SaveExperimentDto`, or either validator's
update path, update this file in the same change.

Last verified against code: 2026-07-30 (branch `feature/security_conventions_unify`). Updated same-day
to record the fix of the `shortName`/`smiles` blanking gap described below (previously tracked as
"Open questions" item 1; now resolved via `ReactantValidator#validateForUpdate`).

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
| `smiles` | preserved | **rejected**: `ValidationException` / 400 (exception to the general convention — see below) | replaces |
| `chemicalType` | preserved (association untouched, not re-resolved) | *(no empty-UUID convention — cannot be cleared this way)* | replaces (validated to exist) |
| `shortName` | preserved | **rejected**: `ValidationException` / 400 (exception to the general convention — see below) | replaces (checked for conflicts) |
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

`shortName` and `smiles` are the two deliberate exceptions to the general "empty clears the field"
convention: unlike every other string/list field in `UpdateReactantDto`, both back a mandatory
entity column (`Reactant.shortName` is `nullable = false, unique = true`; `Reactant.smiles` is
`nullable = false`), so a blank value would corrupt data rather than represent a legitimate cleared
state (an unnamed or structureless reactant is not a valid business state). `null`/omitted still
means "preserve existing value" as usual, but sending an explicit blank or whitespace-only value
(`patchDto.shortName() != null && patchDto.shortName().isBlank()`, and the equivalent for `smiles`)
is rejected in `ReactantValidator#validateForUpdate` with a `ValidationException` (HTTP 400), before
the service layer or any conflict check ever sees it. This is intentionally not expressed as a
Jakarta `@NotBlank` on the DTO field, since that would also reject `null` and break every PATCH that
omits these fields to update something else. Regression coverage:
`updateReactant_ShouldThrowValidationException_WhenShortNameBlanked`,
`updateReactant_ShouldThrowValidationException_WhenShortNameBlankedWithWhitespaceOnly`, and
`updateReactant_ShouldThrowValidationException_WhenSmilesBlanked` in `ReactantServiceTest`, plus unit
tests directly on the validator in `ReactantValidatorTest`
(`validateForUpdate_shouldThrowValidationException_whenShortNameIsBlank`,
`validateForUpdate_shouldThrowValidationException_whenShortNameIsWhitespaceOnly`,
`validateForUpdate_shouldThrowValidationException_whenSmilesIsBlank`).

## Cross-team contract note

The PATCH null-vs-omitted-vs-empty convention above is a cross-team API contract, not just an
internal implementation detail: the frontend (Angular, Jakob) must send `null`/omit a field to
preserve it and send `""`/`[]` to explicitly clear it — sending nothing when the intent was "clear
this field" will silently do nothing. **If this hasn't already been communicated to Jakob, it
should be, ideally by linking this document.**

## Open questions / genuine gaps found during this review (not fixed — flagged for a decision)

1. **No field was found where a real-world use case would need a true `null` distinct from the
   empty-string/empty-list "clear" convention.** All nine `UpdateReactantDto` fields are strings,
   lists, or a single required-association UUID, and none have a business meaning for "unknown" vs.
   "explicitly empty" — so the existing null/empty convention is sufficient as-is. No action needed
   here, called out only because it was explicitly checked.

2. **No other DTO in the codebase mixes both conventions or hides a third update-style endpoint.**
   A project-wide grep for `@PutMapping`/`@PatchMapping` turns up exactly six matches: the two
   update endpoints described above, plus four no-body `.../restore` idempotent state-transition
   endpoints (Experiment, Reactant, User, and the shared `AbstractOptionsEndpoint` used by
   Instrument/Solvent/Modality/ChemicalType). `ExperimentFolderEndpoint` and `StructureEndpoint` have
   no `PUT`/`PATCH` mappings at all. The two-endpoint picture in this document is confirmed
   complete.

(Item 1 in a previous revision of this document — `shortName`/`smiles` blanking via PATCH — was a
genuine gap found during this review. It has since been **fixed**; see the `shortName`/`smiles` row
in the table above and the paragraph directly below it for the current, resolved behavior.)

## Frontend coordination note (for delivery/handoff)

Fixed as part of this review: `PATCH /api/reactant/{uuid}` now rejects `shortName: ""` and
`smiles: ""` with a 400 validation error (previously silently accepted and could corrupt data — see
git history for this file / commit message for details). This is a **behavior change**: if the
current frontend ever sends an empty string for either of these two fields — even by accident, e.g.
an uninitialized form value — a request that previously succeeded (with an undesired blank
persisted) will now be rejected with a 400 instead. This should be communicated to Jakob
(frontend/Angular) before final delivery/handoff, ideally by checking whether the reactant-edit form
on the frontend ever submits an empty string for "Short Name" or "SMILES" in practice (e.g. when a
field is left untouched vs. actively cleared by the user) — worth a quick confirmation conversation
even if it's expected to be a non-issue, since the team won't be reconvening for a few weeks over
the summer break.
