# Integration Test Scenarios (Given-When-Then)

Internal working document. Scoped to backend integration tests only (Tier 1: `@SpringBootTest`,
real H2/security-chain/converter/persistence, mocked `JwtDecoder`/`StructureApiClient`/`S3Client`
— see `ExperimentLifecycleIntegrationTest.java`). Not meant for long-term maintenance/release —
may be deleted or folded into other docs later.

## Origin & methodology

These scenarios are adapted from the original SEPM course "project contract" Given-When-Then
material (written months before implementation, by a different professor's course requirements,
not directly elicited from the chemists). That material described the target *vision*, not the
system as built — so every scenario below was checked directly against the current backend source
before being included, and reworded where the contract's language no longer matches reality.
Divergences are called out explicitly rather than silently corrected, since they're genuinely
useful "original spec vs. as-built" material.

**Filtering principle:** integration tests should earn their keep on genuinely cross-layer
concerns — real security filter chain + real `CustomJwtAuthenticationConverter` DB lookup + real
persistence + multi-step workflows — not re-derive validation branches already thoroughly covered
by the 410 unit tests. Scenarios below were chosen for that reason, not to exhaustively re-test
every DTO field.

### Confirmed divergences from the original contract (resolved, not open questions)
- **"Administrator permanently deletes X"** → the actual system never hard-deletes. Every deletion
  is a soft archive (`archivedAt` timestamp) — Experiments, Reactants, and the 4 Options types.
  Confirmed via the already-settled "why can't you edit an archived experiment" line of decisions
  and `ExperimentServiceImpl`/`ReactantServiceImpl`'s archive methods.
- **"Reviewer or administrator"** → actual roles are `ADMIN`/`SUPERVISOR`, with `SUPERVISOR` having
  a narrower Reactant permission set than the contract anticipated (can create, cannot
  update/archive/restore). See `backend/docs/AUTHORIZATION_POLICY.md` for the full matrix.
- **Archiving a reactant/solvent/instrument/modality never blocks existing-or-updated experiments
  from continuing to reference it.** Confirmed by direct read of `ExperimentValidator.validate()`
  (shared by create+update): it only checks entity *existence* and, for reactants, *active-structure
  presence* — never `archivedAt`. "Archived options don't appear for *new* experiments" is a
  frontend dropdown-filtering concern only, not a backend gate. See scenario INT-04.

### Confirmed out of scope (implemented, but as frontend-only computation — no backend contract to test)
- **Chemical substructure search** — implemented client-side by Jakob via RDKit-WASM
  (`frontend-ang/src/app/shared/services/rdkit.service.ts`, `initRDKitModule`). No backend
  substructure-search endpoint exists (confirmed via the `TODO-Future` comment in
  `ReactantEndpoint.java`). The backend's only obligation is correctly serving reactant/experiment
  data, already covered by INT-10/INT-11.
- **Rate-constant manual calculation (GraphPad Prism workflow)** — external tool + frontend
  presentation only; the backend just stores/returns the already-calculated `rateConstant` value
  (part of `MeasurementDataDto`), already exercised by every experiment-creation scenario.
- **JSON/CSV export** — implemented client-side (`export-modal.component.ts`), built from data
  fetched via the existing `GET /api/reactant/{uuid}` and search endpoints. No dedicated backend
  export endpoint exists or is needed.

### Confirmed out of scope (genuinely not implemented / not applicable to backend integration tests)
- TimescaleDB, automated Postgres backups, binary→CSV instrument-file conversion tooling —
  infrastructure/ops concerns, no application-level endpoint.
- DSGVO / author-name data-privacy questions — unresolved product decisions, not testable code.

