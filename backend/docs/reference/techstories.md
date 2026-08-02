# SEPM Techstories — Reference Material

This document is a curated, English-language summary of the **"techstories"** (technical/
non-functional requirements) from a prior, unrelated academic project: the SEPM ("clean
programming") course assignment at TU Wien, which used a horse-management domain as its example.
The raw source is preserved verbatim (in German, as XHTML) at
`backend/docs/reference/sepm_techstories.xhtml` for anyone who wants the original wording — this
file only translates and reorganizes the **techstories** section (`<section id="techstories">`,
roughly lines 201–769 of that file). The horse-domain **user stories** (IDs 1–9, and the rest of the
document outside the techstories section) are domain-specific to that old project and are not
reproduced here at all.

Unlike ReacTUr's own docs (`AUTHORIZATION_POLICY.md`, `API_UPDATE_SEMANTICS.md`), this file does not
describe ReacTUr's current behavior — it's reference material: a translated checklist of generic
software-quality/architecture requirements that *may* be worth holding ReacTUr to, since they're
largely reusable across backend projects regardless of domain. Each story below is tagged with
whether it plausibly applies to ReacTUr at all. A companion gap-check against the current codebase
(as of this writing) was done separately and is not part of this file.

Techstories are organized by the same 4 stakeholder groupings used in the source document. Each
entry lists the ID, a translated one-line title, a summary of the acceptance criteria, and an
applicability tag:

- **[APPLIES]** — generic engineering requirement, relevant to ReacTUr.
- **[PARTIALLY APPLIES]** — mostly relevant, but with some course-specific or non-backend sub-bullets.
- **[DOES NOT APPLY — course-administrative]** — specific to the old course's grading/logistics, not
  relevant to any other project.

---

## 1. Qualitätsmanager/in (Quality Manager) — IDs 10–17

### ID 10 — Backend must write log files **[APPLIES]**
- Log entries must use sensible log levels: `error` for severe/unexpected failures (DB unreachable,
  programming errors); `warn` for expected-but-abnormal conditions (validation failures, state
  conflicts); `info` for user-triggered actions (e.g., start of a REST endpoint method); `debug` for
  detail useful when hunting bugs in complex code; `trace` at the start of methods not already logged
  another way.
- `info` and above should always appear in the log; for the application's own package (as opposed to
  library code), `debug` should appear too.
- Exceptions must be logged **exactly once**.
- Severe/unexpected (`error`-level) exceptions must log the full stack trace. If an exception is
  wrapped, the **wrapping** (outer) exception must be logged, not the original — the wrapper has a
  longer/more informative stack trace.
- Log files must rotate at least daily; older files' names must include the date.
- The program may only write to stdout/stderr via the logging framework (no raw `System.out`/`err`).
- The original exception (with its stack trace) must never be lost when wrapping/rethrowing.
- Logger configuration must live only in `application.yml`, never in XML config files.
- Logged method calls should include their parameters meaningfully.
- It should be traceable which HTTP request a given log entry belongs to.

### ID 11 — Code in English, well documented **[APPLIES]**
- Source code (class/variable/method names) must be in English.
- JavaDoc required for all interfaces and interface declarations.
- Purpose/meaning of exception classes must be documented.
- JavaDoc must cover parameters, return values, and possible error cases/exceptions.
- JavaDoc itself must be written in English.

### ID 12 — Hour-log maintained during development **[DOES NOT APPLY — course-administrative]**
Requires a Markdown hour-log table in the root `README.md` (name, student ID, total hours; each
entry with date/duration/story worked on) rendered correctly in GitLab. Pure course
grading/logistics — not applicable to any project outside that course.

### ID 13 — Core functionality must be tested **[APPLIES]**
- At least 4 tests per backend layer (REST, Service, Persistence).
- Both positive (happy path) and negative (error path) tests per layer.
- At least one positive test per layer must exercise a write operation.
- At least 12 new tests overall (course-specific numeric target, but the underlying principle —
  meaningful coverage per layer — generalizes).
- Both unit and integration tests are acceptable.
- Tests must use real assertions on returned data, not just HTTP status codes — e.g., an integration
  test that loads an entity by ID must assert on the actual field values, not just that the request
  succeeded.

