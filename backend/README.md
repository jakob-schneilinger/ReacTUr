# ReacTUr Backend

REST API backend for **ReacTUr**, a chemical-kinetics experiment database (reactants, structures,
experiments, metadata, and attachments), built with Spring Boot. See the
[root README](../README.md) for the system-wide architecture, running the full stack, and
creating accounts.

## Tech Stack

`pom.xml` is the source of truth for exact versions.

- Java 21
- Spring Boot
- Spring Security, with the OAuth2 resource server module for JWT validation
- Spring Data JPA / Hibernate
- PostgreSQL (H2 for tests)
- Keycloak, the external identity provider
- MapStruct
- Javers, for the audit trail
- AWS SDK v2's S3 client, used to talk to the SeaweedFS file storage backend
- springdoc-openapi
- Maven wrapper

OWASP Dependency-Check is wired into the build for dependency vulnerability scanning, but it is
not bound to any lifecycle phase, so it has to be run manually:
`./mvnw org.owasp:dependency-check-maven:check`.

## Architecture

The backend is organized by responsibility:

- **`entities/`** — JPA entities: `Reactant`, `ReactantStructure`, `User` at the top level, plus
  `experiment/` (`Experiment`, `ExperimentAttachment`, `ExperimentFolder`, `ExperimentResult`,
  `Metadata`) and `options/` (`BaseOption`, `ChemicalType`, `Instrument`, `Modality`, `Solvent`).
- **`repositories/`** — Spring Data JPA repositories, one per entity, plus an `options/`
  subpackage.
- **`mappers/`** — MapStruct interfaces converting entities to/from DTOs, plus a shared
  `BaseMapper` and an `options/` subpackage.
- **`dtos/`** — request/response DTOs, subdivided by domain (`experiments/`, `folders/`,
  `options/`, `reactants/`, `users/`).
- **`validators/`** — `ExperimentValidator`, `ReactantValidator`: business-rule validation beyond
  what Bean Validation annotations cover.
- **`services/`** — service interfaces at the top level, with `implementations/` (the real
  business logic), `authorization/` (`ExperimentAuthorization`, the ownership/role rules), and
  `options/` (shared options-catalog logic).
- **`endpoints/`** — REST controllers: `ExperimentEndpoint`, `ExperimentFilesEndpoint`,
  `ExperimentFolderEndpoint`, `ReactantEndpoint`, `StructureEndpoint`, `UserEndpoint`, plus
  `options/` for the shared options-catalog controller.
- **`security/`** — `SecurityConfig` (filter chains, CORS, the global access gate),
  `CustomJwtAuthenticationConverter` (resolves a JWT to a database-backed role on every request),
  `RoleExtractor`.
- **`client/`** — `StructureApiClient`/`StructureApiClientImpl`: the outbound HTTP client for the
  external chemistry structure-generation API.
- **`config/`** — non-security `@Configuration` classes: `OpenApiConfig` (Swagger/OpenAPI
  metadata), `SeaweedFsConfig` (S3 client wiring), `JaversConfig` (audit-trail author resolution).
- **`exceptions/`** — custom exception types, plus `GlobalExceptionHandler`, the single place
  every one of them is logged and turned into an HTTP response.
- **`enums/`** — `Role`, `JobStatus`, `StructureFlag`.

## API Documentation

