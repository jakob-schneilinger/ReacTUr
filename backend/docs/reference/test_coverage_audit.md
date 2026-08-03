# Backend Test-Coverage Gap Audit (Third Round)

**Status:** Report-only, read-only investigation. No production or test code was changed to produce
this document. Baseline confirmed at time of writing: **401 tests passing**
(`./mvnw -o clean test`, run and confirmed `BUILD SUCCESS`).

**This is the third round of this audit.** History:
- Original (255-test baseline) → archived as
  [`test_coverage_audit_2026-08-03.md`](./test_coverage_audit_2026-08-03.md)
- Second round (359-test baseline) → archived as
  [`test_coverage_audit_2026-08-03_v2.md`](./test_coverage_audit_2026-08-03_v2.md) (dated the same
  calendar day as the first archive, hence the `_v2` suffix, to avoid overwriting it)
- **This document** (401-test baseline) is a fresh re-read of all current source and test files,
  followed by an explicit reconciliation against the second round's "Final: Review the missing
  gaps" list.

Legend: **Full** = tested, positive + negative (or all applicable branches) covered. **Partial** =
tested, but missing a negative case and/or at least one distinct branch. **Zero** = no test
references this method at all.

---

## 1. Services

### 1.1 `ExperimentServiceImpl`

| Method | Status | Test(s) | Notes |
|---|---|---|---|
| `createExperiment` | Full | `CreateExperiment.*` (4 tests) | Creator/scientist/reactant-not-found all via real `ExperimentValidator`. |
| `getExperiment` | Full | `GetExperiment.*` (6 tests) | All authz branches (creator/scientist/admin/forbidden) + not-found. |
| `archiveExperiment` | Full | `ArchiveExperiment.*` (5 tests) | Not-found, forbidden, idempotent no-op all covered. |
| `restoreExperiment` | Full | `RestoreExperiment.*` (4 tests) | Same as above. |
| `getArchivedExperiments` | Full | `GetArchivedExperiments.*` (3 tests) | Both privileged/non-privileged branches + empty case. |
| `updateExperiment` | Full | `UpdateExperiment.*` (6 tests) | All branches incl. auth-before-conflict ordering. |
| `searchExperiments` / `buildSearchSpecification` (private) | **Full — resolved this round** | `ExperimentServiceTest.SearchExperiments` (mocked happy path) + `ExperimentSearchSpecificationTest` (genuine `@DataJpaTest`, 16 tests) | All 7 previously-open filter groups (`dateMin/Max`, `concentrationA/B_Min/Max`, `wavelengthMin/Max`, `instrument`, `modality`, `pFO_rateConstantMin/Max`) now verified to genuinely narrow persisted data via real Specification execution against H2. See Reconciliation §1. |

### 1.2 `ReactantServiceImpl`

All 7 methods (`createReactant`, `updateReactant`, `getReactantDetail`, `getAllReactants`,
`getArchivedReactants`, `archiveReactant`, `restoreReactant`) — **Full**. `determineAggregateFlag`'s
5 outcomes all genuinely produced; the previously-misleading test name is now fixed (see
Reconciliation §7).

### 1.3 `FileServiceImpl`

| Method | Status | Test(s) | Notes |
|---|---|---|---|
| `storeFile` | **Full — resolved this round** | 5 tests | S3 `putObject`-throws → `FileStorageException` branch now covered (see Reconciliation §2). Archived-conflict, empty-file, not-found all covered. |
| `loadFileAsResource` | **Full — resolved this round** | 2 tests | Not-found branch now covered (see Reconciliation §2). |
| `getAttachmentDetail` | Full | 2 tests | — |
| `getAttachmentsOfExperiment` | Full | 2 tests | — |
| `archiveFile` | Full | 4 tests | — |

### 1.4 `StructureServiceImpl`

All 5 methods — **Full**, unchanged from the prior round (already best-in-class; `fetchPendingStructuresFromAPI` remains exceptionally thorough with 6 tests covering every polling-loop branch).

### 1.5 `UserServiceImpl`

All 5 methods — **Full**, unchanged.

