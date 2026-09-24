# Open Issues

Open work items for ReacTUr, collected before handoff. Design decisions that are not planned to
change are listed in the Known Limitations section of [README.md](backend/README.md) instead.

## Deployment and infrastructure

- [ ] **No production configuration.** The backend only has `application.yml` (which defaults to
  the `dev` profile) and `application-dev.yml`. A production profile needs values that depend on
  the real deployment environment, so it could not be written yet.

- [ ] **Settings that still point to a local development machine.** Several addresses were written
  for one laptop running everything on `localhost`. Each must be changed to the real server
  address before deployment:
  - Backend, `application-dev.yml` (`issuer-uri`): the address of the login server (Keycloak).
    If wrong, the backend rejects every login token.
  - Backend, `SecurityConfig` (CORS allowed origin): the frontend address that is allowed to call
    the backend. If wrong, the browser blocks every call from the frontend to the backend.
  - Frontend, `auth.config.ts` (`issuer`): the login server address, written directly in the
    source file instead of the Angular environment files.
  - Frontend, `app.config.ts` (`allowedUrls`): the backend address the frontend sends the login
    token to. If wrong, no API call carries a token and every call fails with 401.
  - Frontend, `environment.prod.ts`: has no `apiUrl`. This does not break anything today (the
    production code path does not read it), but should be filled in for consistency.

- [ ] **The frontend expects the backend at the same address.** Outside of local development, the
  frontend builds the backend URL from its own address plus `api` (`globals.ts`). The deployment
  must serve both from the same origin, with the backend under that `api` path, or this code has
  to change. To confirm with Jakob.

- [ ] **Replace the development Keycloak setup with TU Wien SSO.** Keycloak runs in `start-dev`
  mode, which Keycloak's documentation marks as not suitable for production, with the `test`
  realm, a development setup without enforced MFA. The target is Keycloak as a broker in front of
  TU Wien's SSO, so only TU accounts can log in and TU's own MFA applies. This is under discussion
  with university staff.

- [ ] **Database schema changes without migrations.** Hibernate (`ddl-auto: update`) adjusts the
  database schema automatically on every start. In production, schema changes should go through
  reviewed migration scripts (Flyway or Liquibase). Missing migration tooling also blocks the
  database constraints needed for the open race conditions below.

- [ ] **No encrypted connections.** The backend and Keycloak are served over plain HTTP, and the
  database connection is not encrypted. Only the outgoing call to the external structure API uses
  HTTPS.

- [ ] **No encryption at rest.** Database contents and uploaded files are stored in plain Docker
  volumes. This is related to the open data-protection (DSGVO) question.

- [ ] **Every service is reachable from outside.** `docker-compose.yml` publishes the ports of all
  eight services on the host machine. That makes the database and the file storage reachable from the
  network, protected only by their passwords, unless a firewall blocks them. In production, only
  what users need should be public (the frontend, the backend API, and the Keycloak login). The
  database and the file storage should only be reachable by the backend, through Docker's internal
  network.

- [ ] **Development secrets in `.env`.** The Postgres, Keycloak, and SeaweedFS credentials come
  from a local `.env` file with placeholder values. Production needs real secrets, managed through
  whatever mechanism the deployment environment provides.

- [ ] **API documentation is public.** Swagger UI and the OpenAPI description are reachable without
  login. They show the shape of the API, not its data. Whether to restrict them to an internal
  network or VPN in production is undecided. The code comment that used to track this question was
  removed as a side effect of an unrelated change (commit `3be8278`), not by a decision.

- [ ] **The OpenAPI version number is hardcoded.** `info.version` in `OpenApiConfig` is a fixed
  string, marked with a `TODO-Deploy` comment. Needs a decision on how API versions are tracked.

- [ ] **No automated dependency scanning.** The OWASP Dependency-Check plugin is configured in
  `pom.xml` but only runs manually (`./mvnw org.owasp:dependency-check-maven:check`). The first run
  downloads the full vulnerability database and takes about an hour without a free NVD API key;
  later runs take under a minute. The last full run reported zero vulnerabilities. A CI job should
  run it regularly once the repository's CI setup is known.

## Concurrency

- [ ] **Open race conditions.** The following checks can be passed by two simultaneous requests,
  because each checks a condition over a whole table rather than one existing row, so no row lock
  helps. Given the small number of users, none of them is urgent.
  - [ ] Reactant short name, CAS number, and InChIKey uniqueness: two simultaneous creates with the
    same value can both succeed. Needs a database unique constraint, which needs migration tooling.
  - [ ] Last-admin protection: two admins demoting each other at the same moment can both pass the
    check, leaving no admin. Needs a locking read over all admin rows, or `SERIALIZABLE` isolation
    with retries.
  - [ ] Structure generation: two simultaneous requests for the same reactant can start two
    external generation jobs. Needs a database constraint, or `SERIALIZABLE` isolation with
    retries.
  - [ ] First login: two simultaneous first requests of a new user can both try to create the user
    row. The primary key already prevents a duplicate; the losing request fails with a server
    error. Low priority.
- [ ] **Race condition on newly created experiment attachments.** Submitting an experiment
  sometimes results in only a subset of uploaded files displaying immediately; a manual browser
  refresh reveals all files.

