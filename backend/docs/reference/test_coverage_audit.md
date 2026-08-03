# Backend Test-Coverage Gap Audit (Re-Audit)

**Status:** Report-only, read-only investigation. No production or test code was changed to produce
this document. Baseline confirmed at time of writing: **359 tests passing**
(`./mvnw -o clean test`, run and confirmed `BUILD SUCCESS` before and during this audit).

**This is a re-audit.** The previous version of this document (baseline: 255 tests) has been
archived to
[`test_coverage_audit_2026-08-03.md`](./test_coverage_audit_2026-08-03.md). A large amount of
test-only follow-up work landed since then, claiming to close nearly every gap that report flagged.
This document is a **fresh** re-read of all current source and test files (not a diff against the
old report's claims), followed by an explicit line-by-line reconciliation against every item in the
old report's "Prioritized Summary of Most Significant Gaps" section.

Legend: **Full** = tested, positive + negative (or all applicable branches) covered. **Partial** =
tested, but missing a negative case and/or at least one distinct branch. **Zero** = no test
references this method at all.

---

## 1. Services

### 1.1 `ExperimentServiceImpl`

| Method | Tested? | Test(s) | Positive | Negative | Branch notes |
|---|---|---|---|---|---|
| `createExperiment` | Full | `CreateExperiment.*` (4 tests) | Yes | Yes (creator/scientist/reactant not found) | All exercised via the **real** `ExperimentValidator` (not mocked). |
| `getExperiment` | Full | `GetExperiment.*` (6 tests) | Yes | Yes (not found, forbidden) | All authz branches (creator/scientist/admin/forbidden) on archived entity covered. |
| `archiveExperiment` | Full | `ArchiveExperiment.*` (5 tests) | Yes | Yes (not-found, forbidden, **idempotent already-archived no-op**) | Was Partial in old report — now resolved. |
| `restoreExperiment` | Full | `RestoreExperiment.*` (4 tests) | Yes | Yes (not-found, forbidden, **idempotent already-active no-op**) | Was Partial in old report — now resolved. |
| `getArchivedExperiments` | Full | `GetArchivedExperiments.*` (3 tests) | Yes | N/A (never throws) | Both privileged/non-privileged branches + empty case. |
| `updateExperiment` | Full | `UpdateExperiment.*` (6 tests) | Yes | Yes (unauthorized, archived-conflict, archived+unauthorized ordering, reactant-not-found) | Experiment-not-found now also covered (was missing before). |
| `searchExperiments` / `buildSearchSpecification` (private) | **Partial** | `ExperimentServiceTest.SearchExperiments` (mocked happy path) + `ExperimentSearchSpecificationTest` (genuine `@DataJpaTest`, real Specification execution, 9 tests) | Yes | N/A | Was zero real exercise before — now genuinely exercised for: archived-exclusion, `params==null`, reactant A/B cross-match, temperature range, comment (case-insensitive), scientist list, solvent equals, measurement rateConstant range, AND-combination. **Still untested:** `dateMin`/`dateMax`, `concentrationA_Min/Max`, `concentrationB_Min/Max`, `wavelengthMin/Max`, `instrument` filter, `modality` filter, `pFO_rateConstantMin/Max`. |

### 1.2 `ReactantServiceImpl`

| Method | Tested? | Test(s) | Positive | Negative | Branch notes |
|---|---|---|---|---|---|
| `createReactant` | Full | `CreateReactant.*` (9 tests) | Yes | Yes | Thorough, unchanged from before. |
| `updateReactant` | Full | `UpdateReactant.*` (11 tests) | Yes | Yes | PATCH semantics (preserve/clear/set) both branches. |
| `getReactantDetail` | Full | `GetReactantDetail.*` (2) | Yes | Yes | — |
| `getAllReactants` | Full | `GetAllReactants.*` (5 tests) | Yes | N/A | **Resolved** — all 5 `determineAggregateFlag` outcomes now produced (see Reconciliation #10). |
| `getArchivedReactants` | Full | `GetArchivedReactants.*` (1) | Yes | N/A | Exercises the `null`-flag (empty structures) branch. |
| `archiveReactant` | Full | `ArchiveReactant.*` (2) | Yes | Yes | — |
| `restoreReactant` | Full | `RestoreReactant.*` (2) | Yes | Yes | — |

### 1.3 `FileServiceImpl`

| Method | Tested? | Test(s) | Positive | Negative | Branch notes |
|---|---|---|---|---|---|
| `storeFile` | **Partial** | 4 tests | Yes | Yes (empty file, experiment-not-found, **archived-experiment conflict — new**) | **Still open:** no test makes `s3Client.putObject(...)` throw, so the `catch(Exception e) → FileStorageException` branch remains untested. |
| `loadFileAsResource` | **Partial** | 1 test | Yes | No | **Still open** — no not-found test; unchanged from old report. |
| `getAttachmentDetail` | Full | 2 tests | Yes | Yes | — |
| `getAttachmentsOfExperiment` | Full | 2 tests | Yes | Yes | — |
| `archiveFile` | Full | 4 tests | Yes | Yes (not-found, **archived-experiment conflict**, **idempotent no-op — both new**) | Was Partial — now resolved. |

### 1.4 `StructureServiceImpl`

| Method | Tested? | Test(s) | Positive | Negative | Branch notes |
|---|---|---|---|---|---|
| `uploadStructure` | Full | 4 tests | Yes | Yes | — |
| `generateStructureFromAPI` | Full | 6 tests | Yes | Yes | All conflict branches covered. |
| `selectStructure` | Full | 4 tests | Yes | Yes | — |
| `getStructuresByReactant` | Full | 3 tests | Yes | Yes | — |
| `fetchPendingStructuresFromAPI` | Full | **7 new tests** | Yes | Yes | **Resolved** — was zero coverage. See Reconciliation #1. |

### 1.5 `UserServiceImpl`

All 5 methods (`getAllUsers`, `setRole`, `getRole`, `archiveUser`, `restoreUser`) — **Full**,
unchanged from the original audit (this class was already best-in-class).

### 1.6 `ExperimentFolderServiceImpl`

| Method | Tested? | Test(s) | Positive | Negative | Branch notes |
|---|---|---|---|---|---|
| `createFolder` | **Partial** | 1 test | Yes | No | **Still open** — no test for `userRepository.findById` empty → `IllegalStateException`. Unchanged from old report. |
| `getMyFolders` | Full | 1 test | Yes | N/A | — |
| `addExperimentToFolder` | **Partial** | 6 tests | Yes | Yes (experiment-not-owned, folder-not-owned ×2 variants) | Experiment-not-found is not explicitly tested (relies on generic `orElseThrow`), a minor residual gap. |
| `removeExperimentFromFolder` | **Partial** | 1 test | Yes | No | **Still open** — no dedicated negative test for this method itself (shared `getFolderAndValidateOwnership` helper's failure branches only proven via `addExperimentToFolder`'s tests). |
| `deleteFolder` | **Partial** | 1 test | Yes | No | **Still open** — same as above. |

### 1.7 `ExperimentHistoryServiceImpl`

| Method | Tested? | Test(s) | Positive | Negative | Branch notes |
|---|---|---|---|---|---|
| `getExperimentHistory` | Full | 13 tests (7 authz-focused + **6 new real-Javers diff tests**) | Yes | Yes (not-found, forbidden) | **Resolved** — the actual diff-building logic (CREATE/UPDATE/ARCHIVE/RESTORE, reference resolution) is now genuinely exercised with a real Javers instance. See Reconciliation #2. Residual minor gap: only the `solvent` case of the `extractId` reference-type switch is exercised; `instrument`/`modality`/`reactantA`/`reactantB`/`scientist`/`createdBy` cases and the `ClassCastException` catch branch are not individually tested. |

### 1.8 `OptionServiceImpl` (shared base) + 4 option service subclasses

Unchanged from the original audit: `ChemicalTypeServiceImpl`/`InstrumentServiceImpl`/
`ModalityServiceImpl`/`SolventServiceImpl` remain 3-line constructor-only subclasses with zero
independent logic. `ChemicalTypeServiceTest` continues to give full positive+negative branch
coverage of all 4 `OptionService` methods (`getKnownOptions`, `createOptions`, `archiveOption`,
`restoreOption`) — all **Full**. Representative pattern re-confirmed correct.

---

## 2. Validators

### 2.1 `ExperimentValidator`

| Method | Tested? | Positive+Negative | Branch coverage |
|---|---|---|---|
| `validateForCreate` | Full | Yes | All 14 shared branches directly exercised. |
| `validateForUpdate` | **Partial** | Yes | Improved substantially (from 2/14 to ~9/13 branches proven *directly* via `validateForUpdate`), but 4 branches remain provable only via the sibling `validateForCreate`: reactant-A/B-has-zero-structures-at-all (as opposed to present-but-inactive), and instrument/modality "not provided, skip lookup." See Reconciliation #15. |

### 2.2 `ReactantValidator`

| Method | Tested? | Positive+Negative | Branch coverage |
|---|---|---|---|
| `validateForCreate` | **Partial** | Yes | Conflict/existence paths (shortName, CAS, InChIKey, chemicalType, user) all covered. **Gap (broader than previously known):** neither `validateForCreate` NOR `validateForUpdate` tests the `additionalShortNames` "blank/empty entry" or "shortName duplicated inside additionalShortNames" sub-branches — only the "duplicate entries, case-insensitive" sub-branch is ever tested. |
| `validateForUpdate` | **Partial** | Yes | **Mostly resolved** — `hasNameConflictExcludingId`, CAS-changed-conflict, CAS-unchanged-skip, InChIKey-changed-conflict, InChIKey-unchanged-skip are now all directly exercised via `validateForUpdate`. Same residual gap as `validateForCreate` above (blank-entry / shortName-included sub-branches untested by either method). |

---

## 3. Mappers

### 3.1 Options mappers — representative pattern re-confirmed

Re-verified by direct read: `ChemicalTypeMapper`/`InstrumentMapper`/`ModalityMapper`/`SolventMapper`
remain byte-for-byte identical one-line empty interfaces extending `BaseMapper<E,T>`.
`SolventMapperTest` remains the **only** test file in `mappers/options/` (confirmed via `find_path`
— no `InstrumentMapperTest`/`ModalityMapperTest`/`ChemicalTypeMapperTest` were added).
**Verdict unchanged: representative pattern is correct and defensible.**

| `BaseMapper` method (via `SolventMapperTest`) | Tested? | Positive | Negative/edge |
|---|---|---|---|
| `toDto` | Partial | Yes | No null-input test (unchanged from original audit — not part of this round's follow-up work). |
| `toEntity` | Full | Yes | Yes (ignored-field verification). |
| `toDtoList` | Partial | Yes (2 elements) | No empty-list test (unchanged). |

### 3.2 Non-options mappers

| Mapper / Method | Tested? | Positive | Null/edge-case | Notes |
|---|---|---|---|---|
| `ExperimentMapper.experimentToGetExperimentDto(Experiment, List<...>)` (2-arg) | Full | Yes | Yes — **new** null `result`/`metadata` test | Resolved. |
| `ExperimentMapper.experimentToGetExperimentDto(Experiment)` (1-arg) | Full | Yes | Yes — **new** non-empty `files` conversion test | Resolved. |
| `FolderMapper.folderToGetFolderDto` | Full | Yes | Yes — **new** `experiments == null` test (verified to genuinely hit the null branch, not just re-test the empty-list case) | Resolved. |
| `MeasurementDataMapper.resultToMeasurementDataDto` | Full | Yes | Yes — **new** null `errorValue` + fully-null `result` tests | Resolved. |
| `MetadataMapper.metadataToGetMetadataDto` | Full | Yes | Yes — **new** null `instrument`/`modality` test | Resolved. |
| `ReactantMapper.toDetailDto` | Full | Yes | Yes — **new** null and empty structures-list tests | Resolved. |
| `ReactantMapper.toDto(Reactant, StructureFlag)` (2-arg) | **Partial** | Yes | Partial — see note | No test calls this 2-arg overload **directly** with a literal `null` flag; the only null-flag test (`toDto_withoutFlag_shouldDelegateWithNullFlag`) calls the 1-arg default method, which delegates internally. Functionally equivalent code path is exercised, but the letter of "explicit null flag via 2-arg overload" is not met. |
| `ReactantMapper.toDto(Reactant)` (1-arg, default) | Full | Yes | Yes | — |
| `StructureMapper.toDto` | Full | Yes | Yes (null nested `reactant`) | Unchanged, still best-covered. |
| `UserMapper.toDto` | Full | Yes | Yes (null input) | Unchanged. |

---

## 4. Endpoints

### 4.1 `ExperimentEndpoint` (no `@PreAuthorize`; authz delegated to service)

| Method | Tested? | Test(s) | Positive | Negative | Notes |
|---|---|---|---|---|---|
| `createExperiment` | Full | 2 tests | 201 | 400 | No 403/409 branch actually exists at controller level for create (no @PreAuthorize, and conflict/scientist-not-found map to 422 already tested via `whenReactantAIsMissing`). |
| `updateExperiment` | **Partial** | 2 tests | 200 | 403 | **No 404 test** (new/residual gap — `ResourceNotFoundException` from the service never exercised here, unlike `getExperiment`). |
| `getExperiment` | Full | 3 tests | 200 | 403, 404 | — |
| `searchExperiments` | Full | 2 tests | 200 (incl. nested DTO binding) | 400 | — |
| `getAuditLogs` | Full | 4 tests | 200 (USER+ADMIN) | 403, 404 | Best-covered method in the file. |
| `getArchivedExperiments` | Full | 3 tests | 200 (USER, ADMIN, empty) | N/A | No error branch exists to test. |
| `archiveExperiment` | **Partial** | 2 tests | 204 | 403 | **No 404 test.** |
| `restoreExperiment` | **Partial** | 2 tests | 200 | 403 | **No 404 test.** |

Anonymous/401: `anyEndpoint_whenUnauthenticated_returns401` — genuinely no `.with(jwt())`, expects 401. ✅

### 4.2 `ExperimentFilesEndpoint` (no `@PreAuthorize`; ownership in `FileService`)

| Method | Tested? | Test(s) | Positive | Negative | Notes |
|---|---|---|---|---|---|
| `uploadFile` | Full | 3 tests | 201 | 404, 403 | Resolved from zero-negative in old report. |
| `downloadFile` | Full | 4 tests | 200 (incl. header-escape edge case) | 404, 403 | Resolved. |
| `getFileDetails` | **Partial** | 2 tests | 200 | 404 | No 403 test (minor, unlike its siblings which all have 403 coverage — new finding). |
| `archiveFile` | Full | 3 tests | 204 | 404, 403 | Resolved. |

Anonymous/401: resolved. ✅

### 4.3 `ExperimentFolderEndpoint` (no `@PreAuthorize`; ownership in `ExperimentFolderService`)

| Method | Tested? | Test(s) | Positive | Negative | Notes |
|---|---|---|---|---|---|
| `createFolder` | Full | 3 tests | 201 | 400 ×2 | — |
| `getMyFolders` | Full | 2 tests | 200 | 500 (genuine unexpected-exception case) | No ownership-based 403/404 branch exists for this method **by design** (only ever lists the caller's own folders) — the 500 test is the only meaningful negative case available, and it's now present. |
| `addExperimentToFolder` | Full | 3 tests | 200 | 403, 404 | Resolved. |
| `removeExperimentFromFolder` | Full | 3 tests | 200 | 403, 404 | Resolved. |
| `deleteFolder` | Full | 3 tests | 204 | 403, 404 | Resolved. |

Anonymous/401: resolved. ✅

### 4.4 `ReactantEndpoint`

| Method | Tested? | Test(s) | Positive | Negative | Notes |
|---|---|---|---|---|---|
| `createReactant` | Full | 4 tests | 201 ×2 | 403, **400 (new)** | — |
| `updateReactant` | **Partial** | 3 tests | 200 | 403 ×2 | **No 404 test** (new/residual gap). |
| `getReactantDetail` | Full | 2 tests | 200 | 404 | — |
| `getAllReactants` | Full | 1 test | 200 | N/A | No error branch exists to test. |
| `getArchivedReactants` | Full | 3 tests | 200 ×2 | 403 | — |
| `archiveReactant` | **Partial** | 3 tests | 204 | 403 ×2 (**SUPERVISOR-forbidden now resolved**) | **No 404 test** remains (new/residual gap). |
| `restoreReactant` | **Partial** | 3 tests | 200 | 403 ×2 | **No 404 test.** |

Anonymous/401: resolved. ✅

### 4.5 `StructureEndpoint` (all 3 methods: `hasAnyRole('ADMIN','SUPERVISOR')`)

| Method | Tested? | Test(s) | Positive | Negative | Notes |
|---|---|---|---|---|---|
| `uploadStructure` | **Partial** | 3 tests | 200 ×2 | 403 | No 404 test. |
| `generateStructureFromAPI` | **Partial** | 3 tests | 204 ×2 | 403 | No 404 test. |
| `selectStructure` | **Partial** | 3 tests | 200 ×2 | 403 | No 404 test. |

Anonymous/401: resolved (`anyEndpoint_whenUnauthenticated_returns401` present, same pattern as siblings). ✅

### 4.6 `UserEndpoint`

| Method | Tested? | Test(s) | Positive | Negative | Notes |
|---|---|---|---|---|---|
| `getAllUsers` | Full | 3 tests | 200 ×2 | 403 | — |
| `setRole` | **Partial** | 4 tests | 200 | 403 ×2, **400 (new)** | No 404 test remains. |
| `getRole` | Full | 4 tests | 200 (self + admin-on-other) | 404, 403 | Best-covered method — both ownership-expression branches. |
| `archiveUser` | **Partial** | 3 tests | 200 | 403 ×2 | No 404 test remains. |
| `restoreUser` | Full | 4 tests | **200 for ADMIN (new)** | 404, 403 ×2 | **Resolved** — old report's key gap (no ADMIN-success test) is fixed. |

Anonymous/401: resolved. ✅

### 4.7 Options endpoints — representative-pattern re-confirmed

Re-verified: `AbstractOptionsEndpoint` still holds all logic/`@PreAuthorize`; the 4 concrete
endpoint classes remain byte-for-byte identical 3-line subclasses. `find_path` confirms no
`InstrumentEndpointTest`/`ModalityEndpointTest`/`SolventEndpointTest` exist —
`ChemicalTypeEndpointTest` is still the sole representative. **Verdict unchanged: defensible.**

| Method (representative, via `ChemicalTypeEndpointTest`) | Tested? | Positive | Negative |
|---|---|---|---|
| `getKnownOptions` | Full | Yes (USER, ADMIN) | Yes — **anonymous/401 test now present (new, resolved)** |
| `createOptions` | Full | Yes | Yes (403, and now covered by the shared anonymous/401 test) |
| `archiveOption` | Full | Yes | Yes |
| `restoreOption` | Full | Yes | Yes |

---

## 5. Repositories

All repository interfaces re-enumerated and re-read fresh. Test directory now contains 6 files (up
from 1): `ReactantRepositoryTest.java` (pre-existing) plus **5 new files**:
`ExperimentAttachmentRepositoryTest.java`, `ExperimentFolderRepositoryTest.java`,
`ExperimentRepositoryTest.java`, `ReactantStructureRepositoryTest.java`, and
`options/ChemicalTypeRepositoryTest.java`. All 34 repository tests pass.

| Repository | Method | Query type | Tested? | Test citation |
|---|---|---|---|---|
| `ExperimentAttachmentRepository` | `findByExperimentIdAndArchivedAtIsNull(UUID)` | derived | Full | `ExperimentAttachmentRepositoryTest`: found, excludes-archived, empty-list (3 tests) |
| `ExperimentFolderRepository` | `findAllByOwner_Id(String)` | derived | Full | `ExperimentFolderRepositoryTest`: match, no-match (2 tests) |
| `ExperimentRepository` | `findByArchivedAtIsNotNull()` | derived | Full | `ExperimentRepositoryTest`: archived-only, empty (2 tests) |
| `ExperimentRepository` | `findArchivedExperimentsForUser(@Param("userId") String)` | `@Query` | Full | `ExperimentRepositoryTest`: creator-branch, scientist-branch, excludes-unarchived, empty (4 tests). Minor untested edge: user matching both creator AND scientist simultaneously (not required for OR-logic correctness). |
| `ReactantRepository` | `hasNameConflict(Collection<String>)` | `@Query` | Full | `ReactantRepositoryTest` (pre-existing, 4 tests) |
| `ReactantRepository` | `hasNameConflictExcludingId(Collection<String>, UUID)` | `@Query` | Full | `ReactantRepositoryTest` (pre-existing, 3 tests) |
| `ReactantRepository` | `findAllByArchivedAtIsNull()` | derived | **Partial** | `ReactantRepositoryTest`: only the "1 active, 1 archived" case tested; no explicit empty-list test. |
| `ReactantRepository` | `findAllByArchivedAtIsNotNull()` | derived | **Partial** | Same as above — no explicit empty-list test. |
| `ReactantRepository` | `existsByCasNumber(String)` | derived | Full | true + false cases |
| `ReactantRepository` | `existsByInChiKey(String)` | derived | Full | true + false cases |
| `ReactantRepository` | `existsByCasNumberAndIdNot(String, UUID)` | derived | Full | true + false (exclusion) cases |
| `ReactantRepository` | `existsByInChiKeyAndIdNot(String, UUID)` | derived | Full | true + false (exclusion) cases |
| `ReactantStructureRepository` | `findByReactant_Id(UUID)` | derived | Full | match (incl. isolation from other reactant) + empty (2 tests) |
| `ReactantStructureRepository` | `findByFlag(StructureFlag)` | derived | Full | match + empty (2 tests) |
| `OptionRepository<E>` (base; inherited by 4 concrete option repos) | `existsByNameIgnoreCase(String)` | derived | **Partial** | Tested only via `ChemicalTypeRepositoryTest` (2 tests). `InstrumentRepository`/`ModalityRepository`/`SolventRepository` have **zero direct persistence tests of their own** — see Reconciliation #13/pattern discussion below. |

**Totals: 15 of 15 custom query methods now have at least one persistence test** (up from 2/15).
3 of the 15 have residual branch-coverage gaps (missing empty-list case for the two
`ReactantRepository` derived finders; representative-only coverage for the 3 non-`ChemicalType`
option repositories).

---

## 6. Overall Totals

| Layer | Methods inventoried | Full | Partial | Zero |
|---|---|---|---|---|
| Services (incl. `OptionServiceImpl` representative) | 39 | 32 | 7 | 0 |
| Validators | 4 | 1 | 3 | 0 |
| Mappers (incl. options representative as 1 set of 3) | 13 | 10 | 3 | 0 |
| Endpoints (incl. options representative as 1 set of 4) | 36 | 24 | 12 | 0 |
| Repositories (custom query methods) | 15 | 12 | 3 | 0 |
| **Total** | **107** | **79** | **28** | **0** |

**Direct comparison to the archived (255-test-baseline) report:**

| | Full | Partial | Zero | Total |
|---|---|---|---|---|
| Original (`test_coverage_audit_2026-08-03.md`) | 38 | 54 | 14 | 106 |
| **This re-audit** | **79** | **28** | **0** | 107 |

(The total moved from 106 → 107 because this fresh count found `ExperimentEndpoint` has 8
public HTTP-mapped methods, not 7 as originally tallied — a minor original miscount, not a change
in the endpoint itself.)

**Zero-coverage methods have been fully eliminated.** Full coverage roughly doubled (38→79).
Partial-coverage count was cut by nearly half (54→28), and the remaining partials are
overwhelmingly narrow, specific branch/edge-case gaps rather than missing tests altogether.

---

## 7. Representative-Testing Pattern — Re-Confirmed

Re-verified fresh (not assumed) for all three cases:

- **Options mappers:** still byte-for-byte identical, `SolventMapperTest` still the sole test file
  in `mappers/options/`. **Pattern remains correct and defensible.**
- **Options services:** `ChemicalTypeServiceImpl`/`InstrumentServiceImpl`/`ModalityServiceImpl`/
  `SolventServiceImpl` still add zero logic beyond constructor wiring. **Pattern remains correct
  and defensible.**
- **Options endpoints:** the 4 concrete endpoint classes remain byte-for-byte identical;
  `ChemicalTypeEndpointTest` remains the sole test file. **Pattern remains correct and defensible**,
  and its previously-flagged completeness gap (missing anonymous/401 test) is now fixed.
- **New finding — options *repositories*:** unlike the other three families, the options
  repositories were **not** covered by a single pre-existing representative test before this round;
  this round added exactly **one** new test (`ChemicalTypeRepositoryTest`) for the shared
  `OptionRepository.existsByNameIgnoreCase` method, structurally identical to the mapper/service/
  endpoint pattern. This is defensible for verifying the shared JPQL/derived-query *mechanics*, but
  unlike mappers/services/endpoints — which are pure code with no I/O — each concrete option
  repository is a distinct Spring-managed JPA bean bound to a distinct entity
  (`Instrument`/`Modality`/`Solvent` vs `ChemicalType`). A `@DataJpaTest` exercises real bean wiring
  and entity-to-column mapping, which genuinely could diverge per entity even if the repository
  interface text does not. **Verdict: the representative pattern is reasonable here too, but it is
  the weakest instance of the four (mappers/services/endpoints/repositories) representative claims,
  since it leaves real Spring/JPA wiring for 3 of 4 concrete beans permanently unexercised** — this
  is a defensible, low-risk gap, not a false claim.

---

## 8. Reconciliation Against the Archived Report's ~15 Flagged Gaps

### Tier 1 (was: zero coverage)

**1. `StructureServiceImpl.fetchPendingStructuresFromAPI()` — RESOLVED.**
`StructureServiceTest.java` now has 7 dedicated tests covering: empty-list early-return,
finished+multi-structure loop, finished+empty/null-structures→FAILED, failed→FAILED, and the
per-job `catch(Exception)` resilience path (one job throws, the other still processes). All
genuinely exercise their claimed branch with real assertions, not just "doesn't throw."

**2. 13 of 15 repository query methods had zero persistence tests — RESOLVED (all 13), with one
residual pattern-level caveat.**
All 13 previously-untested methods now have direct `@DataJpaTest` coverage in 5 new test files
(see §5 table for citations). The one caveat: `OptionRepository.existsByNameIgnoreCase` (item 13
of the original list) is tested only via `ChemicalTypeRepository`, leaving `InstrumentRepository`/
`ModalityRepository`/`SolventRepository` beans themselves unexercised in any `@DataJpaTest` — see
§7 discussion. Not a false "resolved" claim, but worth tracking as intentional residual risk.

**3. `ExperimentHistoryServiceImpl.getExperimentHistory` diff-building logic — RESOLVED.**
6 new tests in `ExperimentHistoryServiceTest.java` use a `delegateToRealJavers(...)` helper that
makes the mocked `javers.findChanges(any())` forward to a genuine in-memory `JaversBuilder`
instance with real committed entity snapshots. Verified this is **not** a disguised empty-stub —
the tests assert exact computed diff strings (e.g. `"Old comment -> New comment"`) and correctly
detect CREATE/ARCHIVE/RESTORE/ignored-property scenarios. Minor residual: only the `solvent` case
of the internal `extractId` reference-type switch is exercised via a real `ReferenceChange`; the
`instrument`/`modality`/`reactantA`/`reactantB`/`scientist`/`createdBy` cases remain untested.

**4. `ExperimentServiceImpl.searchExperiments`/`buildSearchSpecification` — RESOLVED for the
majority of branches, several sub-branches still open.**
New `ExperimentSearchSpecificationTest.java` is a genuine `@DataJpaTest` (verified: real
`ExperimentRepository`/`ReactantRepository`/etc. autowired, only `ExperimentMapper`/`FileService`/
`ExperimentAuthorization`/`ExperimentValidator` are mocked; data persisted via `saveAndFlush`,
`Specification` genuinely executed against H2). Confirmed NOT a disguised mock. Covers: global
archived-exclusion, null-params, reactant A/B cross-match (incl. commutative), temperature range
(boundary-inclusive), comment (case-insensitive partial), scientist-list, solvent-equals,
measurement rateConstant range, and AND-combination of two filters. **Still open:**
`dateMin`/`dateMax`, `concentrationA/B_Min/Max`, `wavelengthMin/Max`, `instrument`-equals,
`modality`-equals, `pFO_rateConstantMin/Max` — each a simple, structurally-identical
`if (x != null) predicates.add(...)` branch that mirrors an already-tested pattern (e.g. `solvent`)
but has not itself been exercised.

**5. `UserEndpoint.restoreUser` — RESOLVED.**
`restoreUser_WhenAdmin_ShouldReturn200_WhenSuccessful` now exists alongside the retained 404 test,
with `verify(userService, times(1)).restoreUser(id)` — genuinely proves a successful restore for
the one authorized role.

### Tier 2 (was: missing negative-path / authz-branch tests)

**6. `ExperimentFolderEndpoint` zero negative-path coverage (all 4 methods) — RESOLVED.**
`addExperimentToFolder`, `removeExperimentFromFolder`, `deleteFolder` each gained a 403
(not-owned) and a 404 (not-found) test. `getMyFolders` gained a 500 (unexpected-exception) test —
the only meaningful negative case available, since this method has no ownership-failure mode by
design (it only ever lists the caller's own folders).

**7. `ExperimentFilesEndpoint` zero negative tests (`uploadFile`/`archiveFile`/`downloadFile`) —
RESOLVED.**
All three gained 404 and 403 tests, each genuinely mocking the service to throw
`ResourceNotFoundException`/`AccessDeniedException` and asserting the corresponding status.
Minor new finding: sibling method `getFileDetails` still has no 403 test (only 200/404) — flagged
as new gap #4 below.

**8. `ReactantEndpoint.archiveReactant` missing SUPERVISOR-forbidden test — RESOLVED.**
`archiveReactant_WhenSupervisor_ShouldReturn403` now exists and verifies the service is never
invoked.

**9. Anonymous/401 tests essentially untested project-wide — RESOLVED.**
Verified line-by-line: `ExperimentEndpointTest`, `ExperimentFilesEndpointTest`,
`ExperimentFolderEndpointTest`, `ReactantEndpointTest`, `UserEndpointTest`, `StructureEndpointTest`,
and `ChemicalTypeEndpointTest` (options representative) each now have an
`anyEndpoint_whenUnauthenticated_returns401`-style test that performs a request with **no**
`.with(jwt())` at all and asserts `status().isUnauthorized()` — confirmed none accidentally use a
zero-authority JWT (which would yield 403, not 401).

**10. Request-body validation (400) tests for `ReactantEndpoint.createReactant`/
`UserEndpoint.setRole` — RESOLVED (both).**
Both new tests supply an invalid body (`shortName = null` / `RoleDto(null)`), assert 400, and
verify the service method is never called — genuine validation-rejection tests.

**11. `ExperimentServiceImpl.archiveExperiment`/`restoreExperiment` and `FileServiceImpl.storeFile`/
`archiveFile` missing not-found/idempotent/conflict tests — MOSTLY RESOLVED, one still open.**
`archiveExperiment`/`restoreExperiment`: fully resolved (not-found + idempotent no-op both added
for both methods). `FileServiceImpl.archiveFile`: fully resolved (conflict + idempotent no-op both
added). `FileServiceImpl.storeFile`: archived-experiment conflict resolved, but the S3-failure
(`catch(Exception e) → FileStorageException`) branch **remains untested** — no test makes
`s3Client.putObject(...)` throw.

**12. `ReactantValidator.validateForUpdate` additionalShortNames/CAS/InChIKey conflict branches
unreached — MOSTLY RESOLVED, with a broader residual finding.**
`hasNameConflictExcludingId`, CAS-changed/unchanged, and InChIKey-changed/unchanged are now all
directly exercised via `validateForUpdate`. However, on this fresh read, the "blank/empty entry"
and "shortName included in additionalShortNames" sub-branches of `validateAdditionalShortNames`
turn out to be untested by **either** `validateForCreate` or `validateForUpdate` — the original
report's framing ("only proven via `validateForCreate`") was itself slightly inaccurate; the
correct, more precise finding is that these two specific sub-branches have never been exercised by
any test, old or new.

### Tier 3 (was: incomplete branch coverage)

**13. `ReactantServiceImpl.determineAggregateFlag` — RESOLVED.**
5 new/updated tests in `ReactantServiceTest.GetAllReactants` now produce all 5 outcomes
(`PENDING`, `ACTIVE`, `READY`, `FAILED`, `REJECTED`) plus the pre-existing `null`-for-empty-list
case, each verified to genuinely force the intended branch through the real `anyMatch`/`allMatch`
chain. **Flag for the record:** `getAllReactants_ShouldReturnOnlyActiveReactants` is a
**misleadingly-named test** — despite its name, it actually builds a `PENDING`-flag structure and
asserts `StructureFlag.PENDING`, not `ACTIVE`. The assertion itself is correct and does test the
PENDING branch as intended; only the test's name is inaccurate. Not a functional problem, but
worth renaming for clarity.

**14. Mapper null/edge-case gaps (`MetadataMapper`/`MeasurementDataMapper`/`FolderMapper`/
`ExperimentMapper`/`ReactantMapper`) — MOSTLY RESOLVED, one nuance.**
`MetadataMapper` (null instrument/modality), `MeasurementDataMapper` (null errorValue, fully-null
result), `FolderMapper` (`experiments == null` — confirmed to genuinely exercise the null branch,
not just re-test empty-list), and `ExperimentMapper` (null result/metadata, non-empty files
conversion) are all resolved with real, verified test bodies. `ReactantMapper`: null/empty
structures lists resolved; however, the specific ask of "explicit null flag via 2-arg overload"
is only indirectly met — the sole null-flag test calls the 1-arg default method, which internally
delegates to the 2-arg overload with `null`, rather than calling the 2-arg overload directly.
Functionally equivalent, but not a literal match to the original gap description.

**15. `ExperimentValidator.validateForUpdate` — MOSTLY RESOLVED.**
Went from 2/14 branches proven directly via `validateForUpdate` to approximately 9/13 (scientist
missing, reactantA/B missing, reactants-equal, structure-present-but-inactive, solvent missing,
instrument/modality provided-but-not-found, happy path). **Still open:** reactant-has-zero-
structures-at-all (as opposed to present-but-inactive) for both A and B, and the
instrument/modality "not provided, skip lookup" branches — these 4 remain provable only via the
sibling `validateForCreate`.

---

## 9. Tests Checked for Misleading Names or Flawed Fixes (Skepticism Pass)

As requested, every follow-up test cited above was checked against its actual body, not trusted by
name. Findings:

- **One misleadingly-named test found:** `ReactantServiceTest.getAllReactants_ShouldReturnOnlyActiveReactants`
  actually exercises the `PENDING` branch of `determineAggregateFlag`, not `ACTIVE`. The assertion
  is correct; only the name is wrong. Recommend a rename in a future pass (not done here — this
  audit is read-only).
- **No flawed/non-exercising fixes found.** Every other cited "resolved" test was verified to
  contain assertions that actually depend on the claimed branch being taken (e.g., `verify(...,
  never())` calls proving a service method was NOT invoked on a validation-rejection path; exact
  diff-string assertions proving real Javers computation occurred rather than a stub; `saveAndFlush`
  + real `Specification` execution against H2 rather than a mocked repository call).
- **One "letter vs. spirit" gap found (not misleading, but incomplete):** the "explicit null flag
  via 2-arg overload" fix for `ReactantMapper` only indirectly exercises that code path (see
  Reconciliation #14). This is not a flaw — the underlying behavior is genuinely covered — but it's
  worth noting the fix didn't literally add the test that was asked for.

---

## 10. Overall Verdict

**This round of follow-up work substantially achieved its goal.** Every Tier-1 (zero-coverage) item
from the archived report is resolved, all zero-coverage methods across the entire codebase have
been eliminated (14 → 0), and the large majority of Tier-2/Tier-3 items are either fully or mostly
resolved with genuine, verified test bodies (not name-only fixes). Full coverage nearly doubled
(38 → 79 of ~107 methods) and partial-coverage count was cut roughly in half (54 → 28).

The remaining 28 partial-coverage items are now uniformly narrow and specific — missing individual
filter branches in `buildSearchSpecification`, a handful of missing 404 tests on endpoints that
otherwise have solid role/authz coverage, one untested exception-catch branch in `FileServiceImpl`,
and a couple of validator sub-branches that were apparently never targeted by name in either the
original report or the follow-up work (the `additionalShortNames` blank/duplicate-inclusion
sub-branches). None of the remaining gaps represent a wholesale untested code path; they are the
kind of granular edge cases that are reasonable to track incrementally rather than treat as
blocking.