### 1.6 `ExperimentFolderServiceImpl`

| Method | Status | Test(s) | Notes |
|---|---|---|---|
| `createFolder` | **Full — resolved this round** | 2 tests | `IllegalStateException` for user-not-found now covered (see Reconciliation §3). |
| `getMyFolders` | Full | 1 test | — |
| `addExperimentToFolder` | Full | 6 tests | Explicit experiment-not-found test now added (see Reconciliation §3). Minor residual: duplicate-add no-op branch untested (new gap, not previously tracked). |
| `removeExperimentFromFolder` | **Partial — still open** | 2 tests | Now has a dedicated folder-**not-found** test calling this method directly, but no dedicated folder-**not-owned** test for this specific method (that branch is only proven via `addExperimentToFolder`/`deleteFolder`). See Reconciliation §3. |
| `deleteFolder` | **Partial — still open** | 2 tests | Inverse of the above: has a dedicated folder-**not-owned** test, but no dedicated folder-**not-found** test for this specific method. See Reconciliation §3. |

### 1.7 `ExperimentHistoryServiceImpl`

| Method | Status | Test(s) | Notes |
|---|---|---|---|
| `getExperimentHistory` | Full | 12 tests | Real-Javers diff-building (CREATE/UPDATE/ARCHIVE/RESTORE, ignored-properties) fully exercised. **Residual, unchanged gap:** the private `extractId` reference-type switch is only exercised for the `solvent` case; `instrument`/`modality`/`reactantA`/`reactantB`/`scientist`/`createdBy` cases remain untested — this was flagged as open in the prior round and confirmed **still open, unchanged** this round (see Reconciliation §item 7 below, "extractId"). |

### 1.8 `OptionServiceImpl` (shared base) + 4 subclasses

Unchanged: still 3-line constructor-only subclasses with zero independent logic; `ChemicalTypeServiceTest` continues to give full coverage of all 4 methods. Representative pattern re-confirmed correct.

---

## 2. Validators

### 2.1 `ExperimentValidator`

| Method | Status | Notes |
|---|---|---|
| `validateForCreate` | Full | All 14 shared branches directly exercised, unchanged. |
| `validateForUpdate` | **Full — resolved this round** | All 4 previously-open branches (reactant-A/B-zero-structures, instrument/modality skip-lookup) now directly exercised through `validateForUpdate` itself, not just via the sibling. See Reconciliation §4. |

### 2.2 `ReactantValidator`

| Method | Status | Notes |
|---|---|---|
| `validateForCreate` | **Full — resolved this round** | `additionalShortNames` blank-entry and shortName-duplicated-in-list sub-branches now covered (see Reconciliation §5). Minor new residual guard-clause gaps found (see §9 New Gaps) — not severe enough to downgrade given they're low-probability guard clauses already indirectly protected by upstream field validation. |
| `validateForUpdate` | **Full — resolved this round** | Same resolution as above, directly through `validateForUpdate`. |

---

## 3. Mappers

### 3.1 Options mappers — representative pattern re-confirmed

`ChemicalTypeMapper`/`InstrumentMapper`/`ModalityMapper`/`SolventMapper` remain byte-for-byte
identical; `SolventMapperTest` remains the sole test file. **Verdict unchanged: defensible.**