The API is documented as an [OpenAPI 3](https://swagger.io/specification/) description, generated
at runtime from the controller and DTO source code using
[springdoc-openapi](https://springdoc.org/). It is not a hand-written specification kept in a
separate file, so there is no separate file to fall out of sync with the code. Every time an
endpoint, request parameter, or response DTO changes, the generated description changes with it
on the next application restart.

### Accessing the docs

With the backend running locally (default `dev` profile, port `8080`):

| What                       | URL                                      |
|----------------------------|------------------------------------------|
| Interactive Swagger UI     | `http://localhost:8080/swagger-ui.html`  |
| OpenAPI description (JSON) | `http://localhost:8080/v3/api-docs`      |
| OpenAPI description (YAML) | `http://localhost:8080/v3/api-docs.yaml` |

Swagger UI's "Try it out" allows quick manual probing of any endpoint during development. The raw
JSON or YAML can feed any OpenAPI-compatible tool, for example to generate a typed API client.

These three paths are reachable without authentication (`permitAll()` in `SecurityConfig`),
while every other endpoint in this API requires a valid Keycloak-issued JWT. This only exposes the
shape of the API: the endpoint list, field names, and schemas. It does not expose any stored data,
and calling any other endpoint still requires a valid token.

Whether these paths should instead be restricted to an internal network or VPN once a real
production profile exists is an open decision, tracked in [`ISSUES.md`](ISSUES.md).

### Authenticating "Try it out" requests

0. Prerequisite: a Keycloak user whose ReacTUr role is not `UNAUTHORIZED`. See the root README's
   [Adding users](../README.md#adding-users) section for how to create one; this section only
   covers getting a token for a user who already exists. An `UNAUTHORIZED` user gets a 403 on
   every endpoint except their own role check, which looks like a broken token if you don't know
   to expect it.
1. Open the Keycloak Admin Console (`http://localhost:8081`) and log in as the Keycloak admin
   (`KEYCLOAK_ADMIN_USER`/`KEYCLOAK_ADMIN_PASSWORD` from `.env`), then click **Manage Realms** and
   switch to the `test` realm.
2. **Clients** → `angular-frontend-client` → **Settings** → enable **"Direct access grants
   enabled"** → Save. This setting only lives in the running Keycloak container, not in
   `realm-export.json`, so it is lost after `docker-compose down -v` or a realm re-import. Adding
   it to `realm-export.json` would ship the password grant enabled on the frontend's client by
   default, so it stays a manual step. Development only.
3. Request a token:
   ```bash
   curl -X POST http://localhost:8081/realms/test/protocol/openid-connect/token \
     -H "Content-Type: application/x-www-form-urlencoded" \
     -d "grant_type=password" \
     -d "client_id=angular-frontend-client" \
     -d "username=<username>" \
     -d "password=<password>"
   ```
4. If the response is `{"error":"invalid_grant","error_description":"Account is not fully set
   up"}`: in the user's **Details** tab, clear the **"Required user actions"** field entirely,
   Save, and retry.
5. Copy the `access_token` value from the response, click **Authorize** in Swagger UI, and paste
   the raw token. Swagger UI adds the `Bearer ` prefix itself, since the security scheme
   configured in `OpenApiConfig` is an HTTP bearer scheme, not an API key.

`401 ... "Jwt expired"` means the token's 5-minute lifespan ran out; re-run the curl command for a
fresh one. `403` means the user's role is still `UNAUTHORIZED`.

### How this prevents documentation drift

- The description is recomputed from the live `@RestController`/DTO classes on every application
  start and cached per request afterward. There is no manually maintained markdown, Word, or
  Confluence page describing endpoints for a developer to remember to update.
- A regression test,
  [`OpenApiDocsIntegrationTest`](src/test/java/com/reactur/backend/integration/OpenApiDocsIntegrationTest.java),
  boots the full application and asserts that the generated description is reachable and lists
  the current endpoint paths. It runs as part of the normal test suite (`./mvnw test`), so a
  change that accidentally breaks or hides the documentation surface is caught automatically, like
  any other regression.

## Authorization

Every request needs an approved role (`USER`, `ADMIN`, or `SUPERVISOR`). A newly synced Keycloak
user starts as `UNAUTHORIZED` and can do nothing but check their own role until an `ADMIN`
promotes them. Beyond that global gate, the codebase uses three authorization shapes depending on
whether a resource has an ownership concept: a pure role check, an ownership-only check with no
privileged override (Folders), and an ownership check with a privileged bypass (Experiments,
Files, Experiment History).

The full resource-by-resource matrix, covering who can view, create, update, archive, or restore
each kind of record, is maintained by hand in
[`AUTHORIZATION_POLICY.md`](AUTHORIZATION_POLICY.md). Unlike the API documentation above, this
file is not generated: it must be updated whenever an endpoint is added or its permissions change.

## Testing

Run the full suite with `./mvnw test`.

Unit tests cover services, validators, mappers, repositories, endpoints, and exceptions in
isolation. Integration tests run against a full Spring context, an H2 database, and the complete
Spring Security filter chain, including the `CustomJwtAuthenticationConverter` role lookup. Only
three things are mocked at the integration level: `JwtDecoder` (no Keycloak instance in this
tier), the external structure-generation API, and the S3 client.

Five integration test classes, each covering one area:

- **`ExperimentLifecycleIntegrationTest`** — the experiment create/view/update/delete lifecycle
  and its creator/scientist/privileged-role authorization rules.
- **`DataIngestionAndStructureLifecycleIntegrationTest`** — data ingestion and metadata
  standardization, plus 3D structure generation and the human-in-the-loop selection flow.
- **`ReactantOptionsSearchAndFilesIntegrationTest`** — reactant and options-catalog management,
  metadata-based experiment search, and experiment file attachment upload and download.
- **`FolderOwnershipAndAuthorizationIntegrationTest`** — folder ownership and the remaining
  authorization-matrix scenarios.
- **`OpenApiDocsIntegrationTest`** — confirms the OpenAPI/Swagger documentation surface is
  reachable without authentication, without widening access to any other endpoint.

## Known Limitations

The following are design decisions and accepted constraints. They explain current behavior and
are not planned to change.

- Most REST paths use singular nouns (`/api/experiment`, `/api/reactant`), except `/api/folders`
  and `/api/users`. This is left as-is to avoid breaking the frontend.
- The four Options DTOs (Instrument, Solvent, Modality, ChemicalType) reuse one DTO for both
  create-input and response-output, so server-managed fields like `uuid` and `archivedAt`
  technically appear on the input side too. A client could try to set its own UUID on create; the
  mapper layer prevents this by ignoring both fields on create.
- Experiments are updated with PUT (full replace), Reactants with PATCH (partial update). The
  difference is intentional; each endpoint's method is visible in Swagger UI.
- No rate limiting or abuse-resistance controls exist. This is a design choice: the trust
  boundary is admin approval of new accounts rather than open access, and the Javers audit trail
  makes any abuse attributable after the fact.
- Every failure calling the external structure-generation API, a 4xx client error, a 5xx server
  error, or the service being unreachable, is collapsed into one exception and surfaced as a
  single 502. A 4xx could just as easily mean ReacTUr sent bad data as mean the external service
  is down, so splitting the two cases further is not as clean as it looks. Response content is
  still validated (a length cap and format check) before any structure payload is persisted.
- The JWT `aud` (audience) claim isn't validated. This has no exploitability today, since
  ReacTUr's backend is the only resource server on this Keycloak realm; it would only matter if a
  second service shared the realm.
- Every endpoint's lock icon in Swagger UI renders as "unlocked", even though authentication is
  enforced on every request (verified by `OpenApiDocsIntegrationTest`'s 401 assertions). The
  security requirement is declared once at the document level, which is valid, spec-compliant
  OpenAPI 3, but Swagger UI's icon-rendering logic doesn't resolve that document-level default, so
  it never draws a lock icon on any individual operation. This is cosmetic and does not affect
  authorization.

Open work items, including deployment hardening and unresolved concurrency issues, are tracked in
[`ISSUES.md`](ISSUES.md).