## Security and authentication

- [ ] **No server-side token revocation.** Logging out ends the Keycloak session and with it the
  refresh token, but an access token issued before logout stays valid for the backend until it
  expires (5 minutes), because the backend checks tokens itself without asking Keycloak. Refresh
  tokens are not rotated (`revokeRefreshToken: false`), so a stolen refresh token stays usable for
  as long as the session lasts (up to 10 hours). Turning on rotation is a single Keycloak realm
  setting, but the frontend's token refresh must be tested with it. A deactivated user loses access
  on their next request (the role is read from the database every time), but a request already in
  progress finishes with the old role.

- [ ] **File upload restrictions are provisional.** Uploads are limited by size only: 10 MB for
  experiment attachments and 1 MB for structure files, both configurable
  (`app.upload.experiment-attachment.max-size-bytes`, `app.upload.structure-file.max-size-bytes`).
  The numbers were taken from the frontend; there are no file-type restrictions. Needs a decision
  from the lab on allowed file types and final sizes, followed by tests.

- [ ] **The external structure API has no authentication.** The backend calls the
  structure-generation API over HTTPS, but without an API key or other credential; trust rests on
  the fixed base URL. Adding a credential needs support from the API's maintainer (Dennis). The
  backend does check each returned structure (length and format) before storing it.

## Incomplete features

- [ ] **"Reject structures" does not reach the backend.** The button only changes the page's local
  state. Nothing is saved, the view reverts after a refresh, and regenerating right afterward fails
  with a 409 because the old candidates still exist. Needs a backend endpoint that rejects all
  pending candidates of a reactant, and the frontend connected to it.

- [ ] **Experiment folders have no frontend.** The backend supports folders (create, list own
  folders, add and remove experiments, delete), but the frontend UI to organize and view experiments
  in folders/groups is not yet implemented.

- [ ] **Empty reactant names are now rejected.** `PATCH /api/reactant/{uuid}` returns 400 for an
  empty `shortName` or `smiles`, which it used to accept. The reactant edit form should be checked
  to never send empty strings for these fields.

- [ ] **Audit log filtered by date or user.** Only a per-experiment history endpoint exists. Feature
  idea: an endpoint to query the audit log by date range or by user.

- [ ] **Substructure search in the backend.** Filtering reactants by substructure happens in the
  browser. As the number of reactants grows, a backend search may be needed.

- [ ] **Missing backend internationalization (i18n).** Error messages returned by the API are 
  currently not localized and lack i18n support.

- [ ] **Include additional short names in `ReactantDto`.** The backend `ReactantDto` needs to
  expose alternative/additional reactant short names so chemists can search by them in both the
  reactant selector and reactant overview.

- [ ] **Substructure search on Reactant and Experiment pages.** Substructure-based filtering is
  currently restricted to the main search; add it to the Reactant overview page and the
  Experiment creation/editing views.

- [ ] **Real-time notifications.** Implement WebSockets for live notifications and update
  propagation on shared experiments.

- [ ] **Enhanced metadata extraction.** Expand the file parser to extract metadata from
  additional proprietary laboratory file formats.

## Code quality

- [ ] **Mapper method names are inconsistent.** Some mappers use short names (`toDto`, `toEntity`),
  others long ones (`experimentToGetExperimentDto`). Needs a team decision on one convention,
  then a rename across services and tests.

- [ ] **Entities are built in two different ways.** Some services create entities directly with
  builders, others through a mapper's `toEntity()`. Unifying this means writing new mapper methods
  for six entity types.

- [ ] **Validation rules for measurement data.** There are no rules for `pFO_rateConstant`, for
  value ranges, or for the error value when `isErrorPercentage` is true. Needs input from the
  chemists before any rule is added.

- [ ] **Smaller clean-ups.** Reactant free-text fields have no length limit; reactant references
  are not checked for duplicates; `commitType` in `ExperimentHistoryDto` is a string instead of an
  enum (changing it affects the frontend).

## Frontend and UI

- [ ] **Missing names for archived reactants on Experiment Detail page.** Archived reactant names
  render as raw IDs because the backend only supplies reactant IDs, and the frontend currently
  resolves names by filtering active reactants.

- [ ] **Experiment history file display and sync delay.** File entries show internal
  paths/identifiers instead of human-readable file names, and file modifications only appear in the
  history timeline after a subsequent metadata update rather than immediately.

- [ ] **Experiment search substructure query UX.** The "NOT" operator is currently placed at the
  beginning of the input field where it causes confusion; move it to the end of the input field,
  provide a visual active/highlighted toggle state, and declutter the search panel layout.

- [ ] **Experiment CSV export formatting.** Adjust the CSV structure and field mappings in the
  experiment export workflow.

- [ ] **Responsive layout for laboratory workstations.** Further refine layout responsiveness and
  split-view behavior for split-screen / half-screen laboratory monitor configurations.

- [ ] **Review German localization.** Review and refine the German translation files, which were
  originally generated by AI.

## Testing

- [ ] **Advanced search end-to-end verification.** Perform complete testing coverage for the
  advanced search criteria combinations and filter edge cases.
