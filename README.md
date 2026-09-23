# ReacTUr — a database for chemical reactions performed at TU Wien

## What is ReacTUr

### Background

Currently, experiment data at one of the TU's Chemistry Labs is scattered: the lab instrument produces
raw data on an air-gapped PC, and the results are afterward manually converted, transferred by USB,
and entered into free-form Excel files with no consistent structure. This workflow has no reliable
way to check whether an experiment has already been performed, allows inconsistent and unvalidated
data entry, and creates a real risk of experiments becoming unusable or lost entirely once the person
who ran them leaves the university.

### ReacTUr

ReacTUr replaces that workflow with a centralized web application. Chemists log experiments through
a structured interface instead of free-form files, and reactants and experiment metadata are validated
and stored consistently. Beyond solving the immediate problem, this consistency has a forward-looking
benefit as well: standardized, structured data makes the Lab's records usable for future computational
and AI applications in a way scattered spreadsheets never could be.

ReacTUr was built as a set of three TU Wien bachelor's theses. Jakob Schneilinger built the Angular frontend, Pablo Ledo
Prado built the backend architecture and security, and Simon Binder worked on data parameters, form design, and database
management.

## Architecture

ReacTUr has five components.

The Angular frontend (`frontend-ang/`) is the chemist-facing web UI.
This README does not duplicate frontend internals; see [`frontend-ang/README.md`](frontend-ang/README.md) for that.

The Spring Boot backend (`backend/`) is the REST API: business logic, validation, and
orchestration. See [`backend/README.md`](backend/README.md) for the full architecture detail and the API
documentation, including a Swagger UI generated automatically from the code.

Keycloak handles identity and access management. It issues JWTs after login, and the backend
validates every request against Keycloak's public keys, then resolves the caller's actual role
from its own database rather than from the token.

PostgreSQL stores all structured data: reactants, experiments, metadata, and users.

SeaweedFS provides S3-compatible object storage for file attachments, such as raw instrument data
and uploaded files, kept separate from the structured relational data in Postgres.

In practice, a chemist logs in through the Angular frontend, which redirects to Keycloak.
Keycloak authenticates the user and returns a JWT, and the frontend attaches that JWT as a bearer
token on every subsequent API call to the backend. The backend validates the token's signature
against Keycloak, then independently looks up the caller's current role in its own
Postgres-backed user table. This lookup happens on every request rather than trusting a role
claim baked into the token, so a role change or account deactivation takes effect on the very
next request instead of waiting for the token to expire. Structured data reads and writes go to
Postgres through JPA, and file attachments are streamed to and from SeaweedFS through its
S3-compatible API.

```
┌──────────┐   login    ┌───────────┐
│ Chemist  │───────────▶│ Keycloak  │
└────┬─────┘            └─────┬─────┘
     │ JWT                    │ public keys
     ▼                        ▼
┌──────────────┐  Bearer  ┌───────────────┐
│   Angular    │─────────▶│  Spring Boot  │
│   Frontend   │◀─────────│    Backend    │
└──────────────┘          └───┬───────┬───┘
                          JPA │       │ S3 API
                              ▼       ▼
                        ┌──────────┐ ┌───────────┐
                        │PostgreSQL│ │ SeaweedFS │
                        └──────────┘ └───────────┘
```

The chemist's browser receives the JWT from Keycloak and holds onto it; the backend never receives a
token directly from Keycloak. Separately, and not shown as a data flow above, the backend fetches
Keycloak's public keys to verify each incoming JWT's signature independently of the frontend.

## Prerequisites and Running the Project

**Important:** this setup is for local development only. No production deployment configuration
exists yet: there is no production Spring profile, no TLS, and Keycloak runs in its own
non-production `start-dev` mode. See [`backend/README.md`](backend/README.md)'s Known Limitations section for the full
detail. Bringing this together into a single production-ready deployment path is planned future
work, not an oversight.

Prerequisites: Docker (Desktop or equivalent) is enough on its own to run the full stack, since
the backend and frontend are both built inside their own containers. A JDK 21 installation and
Node.js with npm (see `frontend-ang/package.json` for the Angular version in use) are only needed
in order to run the backend and frontend directly on the host instead of in a container, as
described below.