| `BaseMapper` method (via `SolventMapperTest`) | Status | Notes |
|---|---|---|
| `toDto` | Partial | No null-input test (unchanged — not part of any round's follow-up work). |
| `toEntity` | Full | — |
| `toDtoList` | Partial | No empty-list test (unchanged). |

### 3.2 Non-options mappers

All previously-open items are now resolved:

| Mapper / Method | Status | Notes |
|---|---|---|
| `ExperimentMapper` (both overloads) | Full | Unchanged from prior round. |
| `FolderMapper.folderToGetFolderDto` | Full | Unchanged. |
| `MeasurementDataMapper.resultToMeasurementDataDto` | Full | Unchanged. |
| `MetadataMapper.metadataToGetMetadataDto` | Full | Unchanged. |
| `ReactantMapper.toDetailDto` | Full | Unchanged. |
| `ReactantMapper.toDto(Reactant, StructureFlag)` (2-arg) | **Full — resolved this round** | A genuinely direct 2-arg call with a literal `null` now exists, verified as a real 2-argument call site (not resolvable to the 1-arg default). See Reconciliation §6. |
| `ReactantMapper.toDto(Reactant)` (1-arg) | Full | Unchanged. |
| `StructureMapper.toDto` | Full | Unchanged, still best-covered. |
| `UserMapper.toDto` | Full | Unchanged. |

---

## 4. Endpoints

### 4.1 `ExperimentEndpoint`

| Method | Status | Test(s) | Notes |
|---|---|---|---|
| `createExperiment` | **Partial** | 2 tests | 201 + 400 (bean-validation only). **New gap:** the service-level `UnprocessableEntityException` (422, semantic reactant validation) is never reached — the only negative test's `reactantA=null` is caught by `@NotNull` bean validation before the service is even called (`verifyNoInteractions` proves this). |
| `updateExperiment` | **Partial** | 3 tests | 200, 403, **404 (resolved this round)**. **New gap:** no 409 test, despite the endpoint's own Javadoc documenting a 409 for updating archived experiments. |
| `getExperiment` | Full | 3 tests | — |
| `searchExperiments` | Full | 2 tests | — |
| `getAuditLogs` | Full | 4 tests | — |
| `getArchivedExperiments` | Full | 3 tests | — |
| `archiveExperiment` | Full | 3 tests | 404 resolved this round. |
| `restoreExperiment` | Full | 3 tests | 404 resolved this round. |

Anonymous/401: confirmed present and genuine. ✅

### 4.2 `ExperimentFilesEndpoint`

| Method | Status | Test(s) | Notes |
|---|---|---|---|
| `uploadFile` | **Partial** | 3 tests | 201, 404, 403. **New gap:** no 409 test (`FileService.storeFile` documents a `ResourceConflictException` for archived experiments, untested at controller level). |
| `downloadFile` | Full | 4 tests | — |
| `getFileDetails` | **Full — resolved this round** | 3 tests | 403 test now added (see Reconciliation §last item). |
| `archiveFile` | **Partial** | 3 tests | 204, 404, 403. **New gap:** no 409 test (same as `uploadFile`). |

Anonymous/401: confirmed present. ✅

### 4.3 `ExperimentFolderEndpoint`

All 5 methods — **Full**. No new gaps found in this controller.

### 4.4 `ReactantEndpoint`

| Method | Status | Test(s) | Notes |
|---|---|---|---|
| `createReactant` | **Partial** | 4 tests | 201×2, 403, 400. **New gap:** no 409 test (shortName-conflict, documented on the service). |
| `updateReactant` | **Partial** | 4 tests | 200, 403×2, **404 (resolved this round)**. **New gap:** no 409 test (shortName-conflict-with-another-reactant). |
| `getReactantDetail` | Full | 2 tests | — |
| `getAllReactants` | Full | 1 test | — |
| `getArchivedReactants` | Full | 3 tests | — |
| `archiveReactant` | Full | 4 tests | 404 resolved this round. |
| `restoreReactant` | Full | 4 tests | 404 resolved this round. |

Anonymous/401: confirmed present. ✅

### 4.5 `StructureEndpoint`

All 3 methods — **Full**. 404 tests added this round for all three (`uploadStructure`,
`generateStructureFromAPI`, `selectStructure`), all genuinely reached past the ADMIN/SUPERVISOR
`@PreAuthorize` gate.

### 4.6 `UserEndpoint`

All 5 methods — **Full**. 404 tests added this round for `setRole`/`archiveUser`; `restoreUser`'s
404 test was already present from a prior round.

### 4.7 Options endpoints — representative pattern re-confirmed

`ChemicalTypeEndpointTest` remains the sole test file; the 4 concrete endpoint classes remain
byte-for-byte identical. **Verdict unchanged: defensible.**

---

## 5. Repositories

Test directory now contains 9 files (all repository interfaces represented): the 5 from the prior
round (`ReactantRepositoryTest`, `ExperimentAttachmentRepositoryTest`,
`ExperimentFolderRepositoryTest`, `ExperimentRepositoryTest`, `ReactantStructureRepositoryTest`)
plus `ChemicalTypeRepositoryTest` (prior round) and **3 new files this round**:
`InstrumentRepositoryTest.java`, `ModalityRepositoryTest.java`, `SolventRepositoryTest.java`.

**All 15 of 15 custom query methods now have full branch coverage (both a true/positive case AND a
false/empty case)**, including the two previously-partial `ReactantRepository` derived finders
(`findAllByArchivedAtIsNull`/`findAllByArchivedAtIsNotNull`, now with genuine zero-matching-rows
empty-list tests) and `existsByNameIgnoreCase` now directly tested against all 4 concrete option
repository beans (`ChemicalType`, `Instrument`, `Modality`, `Solvent` — each with its own
`@Autowired` bean and its own persisted entity, not just re-using `ChemicalTypeRepository`).

**New documentation-consistency gap found:** all 4 option-repository test files
(`ChemicalTypeRepositoryTest`, `InstrumentRepositoryTest`, `ModalityRepositoryTest`,
`SolventRepositoryTest`) carry an **identical, verbatim** class-level Javadoc claiming to be *the*
sole "representative test" standing in for the other three — which is now self-contradictory
across all 4 files simultaneously, since each of the 4 repositories has its own direct test. This
is a copy-paste artifact from creating the 3 new files from the `ChemicalTypeRepositoryTest`
template without updating the framing. Not a test-correctness issue — purely a stale-comment issue.
See Reconciliation §9.

---

## 6. Overall Totals

| Layer | Methods inventoried | Full | Partial | Zero |
|---|---|---|---|---|
| Services (incl. `OptionServiceImpl` representative) | 39 | 37 | 2 | 0 |
| Validators | 4 | 4 | 0 | 0 |
| Mappers (incl. options representative as 1 set of 3) | 13 | 11 | 2 | 0 |
| Endpoints (incl. options representative as 1 set of 4) | 36 | 30 | 6 | 0 |
| Repositories (custom query methods) | 15 | 15 | 0 | 0 |
| **Total** | **107** | **97** | **10** | **0** |

**Direct comparison across all three rounds:**

| Round (baseline) | Full | Partial | Zero | Total |
|---|---|---|---|---|
| Round 1 (255 tests) | 38 | 54 | 14 | 106 |
| Round 2 (359 tests) | 79 | 28 | 0 | 107 |
| **Round 3 (401 tests, this report)** | **97** | **10** | **0** | 107 |

Full coverage is now at **~91%** of all inventoried methods (97/107), up from 74% last round and
36% originally. The remaining 10 partial items are narrow and specific (see §8).

---

## 7. Representative-Testing Pattern — Re-Confirmed (All 4 Families)

- **Options mappers:** unchanged, still correct/defensible.
- **Options services:** unchanged, still correct/defensible.
- **Options endpoints:** unchanged, still correct/defensible.
- **Options repositories — pattern claim explicitly re-verified and now superseded by direct
  testing, exactly as this round's work intended.** Previously (round 2), `existsByNameIgnoreCase`
  was tested only via `ChemicalTypeRepository`, with the other 3 flagged as a residual, low-risk
  gap (real Spring/JPA bean wiring per entity type left unexercised). **This round closed that gap
  properly** — `InstrumentRepositoryTest`/`ModalityRepositoryTest`/`SolventRepositoryTest` were
  added, each genuinely autowiring and persisting against its own concrete repository bean, not
  re-using `ChemicalTypeRepository`. Confirmed via direct read of all 3 new files. **This is the
  one instance where the "representative-only" pattern was deliberately replaced with full
  per-type coverage — and it landed correctly**, though the leftover copy-pasted Javadoc comments
  (still claiming "representative-only" framing in all 4 files) are now inaccurate and should be
  cleaned up (see §5, §8).

---

## 8. Reconciliation Against the Second Round's "Final: Review the Missing Gaps" List

**1. `ExperimentServiceImpl.searchExperiments`/`buildSearchSpecification` (7 filter groups) —
RESOLVED, genuinely.**
`ExperimentSearchSpecificationTest.java` is confirmed to be a real `@DataJpaTest`
(`@AutoConfigureTestDatabase(Replace.NONE)`, `@ActiveProfiles("test")`, `@Import(ExperimentServiceImpl.class)`)
with `ExperimentRepository`/`ReactantRepository`/`UserRepository`/`SolventRepository`/
`InstrumentRepository`/`ModalityRepository` all genuinely `@Autowired` (only the mapper/authz/
file-service/validator are mocked). Each of the 7 previously-flagged filter groups
(`dateMin/Max`, `concentrationA/B_Min/Max`, `wavelengthMin/Max`, `instrument`, `modality`,
`pFO_rateConstantMin/Max`) was individually traced and confirmed to persist at least one row that
would be wrongly included/excluded if the filter were a no-op — i.e., these are genuine
narrowing tests, not accidental pass-throughs. Not a flawed fix.

**2. `FileServiceImpl.storeFile` S3-failure branch, `loadFileAsResource` not-found — RESOLVED.**
`storeFile_ShouldThrowFileStorageException_WhenS3PutObjectFails` genuinely stubs
`s3Client.putObject(...)` to throw and asserts `FileStorageException` + that `save` is never
called. `loadFileAsResource_ShouldThrowNotFound_WhenFileDoesNotExist` genuinely stubs an empty
`Optional` and asserts `ResourceNotFoundException`. Both genuine.

**3. `ExperimentFolderServiceImpl` (createFolder, addExperimentToFolder, removeExperimentFromFolder,
deleteFolder) — MOSTLY RESOLVED, 2 of 4 methods still partial.**
- `createFolder`'s `IllegalStateException` for user-not-found: **RESOLVED**
  (`createFolder_whenUserNotFound_throwsIllegalStateException`).
- `addExperimentToFolder`'s explicit experiment-not-found case: **RESOLVED**, now a dedicated,
  explicit test rather than an incidental byproduct.
- `removeExperimentFromFolder`/`deleteFolder` dedicated negative tests: **STILL PARTIAL.** Each
  method did gain one new, genuinely dedicated negative test that calls that specific method
  directly — but each only covers **one of the two** ownership-helper sub-branches:
  `removeExperimentFromFolder` got a folder-**not-found** test but no folder-**not-owned** test of
  its own; `deleteFolder` got a folder-**not-owned** test but no folder-**not-found** test of its
  own. Both sub-branches are proven *somewhere* in the suite (through the other method or through
  `addExperimentToFolder`), so the underlying shared helper logic is not actually a functional risk
  — but strictly, neither method individually has complete negative-path coverage of its own.

**4. `ExperimentValidator.validateForUpdate` (4 branches) — RESOLVED.**
All 4 (reactant-A-zero-structures, reactant-B-zero-structures, instrument-skip-lookup,
modality-skip-lookup) are now triggered via literal, direct calls to
`experimentValidator.validateForUpdate(...)` — confirmed by tracing each test's stub configuration
to rule out any other branch producing the same outcome. Genuine, not superficial.

**5. `ReactantValidator` additionalShortNames sub-branches (2 sub-branches × 2 methods) —
RESOLVED, all 4 combinations.**
Blank/empty-entry and shortName-duplicated-in-list are both now tested for both
`validateForCreate` and `validateForUpdate`, each test isolated so only the claimed sub-branch
fires (verified by tracing the guard conditions — e.g. the blank-entry tests use inputs with no
case-duplicates and no shortName-inclusion, so only the blank check can be responsible for the
thrown exception).

**6. `ReactantMapper.toDto(Reactant, StructureFlag)` direct 2-arg null invocation — RESOLVED,
genuinely closing the "letter vs. spirit" gap flagged in the prior round.**
`toDto_withExplicitNullFlag_shouldMapNullFlagDirectlyViaTwoArgOverload` contains
`mapper.toDto(reactant, null)` — a literal 2-argument call. Java overload resolution cannot route
a 2-argument call to the 1-arg default method, so this is confirmed to genuinely invoke the 2-arg
overload directly, distinct from the pre-existing `toDto_withoutFlag_shouldDelegateWithNullFlag`
test (which still only exercises the 1-arg delegate). Both tests now coexist, covering both call
paths independently.

**7. `ReactantServiceTest.getAllReactants_ShouldReturnOnlyActiveReactants` rename — RESOLVED.**
Confirmed renamed to `getAllReactants_ShouldAggregateAsPending_WhenOnlyPendingStructureExists`
with the assertion logic unchanged (still correctly asserts `StructureFlag.PENDING`). The name now
accurately describes the branch under test.

**8. `ReactantRepository.findAllByArchivedAtIsNull`/`findAllByArchivedAtIsNotNull` empty-list tests
— RESOLVED.**
Both now have dedicated tests that persist zero matching rows (not just "opposite side present")
and assert an empty result — genuinely closing the previously-flagged gap.

**9. 3 new option-repository test files (Instrument/Modality/Solvent) — RESOLVED, but with a
documentation side-effect.**
All 3 new files exist, each genuinely autowiring and persisting against its own concrete
repository bean (not re-using `ChemicalTypeRepository`), each with both a true (case-insensitive
match) and false (no match) test — structurally identical to `ChemicalTypeRepositoryTest`'s
pattern. **However:** all 4 files (including the original `ChemicalTypeRepositoryTest`) share an
identical, verbatim class-level Javadoc that describes itself as *the* sole representative test
for the other three — this framing is now stale and self-contradictory across all 4 files, since
the "representative-only" pattern has been deliberately superseded by direct per-type testing.
This is a copy-paste documentation artifact, not a test-correctness problem, but should be cleaned
up (the comment should either be removed or rewritten to explain that all 4 now have independent
coverage).

**10. Endpoint 404/403 gaps (`ExperimentEndpointTest`, `ExperimentFilesEndpointTest.getFileDetails`,
`ReactantEndpointTest`, `StructureEndpointTest`, `UserEndpointTest`) — RESOLVED, all of them.**
Every cited 404/403 test genuinely mocks the service to throw the documented exception
(`ResourceNotFoundException`/`AccessDeniedException`) and asserts the corresponding status, under
an authorized principal where relevant (so the branch is genuinely reached past any
`@PreAuthorize` gate rather than short-circuited by a 403). No flawed fixes found among these.

---

## 9. New Gaps Discovered (Not Previously Tracked)

**Controller-layer 409/422 gaps (the most significant new finding this round):** while reconciling
the endpoint 404 fixes, a fresh trace of each endpoint's documented `@throws` against
`GlobalExceptionHandler`'s mappings revealed several **genuinely untested, pre-existing** 409/422
branches that no prior round flagged:
- `ExperimentEndpoint.createExperiment` — no genuine 422 test; the only negative test is caught by
  bean validation before the service is ever called (`verifyNoInteractions` proves it).
- `ExperimentEndpoint.updateExperiment` — no 409 test, despite the endpoint's own Javadoc
  documenting a 409 for updating an archived experiment.
- `ExperimentFilesEndpoint.uploadFile`/`archiveFile` — no 409 test (archived-experiment conflict,
  documented on `FileService`).
- `ReactantEndpoint.createReactant`/`updateReactant` — no 409 test (shortName-conflict, documented
  on `ReactantService`).

These are real, currently-unexercised branches at the `@WebMvcTest` layer, following the exact same
pattern (`doThrow(...).when(service)...; assertStatus(...)`) already established for the 404s this
round — they would be a natural, low-effort next step.

**Minor service/validator-layer residuals (low severity, guard clauses / rare edge cases):**
- `ExperimentHistoryServiceImpl`'s private `extractId` reference-type switch — only the `solvent`
  case is exercised via a real `ReferenceChange`; `instrument`/`modality`/`reactantA`/`reactantB`/
  `scientist`/`createdBy` cases remain untested. Confirmed **unchanged** from the prior round (not
  addressed by this round's work, and not on this round's fix list either).
- `ExperimentFolderServiceImpl.addExperimentToFolder`'s duplicate-add no-op branch (adding an
  experiment already in the target folder) is untested.