### ID 14 — Clean Git workflow **[APPLIES]**
- Regular commits.
- A commit usually corresponds to exactly one user/tech story; the story ID should be referenced at
  the start of the commit message (course-specific convention, but "one concern per commit,
  descriptive message" generalizes).
- Descriptive commit messages describing exactly what was implemented.
- Build/dependency-management output must never be committed (`target/`, frontend `dist/`,
  `node_modules/`).
- Runtime data unrelated to the program (e.g. a database file) must never be committed.
- Force-push disabled on the server; pushed history must not be rewritten.
- All grading-relevant commits must be pushed to `master` before the deadline
  (course-administrative sub-bullet only).

### ID 15 — All input must be validated **[APPLIES]**
- Input parameters (numeric or text) must be validated per the relevant story's rules.
- Validation must account for whether a parameter is required or optional.
- Validation must happen at least at the service layer.
- Validation should be implemented in one or more dedicated validator(s).

### ID 16 — Sensible code design, coding conventions, best practices **[APPLIES]**
- Follow naming conventions of the language/technology in use (variables, classes, packages); classes
  with a specific role should reflect that in their name (e.g. `...Service`, `...Dao`).
- Consistent code formatting (indentation, brace/spacing style).
- No leftover dead code from earlier iterations: no unused imports, no commented-out code blocks.
- Sensible choice of datatypes (e.g. an enum for a fixed set of values).

### ID 17 — CI used sensibly **[DOES NOT APPLY — course-administrative]**
Requires the course-provided `.gitlab-ci.yml` at the repo root, a green pipeline at submission time,
and forbids weakening it (though it may be extended with additional/stricter checks). Tied to the
course's specific CI file and submission process — not applicable as written, though "keep CI green,
never weaken it to make tests pass" is a reasonable general principle.

---

## 2. Usability Engineer — IDs 18–21

These are almost entirely Angular/frontend-specific and are **not evaluated in the gap-check** for
this backend-focused task. Included here only for completeness/translation.

### ID 18 — Uncluttered frontend **[DOES NOT APPLY — frontend, not evaluated here]**
Don't cram too much functionality onto one page (e.g. keep edit forms separate from search screens);
functions should be intuitively discoverable (e.g. deleting shouldn't require entering an edit
dialog); use sensible UI elements/styles (tables for tabular data, clickable things that look
clickable).

### ID 19 — Frontend must be usable **[DOES NOT APPLY — frontend, not evaluated here]**
Use the most fitting input modality per field (numeric input for numbers, free text box, filterable
dropdown for large option sets); no database IDs should ever need to be shown/entered in the UI
(though IDs are fine in DTOs/URLs for REST, and in frontend routes); no full page reloads should ever
be required (including navigating away and back); navigation must be fully possible from the page's
own functionality, never requiring manual address-bar editing; `window.alert`/`window.confirm` must
not be used.

### ID 20 — Frontend must inform the user of events **[DOES NOT APPLY — frontend, not evaluated here]**
Users should always be able to tell what just happened; actions without an immediately visible effect
need a success message; errors need an appropriate error message (validation errors specifically must
indicate which input caused the problem); destructive actions require explicit confirmation.

### ID 21 — Broad browser support **[DOES NOT APPLY — frontend, not evaluated here]**
Frontend must at least run in Firefox 143 and Chrome/Chromium 140 (course-specific version pins).

---

## 3. Technische/r Architekt/in (Technical Architect) — IDs 22–27

### ID 22 — Build and dependency management **[PARTIALLY APPLIES]**
- Backend dependencies must be managed via Maven; backend must be buildable/runnable/testable via
  Maven (`mvn compile`, `mvn test`, `mvn package`) — **applies**, and matches ReacTUr's existing
  Maven setup (though this project uses `./mvnw -o <goal>` in offline mode as its house convention).
- The specific `java -Dspring.profiles.active=datagen -jar target/<matriculation-number>.jar` startup
  command and the `datagen` profile naming are course-specific, but the underlying idea (a seedable
  demo-data profile) is relevant — see ID 28.
- Frontend dependency management via NPM, no external CDN URLs, `npm run start`/`npm ci`/`npm run
  build` — frontend-specific, not evaluated in this backend-focused gap-check, but the "manage
  dependencies via the standard tool, no ad-hoc CDNs" principle is generalizable.

### ID 23 — Good software architecture **[APPLIES]**
- Clean layer separation with interchangeable layers.
- Layers must be separated using the Interface pattern.
- No component may ever be instantiated directly (`new SomeService()`); everything must be wired via
  Dependency Injection.
- Clean separation between DTO and Entity classes: entities represent already-persisted data and are
  never themselves sent over REST; DTOs encapsulate non-entity data within the backend and for REST
  transport (Java records are suggested for immutable DTOs) — e.g. search criteria, "create entity"
  payloads, computed/derived views.
- Search criteria should be encapsulated in a DTO in REST endpoint code too, rather than accepted as
  loose individual parameters — except where accepting several flexible parameters (e.g. multipart
  BLOB handling) is needed for efficient transfer.
- Exceptions should be used to transport errors.
- No full frontend reload (`window.location.reload()`) — frontend-specific.
- Angular forms must use Template-Driven or Reactive Forms, not template-variable-based value
  extraction — frontend-specific.
- No externally-embedded frontend resources — frontend-specific.

### ID 24 — Sensible use of the Angular router **[DOES NOT APPLY — frontend, not evaluated here]**
Every view gets its own route; navigation only via Angular's router mechanisms/`routerLink` (not raw
`href`); routes form a sensible hierarchy and may contain verbs/visible IDs (unlike REST URIs); views
must react to router parameter changes by reloading the relevant data.

