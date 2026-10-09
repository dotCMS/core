# Issue Resolution Specification: A failed folder delete loses the files it already reached

**Feature Branch**: `issue-37782-folder-delete-binaries-after-commit`

**Created**: 2026-10-09

**Status**: Draft

**Type**: Issue / Bug Resolution

**Related GitHub Issue**: [#37782](https://github.com/dotCMS/core/issues/37782)

**Input**: User description: "Folder delete: a failure part-way loses the binaries it already
walked, plus three smaller bulk delete defects."

## Problem Statement *(mandatory)*

When a folder delete fails partway through the folder's tree, the database changes are rolled back
but the **files already removed from disk are not restored**. The folder, its subfolders and every
file asset stay listed in the product, but the binaries of the files the delete had already reached
are gone, and opening them returns `404`. The author is told the delete failed, so nothing suggests
that anything was lost, and there is no way to recover the files from dotCMS afterwards.

It happens with both folder delete entry points: the single-folder delete (Content Drive context
menu, and every caller of `FolderAPI.delete`) and the bulk folder delete. It is not a regression from
the bulk delete; bulk delete only makes it more likely, because one action deletes more folders.

Three smaller defects in the bulk folder delete are carried in the same fix. The bulk folder
duplicate copied each of them and has already fixed them; bulk delete still has all three:

1. **A child folder is reported as covered by its parent even when the parent is never deleted.**
   When an author selects a folder and one of its descendants, the descendant is marked
   *skipped, covered by parent* before the run starts, from the submitted paths alone. If the parent
   then fails (refused, in use, not found, any error) or a cancellation stops the run before it, the
   descendant is never deleted, and the outcome claims the parent took care of it.
2. **A bulk delete request with no body answers `500`.** A body of `{}` already answers
   `400 EMPTY_SELECTION`; a missing body throws a `NullPointerException` instead.
3. **Resolving each submitted path also lists the folder's whole contents.** The run turns each
   path into a folder through a helper that also runs a browse query over everything in the folder,
   only for the folder itself to be read afterwards. On large folders that is repeated, wasted work,
   done before the run reports any progress.

**Severity / Impact**: The main defect is **silent, unrecoverable data loss** for any author whose
folder delete fails after reaching at least one file. It needs no special setup: a non-admin author
with mixed permissions in one tree, or content locked by someone else deeper in the tree, is enough.
The three smaller defects give a wrong outcome report (1), a wrong error status (2), and wasted work
on large folders (3); none of them loses data.

## Reproduction *(mandatory)*

**Environment**: `dotcms/dotcms:trunk_0666682` (includes the bulk delete fixes #37685 and #37688),
PostgreSQL, Elasticsearch, single node. Any back-end user **without** the CMS Administrator role; an
administrator passes every permission check, so the delete never fails for them.

**Steps to Reproduce** (main defect):

1. Create a folder with two subfolders whose names sort so that the first is walked before the
   second (subfolders are walked in name order). Call them the *walked* and the *refused* subfolder.
2. Put a file asset in the walked subfolder and confirm it opens by its path.
3. Give the user's role View, Edit, Edit Permissions and Publish on the top folder, inherited by its
   children.
4. On the refused subfolder only, remove Edit Permissions for that role, keeping View, Edit and
   Publish.
5. As that user, delete the top folder, through the context menu or through the bulk delete.

**Expected Behavior**: The delete fails because of the refused subfolder, and **nothing** under the
top folder changes: every folder, content item and file is still there, and the file still opens.

**Actual Behavior**:

- The delete fails as expected: the bulk delete reports the top folder as `FAILED`,
  `PERMISSION_DENIED`; the context menu says the user does not have edit permissions on the refused
  subfolder.
- The database is unchanged: all folders, content items and the file asset's record are still there,
  and the file still shows in Content Drive.
- **The file's binary is gone from disk**, for every version of the file. Opening it by its path, or
  through `/dA/<inode>/...`, returns `404`.

**Steps to Reproduce** (smaller defects):

1. As a user who may delete a folder's subfolder but not the folder itself, bulk-delete both. The
   folder is `FAILED`; the subfolder is `SKIPPED` / `COVERED_BY_PARENT` and is never deleted.
2. `POST /api/v1/assets/folders/_bulkdelete` with no request body answers `500`.
3. Bulk-delete a folder with many items: each submitted path is resolved through a call that lists
   the folder's whole contents before the folder is read on its own. (Visible in the queries run;
   not in the outcome.)

**Reproducibility**: Always, given the setup above. Any failure after at least one file has been
destroyed produces the main defect, not only a permission refusal: content locked by another user
deeper in the tree, a database trigger refusing a delete, or a transient database error.

## Scope of Investigation *(mandatory)*

- **Affected area**: Content deletion (destroying content and its binaries), folder delete, and the
  Content Drive bulk folder delete job and its REST endpoint.
- **Suspected surface**: Mostly **legacy-adjacent shared code**. The file removal is in the shared
  content destroy path (`com.dotcms.content.elasticsearch.business.ESContentletAPIImpl`), reached
  from the folder delete in `com.dotmarketing.portlets.folders.business.FolderAPIImpl`. Every content
  destroy goes through it, not only folder delete. The three smaller defects are in modern code only:
  `com.dotcms.jobs.business.processor.impl.FolderBulkDeleteProcessor` and
  `com.dotcms.rest.api.v1.asset.bulkdelete.FolderBulkDeleteHelper`.
- **Related known decisions**:
  - The bulk delete spec requires a failed folder to be left "fully deleted or untouched" under every
    interruption (`specs/37063-bulk-folder-delete-backend/spec.md`, FR-023). On disk that guarantee
    does not hold today; this fix makes it true.
  - The same spec decided that a descendant of another selected path is skipped up front, from the
    submitted paths alone (FR-013). Smaller defect 1 shows that decision reports a false outcome
    when the ancestor is not deleted. The bulk duplicate amended its equivalent rule in review on
    2026-09-29 (`specs/37062-folder-copy-backend/spec.md`): a descendant is covered only when its
    ancestor was actually processed. This fix applies the same amendment to bulk delete.
  - Bulk delete keeps going across the selected folders (a failed folder is recorded and the run
    moves on), and inside one folder a refusal stops and rolls back that folder. **Both behaviours
    are preserved.**
  - The plan formally consults `dotCMS/platform-adrs`.

## Root-Cause Hypothesis

**Main defect.** `FolderAPI.delete` runs one database transaction for the whole folder and walks it
depth-first. When it reaches a subfolder, it destroys that subfolder's content, and the content
destroy removes the content's binary directory and resized-image cache from disk **immediately**
(`deleteBinaryFiles`, which calls `FileUtil.deltree`), along with its stored metadata. That removal
is outside the transaction. When the walk later fails on another subfolder, the transaction rolls
the rows back, but the files are already gone.

The same file removal is reached from four destroy paths in `ESContentletAPIImpl`, so the fix
belongs in the shared removal, not in folder delete: any destroy that runs inside a transaction that
later rolls back loses its files the same way. dotCMS already has a hook that runs code only once
the current transaction commits, and runs it immediately when there is no transaction open
(`HibernateUtil.addCommitListener`). Registering the removal there leaves a rolled-back delete's
files in place and keeps today's timing for callers outside a transaction.

The removal from the search index already works this way: a destroy registers it with the same hook
(`ContentletIndexAPIImpl.removeContentFromIndex`), which is why, after the rollback, the file still
shows in Content Drive while its binary is gone. This fix brings the disk in line with the index.
The same class already defers a binary file removal this way, too: deleting a binary field from a
content type moves its files to the trash only after commit
(`ESContentletAPIImpl`, `addCommitListener(() -> moveBinaryFilesToTrash(...))`).

The hook fires on the commit of the **outermost** transaction. Each destroy method opens its own
transaction, but inside a folder delete it joins the folder's, so the files of everything the folder
delete reached wait for the folder as a whole. That is what makes a refusal anywhere in the folder
keep every file; the plan proves it for each of the four destroy paths, since the fix depends on
it.

Four details of the existing code shape the fix:

- **What to remove must be worked out during the destroy, not after the commit.** The metadata
  removal reads the content type's binary fields and walks the binary directory to find what to
  remove. By the time the transaction commits, the content type may be gone too (deleting a content
  type destroys its content in the same transaction), and the directory listing must happen before
  the directory is deleted. So the paths and metadata keys are collected at destroy time, and only
  the removal itself waits for the commit.
- **Not all metadata removal goes through `deleteBinaryFiles`.** Deleting a single version also
  removes that version's metadata directly (`ESContentletAPIImpl.deleteVersion`, through
  `fileMetadataAPI.removeVersionMetadata`). It needs the same deferral.