- `ReactantValidator`'s `namesToCheck.isEmpty()` guard, the `additionalShortNames == null/empty`
  early-return, and `collectNames`'s blank-primary skip branch are all untested — low risk since
  they're guarded by upstream bean validation in practice, but technically distinct branches.
- `FileServiceImpl.calculateSha256`'s own internal exception-catch branch (checksum computation
  failure) remains untested — low priority, hard to trigger without fault injection.

**Documentation-only issue:** the stale, duplicated "representative test" Javadoc across all 4
option-repository test files (§5, §8 item 9).

---

## 10. Misleading-Test / Flawed-Fix Skepticism Pass

As requested, every cited "resolved" test across all four sub-investigations was checked against
its actual body, not trusted by name.

- **No misleading test names or flawed fixes were found in this round's new work.** Every
  previously-flagged item's fix was traced and confirmed to genuinely exercise the claimed branch:
  the `ExperimentSearchSpecificationTest` filter tests all persist data that would produce a
  different (wrong) result if the filter were a no-op; the `ReactantMapper` 2-arg null test is
  confirmed to be a literal 2-argument call site, not a disguised 1-arg delegate call; the
  `ExperimentValidator`/`ReactantValidator` branch tests were each traced to confirm no other
  guard condition could have produced the same thrown exception; the endpoint 404 tests were all
  confirmed to be reached under an authorized principal (not short-circuited by 403); the
  repository empty-list and per-bean tests were confirmed to genuinely persist zero-matching-row
  scenarios and genuinely instantiate their own repository beans respectively.