### Flagged as missing, tracked as future work (not a testable scenario today)
- **Grouping multiple experimental repetitions under one parent experiment**, to preserve
  scientific context for standard-deviation calculations — present in the original contract, but
  never implemented, and not equivalent to anything currently in the codebase. The new Folder
  feature (added during the team's "final" review) is a *partial*, not exact, fit — a Folder can
  group experiments together, but has no dedicated "these are repetitions of the same underlying
  reaction" semantics (no parent/child link, no standard-deviation aggregation).
  **TODO-FUTURE**: decide whether Folders should be extended to formally model experiment
  repetition-grouping, or whether a dedicated parent-experiment concept is needed instead. Not
  acted on now — flagging for the team TODO list / thesis future-work section.

---

## Scenario list

Each scenario below is intended to become exactly one `@Test` method (or a small tightly-related
cluster) in a Tier-1 `@SpringBootTest`, following the `ExperimentLifecycleIntegrationTest.java`
pattern: real `IntegrationTestFixtures`-style seeding, `asRealJwtSubject(...)` (never the
`.with(jwt().authorities(...))` shortcut — see that file's class Javadoc for why), and at least one
independent follow-up read proving real persistence, not an echoed response.

### A. Data Ingestion & Metadata Standardization

**INT-01 — Full round trip: experiment creation, metadata/file separation, independent re-read**
*Adapted from: Happy_SystemCalculation / "Successful upload of data and mandatory metadata".*
- **Given** a real `USER` acting as creator+scientist, plus real `Reactant`(x2, each with an ACTIVE
  structure)/`Solvent`/`Instrument`/`Modality` rows.
- **When** they POST a valid `SaveExperimentDto` to `/api/experiment`, and then POST a raw data file
  (e.g. a CSV) to `/api/experiment/{uuid}/files` (`ExperimentFilesEndpoint`).
- **Then** both succeed (201), and an independent `GET /api/experiment/{uuid}` shows the metadata
  correctly persisted, separate from the attachment, and the attachment is independently
  retrievable/listed.
- **Note**: extends the existing Tier-1 pilot's Scenario A (which only covered the metadata part) by
  adding the file-attachment half — don't duplicate the metadata-only assertions wholesale, build on
  the established pattern.

**INT-02 — Mandatory metadata rejected end-to-end, nothing persisted**
*Adapted from: Sad_MissedDataCleaning / "Rejecting incomplete data entries".*
- **Given** a real `USER`, real reactants etc. as above.
- **When** they POST a `SaveExperimentDto` missing a mandatory metadata field (e.g. null `solvent`).
- **Then** 400 (Bean Validation) — real end-to-end proof, and a follow-up search confirms nothing was
  persisted (no experiment exists for that user).

**INT-03 — Reactant without an ACTIVE structure blocks experiment creation**
*New — not in the original contract (structure lifecycle didn't exist as a concept back then).*
- **Given** a real `Reactant` with only a `PENDING` or `READY` (not yet `ACTIVE`) structure.
- **When** a `USER` attempts to create an experiment referencing it.
- **Then** 422, via the real `ExperimentValidator.validateReactantHasActiveStructure` path — and
  nothing is persisted.

**INT-04 — Archived-reference resilience on update**
*New — resolves the "what happens on UPDATE if a referenced entity was archived" question raised
during scenario planning; see "Confirmed divergences" above.*
- **Given** a real experiment referencing a real `Reactant` (and/or `Solvent`/`Instrument`/
  `Modality`), created while that entity was active.
- **When** an `ADMIN` archives that referenced entity, and the experiment's creator/scientist
  subsequently issues a real `PUT /api/experiment/{uuid}` re-sending the *same* (now-archived)
  reference unchanged.
- **Then** the update succeeds (200) — proving archiving never retroactively invalidates existing
  references. This is a regression guard: if `ExperimentValidator` ever grows an `archivedAt` check,
  this test should catch it.

### B. 3D Structure Generation (Human-in-the-Loop)

**INT-05 — Full structure lifecycle: PENDING → READY → ACTIVE → usable in an experiment**
*Adapted from: "Pending status for backend 3D generation" / AdaptationPath "Resolving Ambiguous
Chemical Structures".*
- **Given** a real `Reactant` with no existing structures, and a mocked `StructureApiClient` whose
  `triggerJob` returns a canned job ID and whose `checkJobStatus` reports success with one or more
  generated structures.
- **When** an `ADMIN` calls `POST /api/structure/{reactantId}/generate` (→ real `PENDING` row
  created), the scheduled poll runs (`StructureServiceImpl.fetchPendingStructuresFromAPI`, call
  directly rather than waiting on `@Scheduled` timing) turning it `READY`, and the `ADMIN` then calls
  `POST /api/structure/{structureId}/select`.
- **Then** the structure becomes `ACTIVE`, and a subsequent experiment-creation request referencing
  that reactant succeeds (ties directly into INT-03's negative case — this is its positive
  counterpart).

**INT-06 — Structure endpoints are ADMIN/SUPERVISOR-only**
*New — systematic authorization-shape coverage (pure role gate, shape 1).*
- **Given** a real `Reactant`.
- **When** a plain `USER` calls any of the three `/api/structure/**` endpoints.
- **Then** 403 for all three. Include the positive confirmation that both `ADMIN` and `SUPERVISOR`
  succeed (parity, per the settled matrix).

### C. Reactant & Options Catalog Management

**INT-07 — Reactant creation, required + optional fields**
*Adapted from: "Creating a new reactant profile".*
- **Given** an `ADMIN` (or `SUPERVISOR` — both can create reactants per the settled matrix).
- **When** they POST a `CreateReactantDto` with only the required fields (`shortName`, `smiles`,
  chemical type), and separately, one with optional fields also populated (chemical name,
  additional short names, CAS number, comment, references).
- **Then** both succeed and are independently retrievable with all submitted fields correctly
  persisted.

**INT-08 — Options catalog creation: ADMIN succeeds, SUPERVISOR forbidden**
*Adapted from: "Administrator adds new chemical types/options" + new authorization detail the
contract never anticipated (SUPERVISOR has no elevated rights on the options catalog at all).*
- **Given** an `ADMIN` and a `SUPERVISOR`.
- **When** each attempts `POST` on one of the 4 options endpoints (representative: pick one, e.g.
  `ChemicalTypeEndpoint` — consistent with the established representative-testing convention for
  these 4 genuinely-identical siblings).
- **Then** `ADMIN` succeeds (201), `SUPERVISOR` is forbidden (403).

**INT-09 — Reactant archive: ADMIN succeeds, SUPERVISOR forbidden**
*Rewritten from: "Deleting a reactant" (hard delete → soft archive, per the confirmed divergence
above), plus the SUPERVISOR asymmetry the contract never modeled.*
- **Given** a real `Reactant` with no experiments referencing it.
- **When** an `ADMIN` archives it (200/204, whichever the endpoint returns) and, separately, a
  `SUPERVISOR` attempts the same.
- **Then** `ADMIN` succeeds and a follow-up read shows `archivedAt` set; `SUPERVISOR` gets 403 and
  the entity remains unarchived.

### D. Search & Retrieval

**INT-10 — Metadata + rate-constant-range filtering, full real round trip**
*Adapted from: "Filtering experiments by metadata parameters" + "Searching by rate constant
ranges" (consolidated into one scenario, since both exercise the same `searchExperiments`
mechanism, whose `Specification`-building logic is already thoroughly unit-tested via
`ExperimentSearchSpecificationTest`).*
- **Given** several real experiments with varying `metadata`/`measurementData` values.
- **When** a real `USER` calls `GET /api/experiment` with a combination of metadata and rate-constant
  range query parameters, over real HTTP (not direct repository/service wiring).
- **Then** exactly the matching experiments are returned — this scenario's value is proving the full
  controller→real-converter→real-service→real-repository wiring works end-to-end, not re-deriving
  the filter logic itself.

**INT-11 — Raw file retrieval round trip**
*Adapted from: "Retrieving raw measurement data".*
- **Given** a real experiment with a real attachment (mocked `S3Client.getObject` returns canned
  bytes).
- **When** the creator downloads it via `ExperimentFilesEndpoint`.
- **Then** the returned resource matches the canned bytes, and a non-creator/non-scientist/
  non-privileged `USER` attempting the same download gets 403 (folds in the existing authorization
  shape for Files, already covered at unit level, but not yet through a real end-to-end request in
  this tier).

### E. Folder Ownership (not in the original contract at all)

**INT-12 — Owner CRUD succeeds**
*New.*
- **Given** a real `USER` (`UserA`) and a real experiment they created.
- **When** `UserA` creates a folder, adds their experiment to it, removes it, and deletes the folder.
- **Then** every step succeeds (real persistence confirmed at each step via a follow-up
  `GET /api/folders`).

**INT-13 — Zero privileged bypass, even for ADMIN/SUPERVISOR**
*New — the headline Folder scenario, since this is the one place in the whole system where the
usual privileged-bypass pattern deliberately does not apply.*
- **Given** `UserA`'s real folder (containing a real experiment).
- **When** a real `ADMIN` and, separately, a real `SUPERVISOR` attempt to add/remove an experiment
  from it, or delete it outright.
- **Then** 403 in every case, for both roles — proving `ExperimentFolderService`'s ownership check has
  no bypass clause at all, unlike every other privileged resource in the system.

### F. Authorization Matrix — Systematic Coverage

**INT-14 — UNAUTHORIZED global gate**
*Already implemented in the Tier-1 pilot (`ExperimentLifecycleIntegrationTest.scenarioC_*`) —
listed here for completeness/traceability only. Do not reimplement; reference the existing test.*

**INT-15 — Privileged bypass, the allowed direction**
*New — completes the pair with the existing pilot's Scenario B, which only proved the *forbidden*
direction (a non-creator/non-scientist plain `USER`). This scenario proves the *allowed* direction.*
- **Given** an experiment created by `UserA` (creator+scientist), and a real `SUPERVISOR` who is
  neither.
- **When** the `SUPERVISOR` archives (`DELETE /api/experiment/{uuid}`) or otherwise modifies it.
- **Then** it succeeds (unlike the pilot's plain-`USER` case) — proving
  `ExperimentAuthorization.isPrivileged` genuinely grants access, not merely that non-privileged
  access is denied.

---

## Summary

15 scenarios (INT-01 through INT-15, with INT-14 pointing back to existing pilot coverage rather
than duplicating it), plus one explicitly-flagged future-work gap (experiment-repetition grouping)
and several explicitly-excluded areas (frontend-only features, infra/ops concerns, unresolved
product decisions).

**Tiering note**: all scenarios above are scoped to Tier 1 (`@SpringBootTest`, real H2, mocked
`JwtDecoder`/`StructureApiClient`/`S3Client`) since that tier already exercises the real security
chain, real converter, real persistence, and real authorization logic end-to-end. Tier 2 (real
Postgres/Keycloak/SeaweedFS via the existing `docker-compose.yml`) is planned as a later
*re-validation* pass over this same scenario set, not a reason to defer writing/implementing them
now.