Copy `.env.example` at the repository root to `.env` and fill in the actual values to use
(Postgres credentials, Keycloak admin credentials, SeaweedFS access/secret keys, and the
backend's `REACTUR_API_BASE_URL`).

The simplest way to run the full stack is a single command from the repository root:

```
docker-compose up -d
```

This builds and starts every component: Postgres, Keycloak, SeaweedFS, the backend (built with
Maven inside its own container), and the frontend (built and served through nginx). The backend is
reachable on `8080`, the frontend on `4200`.

In order to edit backend or frontend code and see the change without rebuilding a container,
start only the infrastructure services and run the backend and frontend directly on the host
instead:

1. Start the infrastructure services only:
   ```
   docker-compose up -d postgres keycloak seaweedfs-master seaweedfs-volume seaweedfs-filer seaweedfs-s3
   ```
2. Run the backend. Spring Boot does not automatically load `.env` files, so export it into the
   shell first:
   ```
   set -a
   source .env
   set +a
   cd backend
   ./mvnw spring-boot:run
   ```
   The log line `Started BackendApplication in X seconds` confirms it is up, on port `8080` by
   default.
3. Run the frontend, separately:
   ```
   cd frontend-ang
   npm install
   npm start
   ```
   This serves on port `4200` by default. See `frontend-ang/README.md` for its own environment
   configuration details.

Running these manual steps while the containerized `backend`/`frontend` services from
`docker-compose up -d` are also running will conflict: both bind ports `8080` and `4200`. Stop
those two containers first (`docker-compose stop backend frontend`), or start only the
infrastructure services in the first place, as above.

### Adding users

In development, Keycloak has no self-service sign-up: a person cannot create their own login.
Creating one is a separate, infrastructure-level step from the chemist-facing login flow described
in Architecture. Every account exists because the Keycloak admin, a preconfigured infrastructure
account (`KEYCLOAK_ADMIN_USER`/`KEYCLOAK_ADMIN_PASSWORD` in `.env`), not to be confused with
ReacTUr's own `ADMIN` role covered below, created it by hand through Keycloak's own Admin Console
at `http://localhost:8081`. That means logging in there as the Keycloak admin, clicking on Manage Realms and switching to the
`test` realm, then using Users → Create new user, and setting a non-temporary password on the new user's
Credentials tab. Only once that Keycloak account exists can the person log in to ReacTUr at all;
the role-based access described below is a separate step, inside ReacTUr itself, that applies only
afterward.

A brand-new ReacTUr installation has no privileged accounts. Every new user who logs in for the
first time is automatically added to the database with the role `UNAUTHORIZED`, including the
very first one, since there is no admin yet to promote them through the normal flow. The only way
to create the first privileged account is to log in with the just now create keycloak account and 
afterwards set the role with direct database access:

```
docker exec -it kinetics-postgres psql -U <POSTGRES_USER> -d <POSTGRES_DB> \
  -c "UPDATE app_user SET role = 'ADMIN' WHERE id = '<keycloak-user-uuid>';"
```

Find the value for `<POSTGRES_USER>` and `<POSTGRES_DB>` in the `.env` file (`.env.example` provides an example).

Find `<keycloak-user-uuid>` in Keycloak's own Admin Console under that user's details. It is the
same ID ReacTUr stores internally as the user's identifier.

Once at least one admin account exists, all further role changes go through the API instead of
SQL: `POST /api/users/{uuid}` with a body of `{"role": "USER"}` (or `ADMIN`, `SUPERVISOR`,
`UNAUTHORIZED`, see further infos in [`backend/AUTHORIZATION_POLICY.md`](backend/AUTHORIZATION_POLICY.md)), callable only by an existing `ADMIN`. Direct database access is a one-time
bootstrap step, not a normal operating procedure.

Authentication currently uses ReacTUr's own local Keycloak realm. Federating to TU Wien's
institutional SSO is the intended target for the production system, with Keycloak remaining in
place as the broker between TU's identity provider and ReacTUr rather than being replaced. This
is not yet implemented and is currently under discussion with university staff.

## Project Structure

At the top level:

```
reactur/
├── backend/            Spring Boot REST API, see backend/README.md
├── frontend-ang/       Angular web application, see frontend-ang/README.md
├── keycloak-config/    Keycloak realm import used by docker-compose.yml
├── docker-compose.yml  Starts the full ReacTUr stack (Postgres, Keycloak, SeaweedFS, backend, frontend) for local development
└── .env.example        Template for the environment variables docker-compose.yml and the backend need
```

## Known Limitations

See [`backend/README.md`](backend/README.md#known-limitations) for known limitations and current
development status. Many tracked limitations are deployment-specific.

## License

MIT — see [`LICENSE`](LICENSE).
