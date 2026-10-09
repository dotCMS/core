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

Ordering matters: the metadata removal walks the binary directory to find what to remove, so it must
run before the directory is deleted, as it does today.

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
  removed.
- A content destroy outside any transaction keeps removing them immediately, as today.
- Bulk delete reports a descendant as `COVERED_BY_PARENT` only when its selected ancestor was
  actually deleted. Otherwise the descendant goes through its own checks and is deleted on its own,
  or, if a cancellation stopped the run before it, is recorded as skipped with no reason like the
  rest of the remainder. To make this decidable, descendants run after the selected folders that
  contain them; otherwise submission order is kept.
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
  difference. The plan must look for such callers. The three smaller fixes are confined to the bulk
  delete job and endpoint.
- **Backward compatibility**: No API signature, REST contract, database schema or index mapping
  changes. The bulk delete outcome keeps the same statuses and reasons; only *when*
  `COVERED_BY_PARENT` is used changes, which matches what the bulk duplicate already does. The
  no-body response changes from `500` to `400`, which only fixes an error. Nothing here is in the
  rollback-unsafe categories (`docs/core/ROLLBACK_UNSAFE_CATEGORIES.md`); a rollback to a version
  without the fix returns to removing files immediately.
- **Data considerations**:
  - Files lost before this fix cannot be recovered; no repair task is proposed.
  - If the deferred removal itself fails after commit (for example an I/O error), the files stay on
    disk with no rows pointing to them: orphaned, wasting space, but losing nothing. Today the same
    failure surfaces during the delete. The removal must log such a failure.
  - Large folder deletes will hold the list of directories to remove until commit. It is one entry
    per destroyed version, small next to the content objects the delete already holds.

## Acceptance & Verification *(mandatory)*

- **AC-001**: Following the main reproduction, after the delete fails the file still opens by its
  path and through `/dA/`, and its binary directory and every version's files are still on disk.
- **AC-002**: The same holds when the failure is not a permission refusal: content locked by another
  user deeper in the tree, and a database error forced after the first file is destroyed.
- **AC-003**: A folder delete that succeeds still removes every destroyed version's binaries,
  resized-image cache and stored metadata from disk once it commits.
- **AC-004**: A content destroy run outside any transaction still removes the files immediately, so
  callers that are not transactional see no change.
- **AC-005**: Bulk delete keeps its two levels of behaviour: a failed folder does not stop the other
  selected folders, and a refused subfolder rolls back its whole top-level folder.
- **AC-006**: In a bulk delete, a descendant whose selected ancestor is refused, fails for any other
  reason (`IN_USE`, `PATH_NOT_FOUND`, `UNCLASSIFIED`) or is never reached because of a cancellation is
  not reported `COVERED_BY_PARENT`. A refused or failed ancestor's descendant goes through its own
  checks; a cancelled run's descendant is `SKIPPED` with no reason. A descendant whose ancestor was
  deleted is still `COVERED_BY_PARENT`.
- **AC-007**: `POST /api/v1/assets/folders/_bulkdelete` with no body answers `400 EMPTY_SELECTION`.
- **AC-008**: Submitted paths are resolved without listing the folders' contents. A path naming a
  file, with or without a trailing slash, is still `PATH_NOT_FOUND`.
- **Verification method**:
  - Integration tests (`dotcms-integration`) for AC-001 to AC-006, each **failing on today's code**
    first: a folder delete refused partway checking the file on disk, the lock and forced-error
    variants, a successful delete still clearing disk, a non-transactional destroy, and one test per
    AC-006 ancestor outcome. New test classes are registered in a `MainSuite*` / `Junit5Suite*`
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
  object storage). Deferring its removal until commit is correct for all of them, because a
  rolled-back destroy should keep the metadata as much as the files.
- No caller relies on the files being gone before its own transaction commits. The plan verifies
  this.