- The previously-flagged misleading test name
  (`ReactantServiceTest.getAllReactants_ShouldReturnOnlyActiveReactants`) is confirmed **fixed**
  (renamed, assertion unchanged and still correct).
- The one remaining honesty-adjacent finding is the **stale Javadoc** issue (§5, §8 item 9) — not
  a test-correctness problem, but worth flagging since it inaccurately describes the current state
  of the test suite to future readers.

---

## 11. Overall Verdict

**This round of follow-up work achieved what it set out to do.** Every one of the ~10 items on the
second round's "Final: Review the missing gaps" list is either fully resolved (8 of 10) or mostly
resolved with a narrow, explicitly-scoped residual gap (2 of 10: `removeExperimentFromFolder`/
`deleteFolder`'s asymmetric negative-path coverage). No flawed or superficial fixes were found —
every claimed resolution was traced and confirmed to genuinely exercise the intended branch. Full
coverage rose from 79/107 (74%) to 97/107 (91%), and the total partial-coverage count dropped from
28 to 10, with zero-coverage methods remaining at zero across all three rounds.

## 12. Has the Suite Reached a Stable, Comprehensive State?

**Largely yes, with one clear, bounded pocket of remaining work — not a case for another
open-ended round.** The remaining 10 partial-coverage items fall into two categories:

1. **A newly-discovered, well-defined cluster of controller-layer 409/422 gaps** (§9) — 5 specific
   methods across 3 controllers, all following the exact same test pattern already used
   successfully for this round's 404 fixes. This is the most actionable next step, and it's narrow
   enough to close in a single, small follow-up rather than another full audit round.
2. **A handful of low-risk, low-probability guard-clause/edge-case branches** (private helper
   switch arms, idempotent no-op branches, deep validator guard clauses) that are unlikely to hide
   real bugs given the surrounding validation, and a doc-only Javadoc staleness issue.

Given the trajectory (36% → 74% → 91% full coverage across three rounds, zero-coverage eliminated
after round two, and no flawed fixes found in any round), the test suite is in a genuinely solid
state. I would recommend one targeted, small follow-up specifically for the 409/422 controller
gaps (and, incidentally, the stale Javadoc cleanup), but would **not** recommend another full
audit-and-reconciliation cycle at this scale — the remaining gaps are narrow enough to fix directly
without needing a fourth full re-audit to verify.
