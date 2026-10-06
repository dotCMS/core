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
| `maintenanceFixAssets` | Requires the **Maintenance portlet**; at most one active job in the cluster (cluster lock) | The permission **and** the singleton. The job runs as the system user (clean) or with no user (fix). |
| `maintenanceCleanAssets` | Same as above; the job deletes orphan files from disk | Same as above. |

`bulkRefreshContentlets` and `importContentlets` are not affected: their processors implement `Validator`, which the job framework runs on every creation path. They keep answering through the generic endpoint exactly as today.

**Severity / Impact**: The privilege escalation is limited to the two maintenance queues. `CleanAssetsJobProcessor` does all its work as the system user (`resolveSystemUser`) and `FixAssetsJobProcessor` runs `FixTasksExecutor.execute(null)` and only logs the `userId`; neither checks a permission. The only barrier is `requiredPortlet(MAINTENANCE)` in `MaintenanceResource`, which the generic endpoint does not apply. Nobody can choose what gets deleted, but any back-end user could start a clean-up whenever they want, and several at once, which loads shared storage and may remove the binary of content being saved at that moment (to be confirmed).

The folder and upload queues run as the submitting user (`FolderBulkDeleteProcessor` deletes through `FolderAPI.delete(folder, user, false)`), so what the generic endpoint skips there is the load limit, the ceilings and the concurrency guard, not permissions. Normal authors never hit any of this, since the UI only calls the dedicated endpoints; it takes a hand-written API call. The reading comes from the code and has not been reproduced yet; the issue's severity is set from the result of AC-006.


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

**Expected Behavior**: Step 4 answers `403` naming `POST /api/v1/assets/folders/_bulkduplicate`; no job is created. The same for steps 5 to 7, each naming its own dedicated endpoint. Jobs on `importContentlets` and `bulkRefreshContentlets` through the generic endpoint keep working.

**Actual Behavior**: Step 3 answers `400 OVER_MAX_PATHS`. Step 4 answers `200` with a `jobId`; the job ends `SUCCESS` with `processed: 51`. Side effect: sending one folder three times reports `total: 3` for a single copy.

**Reproducibility**: Always, for any user with back-end access.

## Scope of Investigation *(mandatory)*

