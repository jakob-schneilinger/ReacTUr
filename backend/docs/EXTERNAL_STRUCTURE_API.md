# External Structure-Generation API — Dependency, Contract, and Trust Boundary

## Purpose of this document

ReacTUr depends on one external, third-party HTTP service to generate 3D molecular structures from
a reactant's SMILES string (`StructureApiClient`/`StructureApiClientImpl`,
`StructureServiceImpl`). This dependency has never been formally documented anywhere in the
codebase or thesis material before now — the only trace of it was a loose inline Javadoc guess
("e.g., in XYZ format"). This document exists to fix that: describe the dependency precisely as
it's actually coded today, be explicit about what is and isn't validated, and hand over the open
questions clearly to whoever becomes responsible for this project next.

**Context on timing**: the team does not expect to be able to reach the API's owner (Dennis) before
handoff to whoever deploys/maintains this project going forward. The open items below are written
as **forward-looking TODOs for the next maintainer**, not as "pending a reply" — there is no active
conversation in progress.

## What the dependency is

- **Purpose**: given a reactant's SMILES string, asynchronously generate one or more 3D structure
  candidates (to handle cases like chirality, where a single SMILES string can correspond to more
  than one valid 3D structure — see `StructureFlag`'s lifecycle below).
- **Configuration**: `external-api.reactur.base-url` (`application-dev.yml`), sourced from the
  `REACTUR_API_BASE_URL` environment variable (see the project root's `.env`/`.env.example`).
  Consumed by `StructureApiClientImpl`'s constructor, which builds a single `RestClient` against
  this base URL.
- **Only 2 endpoints are ever called**, both from `StructureApiClientImpl`:
  - `POST {base-url}/jobs` — triggers a new generation job.
  - `GET {base-url}/jobs/{jobId}` — polls for that job's status/result.

## The request/response contract, exactly as coded (not assumed)

This section describes what the code actually sends/expects — it has never been cross-checked
against the external API's own documentation or a real example response, only against what
`StructureApiClientImpl` currently parses.

**Triggering a job** — `StructureApiClientImpl.triggerJob(String smiles)`:
- Request: `POST /jobs`, JSON body `{"smiles": "<smiles>"}`.
- Response, deserialized into `private record TriggerResponse(String job_id, String status, String status_url)`.
  Only `job_id` is actually used; `status`/`status_url` are parsed but currently ignored.

**Polling job status** — `StructureApiClientImpl.checkJobStatus(String jobId)`:
- Request: `GET /jobs/{jobId}`.
- Response, deserialized into:
  ```java
  private record StatusResponse(String id, String status, ResultData result, String error) {}
  private record ResultData(int structure_count, List<String> structures) {}
  ```
- `status` is parsed into the internal `JobStatus` enum (`QUEUED`, `RUNNING`, `SUCCEEDED`, `FAILED`,
  `UNKNOWN` — unrecognized strings fall back to `UNKNOWN` via `JobStatus.fromString`).
- `result.structures` — **a list of raw strings, one per generated structure candidate.** This is
  the field this whole document is really about: **the actual internal format/encoding of each
  string has never been confirmed.** The only hint anywhere in the codebase is
  `StructureDto`'s Javadoc: `"The actual 3D structure data (e.g., in XYZ format)"` — phrased as an
  example/guess, not a confirmed spec. No format-specific parsing exists anywhere; each string is
  treated as an opaque blob and stored as-is.

## The structure lifecycle (how the response is used)

Polling happens on a fixed schedule (`StructureServiceImpl.fetchPendingStructuresFromAPI`,
`@Scheduled(fixedDelay = 60000)` — every 60 seconds), independent of any HTTP request. Each
`ReactantStructure` row moves through `StructureFlag`:

`PENDING` (job triggered, awaiting poll) → on success, the first returned structure becomes
`READY` and any additional candidates are persisted as separate `READY` rows too (to let a user
pick among them, e.g. for chirality ambiguity) → a user calls `POST /api/structure/{structureId}/select`
to promote exactly one to `ACTIVE` (all its `READY` siblings become `REJECTED`) → `FAILED` if the
job failed, returned no structures, or (see below) failed content validation.

## What's validated today — and, just as importantly, what is not

As of this cycle's work (OWASP A08 finding), each individual structure string is checked via
`StructureServiceImpl.isValidStructure(String)` before being persisted:
- Rejected if `null`, blank/whitespace-only, or longer than `MAX_STRUCTURE_LENGTH` (1,000,000
  characters — see the Javadoc on that constant for the full sizing rationale).
- A rejected **primary** candidate marks the whole `ReactantStructure` row `FAILED`. A rejected
  **additional** candidate (2nd+ in a batch) is simply skipped, without discarding valid siblings.

**Be precise about what this is, for anyone relying on this document later**: this is a **defensive
hygiene guard**, not chemical or structural validation. It protects against empty/truncated
responses, obviously-wrong payloads (e.g. an HTML error page saved as if it were a structure), and
runaway/garbage data — nothing more. It does **not**:
- Verify the string is syntactically valid XYZ, MOL/SDF, PDB, or any other real structure format.
- Verify atom-count headers (if any) match the actual number of atom lines.
- Verify element symbols, bond information, or any other chemically meaningful content.
- Have any awareness of chemistry at all.

Doing real structural validation would require two things that don't exist today: (1) a confirmed
specification of the external API's actual response format, and (2) either a format-specific parser
or a chemistry-aware parsing library on the backend (the frontend's RDKit-WASM usage,
`frontend-ang/src/app/shared/services/rdkit.service.ts`, is browser-only and doesn't help here — no
equivalent exists server-side). This is a genuinely separate, larger piece of work, not a natural
follow-on to the hygiene guard above.

## Known gaps / TODOs for the next maintainer

- **No confirmed response format.** The exact structure encoding this API returns has never been
  verified against real documentation or a real example payload — only inferred from a one-line
  code comment. Before attempting any real structural validation, confirm this with whoever
  maintains the external API.
- **No application-level authentication on this call at all.** Trust is established purely by the
  fixed base URL; there is no API key/token/Authorization header. Transport security itself is fine
  (HTTPS, confirmed during the OWASP audit) — this is specifically about the missing credential
  layer on top of that. Adding one would require support on the external API's side; not something
  this codebase can add unilaterally.
- **No real structural/chemical validation**, as detailed above — only the generic hygiene guard
  exists today.
- If this dependency is ever replaced, forked, or given a real credential mechanism, the natural
  place to start is confirming the actual response contract described in this document is still
  accurate, since it was reverse-engineered from code rather than an authoritative spec.

## Related documents

- `backend/docs/Software_todo.md` — "A08, CWE-345" entry (the hygiene-guard fix this document
  grew out of) and the "DISCUSS WITH DENNIS" auth item.
- `backend/docs/THESIS_CONTEXT.md` §10 — the full OWASP Top 10:2025 audit this finding came from.
