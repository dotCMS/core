# Issue Resolution Specification: Content Drive folder tree hides readable folders from non-admin users

**Feature Branch**: `issue-37749-content-drive-folder-search-permissions`

**Created**: 2026-10-16

**Status**: Draft

**Type**: Issue / Bug Resolution

**Related GitHub Issue**: [#37749](https://github.com/dotCMS/core/issues/37749)

**Input**: User description: "https://github.com/dotCMS/core/issues/37749"

## Problem Statement *(mandatory)*

The Content Drive folder tree (left sidebar) leaves out folders that a non-admin user is allowed
to read. The same user sees those folders in the classic Site Browser. The folders come back once
something checks them one at a time (for example, opening the Site Browser), so the problem
looks intermittent. Users with the CMS Administrator role never see it.

The folders are already missing from the server's response. The Content Drive front end shows
what it receives and is not the cause.

**Severity / Impact**: Medium. It affects every non-admin Content Drive user on a site at once.
Folders they need are hidden, and their children are hidden with them because the tree cannot
expand a folder it does not show. The tree can also show a folder as having no subfolders when
it has readable ones. A folder created by Content Drive's bulk duplicate is hidden every time
until something else checks it. A customer reported it (Freshdesk #39531).

## Clarifications

### Session 2026-10-16

- Q: When folders have no cached permission row yet, can the first load be slower, checking those folders one at a time? → A: Yes. Uncached items fall back to the one-at-a-time check, which fills the cache, so later loads are one query again. No cap on these checks, and no rewrite of the batch SQL to resolve inheritance.
- Q: Should the fix go in the shared batch permission filter, so content import's related-content check gets it too, or only in Content Drive's folder search? → A: The shared filter. Every caller is fixed, including content import's related-content check, which gets its own test.

## Reproduction *(mandatory)*

**Environment**: Reproduced on demo.dotcms.com (26.09.24-01) as the stock non-admin backend user
Joe Contributor, who has Content Drive access. Also observed on an Evergreen customer instance
(build dated 2026-09-02). Present on `main` as of 2026-10-16. Content Drive has used the affected
endpoint since 26.07.27-01.

**Steps to Reproduce**:

1. As a CMS Administrator, create a folder `parent` and give a non-admin role READ on it. Create
   a subfolder `parent/child` that inherits its permissions (no permissions of its own).
2. Make sure nobody has opened `parent/child` through the Site Browser or any other per-folder
   check since step 1. A permission change on `parent` or on the site also resets this state.
3. As a user with only that non-admin role, open Content Drive and expand `parent` in the tree.
   The same listing is `GET /api/v1/folder/search?siteId=<site>&path=/parent/&recursive=false`.
4. As the same user, list the same level in the Site Browser (or call
   `POST /api/v1/folder/byPath` with `{"path":"//<site>/parent/"}`).
5. Repeat step 3.

Faster variant: as the non-admin user, duplicate a folder in Content Drive. When the duplicate
finishes, the tree reloads that level and the copy is missing.

**Expected Behavior**: Step 3 lists `child`, and so do steps 4 and 5. Content Drive and the Site
Browser show the same folders to the same user. A duplicated folder appears in the tree as soon
as the duplicate finishes.

**Actual Behavior**: Step 3 does not list `child`. Step 4 does. After step 4, step 5 lists it
too, with no permission change in between. On demo, `/application/` returned 5 folders before the
per-folder check and 7 after; the site root returned 10 and then 12. On the customer instance, a
recursive search over the whole site returned 61 folders and then 90.

**Reproducibility**: Always, when the folder inherits its permissions and has not been checked
one at a time since it was created or since a permission changed above it. Once checked, it stays
visible until the next permission change above it.

## Scope of Investigation *(mandatory)*

- **Affected area**: Folder listing for Content Drive (`GET /api/v1/folder/search`): the folders
  returned, whether each one is shown as having subfolders, and the edit, publish,
  edit-permissions and add-children flags returned per folder. Other features that use the same
  batch READ filter may have the same gap: today that includes the related-content check during
  content import.
- **Suspected surface**: Legacy. The batch permission filter lives in
  `com.dotmarketing.business` (`PermissionBitAPIImpl`, `PermissionBitFactoryImpl`) and is called
  from `com.dotmarketing.portlets.folders.business.FolderAPIImpl`. The REST resource is modern
  (`com.dotcms.rest`) but is not expected to change.
- **Related known decisions**: The batch filter exists to avoid one permission query per folder
  (#36940, #36308), and project standards prefer it over per-item checks
  (`docs/backend/JAVA_STANDARDS.md`, "Permission Checks — batch vs scalar"). The fix must keep
  that property for the common case. The plan consults `dotCMS/platform-adrs`.

## Root-Cause Hypothesis

The batch READ filter answers "can this user read these folders?" in one query, and that query
grants access in only two ways: the folder has a cached link to the parent it inherits from (a
`permission_reference` row), or the folder has permissions of its own. The cache is filled
lazily, only when something checks a folder one at a time, and it is cleared for everything below
a folder or site whose permissions change. A folder that inherits and has not been cached yet
matches neither way, so it is dropped. The one-at-a-time check walks up to the parent instead of
relying on the cache, which is why the Site Browser shows the folder and why it fills the cache
that makes Content Drive show it afterwards.

CMS Administrators skip the filter entirely, so they never see the problem.

## Fix Scope & Non-Goals *(mandatory)*

**In scope**:

- The batch READ filter returns every item the user can read according to the one-at-a-time
  permission check, including items that inherit and have no cached reference yet.
- The same holds for the other permission types the folder search asks for (edit, publish,
  edit-permissions, add-children), so the per-folder flags are right for those folders.
- The "has subfolders" indicator in the folder search counts children with the same corrected
  filter.
- Every current caller of the batch filter gets the corrected behavior, since the fix is in the
  shared filter rather than in Content Drive.

**Explicitly out of scope / non-goals**:

- Changing how or when the permission reference cache is written or cleared.
- Changing the legacy list filter (`filterCollection(List, int, boolean, User)`) or its callers.
- Changing the Content Drive front end, the folder search request or its response shape.
- Changing the folder bulk duplicate.
- Making `permission_reference` an unlogged table (#35186). This fix should make that change
  safer, but it is not part of it.
- A one-off data repair. Missing cache rows are expected and should be handled at read time.

## Regression Risk *(mandatory)*

- **Blast radius**: All callers of the batch filter: the folder search (folders listed, the
  "has subfolders" indicator, and per-folder edit/publish/edit-permissions/add-children flags)
  and the related-content READ check during content import (`RelationshipUtil`). Any of them
  could start returning items it wrongly hid before, which is the intended change. None of them
  should start returning items the one-at-a-time check denies.
- **Performance**: Items that miss the batch query fall back to the one-at-a-time check, with no
  cap per request. In the worst case (right after a permission change on a site, a recursive
  search over many folders) one request may run many checks. This is accepted: each check fills
  the cache, so the next request is one query again. The common case (cache already filled)
  must stay one query, with no per-folder query (#36940).
- **Backward compatibility**: No database, index, API contract or serialized-state change. Safe
  to roll back.
- **Data considerations**: None. Existing data stays as it is. The slower check may fill missing
  cache rows as a side effect, which is what the Site Browser already does.

## Acceptance & Verification *(mandatory)*

- **AC-001**: For a non-admin user, `GET /api/v1/folder/search` returns a folder that inherits
  READ from its parent and has no cached reference row, on the first request, without anyone
  opening the Site Browser first.
- **AC-002**: Right after a permission change on a site or parent folder, a non-admin user sees
  the same folders in the Content Drive tree as in the Site Browser.
- **AC-003**: For any set of folders, the batch filter returns exactly the folders that
  `PermissionAPI.doesUserHavePermission` allows for the same user and permission type: no
  missing folders and no extra ones.
- **AC-004**: The edit, publish, edit-permissions and add-children flags returned by the folder
  search for an inheriting, uncached folder match the one-at-a-time check.
- **AC-005**: The "has subfolders" indicator is true for a folder whose only readable child
  inherits and is uncached.
- **AC-006**: A folder created by Content Drive's bulk duplicate, as a non-admin, appears in the
  folder search as soon as the duplicate finishes.
- **AC-007**: When every folder already has a cached reference row or permissions of its own,
  the batch filter still resolves them in one query, with no per-folder permission query. Only
  items with neither a cached row nor permissions of their own take the one-at-a-time check.
- **AC-010**: After a request that took the one-at-a-time check for an uncached item, a later
  request for the same item resolves it in the batch query, once the cache row is written.
- **AC-011**: During content import, a related item the importing user can read, which inherits
  its permissions and has no cached row, is accepted as related content rather than rejected.
- **AC-008**: A folder the user cannot read is still left out, whether or not it has a cached
  row.
- **AC-009**: CMS Administrators and the system user see the same results as before.
- **Verification method**:
  - Integration tests (registered in a `MainSuite*` so CI runs them) that delete the reference
    rows for an inheriting subfolder, then call the folder search and the batch filter as a
    non-admin user. Covers AC-001, AC-003, AC-004, AC-005, AC-008.
  - An integration test that compares the batch filter with `doesUserHavePermission` over a mixed
    set (own permissions, cached inheriting, uncached inheriting, denied). Covers AC-003, AC-008,
    AC-009.
  - An integration test for the related-content check on import with an uncached related item.
    Covers AC-011.
  - An integration test that runs the batch filter twice on an uncached item and confirms the
    second run finds it through the cache. Covers AC-010.
  - Manual: repeat the issue's demo steps and the duplicate variant on a build with the fix.
    Covers AC-002, AC-006.
  - AC-007 checked by a test that counts permission queries, or by review if counting is not
    practical; the plan decides.

## Assumptions

- The one-at-a-time check (`doesUserHavePermission`) is the correct answer, and the batch filter
  must agree with it.
- Folders are not the only items that can inherit without a cached row, so the fix belongs in
  the shared batch filter, not in the folder search alone.
- The permission-change trigger in AC-002 has not been reproduced directly. It comes from
  reading the code, and the integration tests will confirm it.
