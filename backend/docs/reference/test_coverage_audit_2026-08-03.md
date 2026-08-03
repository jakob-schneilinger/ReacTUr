# Backend Test-Coverage Gap Audit

**Status:** Report-only, read-only investigation. No production or test code was changed to produce
this document. Baseline at time of writing: 255 tests passing (`./mvnw -o clean test`, not re-run
for this audit — findings are based entirely on reading source and test files).

**Scope:** Services, Validators, Mappers, Endpoints, Repositories, as requested. For every `public`
method (excluding trivial getters/setters, Lombok-generated code, and constructors) we report:
tested (yes/no), which test(s) cover it, whether both a positive and a negative/error case exist,
and whether all distinct branches are exercised.

Legend: **Full** = tested, positive + negative (or all applicable branches) covered. **Partial** =
tested, but missing a negative case and/or missing at least one distinct branch. **Zero** = no test
references this method at all.

---

## 1. Services

### 1.1 `ExperimentServiceImpl`

| Method | Tested? | Test(s) | Positive | Negative | Branch notes |
|---|---|---|---|---|---|
| `createExperiment` | Full | `createExperiment_whenValidInput_returnsGetExperimentDto`, `createExperiment_whenCreatorNotFound_throwsIllegalStateException`, `createExperiment_whenScientistNotFound_throwsUnprocessableEntityException`, `createExperiment_whenReactantIsMissing_throwsUnprocessableEntityException` | Yes | Yes | Creator/scientist/reactant-not-found all covered. |
| `getExperiment` | Full | `getExperiment_whenFound_returnsDto`, `getExperiment_whenNotFound_throwsResourceNotFoundException`, `getExperiment_whenArchivedAndUserIsCreator_returnsDto`, `...UserIsScientist_returnsDto`, `...UserIsAdmin_returnsDto`, `...UserIsForbidden_throwsAccessDeniedException` | Yes | Yes | All authz branches (creator/scientist/admin/forbidden) + not-found covered. |
| `archiveExperiment` | Partial | `archiveExperiment_whenUserIsCreator_archivesAndSaves`, `...UserIsAdmin_archivesAndSaves`, `...UserIsForbidden_throwsAccessDeniedException` | Yes | Authz only | **Missing:** not-found (`ResourceNotFoundException`) test; idempotent "already archived" no-op branch untested. |
| `restoreExperiment` | Partial | `restoreExperiment_whenUserIsScientist_restoresAndSaves`, `...UserIsForbidden_throwsAccessDeniedException` | Yes | Authz only | **Missing:** not-found test; idempotent "already active" branch untested. |
| `getArchivedExperiments` | Full | `getArchivedExperiments_whenUserIsAdmin_fetchesAllArchived`, `...UserIsStandardUser_fetchesUserSpecificArchived`, `...whenEmptyResults_returnsEmptyArray` | Yes | N/A (never throws) | Both privileged/non-privileged branches + empty-result covered. |
| `updateExperiment` | Partial | `updateExperiment_whenValidAndAuthorized_updatesFieldsAndReturnsDto`, `...ShouldFullyReplaceEveryDtoField_WhenUpdating`, `...whenUnauthorizedUser_throwsAccessDeniedException`, `...whenArchived_throwsIllegalStateException` (actually asserts `ResourceConflictException`), `...whenArchivedAndUnauthorizedUser_throwsAccessDeniedException`, `...whenReactantNotFound_throwsUnprocessableEntityException` | Yes | Yes | **Missing:** experiment-not-found (`ResourceNotFoundException`) test. Full field-replacement and archived-conflict ordering well covered. |
| `searchExperiments` | Partial | `searchExperiments_executesPaginatedQueryAndReturnsMappedDtos` | Yes | No | **Significant gap:** only one happy-path test, and `experimentRepository.findAll` is mocked, so the `Specification` lambda built by `buildSearchSpecification` (params-null check, reactant A/B cross-match, all metadata range filters, comment/scientist/solvent/instrument/modality/measurement filters) is never actually invoked by any test. |

### 1.2 `ReactantServiceImpl`

| Method | Tested? | Test(s) | Positive | Negative | Branch notes |
|---|---|---|---|---|---|
| `createReactant` | Full | `createReactant_ShouldSaveSuccessfully_WhenValidData`, `...ShouldThrowUnprocessableEntity_WhenChemicalTypeDoesNotExist`, `...ShouldThrowIllegalState_WhenCurrentUserDoesNotExist`, `...ShouldThrowResourceConflictException_WhenShortNameExists`, `...WhenInChiKeyExists`, `...ShouldThrowConflict_WhenAdditionalShortNameAlreadyExistsAsPrimary`, `...ShouldThrowValidationException_WhenAdditionalShortNamesDifferOnlyByCase`, `...WhenShortNameAndAdditionalShortNameDifferOnlyByCase`, `...ShouldPassLowercasedNamesToRepository_WhenCheckingForConflicts` | Yes | Yes | Both null-vs-provided-field ternary branches covered. |
| `updateReactant` | Full | `updateReactant_ShouldUpdateSuccessfully_WhenValidData`, `...ShouldUpdateChemicalType_WhenNewChemicalTypeProvided`, `...ShouldThrowUnprocessableEntity_WhenNewChemicalTypeDoesNotExist`, `...ShouldThrowResourceNotFound_WhenIdDoesNotExist`, `...ShouldThrowResourceConflict_WhenNewShortNameBelongsToOtherReactant`, `...ShouldOnlyUpdateProvidedFields_WhenPatching`, `...ShouldClearFields_WhenEmptyStringOrListProvided`, `...ShouldSetSmilesAndShortNameToNewValue_WhenPatching`, `...ShouldThrowValidationException_WhenShortNameBlanked(WithWhitespaceOnly)`, `...ShouldThrowValidationException_WhenSmilesBlanked` | Yes | Yes | Excellent PATCH-semantics coverage (omitted/new-value/cleared fields all individually verified). |
| `getReactantDetail` | Full | `getReactantDetail_ShouldReturnDetails_WhenExists`, `...ShouldThrowResourceNotFound_WhenDoesNotExist` | Yes | Yes | Fully covered. |
| `getAllReactants` | Partial | `getAllReactants_ShouldReturnOnlyActiveReactants` | Yes | N/A | Only exercises the `PENDING`-flag branch of the private `determineAggregateFlag` helper; `ACTIVE`/`READY`/all-`FAILED`/fallback-`REJECTED` branches never produced. |
| `getArchivedReactants` | Partial | `getArchivedReactants_ShouldReturnOnlyArchivedReactants` | Yes | N/A | Only exercises the "empty structures → null flag" branch of `determineAggregateFlag`. |
| `archiveReactant` | Full | `archiveReactant_ShouldSetArchivedAt_WhenExists`, `...ShouldThrowResourceNotFound_WhenDoesNotExist` | Yes | Yes | Fully covered (no idempotency guard in source). |
| `restoreReactant` | Full | `restoreReactant_ShouldClearArchivedAt_WhenExists`, `...ShouldThrowResourceNotFound_WhenDoesNotExist` | Yes | Yes | Fully covered. |