- **After-commit work runs in the background by default.** A plain listener is queued to a
  background thread (`ASYNC_COMMIT_LISTENERS` defaults to `true`). For file removal that would let a
  committed delete's files linger for a moment, and on a push publishing receiver, which keeps the
  sender's inodes, a removal still pending could land after the same inode's files were written
  again. Running the removal synchronously after the commit (`HibernateUtil.DotSyncRunnable`), before
  the committing call returns, keeps today's ordering.
- **After-commit listeners have no error handling between them.** They run one after another
  (`DotRunnableThread`), so an exception in one stops the rest of that commit's after-commit work,
  such as index updates and cache flushes, and in synchronous mode it reaches the caller as an error
  after the delete has already committed. The removal must catch and log its own failures.

**Smaller defects.** (1) `COVERED_BY_PARENT` is decided before the run from string prefixes of the
submitted paths, with no knowledge of whether the ancestor will be deleted. (2) The helper reads the
submitted path list without a null check, so a missing body dereferences null. (3) Each path is
resolved through `WebAssetHelper.getAssetInfo`, which also lists the folder's contents; the bulk
duplicate resolves with `AssetPathResolver` alone, which finds the same site and folder and raises
the same exceptions the `PATH_NOT_FOUND` mapping relies on.

## Fix Scope & Non-Goals *(mandatory)*

