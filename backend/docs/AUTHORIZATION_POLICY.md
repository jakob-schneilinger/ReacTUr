# Authorization Policy

This document is the single source of truth for **who can do what** in the ReacTUr backend. If you change a
`@PreAuthorize` annotation, `ExperimentAuthorization`, or any service-layer ownership/role check, update this file in
the same change.

Last verified against code: 2026-07-22 (commit `b0fb2f0`, branch `feature/security_conventions_unify`).

## Roles

`com.reactur.backend.enums.Role`: `UNAUTHORIZED`, `USER`, `ADMIN`, `SUPERVISOR`.

- **UNAUTHORIZED** — a freshly-synced Keycloak user pending manual approval. No read/write access anywhere. The only
  exception is checking your own role (see Users below), so the frontend can detect "pending approval" and show an
  appropriate screen.
- **USER** — standard scientist. Creates experiments, views active experiments/reactants, manages own folders.
- **ADMIN** — global read/write over everything.
- **SUPERVISOR** — global read/write over experiments and files (same as ADMIN there). Elevated but not full rights over
  reactants/structures (see below). No rights over user management or the options catalog. No special rights over
  folders (nobody gets those but the owner).

## Global gate

Enforced in `security/SecurityConfig.java`: every request requires an approved role (`USER`/`ADMIN`/`SUPERVISOR`), with
exactly one carve-out: `GET /api/users/{id}/role`, open to any authenticated principal (including `UNAUTHORIZED`) for
the self-check described above.

## The three authorization mechanisms

The codebase deliberately uses three different mechanisms, chosen per-resource based on whether the resource has an
ownership concept:

1. **Pure role gate** (`@PreAuthorize` on the controller, no entity load) — used when there's no ownership concept at
   all: Reactants, Structures, the Options catalog, Users.
2. **Ownership-only, no privileged override** (explicit service-layer check) — used for Folders, which are strictly
   personal.
3. **Ownership + privileged bypass** (service methods take `requestingUserId`/`requestingUserRole`, load the entity,
   delegate to `services/authorization/ExperimentAuthorization.java`) — used for Experiments, Files, and Experiment
   History, where an ADMIN/SUPERVISOR override is needed on top of creator/scientist ownership.

## Resource-by-resource matrix

### Experiments (`ExperimentEndpoint` / `ExperimentServiceImpl`)

| Action                            | Who                                                                       |
|-----------------------------------|---------------------------------------------------------------------------|
| View an active experiment         | Any approved user (USER/ADMIN/SUPERVISOR)                                 |
| View an archived experiment       | Creator, designated scientist, ADMIN, or SUPERVISOR                       |
| Create                            | Any approved user (becomes the creator)                                   |
| Update, archive, restore          | Creator, designated scientist, ADMIN, or SUPERVISOR                       |
| Update an **archived** experiment | **Nobody, regardless of role** — blocked with 409, must be restored first |

### Files / Attachments (`ExperimentFilesEndpoint` / `FileServiceImpl`)

Inherits the parent experiment's view/modify rules above. Upload and delete are additionally blocked (regardless of
role) while the parent experiment is archived.

### Experiment History / Audit Log (`ExperimentEndpoint#getAuditLogs` / `ExperimentHistoryServiceImpl`)

Same view rule as the parent experiment (active → any approved user, archived → creator/ scientist/privileged).

### Folders (`ExperimentFolderEndpoint` / `ExperimentFolderServiceImpl`)

Strictly personal. Owner-only for every operation (create/list/add-experiment/remove-experiment/ delete) — **no
ADMIN/SUPERVISOR override, ever.** This is a deliberate exception to the "privileged bypass" pattern used everywhere
else: adding an experiment to your own folder additionally requires being the experiment's creator or designated
scientist (no privileged bypass on that check either), confirmed by the team as intentional.

### Reactants (`ReactantEndpoint`)

| Action                     | Who                                                                                                                                  |
|----------------------------|--------------------------------------------------------------------------------------------------------------------------------------|
| Create                     | ADMIN, SUPERVISOR                                                                                                                    |
| View (detail, active list) | Any approved user                                                                                                                    |
| View archived list         | ADMIN, SUPERVISOR                                                                                                                    |
| Update                     | ADMIN only                                                                                                                           |
| Archive                    | ADMIN only                                                                                                                           |
| Restore                    | ADMIN only (restoring is treated as archive-management, not "adding back" — confirmed by the team even though SUPERVISOR can create) |

### Structures (`StructureEndpoint`)

| Action                                            | Who               |
|---------------------------------------------------|-------------------|
| Upload, generate (external API), select as active | ADMIN, SUPERVISOR |

SUPERVISOR needs these rights because otherwise a SUPERVISOR-created reactant could never get an active structure.

### Options catalog — Instrument / Solvent / Modality / ChemicalType (`AbstractOptionsEndpoint`)

| Action                   | Who               |
|--------------------------|-------------------|
| View known options       | Any approved user |
| Create, archive, restore | ADMIN only        |

SUPERVISOR has no elevated rights here.

### Users (`UserEndpoint`)

| Action                   | Who                                                                                 |
|--------------------------|-------------------------------------------------------------------------------------|
| View own role            | Any authenticated principal, including UNAUTHORIZED (the one global-gate carve-out) |
| View all users           | ADMIN, SUPERVISOR                                                                   |
| View someone else's role | ADMIN only                                                                          |
| Set a user's role        | ADMIN only                                                                          |
| Archive / restore a user | ADMIN only                                                                          |

## Source of truth for each mechanism

- `security/SecurityConfig.java` — the global gate and the one carve-out.
- `security/RoleExtractor.java` — maps JWT authorities to `Role`.
- `services/authorization/ExperimentAuthorization.java` — canonical experiment view/modify rules.
- `@PreAuthorize` annotations directly on `ReactantEndpoint`, `StructureEndpoint`, `UserEndpoint`,
  `AbstractOptionsEndpoint` — canonical for those resources; this document should always match them exactly.