- **Affected area**: Job queue REST API (`JobQueueResource`, `JobQueueHelper`) and the job framework's queue registration (`@Queue` on each processor). Touches the folder bulk operations and the assets maintenance feature only as registered queues.
- **Suspected surface**: Modern (`com.dotcms.rest.api.v1.job`, `com.dotcms.jobs.business`). No legacy `com.dotmarketing.*` code is involved.
- **Related known decisions**: The three bulk features reuse the job framework and must not diverge (#37062, #37063, #37166 specs). The plan consults `dotCMS/platform-adrs` for any decision on job queues.

## Root-Cause Hypothesis

The generic endpoint was designed as the **single entry point** of the job queue: the queue validates itself (`Validator`) and the generic endpoint feeds it. The newer features (bulk folder operations, bulk upload, assets maintenance) put validation in an endpoint of their own instead. That created two entry models in the same framework, and nothing in the framework records which model a queue follows, so the generic endpoint keeps accepting queues that were never meant to be fed that way.

The code evidence, all on `origin/main`: `JobQueueManagerAPIImpl.createJob` validates only when the processor implements `Validator` (line 306); `FolderBulkDuplicateProcessor`, `FolderBulkDeleteProcessor`, `BulkUploadProcessor`, `FixAssetsJobProcessor` and `CleanAssetsJobProcessor` do not; `JobQueueResource` requires only `requiredBackendUser(true)` on both creation paths.

## Fix Scope & Non-Goals *(mandatory)*

**Decisions** (the "decide where the check lives, and record why" criterion of the issue; the first four were reworked after the review on PR #37906):

- **The generic endpoint refuses queues whose entry is a dedicated endpoint** (Option B), so those jobs can only be created through the endpoint that validates them. Option A (a `Validator` per processor) is rejected as the main fix. Not because it cannot check the maintenance permission: it can, since the generic path adds `userId` to the parameters after the client's (`JobQueueHelper`, so a client cannot spoof it). It is rejected because three guards must be held while the job is created and cannot run in a separate `validate()` call beforehand: the folder-delete overlap lock, the maintenance cluster singleton, and the upload ceilings (applied while the multipart is being read). A `Validator` also leaves a future queue unprotected unless its author remembers to write one, which is the same weakness as an optional mark.
- **Every queue declares its entry point, and the framework enforces it** (no optional mark that defaults to open):
  - A dedicated endpoint declares which queue it feeds with an annotation on its REST method (working name `@JobQueueEntryPoint("folderBulkDuplicate")`; the final name is a plan decision).
  - A queue fed by the generic endpoint declares that on `@Queue`.
  - At startup the framework scans these declarations with Jandex (`docs/backend/JANDEX_METADATA_SCANNING.md`). A core queue that declares no entry point, or more than one, **fails startup**.
  - The generic endpoint accepts only queues that declare it as their entry. Any other queue gets `403`.
  - The path named in the message is built from the annotated method's own `@Path` (class path plus method path), never from a hand-copied string, so it cannot go stale.
- **Plugins keep working.** A plugin queue that declares nothing is treated as generic-entry, and a warning is logged at startup, so existing plugins do not break.
- **The refusal is `403 Forbidden`**, and its message names the queue's dedicated endpoint (for example "queue `folderBulkDuplicate` only accepts jobs through `POST /api/v1/assets/folders/_bulkduplicate`"). The caller is authenticated, but this route is not allowed for that queue. `404` was rejected because `GET /api/v1/jobs/queues` lists these queues, and `400` because the request body is not the problem.
- **`GET /api/v1/jobs/queues` keeps listing every queue.** Only creation is closed; status, monitoring and the other read paths of the generic endpoint are unchanged.
- **One PR** covers the declaration mechanism and all the queues. If it grows too large it can be split into the framework part and the queue declarations, decided at planning.

**Declarations**:

| Queue | Entry point |
|---|---|
| `folderBulkDuplicate` | dedicated: `POST /api/v1/assets/folders/_bulkduplicate` |
| `folderBulkDelete` | dedicated: its bulk-delete endpoint |
| `assetBulkUpload` | dedicated: its bulk-upload endpoint |
| `maintenanceFixAssets`, `maintenanceCleanAssets` | dedicated: their `MaintenanceResource` endpoints |
| `bulkRefreshContentlets`, `importContentlets` | generic (unchanged; they already validate) |
| `failSuccess`, `demo` | generic (test queues) |

**In scope**:

- The entry-point annotation, the Jandex scan and the startup check described above.
- The two creation paths of the generic endpoint (JSON and `/upload`) refuse a dedicated-entry queue with `403` and the endpoint's path, before anything is staged or created.
- The declarations in the table, for every queue in core.
- The refusal documented in the endpoint's OpenAPI annotations (`openapi.yaml` is regenerated).
- Tests, including one that proves the limit cannot be skipped through the generic endpoint.

**Explicitly out of scope / non-goals**:

- Changing any dedicated endpoint's validation or response.
- Changing how `importContentlets` and `bulkRefreshContentlets` answer through the generic endpoint: they keep their `200`.
- Protecting Java callers of `JobQueueManagerAPI.createJob` (plugins, internal code). The declaration guards the **REST entry**, not the API; covering that door would be a larger framework change.
- Closing, removing or restricting to administrators the creation path of the generic endpoint as a whole. Whether the generic endpoint is still worth having depends on who uses it outside this repository, and is a separate decision.
- The read side of the generic endpoint (status, active, completed, cancel). Whether any back-end user may read or cancel another user's domain job is a separate question, to be raised as its own ticket if confirmed.
- De-duplicating paths inside the processor. The `total` mismatch disappears for the REST path because the generic endpoint no longer reaches the processor.
- Deleting the temporary files that the multipart parser writes before the resource method runs (see AC-003).

## Regression Risk *(mandatory)*

- **Blast radius**: `POST /api/v1/jobs/{queueName}` and `/upload` for the five dedicated-entry queues; every other queue behaves as today. The frontend, the CLI (`tools/`) and the SDK were searched and none posts to those five queues through the generic endpoint; the frontend uses `/jobs/...` only for status and cancel. Integration and Postman tests of the dedicated endpoints only read `statusUrl`. The Postman collection `ContentImportResource` uses `importContentlets` as a queue name, which is why that queue stays open.
- **Startup**: a core queue without a declaration, or with two, now stops dotCMS from starting. This is intended, but it makes a missing declaration a hard failure, so a test must enumerate every queue in core and fail the build before it can reach a release. Plugin queues are exempt and only log a warning.
- **Backward compatibility**: A client that created the five queues' jobs through the generic endpoint, undocumented for them, will now be refused. That is the intent. The new annotations are additive. This changes an API response (a `403` where `200` was returned); the plan must apply the rollback-unsafe checklist (`docs/core/ROLLBACK_UNSAFE_CATEGORIES.md`) and confirm whether it counts as an API contract change.
- **Data considerations**: None. Jobs already in the queue are untouched. No migration.

## Acceptance & Verification *(mandatory)*

- **AC-001**: `POST /api/v1/jobs/folderBulkDuplicate` with 51 paths is refused with `403` and creates no job. The message names `POST /api/v1/assets/folders/_bulkduplicate`. The dedicated endpoint still answers `400 OVER_MAX_PATHS` for the same body and `200` for 50 paths.
- **AC-002**: The same holds for `folderBulkDelete`, `assetBulkUpload` (both generic paths, JSON and `/upload`), `maintenanceFixAssets` and `maintenanceCleanAssets`, whatever the body or the caller's permissions. For maintenance this closes the privilege escalation: a back-end user without the Maintenance portlet cannot create either job.
- **AC-003**: A refused submission on the generic endpoint creates nothing that dotCMS owns: no job row, no `TempFileAPI` file, no lock held. The check runs before `helper.createJob`, which is what guarantees it, and a plain test proves it without a large upload. For `/upload`, the multipart body is spooled before the resource method runs (by Jersey into the JVM temp directory and, for large bodies, into a temp file under the asset temp path); those spools are removed by `BinaryCleanupJob` and are outside this fix.
- **AC-004**: Open queues are unaffected: `failSuccess`, `demo`, `importContentlets` and `bulkRefreshContentlets` still accept jobs through the generic endpoint with the same response as today, and `JobQueueResourceAPITests` and `ContentImportResource` pass unchanged.
- **AC-005**: Startup enforces the declaration: a core queue with no entry point fails startup; one with two (for example a REST method annotation plus a generic declaration on `@Queue`) fails startup; a plugin queue with none is treated as generic-entry and logs a warning. Unit tests on the scan.
- **AC-005a**: A test enumerates every queue registered in core and asserts it has exactly one declared entry point.
- **AC-005b**: Every refusal message contains the dedicated endpoint's path for that queue, built from the annotated method's `@Path`, and the `403` body follows the standard error response shape. A test asserts the message against the real `@Path` values, not against literals.
- **AC-006**: The bypass is reproduced on unmodified `main` for the three families (folder queues, upload, maintenance) before the fix, using for maintenance a back-end user without the Maintenance portlet. Each reproduction becomes a test that fails first (constitution Principle V). The issue's severity is set from the maintenance result.
- **AC-007**: The `403` appears in the OpenAPI responses of both generic creation endpoints, and the regenerated `openapi.yaml` is committed.
- **Verification method**: Integration tests in `dotcms-integration` next to `JobQueueHelperIntegrationTest` (class-level selection, confirm `Tests run: N`), registered in a `MainSuite*`; unit tests for the scan; the existing `FolderBulkDuplicateResourceIT`, `FolderBulkDeleteResourceIT`, `BulkUploadResourceIT` and `MaintenanceResourceIntegrationTest` for the dedicated paths; Postman `JobQueueResourceAPITests` and `ContentImportResource` for open queues.

## Open Questions

- [NEEDS CLARIFICATION: How does the framework tell a "core" queue (whose missing declaration fails startup) from a plugin queue (which only warns)? Suggested: by the Jandex index it was found in, core being the `dotcms-core` artifact, not by package name, which a plugin can imitate.]

## Assumptions

- Each of the five dedicated-entry queues has exactly one REST method that feeds it.
- Plugins that register their own queues keep working without changes.
- The reading of the code on `origin/main` (2026-10-05) holds; AC-006 verifies it.