### ID 25 — Forward-looking resource usage **[APPLIES]**
- Close external resources (e.g. DB connections) when done with them.
- Searches must happen in the database for performance reasons, not by filtering in application
  memory.
- Avoid unnecessary HTTP requests — in particular, a list/table view (or a tree structure) should
  receive all data needed to render it in a single request.
- Never transfer more records in a response than actually needed (e.g. don't send an entire table
  when only 10 rows are displayed); pagination is explicitly *not* required by this story.

### ID 26 — Thoughtful exception handling **[APPLIES]**
- The exception type chosen should fit the kind of error.
- Exceptions are used exclusively to transport errors.
- Checked exceptions for cases expected during normal operation (e.g. invalid data arriving from the
  frontend); unchecked exceptions for unexpected failures (programming errors, runtime-environment
  problems, failures reaching a dependent service like the database).
- No catch-all clauses.
- Errors must always appear in the log.
- Errors must always be either handled or propagated — never silently dropped.
- The program must never simply crash.

### ID 27 — REST-conformant backend **[APPLIES]**
- Every response must use the correct HTTP status code; at minimum distinguish 2xx/4xx/5xx correctly.
- The exact status codes expected, with precise usage rules:
  - **200 OK**
  - **201 Created**
  - **204 No Content**
  - **400 Bad Request** — for a syntactically malformed request.
  - **404 Not Found** — when a specific requested resource doesn't exist (e.g. an entity looked up by
    ID); *not* used for an empty result set/collection.
  - **422 Unprocessable Entity** — for a validation error (the request is syntactically fine, but the
    content fails validation).
  - **409 Conflict** — when the entity in the request is itself valid, but can't be accepted because
    of the system's overall state.
  - **500 Internal Server Error** — programming errors, unhandleable failures.
- Requests/responses should only carry information that's actually needed.
- Always use the correct HTTP verb for the operation.
- The backend itself must never store request state; all application state lives in the database
  (statelessness).
- URIs consist mainly of plural nouns and contain no verbs.
- Structured data is transported primarily as JSON (`Content-Type: application/json`); exceptions:
  search parameters (which should be query parameters on the URI) and BLOBs (application-opaque
  binary data embedded in a domain object) — for BLOBs, both upload and download should use whichever
  transport mechanism is resource-efficient and doesn't lose the BLOB's media type.

---

## 4. Datenmanager/in (Data Manager) — IDs 28–31

### ID 28 — Persist data in an RDBMS **[APPLIES]**
- Application data must be persisted in a relational database, with two exceptions:
  - **Logs** are not application data and must *not* live in the database (see ID 10).
  - **BLOBs** (application-opaque, possibly large binary objects) *may* live in the database with a
    suitable datatype, *or* be stored outside the database (in a dedicated data directory/store) and
    referenced from it; when a BLOB is no longer needed it must be removed. *(Note: this matches
    ReacTUr's SeaweedFS-based approach — `ExperimentAttachment.storageReferenceId` stores an external
    reference rather than the blob itself.)*
- A BLOB's media type should also be persisted.
- Data must survive an application restart; the application must never delete existing data on
  startup; it must be able to create its own required DB structures on startup.
- The application must be able to insert a reasonable set of demo data (at least 10 rows per domain
  object) when started with a `datagen` Spring profile, without altering data that wasn't inserted by
  that mechanism; the app must be fully testable against this demo data.

### ID 29 — Sensible data model **[PARTIALLY APPLIES]**
- Primary keys should be sequential, DB-generated numbers. *(Note: ReacTUr deliberately uses UUID
  primary keys everywhere via `@GeneratedValue(strategy = GenerationType.UUID)` — this is a
  conscious, defensible modern deviation from this specific rule, not an oversight.)*
- Sensible foreign keys to model relations, with FK integrity enforced via a DB-level constraint —
  **applies**.
- Sensible constraints to enforce data invariants, e.g. FK constraints (above) and uniqueness
  constraints on fields that must not have duplicate values (unique email, primary keys, ...) —
  **applies**.
- Use only as many tables as needed while respecting third normal form (3NF) — **applies**.
- Use sensible column datatypes (e.g. `DATE`/`TIMESTAMP`, not `VARCHAR`, for a point in time) —
  **applies**.
- Every relationship with multiplicity > 1 on both sides must be modeled as an m:n relationship —
  **applies**.

### ID 30 — Clean database access **[APPLIES]**
- SQL statements must be executed as prepared statements.
- SQL injection must not be possible.
- No more data should be fetched from the database than needed — e.g. don't query an entire table
  when only rows matching some criterion are needed.

### ID 31 — Separation of production and test databases **[APPLIES]**
- No test run may access the production database.
- The test database must be in the same state at the start of every test run; a previous test run
  must not affect the next one (deterministic test DB state).
