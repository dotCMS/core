# Issue Resolution Specification: The generic job endpoint bypasses the validation and authorization of domain queues

**Feature Branch**: `issue-37883-job-queue-domain-queues-bypass`

**Created**: 2026-10-05

**Status**: Draft

**Type**: Issue / Bug Resolution

**Related GitHub Issue**: [dotCMS/core#37883](https://github.com/dotCMS/core/issues/37883) (found during QA of #37062; related to #37063 bulk delete and #37166 bulk upload)

**Input**: Issue #37883, extended after reading the code on `origin/main`: the bypass is not limited to `folderBulkDuplicate`.

---

## Problem Statement *(mandatory)*

dotCMS runs long operations (bulk folder duplicate, bulk folder delete, bulk upload, assets maintenance) as background jobs on a job queue. A job is created in one of two ways:

1. through a **dedicated endpoint** (for example `POST /api/v1/assets/folders/_bulkduplicate`), which checks the request, enforces the limits and the caller's rights, and only then puts the job on the queue; or
2. through the **generic endpoint** `POST /api/v1/jobs/{queueName}` (and its multipart twin `POST /api/v1/jobs/{queueName}/upload`), which puts a job on **any** queue by name and checks nothing about the job itself.

The checks live in the dedicated endpoints, not in the queue. So a caller who names the same queue on the generic endpoint skips every one of them. The generic endpoint only requires a logged-in back-end user.

This is not only the 50-folder limit from the issue. Reading the code shows the same hole on five queues, and on two of them it is an authorization gap:

| Queue | What the dedicated endpoint enforces | Skipped through the generic endpoint |
|---|---|---|
| `folderBulkDuplicate` | At most 50 distinct paths (`FOLDER_BULK_DUPLICATE_MAX_PATHS`), non-empty, de-duplicated | The limit, the empty check and the de-duplication. The job's reported `total` counts repeats. |
| `folderBulkDelete` | At most 50 paths, plus a per-site advisory lock and an overlap check against jobs already running | The limit **and the overlap guard**: two runs can delete the same tree at once. |
| `assetBulkUpload` | File-count and total-byte ceilings, with a bounded multipart reader | The ceilings and the bounded staging. |
| `maintenanceFixAssets` | Requires the **Maintenance portlet**; at most one active job in the cluster (cluster lock) | The permission **and** the singleton. |
| `maintenanceCleanAssets` | Same as above; the job deletes orphan files from disk | Same as above. |

`bulkRefreshContentlets` and `importContentlets` are not affected: their processors implement `Validator`, which the job framework runs on every creation path.

**Severity / Impact**: Medium for the three folder/upload queues (load: each folder copy is a long database transaction, and runs on the same parent wait for each other). For the two maintenance queues it is a **privilege escalation**: any back-end user without access to the Maintenance portlet could start a clean-assets job. Normal authors never hit it, since the UI only calls the dedicated endpoints; it takes a hand-written API call. This reading comes from the code and has not been reproduced yet (see AC-006).

## Reproduction *(mandatory)*

**Environment**: `dotcms/dotcms:trunk_d4a354c` or later from `main`, demo site, default configuration.

**Steps to Reproduce** (the issue's case, `folderBulkDuplicate`):

1. Create 51 folders under `//demo.dotcms.com/qa-bulkdup-max/` (`f01` ... `f51`).
2. `GET /api/v1/appconfiguration` shows `folderBulkDuplicate.maxPaths: 50`.
3. Send all 51 paths to `POST /api/v1/assets/folders/_bulkduplicate`.
4. Send the same body to `POST /api/v1/jobs/folderBulkDuplicate`.

Additional cases to confirm during planning (same pattern, not yet run):

5. As a back-end user **without** the Maintenance portlet, `POST /api/v1/jobs/maintenanceCleanAssets` with `{}`; repeat it while the first job is running.
6. `POST /api/v1/jobs/folderBulkDelete` with 51 paths, and with two overlapping selections one after the other.
7. `POST /api/v1/jobs/assetBulkUpload/upload` with more files than `CONTENT_BULK_UPLOAD_MAX_FILES`.

**Expected Behavior**: Step 4 answers `403` naming `POST /api/v1/assets/folders/_bulkduplicate`; no job is created. The same for steps 5 to 7, each naming its own dedicated endpoint.

**Actual Behavior**: Step 3 answers `400 OVER_MAX_PATHS`. Step 4 answers `200` with a `jobId`; the job ends `SUCCESS` with `processed: 51`. Side effect: sending one folder three times reports `total: 3` for a single copy.

**Reproducibility**: Always, for any user with back-end access.

## Scope of Investigation *(mandatory)*

- **Affected area**: Job queue REST API (`JobQueueResource`, `JobQueueHelper`) and the job framework's queue registration (`@Queue` on each processor). Touches the folder bulk operations and the assets maintenance feature only as registered queues.
- **Suspected surface**: Modern (`com.dotcms.rest.api.v1.job`, `com.dotcms.jobs.business`). No legacy `com.dotmarketing.*` code is involved.
- **Related known decisions**: The three bulk features reuse the job framework and must not diverge (#37062, #37063, #37166 specs). The plan consults `dotCMS/platform-adrs` for any decision on job queues.

## Root-Cause Hypothesis

Validation and authorization were put where each feature's endpoint is, and the generic endpoint was written earlier for queues that validate themselves (`Validator`) or need nothing. When the new features reused the framework, nothing in it said "this queue must only be fed by its own endpoint", so the generic endpoint kept accepting them.

The code evidence, all on `origin/main`: `JobQueueManagerAPIImpl.createJob` validates only when the processor implements `Validator` (line 306); `FolderBulkDuplicateProcessor`, `FolderBulkDeleteProcessor`, `BulkUploadProcessor`, `FixAssetsJobProcessor` and `CleanAssetsJobProcessor` do not; `JobQueueResource` requires only `requiredBackendUser(true)` on both creation paths.

## Fix Scope & Non-Goals *(mandatory)*

**Decisions already taken with the developer** (the "decide where the check lives, and record why" criterion of the issue):

- **Option B**: the generic endpoint refuses queues that have their own dedicated endpoint, so those jobs can only be created through the endpoint that validates them. Option A (a `Validator` per processor) was rejected as the main fix because it cannot carry the folder-delete overlap guard (it needs a database lock and a query on active jobs), the upload ceilings (they apply to a multipart stream that no longer exists at validation time) or the maintenance permission and cluster lock. It also leaves every future queue open by default.
- **The mark is an attribute on `@Queue`** (for example "dedicated endpoint only"), not a marker interface and not a hardcoded list in the endpoint. It sits next to the queue name on each processor, so a new queue shows its own status in review.
- **One PR** covers all five affected queues.
- **The refusal is `403 Forbidden`**, and its message names the queue's dedicated endpoint (for example "queue `folderBulkDuplicate` only accepts jobs through `POST /api/v1/assets/folders/_bulkduplicate`"). The caller is authenticated, but this route is not allowed for that queue. `404` was rejected because `GET /api/v1/jobs/queues` lists these queues, and `400` because the request body is not the problem. Because the message names the endpoint, the mark on `@Queue` carries that endpoint's path rather than a bare yes/no.
- **`GET /api/v1/jobs/queues` keeps listing marked queues.** Only creation is closed; status, monitoring and the other read paths of the generic endpoint are unchanged.

**In scope**:

- The attribute on `@Queue`, holding the path of the dedicated endpoint; absent (default) means the queue stays open, so existing queues keep working.
- The two creation paths of the generic endpoint (JSON and `/upload`) refuse a marked queue with `403` and the dedicated endpoint's path in the message, before anything is staged or created.
- Mark `folderBulkDuplicate`, `folderBulkDelete`, `assetBulkUpload`, `maintenanceFixAssets` and `maintenanceCleanAssets`. Also mark `bulkRefreshContentlets` and `importContentlets` for coherence: they already validate, but they too have a dedicated endpoint.
- The refusal is documented in the endpoint's OpenAPI annotations (`openapi.yaml` is regenerated).
- Tests, including one that proves the limit cannot be skipped through the generic endpoint.

**Explicitly out of scope / non-goals**:

- Changing any dedicated endpoint's validation or response.
- Closing `failSuccess` and `demo` (test queues; the Postman collection `JobQueueResourceAPITests` uses `failSuccess` through the generic endpoint) or any queue a plugin registers without the mark.
- Protecting callers of `JobQueueManagerAPI.createJob` from Java code (plugins, internal code). The mark guards the REST door, not the API.
- The read side of the generic endpoint (status, active, completed, cancel). Whether any back-end user may read or cancel another user's domain job is a separate question, to be raised as its own ticket if confirmed.
- De-duplicating paths inside the processor. The `total` mismatch disappears for the REST path because the generic endpoint no longer reaches the processor; hardening the processor itself is not needed for this fix.

## Regression Risk *(mandatory)*

- **Blast radius**: `POST /api/v1/jobs/{queueName}` and `/upload` for the seven marked queues; every other queue behaves as today. The frontend, the CLI (`tools/`) and the SDK were searched and none posts to a domain queue through the generic endpoint; the frontend uses `/jobs/...` only for status and cancel. Integration and Postman tests of the dedicated endpoints only read `statusUrl`.
- **Backward compatibility**: A client that created these jobs through the generic endpoint, undocumented for these queues, will now be refused. That is the intent. The new annotation attribute has a default, so existing `@Queue` uses compile unchanged. This changes an API response (a documented refusal where `200` was returned); the plan must apply the rollback-unsafe checklist (`docs/core/ROLLBACK_UNSAFE_CATEGORIES.md`) and confirm whether it counts as an API contract change.
- **Data considerations**: None. Jobs already in the queue are untouched. No migration.

## Acceptance & Verification *(mandatory)*

- **AC-001**: `POST /api/v1/jobs/folderBulkDuplicate` with 51 paths is refused with `403` and creates no job. The message names `POST /api/v1/assets/folders/_bulkduplicate`. The dedicated endpoint still answers `400 OVER_MAX_PATHS` for the same body and `200` for 50 paths.
- **AC-002**: The same holds for `folderBulkDelete`, `assetBulkUpload` (both generic paths, JSON and `/upload`), `maintenanceFixAssets` and `maintenanceCleanAssets`, whatever the body or the caller's permissions. For maintenance this closes the privilege escalation: a back-end user without the Maintenance portlet cannot create either job.
- **AC-003**: A refused submission leaves nothing behind: no job row, no staged file, no lock held.
- **AC-004**: Open queues are unaffected: `failSuccess` and `demo` still accept jobs through the generic endpoint, and `JobQueueResourceAPITests` passes unchanged.
- **AC-005**: A new queue with a dedicated endpoint is protected by adding the mark; a queue without it stays open. Unit test on the registration or discovery of the attribute.
- **AC-005a**: Every refusal message contains the dedicated endpoint's path for that queue, and the `403` body follows the standard error response shape.
- **AC-006**: The bypass is reproduced on unmodified `main` for the three families (folder queues, upload, maintenance) before the fix, and each reproduction becomes a test that fails first (constitution Principle V).
- **AC-007**: The `403` appears in the OpenAPI responses of both generic creation endpoints, and the regenerated `openapi.yaml` is committed.
- **Verification method**: Integration tests in `dotcms-integration` next to `JobQueueHelperIntegrationTest` (class-level selection, confirm `Tests run: N`), registered in a `MainSuite*`; the existing `FolderBulkDuplicateResourceIT`, `FolderBulkDeleteResourceIT`, `BulkUploadResourceIT` and `MaintenanceResourceIntegrationTest` for the dedicated paths; Postman `JobQueueResourceAPITests` for open queues.

## Open Questions

- [NEEDS CLARIFICATION: Is the maintenance case a security report that must follow the security process (private disclosure, severity change on the issue) before this spec is public? To be answered by the team once AC-006 confirms it.]

## Assumptions

- The five queues keep one dedicated endpoint each; none is meant to be fed from the generic endpoint.
- Plugins that register their own queues are not marked and keep working.
- The reading of the code on `origin/main` (2026-10-05) holds; AC-006 verifies it.