**In scope**:

- A content destroy that runs inside a transaction removes the content's binaries, resized-image
  cache and stored metadata **only after that transaction commits**. If it rolls back, nothing is
  removed. This covers every destroy path that removes them: destroying content, deleting it,
  deleting all its versions with a backup, and deleting a single version (including the single
  version's metadata, which is removed outside the shared removal today).
- What to remove is determined during the destroy; only the removal waits for the commit.
- The removal runs synchronously once the transaction commits, before the call that committed
  returns, so the ordering of later writes is the same as today.
- A failure while removing is logged and does not stop the rest of the commit's after-commit work,
  and is not reported to the caller as a failed delete. This covers every part of the removal: the
  files, the resized-image cache, the metadata, and the single version's metadata.
- The removal needs no fixed position among the commit's other after-commit work, because none of
  that work reads the content's files or metadata. The plan confirms this; if any listener turns
  out to depend on them, the removal's order is pinned explicitly rather than left to registration
  order.
- A content destroy outside any transaction keeps removing them immediately, as today.
- Bulk delete reports a descendant as `COVERED_BY_PARENT` only when its selected ancestor was
  actually deleted. Otherwise the descendant goes through its own checks and is deleted on its own,
  or, if a cancellation stopped the run before it, is recorded as skipped with no reason like the
  rest of the remainder. To make this decidable, descendants run after the selected folders that
  contain them; otherwise submission order is kept. Three existing rules carry over unchanged:
  - Coverage is transitive: a descendant of a folder that was itself covered by a deleted ancestor
    is covered too.
  - A site root never covers anything, since it is always refused and never deleted.
  - On a cancelled run, the first unreached folder reported as where the run stopped is the first
    in the order the run actually used, not in submission order.
- A bulk delete request with no body answers `400 EMPTY_SELECTION`, as `{}` already does.
- Bulk delete resolves each submitted path without listing the folder's contents. A path naming a
  file, with or without a trailing slash, is still `PATH_NOT_FOUND`.

**Explicitly out of scope / non-goals**:

- **No change to how folder delete walks or commits.** One top-level folder stays one transaction;
  the walk still stops and rolls back a folder on the first refusal; bulk delete still continues
  with the other selected folders. Paging the walk and the transaction decision are tracked in
  #37565.
- **No pre-check of the whole tree before deleting.** It would avoid the permission case only, not
  locks, database errors or changes made between the check and the delete.
- **No recovery of files already lost** on existing installations. There is nothing to restore them
  from.
- The search-index documents a folder delete leaves behind (#37599) and the delete's final
  database sweep that skips permission and lock checks are separate defects with their own tickets.
- No change to the single-folder delete REST endpoint or its responses.
- The bulk folder duplicate is not touched; it already has the three smaller fixes.

## Regression Risk *(mandatory)*

- **Blast radius**: The file-removal change sits in the shared content destroy path, so it affects
  **every** content destroy, not only folder delete: destroying content from the editor, from
  workflows, from site deletion, from content type deletion, from push publishing on a receiver,
  and from plugins. The risk is in the timing only: inside a transaction, files are removed after
  commit instead of during the transaction. Code that destroys content and then, **in the same
  transaction**, checks that the files are gone or writes new files to the same path would see a
  difference. The plan must look for such callers. The concrete case to check first is the push
  publishing receiver, which keeps the sender's inodes, so a destroy and a later write can share a
  path; running the removal synchronously after the commit (see In scope) is what keeps that safe.
  The three smaller fixes are confined to the bulk delete job and endpoint.
- **Backward compatibility**: No API signature, database schema or index mapping changes. Two REST
  responses change, both compatibly:
  - The bulk delete outcome keeps the same statuses and reasons; only *when* `COVERED_BY_PARENT` is
    used changes, so a descendant can now come back `SUCCESS` or `FAILED` where it used to come
    back skipped. The Content Drive client already maps every status and reason generically
    (`folder-delete-outcome.ts`), and the bulk duplicate already behaves this way.
  - A request with no body answers `400` instead of `500`, which only fixes an error.

  Nothing here is in the rollback-unsafe categories (`docs/core/ROLLBACK_UNSAFE_CATEGORIES.md`); a
  rollback to a version without the fix returns to removing files immediately.
- **Data considerations**:
  - Files lost before this fix cannot be recovered; no repair task is proposed.
  - If the deferred removal itself fails after commit (for example an I/O error), the files stay on
    disk with no rows pointing to them: orphaned, wasting space, but losing nothing. Today the same
    failure surfaces during the delete. The removal logs the failure with the paths left behind, so
    they can be cleaned up by hand.
  - Large folder deletes will hold the list of directories to remove until commit. It is one entry
    per destroyed version, small next to the content objects the delete already holds.

## Acceptance & Verification *(mandatory)*

- **AC-001**: Following the main reproduction, after the delete fails the file still opens by its
  path and through `/dA/`, its binary directory and every version's files are still on disk, and its
  metadata can still be read.
- **AC-002**: The same holds when the failure is not a permission refusal: content locked by another
  user deeper in the tree.
- **AC-002a**: For each destroy path (destroy, delete, delete all versions with backup, delete a
  single version), destroying content inside a transaction that then rolls back leaves its binaries,
  resized-image cache and metadata in place. This is tested directly on the content API, so it does
  not depend on how a folder delete happens to fail.
- **AC-003**: A folder delete that succeeds still removes every destroyed version's binaries,
  resized-image cache and stored metadata from disk, and they are gone by the time the commit of
  the outermost transaction returns (for the single-folder delete, before its response is sent; for the bulk delete, before that
  folder's outcome is recorded).
- **AC-003a**: Deleting a content type that has content with binaries still removes that content's
  binaries and metadata after the commit, although the type no longer exists by then.
- **AC-003b**: When the deferred removal fails, whether in removing files or metadata (including a
  single version's metadata), it catches and logs the failure itself: the delete still reports
  success, and an after-commit listener registered later in the same transaction still runs.
- **AC-004**: A content destroy run outside any transaction still removes the files immediately, so
  callers that are not transactional see no change.
- **AC-005**: Bulk delete keeps its two levels of behaviour: a failed folder does not stop the other
  selected folders, and a refused subfolder rolls back its whole top-level folder.
- **AC-006**: In a bulk delete, a descendant whose selected ancestor is refused, fails for any other
  reason (`IN_USE`, `PATH_NOT_FOUND`, `UNCLASSIFIED`) or is never reached because of a cancellation is
  not reported `COVERED_BY_PARENT`. A refused or failed ancestor's descendant goes through its own
  checks; a cancelled run's descendant is `SKIPPED` with no reason. A descendant whose ancestor was
  deleted is still `COVERED_BY_PARENT`, including through a covered intermediate folder, and a site
  root still covers nothing.
- **AC-007**: `POST /api/v1/assets/folders/_bulkdelete` with no body answers `400 EMPTY_SELECTION`.
- **AC-008**: Submitted paths are resolved without listing the folders' contents. A path naming a
  file, with or without a trailing slash, is still `PATH_NOT_FOUND`.
- **Verification method**:
  - Integration tests (`dotcms-integration`) for AC-001 to AC-006, each **failing on today's code**
    first where today's code is wrong: a folder delete refused partway checking the file on disk,
    the lock variant, a rolled-back destroy for each destroy path, a successful delete and a
    content type delete still clearing disk, a removal failure that does not break the commit, a
    non-transactional destroy, and one test per AC-006 ancestor outcome. AC-003 is asserted on the
    calling thread right after the call returns, with no waiting or polling, so a regression to
    background removal fails the test instead of being hidden by the wait. AC-003, AC-003a and AC-004
    describe behaviour that is correct today, so they pass before and after; they guard against the
    fix breaking it. New test classes are registered in a `MainSuite*` / `Junit5Suite*`
    suite, run with `-Dmaven.build.cache.enabled=false`, and `Tests run: N` is confirmed in
    `target/failsafe-reports/*.txt`.
  - A unit test on `FolderBulkDeleteHelper` and a Postman request for AC-007.
  - An integration test for AC-008 covering a file path with and without a trailing slash.
  - Manual: the main reproduction on a local instance, through both the context menu and the bulk
    delete, checking the file still opens.

## Assumptions

- The bulk delete spec's FR-013 ("descendant skipped up front") is amended by this fix, the same way
  the bulk duplicate amended its equivalent rule in review on 2026-09-29. The amendment is recorded
  here; the approved #37063 spec is not edited.
- Content metadata is removed through whichever storage is configured (file system, database or
  object storage), and none of them takes part in the delete's transaction today: the database
  storage uses its own pooled connection (`DataBaseStoragePersistenceAPIImpl.getConnection`), so a
  rollback does not undo its removal either. Deferring the removal until commit therefore treats
  every backend the same way, and a rolled-back destroy keeps the metadata as much as the files.
- No caller relies on the files being gone before its own transaction commits. The plan verifies
  this, starting with the push publishing receiver.