Cross-cutting: only 2 of 5 possible outcomes of the shared private `determineAggregateFlag` helper (used by `getAllReactants`, `getArchivedReactants`, `restoreReactant`) are ever produced across the whole test file.

### 1.3 `FileServiceImpl`

| Method | Tested? | Test(s) | Positive | Negative | Branch notes |
|---|---|---|---|---|---|
| `storeFile` | Partial | `storeFile_ShouldStoreSuccessfully`, `...ShouldThrowNotFound_WhenExperimentDoesNotExist`, `...ShouldThrowIllegalArgumentException_WhenFileIsEmpty` | Yes | Yes | **Missing:** archived-experiment conflict branch (`ResourceConflictException`); S3-upload-failure → `FileStorageException` branch. Authorization mocked to never throw (by design, deferred to `ExperimentAuthorization`'s own tests). |
| `loadFileAsResource` | Partial | `loadFileAsResource_ShouldReturnResource` | Yes | No | **Missing:** not-found test for this method. |
| `getAttachmentDetail` | Full | `getAttachmentDetail_ShouldReturnDto`, `...ShouldThrowNotFound` | Yes | Yes | Covered. |
| `getAttachmentsOfExperiment` | Full | `getAttachmentsOfExperiment_ShouldReturnList`, `...ShouldThrowNotFound_WhenExperimentDoesNotExist` | Yes | Yes | Covered. |
| `archiveFile` | Partial | `archiveFile_ShouldThrowNotFound_WhenFileDoesNotExist`, `archiveFile_ShouldArchiveSuccessfully_WhenFileIsActive` | Yes | Yes (not-found only) | **Missing:** archived-experiment conflict branch; already-archived-file idempotent early-return branch. |

### 1.4 `StructureServiceImpl`

| Method | Tested? | Test(s) | Positive | Negative | Branch notes |
|---|---|---|---|---|---|
| `uploadStructure` | Partial | `uploadStructure_shouldSaveAndReturnDto`, `...whenReactantNotFound_shouldThrowResourceNotFoundException`, `...whenFileIsEmpty_shouldThrowIllegalArgumentException`, `...shouldDeleteExistingAndSaveNewAsActive` | Yes | Yes | **Missing:** `IOException` → `RuntimeException` branch. |
| `generateStructureFromAPI` | Full | `generateStructureFromAPI_shouldTriggerApiAndSavePendingEntity`, `...shouldTriggerApi_whenOnlyActiveExistsFromManualUpload`, `...whenReactantNotFound_shouldThrowResourceNotFoundException`, `...whenApiFails_shouldBubbleUpException`, `...shouldThrowConflict_whenPendingExists`, `...shouldThrowConflict_whenReadyExists`, `...shouldThrowConflict_whenActiveAndRejectedExist` | Yes | Yes | All conditional branches explicitly covered. Best-covered method in this class. |
| `selectStructure` | Full | `selectStructure_shouldSetActiveAndRejectOthers`, `...whenStructureNotFound_shouldThrowResourceNotFoundException`, `...whenStructureIsAlreadyActive_shouldDoNothingAndReturnDto`, `...shouldSwapActiveAndRejected` | Yes | Yes | Both branches (already-active early-return; promote/reject) covered. |
| `getStructuresByReactant` | Full | `getStructuresByReactant_shouldReturnMappedDtos`, `...whenReactantNotFound_shouldThrowResourceNotFoundException`, `...whenReactantExistsButNoStructures_shouldReturnEmptyList` | Yes | Yes | Covered, incl. empty-result case. |
| `fetchPendingStructuresFromAPI` | **Zero** | — | No | No | **No test at all.** Entire scheduled-polling logic (empty-list early-return, finished+multi-structure loop, finished-but-empty→FAILED, failed→FAILED, per-job exception resilience) is untested. |

### 1.5 `UserServiceImpl`

| Method | Tested? | Test(s) | Positive | Negative | Branch notes |
|---|---|---|---|---|---|
| `getAllUsers` | Full | `getAllUsers_ShouldReturnMappedDtos`, `...ShouldReturnEmptyList_WhenNoUsersExist` | Yes | N/A | Covered, incl. empty case. |
| `setRole` | Full | `setRole_ShouldUpdateRole_WhenUserExists`, `...ShouldNotSave_WhenRoleIsAlreadySet`, `...ShouldThrowNotFound_WhenUserDoesNotExist` | Yes | Yes | Both change/no-op branches + not-found covered. |
| `getRole` | Full | `getRole_ShouldReturnRole_WhenUserExists`, `...ShouldThrowNotFound_WhenUserDoesNotExist` | Yes | Yes | Covered. |
| `archiveUser` | Full | `archiveUser_ShouldSetDeactivatedAt_WhenUserIsActive`, `...ShouldNotUpdateTimestamp_WhenUserIsAlreadyArchived`, `...ShouldThrowNotFound_WhenUserDoesNotExist` | Yes | Yes | Both idempotency branches + not-found covered. |
| `restoreUser` | Full | `restoreUser_ShouldNullifyDeactivatedAt_WhenUserIsArchived`, `...ShouldNotSave_WhenUserIsAlreadyActive`, `...ShouldThrowNotFound_WhenUserDoesNotExist` | Yes | Yes | Best-covered class of the 7 non-options services. |

### 1.6 `ExperimentFolderServiceImpl`

| Method | Tested? | Test(s) | Positive | Negative | Branch notes |
|---|---|---|---|---|---|
| `createFolder` | Partial | `createFolder_whenValidInput_returnsGetFolderDto` | Yes | No | **Missing:** `IllegalStateException` when owner user profile lookup fails. |
| `getMyFolders` | Full | `getMyFolders_returnsOnlyOwnedFolders` | Yes | N/A | Covered. |
| `addExperimentToFolder` | Partial | `addExperimentToFolder_whenExperimentInAnotherFolder_movesExperimentToNewFolder`, `...whenExperimentNotOwned_throwsAccessDeniedException`, `...whenFolderNotOwned_throwsAccessDeniedException`, `...whenDifferentUsersAddSameExperiment_doesNotInterfere`, `...whenUserDoesNotOwnFolder_throwsAccessDeniedException` | Yes | Yes | **Missing:** experiment-not-found test; "already exists in target folder" (no-duplicate) branch. |
| `removeExperimentFromFolder` | Partial | `removeExperimentFromFolder_removesAndSaves` | Yes | No | **Missing:** dedicated negative test (relies only on shared helper tested via `addExperimentToFolder`). |
| `deleteFolder` | Partial | `deleteFolder_whenAuthorized_deletesFolder` | Yes | No | **Missing:** dedicated negative test (not directly proven to throw when unauthorized). |

### 1.7 `ExperimentHistoryServiceImpl`

| Method | Tested? | Test(s) | Positive | Negative | Branch notes |
|---|---|---|---|---|---|
| `getExperimentHistory` | Partial | `getExperimentHistory_whenNotFound_throwsResourceNotFoundException`, `...whenActiveAndUnrelatedApprovedUser_returnsHistory`, `...whenArchivedAndUserIsCreator_returnsHistory`, `...UserIsScientist_returnsHistory`, `...UserIsAdmin_returnsHistory`, `...UserIsSupervisor_returnsHistory`, `...UserIsForbidden_throwsAccessDeniedException` | Yes | Yes | Excellent authz-branch coverage + not-found, **but** every test stubs `javers.findChanges(any())` to return an empty result — so `buildEntityLookupMap`, CREATE/UPDATE/ARCHIVE/RESTORE detection, and value/reference-change diffing (the actual "history" logic) is **never exercised at all**. |

### 1.8 `OptionServiceImpl` (shared base for options services) + 4 subclasses

Confirmed by direct read: `ChemicalTypeServiceImpl`, `InstrumentServiceImpl`, `ModalityServiceImpl`,
`SolventServiceImpl` are 3-line classes that only wire a repository/mapper into the constructor —
**zero additional logic**. All behavior (including the `ResourceConflictException` /
`ResourceNotFoundException` throws) lives in `OptionServiceImpl`, tested via `ChemicalTypeServiceTest`
(which directly instantiates `ChemicalTypeServiceImpl`).

| Method | Tested? | Test(s) | Positive | Negative | Branch notes |
|---|---|---|---|---|---|
| `getKnownOptions` | Full | `getKnownOptions_ShouldReturnList_WhenOptionsExist` | Yes | N/A | Covered. |
| `createOptions` | Full | `createOptions_ShouldSaveAndReturnUpdatedList_WhenValidData`, `...ShouldThrowResourceConflictException_WhenNameAlreadyExists` | Yes | Yes | Both branches covered. |
| `archiveOption` | Full | `archiveOption_ShouldSetArchivedAt_WhenExists`, `...ShouldThrowResourceNotFound_WhenDoesNotExist` | Yes | Yes | Covered. |
| `restoreOption` | Full | `restoreOption_ShouldClearArchivedAt_WhenExists`, `...ShouldThrowResourceNotFound_WhenDoesNotExist` | Yes | Yes | Covered. |

**Verdict:** the "one representative test" pattern for options services is defensible — confirmed,
not assumed — since the 4 subclasses genuinely add no logic of their own.

---

## 2. Validators

### 2.1 `ExperimentValidator`

Both public methods delegate to a shared private `validate(dto, message)` (14 distinct branches:
scientist missing, reactantA/B missing, reactantA==reactantB, reactantA/B missing active structure,
structure present-but-not-ACTIVE, solvent missing, instrument provided-but-missing, instrument
skipped, modality provided-but-missing, modality skipped, full happy path, partial happy path).

| Method | Tested? | Test(s) | Positive | Negative | Branch notes |
|---|---|---|---|---|---|
| `validateForCreate` | Full | 14 tests in `ExperimentValidatorTest` (see below) | Yes (2 variants) | Yes | All 14 branches of the shared validation logic are exercised through this entry point. |
| `validateForUpdate` | Partial | `validateForUpdate_shouldReturnResolvedReferences_whenAllReferencesValid`, `validateForUpdate_shouldThrowValidationException_whenReactantsAreTheSame` | Yes | Yes (1 case) | Only 2 of the 14 shared branches are exercised **directly** through `validateForUpdate`; the other 12 (missing scientist/reactant, inactive structure, missing solvent/instrument/modality) are only ever invoked via `validateForCreate` in the test suite. Low risk since the logic is shared, but `validateForUpdate` itself has no dedicated proof. |

`validateForCreate` full test list: `...shouldReturnResolvedReferences_whenAllReferencesValid`,
`...shouldReturnNullInstrumentAndModality_whenNotProvided`, `...shouldThrowUnprocessableEntity_whenScientistNotFound`,
`...whenReactantAMissing`, `...whenReactantBMissing`, `...shouldThrowValidationException_whenReactantsAreTheSame`,
`...whenReactantAHasNoActiveStructure`, `...whenReactantBHasNoActiveStructure`, `...whenReactantStructureIsNotActive`,
`...whenSolventNotFound`, `...whenInstrumentProvidedButNotFound`, `...shouldSkipInstrumentLookup_whenInstrumentNotProvided`,
`...whenModalityProvidedButNotFound`, `...shouldSkipModalityLookup_whenModalityNotProvided`.

### 2.2 `ReactantValidator`

| Method | Tested? | Test(s) | Positive | Negative | Branch notes |
|---|---|---|---|---|---|
| `validateForCreate` | Partial | `...shouldReturnResolvedReferences_whenAllReferencesValid`, `...shouldThrowIllegalState_whenCurrentUserDoesNotExist`, `...shouldThrowUnprocessableEntity_whenChemicalTypeDoesNotExist`, `...shouldThrowResourceConflict_whenShortNameAlreadyExists`, `...shouldThrowResourceConflict_whenCasNumberAlreadyTaken`, `...shouldThrowResourceConflict_whenInChiKeyAlreadyExists`, `...shouldReturnResolvedReferences_whenInChiKeyIsNovel`, `...shouldThrowValidationException_whenAdditionalShortNamesDifferOnlyByCase` | Yes | Yes | **Missing:** blank/empty entries inside `additionalShortNames`; `shortName` duplicated inside `additionalShortNames`; `namesToCheck` empty (skip-check) branch; blank `casNumber`/`inChiKey` skip-check branches. |
| `validateForUpdate` | Partial | `...shouldReturnNewChemicalType_whenChemicalTypeChanged`, `...shouldReturnNullChemicalType_whenChemicalTypeNotChanged`, `...shouldThrowUnprocessableEntity_whenNewChemicalTypeDoesNotExist`, `...shouldThrowValidationException_whenShortNameIsBlank`, `...whenShortNameIsWhitespaceOnly`, `...whenSmilesIsBlank` | Yes | Yes (partial) | **Significant gap:** no test ever sets `additionalShortNames` on the update path, so `validateAdditionalShortNames` is entirely unreached via `validateForUpdate`. Also untested: `hasNameConflictExcludingId` → conflict; CAS/InChIKey changed-and-conflicting; CAS/InChIKey unchanged (skip) branches. |

**Cross-cutting:** the private `validateAdditionalShortNames` helper has 3 branches (blank/empty
entry, duplicate entries case-insensitive, shortName-included-in-list); only the duplicate-entries
branch is tested (once, via `validateForCreate`), and never at all via `validateForUpdate`.

---

## 3. Mappers

### 3.1 Options mappers (`ChemicalTypeMapper`, `InstrumentMapper`, `ModalityMapper`, `SolventMapper`)

Confirmed by direct read of all 4 interfaces: each is a one-line empty interface —
`public interface XMapper extends BaseMapper<X, XDto> {}` — with **no** `@Mapping`, no default
methods, no per-type customization. `SolventMapperTest` is explicitly documented in-file as the
representative test for all four. **Verdict: the representative-testing pattern here is confirmed
correct and defensible** — there is genuinely nothing to differ per type.

| `BaseMapper` method (tested via `SolventMapperTest`) | Tested? | Test(s) | Positive | Negative/edge |
|---|---|---|---|---|
| `toDto` | Partial | `toDto_shouldMapIdToUuid` | Yes | No null-input test |
| `toEntity` | Full | `toEntity_shouldIgnoreClientSuppliedUuidAndArchivedAt_soIdIsServerGeneratedAndOptionStartsUnarchived` | Yes | Yes (verifies client-supplied `id`/`archivedAt` are ignored) |
| `toDtoList` | Partial | `toDtoList_shouldMapEachEntity` | Yes (2 elements) | No empty-list test |

### 3.2 Non-options mappers

| Mapper / Method | Custom `@Mapping` logic | Tested? | Test(s) | Positive | Negative/edge |
|---|---|---|---|---|---|
| `ExperimentMapper.experimentToGetExperimentDto(Experiment, List<...>)` (2-arg) | `uuid←id`, `measurementData←result`, `dois`, `attachments←param` | Partial | `experimentToGetExperimentDto_withExplicitAttachments_shouldMapIdAndUseProvidedAttachments` | Yes | No null-nested (`result`/`metadata`) or empty-attachments test |
| `ExperimentMapper.experimentToGetExperimentDto(Experiment)` (1-arg) | `uuid←id`, `measurementData←result`, `attachments←files` | Partial | `experimentToGetExperimentDto_singleArgOverload_shouldMapIdAndDeriveAttachmentsFromFiles` | Yes (empty `files` case) | No non-empty `files` conversion test |
| `FolderMapper.folderToGetFolderDto` | `experimentIds` via `@Named("mapExperimentsToIds")` | Partial | `folderToGetFolderDto_shouldMapExperimentsToTheirIds`, `...shouldMapEmptyList_whenNoExperiments` | Yes | Empty-list branch covered; **`experiments == null` branch untested** |
| `MeasurementDataMapper.resultToMeasurementDataDto` | `pFO_rateConstant←pfoRateConstant`, `error←errorValue` | Partial | `...shouldMapPfoRateConstantToPFO_andErrorValueToError`, `...shouldMapNullPfoRateConstant` | Yes | Null-handling only tested for `pfoRateConstant`, not `errorValue` or a fully-null `result` |
| `MetadataMapper.metadataToGetMetadataDto` | `uuid←id`; composes `UserMapper`/`SolventMapper`/`InstrumentMapper`/`ModalityMapper` | Partial | `metadataToGetMetadataDto_shouldMapIdToUuid_andNestedOptionEntities` | Yes (all nested present) | No test with nullable `instrument`/`modality` (legitimately optional per `ExperimentValidator`) |
| `ReactantMapper.toDetailDto` | `uuid←reactant.id`, `chemicalType←reactant.type`, `structures←param` | Partial | `toDetailDto_shouldMapReactantIdToUuid_andIncludeProvidedStructures` | Yes | No empty/null-structures-list test |
| `ReactantMapper.toDto(Reactant, StructureFlag)` | `uuid←reactant.id`, `chemicalType←reactant.type`, `flag←param` | Partial | `toDto_withFlag_shouldMapReactantIdToUuid_andPassThroughFlag` | Yes | No explicit `flag = null` test via this overload directly |
| `ReactantMapper.toDto(Reactant)` (default, delegates with `null` flag) | — | Full | `toDto_withoutFlag_shouldDelegateWithNullFlag` | Yes | Delegation + null-flag result explicitly asserted |
| `StructureMapper.toDto` | `reactant←reactant.id`, `name←fileName` | Full | `toDto_shouldMapReactantToReactantIdAndFileNameToName`, `toDto_shouldMapNullReactantToNullReactantId` | Yes | Yes (null nested `reactant`) — best-covered mapper overall |
| `UserMapper.toDto` | none (plain field mapping) | Full | `toDto_shouldMapAllFields`, `toDto_shouldReturnNull_whenUserIsNull` | Yes | Yes (`null` input → `null` output) |

---

## 4. Endpoints

### 4.1 `ExperimentEndpoint` (no `@PreAuthorize`; authz delegated to service)

| Method | Tested? | Test(s) | Positive | Negative | Notes |
|---|---|---|---|---|---|
| `createExperiment` (POST) | Partial | `createExperiment_whenInputIsValid_returns201Created`, `...whenReactantAIsMissing_returns400BadRequest` | Yes (201) | Yes (400) | No test of service throwing 403/409 during create. |
| `updateExperiment` (PUT) | Partial | `updateExperiment_whenInputIsValid_returns200Ok`, `...whenServiceThrowsForbidden_returns403` | Yes | Yes (403) | Javadoc documents a 409 for updating archived experiments — **untested**; no 404 test; no body-validation (400) test. |
| `getExperiment` (GET) | Full | `getExperiment_whenFoundAndAuthorized_returns200Ok`, `...whenServiceThrowsForbidden_returns403`, `...whenServiceThrowsNotFound_returns404` | Yes | Yes (403, 404) | Good coverage. |
| `searchExperiments` (GET) | Partial | `searchExperiments_whenValidFiltersAndPagination_returns200OkWithPage`, `...whenInvalidFilterProvided_returns400BadRequest` | Yes | Yes (400) | No test of "no optional filters supplied" branch. |
| `getAuditLogs` (GET `/history`) | Full | `getAuditLogs_whenFoundAndAuthorized_returns200OkWithHistory`, `...whenServiceThrowsForbidden_returns403`, `...whenServiceThrowsNotFound_returns404`, `...whenAuthorizedAsAdmin_passesRequestingUserIdAndRoleToService` | Yes (USER+ADMIN) | Yes (403,404) | Best-covered endpoint in this controller. |
| `getArchivedExperiments` (GET) | Partial | `...whenAuthorizedAsUser_returns200OkWithData`, `...whenAuthorizedAsAdmin_...`, `...whenEmpty_...` | Yes (USER,ADMIN,empty) | No | No error-path test. |
| `archiveExperiment` (DELETE) | Partial | `archiveExperiment_whenAuthorized_returns204NoContent`, `...whenServiceThrowsForbidden_returns403` | Yes | Yes (403) | No 404 test. |
| `restoreExperiment` (PUT `/restore`) | Partial | `restoreExperiment_whenAuthorized_returns200Ok`, `...whenServiceThrowsForbidden_returns403` | Yes | Yes (403) | No 404 test. |

### 4.2 `ExperimentFilesEndpoint` (no `@PreAuthorize`; authz delegated to service)

| Method | Tested? | Test(s) | Positive | Negative | Notes |
|---|---|---|---|---|---|
| `uploadFile` (POST) | Partial | `uploadFile_ShouldReturn201_WhenUploadIsSuccessful` | Yes | **No** | No 403/404 test at all. |
| `downloadFile` (GET) | Partial | `downloadFile_ShouldReturnResourceWithCorrectHeaders`, `...ShouldEscapeEmbeddedQuote_WhenFileNameContainsOne` | Yes (incl. header-escaping edge case) | **No** | No 403/404 test. |
| `getFileDetails` (GET `/details`) | Full | `getFileDetails_ShouldReturn200_WhenFileExists`, `...ShouldReturn404_WhenFileNotFound` | Yes | Yes (404) | No 403 test, otherwise solid. |
| `archiveFile` (DELETE) | Partial | `archiveFile_ShouldReturn204_WhenSuccessful` | Yes | **No** | No 403/404 test at all. |

### 4.3 `ExperimentFolderEndpoint` (no `@PreAuthorize`; ownership delegated to service)

| Method | Tested? | Test(s) | Positive | Negative | Notes |
|---|---|---|---|---|---|
| `createFolder` (POST) | Full | `createFolder_whenValidInput_returns201Created`, `...whenNameIsBlank_returns400BadRequest`, `...whenNameExceedsMaxLength_returns400BadRequest` | Yes | Yes (2 validation cases) | Good coverage. |
| `getMyFolders` (GET) | Partial | `getMyFolders_returns200OkWithList` | Yes | **No** | No empty-list/error test. |
| `addExperimentToFolder` (POST) | Partial | `addExperimentToFolder_returns200Ok` | Yes | **No** | Zero negative-path coverage (no test of unowned folder or missing experiment). |
| `removeExperimentFromFolder` (DELETE) | Partial | `removeExperimentFromFolder_returns200Ok` | Yes | **No** | Zero negative-path coverage. |
| `deleteFolder` (DELETE) | Partial | `deleteFolder_returns204NoContent` | Yes | **No** | Zero negative-path coverage. |

### 4.4 `ReactantEndpoint`

`@PreAuthorize`: `createReactant`→`hasAnyRole('ADMIN','SUPERVISOR')`; `updateReactant`/`archiveReactant`/`restoreReactant`→`hasRole('ADMIN')`; `getArchivedReactants`→`hasAnyRole('ADMIN','SUPERVISOR')`; `getReactantDetail`/`getAllReactants`→open.

| Method | Tested? | Test(s) | Positive | Negative | Notes |
|---|---|---|---|---|---|
| `createReactant` (POST) | Partial | `createReactant_WhenAdmin_ShouldReturn201`, `...WhenSupervisor_ShouldReturn201`, `...WhenUser_ShouldReturn403` | Yes (ADMIN,SUPERVISOR) | Yes (403 USER) | No 400 (body validation) or 409 (duplicate CAS/InChIKey) test. |
| `updateReactant` (PATCH) | Partial | `updateReactant_WhenAdmin_ShouldReturn200`, `...WhenSupervisor_ShouldReturn403`, `...WhenUser_ShouldReturn403` | Yes | Yes (403×2) | No 404 test; no explicit partial-payload (PATCH semantics) test at controller level. |
| `getReactantDetail` (GET) | Full | `getReactantDetail_ShouldReturn200_WhenReactantExists`, `...ShouldReturn404_WhenReactantNotFound` | Yes | Yes (404) | Good. |
| `getAllReactants` (GET) | Partial | `getAllReactants_ShouldReturn200_WithList` | Yes | **No** | No empty-list/error test. |
| `getArchivedReactants` (GET) | Full | `getArchivedReactants_WhenAdmin_ShouldReturn200`, `...WhenSupervisor_ShouldReturn200`, `...WhenUser_ShouldReturn403` | Yes | Yes (403) | Good. |
| `archiveReactant` (DELETE) | Partial | `archiveReactant_WhenAdmin_ShouldReturn204`, `...WhenUser_ShouldReturn403` | Yes | Yes (403 USER) | **Missing SUPERVISOR-forbidden test** — the key distinguishing behavior (SUPERVISOR can create but not archive) is unverified. |
| `restoreReactant` (PUT `/restore`) | Partial | `restoreReactant_WhenAdmin_ShouldReturn200`, `...WhenSupervisor_ShouldReturn403`, `...WhenUser_ShouldReturn403` | Yes | Yes (403×2) | No 404 test. |

### 4.5 `StructureEndpoint` (all 3 methods: `hasAnyRole('ADMIN','SUPERVISOR')`)

| Method | Tested? | Test(s) | Positive | Negative | Notes |
|---|---|---|---|---|---|
| `uploadStructure` (POST) | Partial | `uploadStructure_whenAdmin_returns200Ok`, `...whenSupervisor_returns200Ok`, `...whenUser_returns403Forbidden` | Yes | Yes (403) | No 404/400 test. |
| `generateStructureFromAPI` (POST) | Partial | `generateStructureFromAPI_whenAdmin_returns204NoContent`, `...whenSupervisor_...`, `...whenUser_returns403Forbidden` | Yes | Yes (403) | No 404/external-API-failure test. |
| `selectStructure` (POST) | Partial | `selectStructure_whenAdmin_returns200Ok`, `...whenSupervisor_...`, `...whenUser_returns403Forbidden`, `anyEndpoint_whenUnauthenticated_returns401` | Yes | Yes (403, **401 anonymous** — unique in this codebase) | No 404 test; the 401-anonymous test covers only this one method, not the other two in the same controller. |

### 4.6 `UserEndpoint`

`@PreAuthorize`: `getAllUsers`→`hasAnyRole('ADMIN','SUPERVISOR')`; `setRole`/`archiveUser`/`restoreUser`→`hasRole('ADMIN')`; `getRole`→`#id == authentication.name or hasRole('ADMIN')`.

| Method | Tested? | Test(s) | Positive | Negative | Notes |
|---|---|---|---|---|---|
| `getAllUsers` (GET) | Full | `getAllUsers_WhenAdmin_ShouldReturn200AndList`, `...WhenSupervisor_...`, `...WhenUser_ShouldReturn403` | Yes | Yes (403) | Good. |
| `setRole` (POST) | Partial | `setRole_WhenAdmin_ShouldReturn200_WhenSuccessful`, `...WhenSupervisor_ShouldReturn403`, `...WhenUser_ShouldReturn403` | Yes | Yes (403×2) | No 400 (`RoleDto.role` is `@NotNull`) or 404 test. |
| `getRole` (GET `/role`) | Full | `getRole_ShouldReturn200AndRole_WhenUserExists`, `...ShouldReturn404_WhenUserNotFound`, `...WhenAdmin_ChecksSomeoneElsesRole_ShouldReturn200`, `...WhenNonAdmin_ChecksSomeoneElsesRole_ShouldReturn403` | Yes (self, admin-on-other) | Yes (404, 403) | Best-covered method in the file — both branches of the ownership expression covered. |
| `archiveUser` (DELETE) | Partial | `archiveUser_WhenAdmin_ShouldReturn200_WhenSuccessful`, `...WhenSupervisor_ShouldReturn403`, `...WhenUser_ShouldReturn403` | Yes | Yes (403×2) | No 404 test (unlike `restoreUser`, which does test 404). |
| `restoreUser` (PUT `/restore`) | Partial | `restoreUser_WhenAdmin_ShouldReturn404_WhenUserNotFound`, `...WhenSupervisor_ShouldReturn403`, `...WhenUser_ShouldReturn403` | **No** | Yes (404, 403×2) | **Gap: no test proves a successful (200) restore for the one role authorized to call it (ADMIN)** — only a 404 failure path is exercised for ADMIN. |

### 4.7 Options endpoints — representative-pattern check

Confirmed by direct read: `AbstractOptionsEndpoint` holds ALL logic/`@PreAuthorize` (GET open;
POST/DELETE/PUT-restore all `hasRole('ADMIN')`). `ChemicalTypeEndpoint`, `InstrumentEndpoint`,
`ModalityEndpoint`, `SolventEndpoint` are byte-for-byte identical 3-line subclasses — different
`@RequestMapping` path and generic type only, no overrides, no custom `@PreAuthorize`.
**Verdict: the representative pattern (one test class, `ChemicalTypeEndpointTest`, for all 4) is
defensible** — nothing differs per type at the endpoint layer.

| Method (via `ChemicalTypeEndpointTest`, representative for all 4 controllers) | Tested? | Test(s) | Positive | Negative |
|---|---|---|---|---|
| `getKnownOptions` (GET, open) | Partial | `getKnownOptions_asUser_shouldReturnOk`, `...asAdmin_shouldReturnOk` | Yes (both roles) | **No anonymous/401 test** |
| `createOptions` (POST, ADMIN) | Partial | `createOptions_asUser_shouldReturnForbidden`, `...asAdmin_shouldReturnOk` | Yes | Yes (403) — but no anonymous/401 test |
| `archiveOption` (DELETE, ADMIN) | Partial | `archiveOption_asUser_shouldReturnForbidden`, `...asAdmin_shouldReturnOk` | Yes | Yes (403) — but no anonymous/401 test |
| `restoreOption` (PUT, ADMIN) | Partial | `restoreOption_asUser_shouldReturnForbidden`, `...asAdmin_shouldReturnOk` | Yes | Yes (403) — but no anonymous/401 test |

Confirmed by direct read of `ChemicalTypeEndpointTest.java`: every single test method supplies
`.with(jwt().authorities(...))`. There is no fully-anonymous MockMvc call anywhere in the file, so
a 401-for-anonymous-GET (or any anonymous call) scenario is untested for these 4 endpoint classes
project-wide — this is a real, reproducible gap, not a pattern-representativeness issue.

---

## 5. Repositories

`backend/src/test/java/com/reactur/backend/repositories/` contains **exactly one file**:
`ReactantRepositoryTest.java` (a `@DataJpaTest`). Every other repository interface — and 6 of
`ReactantRepository`'s own 8 custom methods — has **zero** persistence-layer test coverage.

| Repository | Method | Query type | Tested? | Test citation |
|---|---|---|---|---|
| `ExperimentAttachmentRepository` | `findByExperimentIdAndArchivedAtIsNull(UUID)` | derived | **No** | — |
| `ExperimentFolderRepository` | `findAllByOwner_Id(String)` | derived | **No** | — |
| `ExperimentRepository` | `findByArchivedAtIsNotNull()` | derived | **No** | — |
| `ExperimentRepository` | `findArchivedExperimentsForUser(@Param("userId") String)` | `@Query`: `SELECT e FROM Experiment e WHERE e.archivedAt IS NOT NULL AND (e.createdBy.id = :userId OR e.metadata.scientist.id = :userId)` | **No** | — |
| `ReactantRepository` | `findAllByArchivedAtIsNull()` | derived | **No** | — |
| `ReactantRepository` | `findAllByArchivedAtIsNotNull()` | derived | **No** | — |
| `ReactantRepository` | `hasNameConflict(Collection<String>)` | `@Query`: `SELECT COUNT(r) > 0 FROM Reactant r LEFT JOIN r.additionalShortNames asn WHERE LOWER(r.shortName) IN :names OR LOWER(asn) IN :names` | **Yes** | `ReactantRepositoryTest`: `hasNameConflict_ShouldReturnTrue_WhenShortNameMatchesExactCase`, `...WhenShortNameMatchesCaseInsensitively`, `...WhenAdditionalShortNameMatchesCaseInsensitively`, `...ShouldReturnFalse_WhenNoNameMatches` |
| `ReactantRepository` | `hasNameConflictExcludingId(Collection<String>, UUID)` | `@Query`: `SELECT COUNT(r) > 0 FROM Reactant r LEFT JOIN r.additionalShortNames asn WHERE (LOWER(r.shortName) IN :names OR LOWER(asn) IN :names) AND r.id != :excludeId` | **Yes** | `ReactantRepositoryTest`: `hasNameConflictExcludingId_ShouldReturnFalse_WhenOnlyMatchIsTheExcludedReactant`, `...ShouldReturnTrue_WhenAnotherReactantMatchesCaseInsensitively`, `...ShouldReturnFalse_WhenNoNameMatches` |
| `ReactantRepository` | `existsByCasNumber(String)` | derived | **No** | — |
| `ReactantRepository` | `existsByInChiKey(String)` | derived | **No** | — |
| `ReactantRepository` | `existsByCasNumberAndIdNot(String, UUID)` | derived | **No** | — |
| `ReactantRepository` | `existsByInChiKeyAndIdNot(String, UUID)` | derived | **No** | — |
| `ReactantStructureRepository` | `findByReactant_Id(UUID)` | derived | **No** | — |
| `ReactantStructureRepository` | `findByFlag(StructureFlag)` | derived | **No** | — |
| `OptionRepository<E>` (base; inherited by `ChemicalTypeRepository`, `InstrumentRepository`, `ModalityRepository`, `SolventRepository`) | `existsByNameIgnoreCase(String)` | derived | **No** | — |

`UserRepository` declares no custom query methods (only `extends JpaRepository<User, String>`), so
it contributes nothing to this table. The 4 options repositories declare zero methods of their own.

**Totals:** 15 custom query methods found across all repositories; **13 have zero persistence-layer
test coverage**; only 2 (`hasNameConflict`, `hasNameConflictExcludingId`, both on `ReactantRepository`)
are tested, and both have full positive+negative branch coverage.

**Repository interfaces with zero persistence tests:** `ExperimentAttachmentRepository`,
`ExperimentFolderRepository`, `ExperimentRepository`, `ReactantStructureRepository`,
`OptionRepository` (and all 4 concrete option subinterfaces). `ReactantRepository` itself is only
partially tested (2 of 8 custom methods).

---

## 6. Overall Totals

| Layer | Methods inventoried | Full | Partial | Zero |
|---|---|---|---|---|
| Services (incl. `OptionServiceImpl` representative) | 39 | 23 | 15 | 1 |
| Validators | 4 | 1 | 3 | 0 |
| Mappers (incl. options representative as 1 set of 3) | 13 | 4 | 9 | 0 |
| Endpoints (incl. options representative as 1 set of 4) | 35 | 8 | 27 | 0 |
| Repositories (custom query methods) | 15 | 2 | 0 | 13 |
| **Total** | **106** | **38** | **54** | **14** |

Note: for the options mappers/services/endpoints, the table above counts the *representative test
class* once rather than multiplying by 4, since the underlying types were confirmed identical.
Counted per concrete class instead, the "zero coverage" repository figure would also multiply
(3 concrete option repositories × 1 untested method each), and the endpoint/mapper/service partial
counts would multiply by 4 — but the underlying finding (nothing differs per type) would not change.

---

## 7. Is the representative-testing pattern intentional and defensible?

**Yes, for all three cases asked about — confirmed by direct source reading, not assumed:**

- **Options mappers** (`ChemicalTypeMapper`/`InstrumentMapper`/`ModalityMapper`/`SolventMapper`):
  each is a literal one-line empty interface extending `BaseMapper<Entity, Dto>` with no
  `@Mapping`, no default methods. `SolventMapperTest` says so explicitly in a doc comment.
  **Nothing differs per type. Pattern is correct.**
- **Options services** (`ChemicalTypeServiceImpl`/`InstrumentServiceImpl`/`ModalityServiceImpl`/`SolventServiceImpl`):
  each is a 3-line class whose only content is a constructor forwarding to
  `OptionServiceImpl(repository, mapper)`. All actual logic (conflict/not-found checks, archiving)
  lives in the shared abstract base, which `ChemicalTypeServiceTest` tests with full positive+
  negative branch coverage. **Nothing differs per type. Pattern is correct.**
- **Options endpoints** (`ChemicalTypeEndpoint`/`InstrumentEndpoint`/`ModalityEndpoint`/`SolventEndpoint`):
  each is a 3-line `@RestController` subclass of `AbstractOptionsEndpoint`, differing only in
  `@RequestMapping` path and generic type parameter. All `@PreAuthorize` annotations and logic live
  in the abstract base. **Nothing differs per type. Pattern is correct.**

**However**, the representative pattern being *defensible* does not mean the representative test
itself is *complete* — see §4.7: `ChemicalTypeEndpointTest` never issues a fully-anonymous
(no-JWT) request, so the anonymous/401 case is untested for all 4 controllers, not just for
`ChemicalTypeEndpoint`. That's a genuine coverage gap that happens to also confirm the pattern is
being applied correctly (fixing it once in `ChemicalTypeEndpointTest` would fix it for all 4).

---

## 8. Prioritized Summary of Most Significant Gaps

### Tier 1 — Zero coverage (fix first)

1. **`StructureServiceImpl.fetchPendingStructuresFromAPI()`** — no test exists at all. Entire
   scheduled polling/job-status logic (finished/failed/exception handling, multi-structure loop) is
   untested.
2. **13 of 15 repository custom query methods have no persistence-layer test**, including two
   `@Query`-annotated methods on `ExperimentRepository` (`findArchivedExperimentsForUser`) and every
   custom method on `ExperimentAttachmentRepository`, `ExperimentFolderRepository`,
   `ReactantStructureRepository`, and `OptionRepository` (shared by all 4 option repos). This is the
   concrete list the existing "Persistence Unit Tests" TODO item has been missing.
3. **`ExperimentHistoryServiceImpl.getExperimentHistory`** — the actual diff/history-building logic
   (`buildEntityLookupMap`, CREATE/UPDATE/ARCHIVE/RESTORE detection) is never exercised because every
   test stubs `javers.findChanges()` to return empty; only the authorization gate is proven.
4. **`ExperimentServiceImpl.searchExperiments` / `buildSearchSpecification`** — the entire filter-
   building logic (reactant A/B cross-match, metadata ranges, comment/scientist/solvent/instrument/
   modality/measurement filters) is never invoked because the repository call is fully mocked in the
   one existing test.
5. **`UserEndpoint.restoreUser`** — no test proves a successful (200) restore for ADMIN, the only
   role authorized to call it; the only ADMIN-role test asserts a 404 failure.

### Tier 2 — Missing negative-path or authorization-branch tests

6. **`ExperimentFolderEndpoint`** — `getMyFolders`, `addExperimentToFolder`,
   `removeExperimentFromFolder`, `deleteFolder` each have exactly one happy-path test and zero
   negative tests (no ownership-violation 403, no not-found 404) despite ownership being the core
   concern of this controller.
7. **`ExperimentFilesEndpoint`** — `uploadFile` and `archiveFile` have zero negative tests;
   `downloadFile` has zero negative tests (only sibling `getFileDetails` tests 404).
8. **`ReactantEndpoint.archiveReactant`** — missing the SUPERVISOR-forbidden test, which is the
   endpoint's key distinguishing behavior (SUPERVISOR can create reactants but not archive them).
9. **Anonymous/unauthenticated (no-JWT → 401) requests are essentially untested project-wide** —
   only one test in the entire endpoints layer (`StructureEndpointTest.anyEndpoint_whenUnauthenticated_returns401`,
   covering only `selectStructure`) exercises this. All other controllers, including the 4 options
   endpoints, never test it.
10. **Request-body validation failures (400)** are untested for `ReactantEndpoint.createReactant`
    and `UserEndpoint.setRole` (`RoleDto.role` is `@NotNull`), despite both using `@Valid @RequestBody`.
11. **`ExperimentServiceImpl.archiveExperiment` / `restoreExperiment`** and **`FileServiceImpl.storeFile`
    / `archiveFile`** — all missing not-found and/or idempotent-no-op / conflict-branch tests.
12. **`ReactantValidator.validateForUpdate`** — the entire `additionalShortNames` validation path and
    CAS/InChIKey-conflict-on-update paths are unreached by any test (only proven via `validateForCreate`).

### Tier 3 — Incomplete branch coverage (tested, but not exhaustively)

13. **`ReactantServiceImpl.determineAggregateFlag`** (private, reached via `getAllReactants`/
    `getArchivedReactants`/`restoreReactant`) — only 2 of 5 possible outcomes are ever produced by
    any test (`PENDING`, empty→`null`); `ACTIVE`, `READY`, all-`FAILED`, fallback-`REJECTED` untested.
14. **Mapper null/edge-case handling** — `MetadataMapper` (nullable `instrument`/`modality`),
    `MeasurementDataMapper` (`errorValue` null, fully-null `result`), `FolderMapper`
    (`experiments == null` vs. empty), `ExperimentMapper` (null nested `result`/`metadata`) all lack
    at least one null/edge-case test.
15. **`ExperimentValidator.validateForUpdate`** — only 2 of 14 shared validation branches are proven
    directly through this entry point (the rest are proven only via the sibling `validateForCreate`).

Given the resources and time available, everything above is grounded in direct reads of the
relevant source and test files (method bodies and test assertions), not inferred from naming alone.
