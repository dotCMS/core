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

- Q: When folders have no cached permission row yet, can the first load be slower, checking those folders one at a time? → A: Yes. Uncached items fall back to a one-at-a-time walk up to the parent they inherit from, which fills the cache, so later loads are one query again. No cap on these checks, and no rewrite of the batch SQL to resolve inheritance. (Amended after review: the walk-up decides which permissions apply, but the answer follows the batch filter's rules, so an item gets the same answer before and after it is cached. See the next question.)
- Q: When the fallback and the batch query disagree for reasons unrelated to the cache (folder owner, Liferay admin, PUBLISH on folders), which answer does an uncached item get? → A: The batch filter's. An item must get the same answer whether or not it is cached; the three differences stay as they are today and go to a follow-up issue.
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
  edit-permissions and add-children flags returned per folder. Four features call the same batch
  filter and share the gap:
  - **Content Drive folder search** (`FolderAPIImpl`): which folders the tree lists, their
    per-folder flags, and "has subfolders".
  - **Content import** (`RelationshipUtil`): which related items an import accepts (READ).
  - **Publishing Queue, Pending tab** (`PublishQueuePermissionFilter`, used by
    `view_publish_queue_list.jsp`): which pending bundles a non-admin sees, and so can select
    for delete. It checks PUBLISH on each bundle's first asset, or EDIT when that asset is a
    folder.
  - **Folder bulk duplicate** (`FolderBulkDuplicateProcessor`): whether each folder can be
    duplicated. It checks READ on the source folder and add-children on the destination.
- **Suspected surface**: Legacy. The batch permission filter lives in
  `com.dotmarketing.business` (`PermissionBitAPIImpl`, `PermissionBitFactoryImpl`) and is called
  from `com.dotmarketing.portlets.folders.business.FolderAPIImpl`, `com.dotcms.util`,
  `com.dotcms.publisher.business` and `com.dotcms.jobs.business.processor.impl`. None of the
  callers is expected to change; the REST resource (`com.dotcms.rest`) is not expected to change
  either.
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
  permission check, including items that inherit and have no cached reference yet, apart from
  the three known differences listed under Non-goals.
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
- Changing the code of the folder bulk duplicate or the Publishing Queue. Their behavior does
  change, because they call the shared filter (see Regression Risk, AC-013 and AC-014).
- Three places where the batch filter and the one-at-a-time check disagree for reasons that have
  nothing to do with the cache. They are known, separate gaps for a follow-up issue. The fix
  keeps the batch filter's current answer in all three, for cached and uncached items alike
  (AC-003, AC-004):
  - **Folder owner**: the one-at-a-time check grants a permission given to the CMS Owner role
    when the user owns the item. The batch query matches only the user's own roles, so it never
    sees that grant. This touches the bulk duplicate, because a copy is owned by the user who
    made it.
  - **Liferay admin**: the one-at-a-time check grants everything to a user whose Liferay
    `isAdmin()` is true. The batch filter deliberately bypasses only for the CMS Administrator
    role and the system user.
  - **PUBLISH on folders**: the one-at-a-time check treats PUBLISH on a folder as EDIT. Folder
    search asks the batch filter for PUBLISH on folders as-is, so Content Drive's publish flag
    follows the PUBLISH permission. The Publishing Queue already works around this by asking
    for EDIT on folders.
- Making `permission_reference` an unlogged table (#35186). This fix should make that change
  safer, but it is not part of it.
- A one-off data repair. Missing cache rows are expected and should be handled at read time.

## Regression Risk *(mandatory)*

- **Blast radius**: All four callers of the batch filter. Any of them could start allowing items
  it wrongly refused before, which is the intended change. None of them should start allowing
  items the one-at-a-time check denies.
  - **Folder search**: folders listed, "has subfolders", and per-folder
    edit/publish/edit-permissions/add-children flags.
  - **Content import**: related items an import accepts.
  - **Publishing Queue, Pending tab**: a non-admin starts seeing a pending bundle whose first
    asset inherits and has no cached row, with its delete checkbox. That matches what the page
    did before #36861, when it checked each bundle one at a time.
  - **Folder bulk duplicate**: a duplicate that was refused as `PERMISSION_DENIED` or
    `PARENT_PERMISSION_DENIED` because the source or destination had no cached row now goes
    through. A likely trigger, from reading the code: duplicate a folder, then duplicate the copy
    before anything else checks it.
- **Performance**: Only items with neither a cached row nor permissions of their own fall back to
  the one-at-a-time check, with no cap per request. Here that means its walk up to the parent;
  the final answer still follows the batch filter's rules (see Clarifications). That set is not the same as "every item the
  batch query did not return": a missed item may simply be denied. Today's batch query returns
  only the permitted ids, so it cannot tell a denied item from an uncached one; the fix needs a
  way to tell them apart. Without it, every denied item would be re-checked one at a time on
  every request, which for a low-privilege user browsing a large site is a permanent cost, not a
  one-time one.
  - Denied items that have a cached row are resolved in the batch query and never take the slow
    path.
  - The one-at-a-time check writes the cached row whether it grants or denies access, so an
    uncached item, readable or denied, walks up to its parent at most once.
  - The row is written on a background thread by default (`PERMISSION_REFERENCES_UPDATE_ASYNC`),
    and a failed write is only logged. The result goes into the in-memory permission cache right
    away. Requests that arrive before the row is saved, such as the tree's parallel requests,
    still take the one-at-a-time check, answered from the in-memory cache rather than by walking
    up again. So "at most once" holds for the walk-up, not for the number of one-at-a-time checks.
  - Edge case: when the one-at-a-time check is answered from the in-memory permission cache, it
    does not write the cached row today. If the background write failed, such an item keeps
    taking the slow path; each of those checks is a memory lookup, not a database query. The plan
    decides whether the fix must also make sure the row is written.
  - The worst case is the first request after a permission change on a site. Paging does not
    limit it: folder search loads every matching folder and runs the READ filter on all of them
    before it pages. So a recursive search can run one check for every inheriting folder on the
    site in a single request. The per-folder flags are checked only for the current page, so
    page size limits them. This is accepted for now, pending the measurement below; the next
    request is one query again.
  - The plan measures that worst case on a large site (a recursive search right after a
    permission change on a site with a few thousand folders) and states an acceptable latency.
    If the result is too slow, the "no cap" decision in Clarifications is revisited with that
    data.
  - The common case (cache already filled) must stay one query, with no per-folder query
    (#36940).
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
- **AC-003**: For any set of folders that inherit their permissions and have no cached row, the
  batch filter returns exactly the folders it returns once those folders are cached: no missing
  folders and no extra ones. Apart from the three known differences in Non-goals (folder owner,
  Liferay admin, PUBLISH on folders), that is also exactly what
  `PermissionAPI.doesUserHavePermission` allows for the same user, permission type and
  `respectFrontendRoles` value. Both sides must use the same inputs, because that flag adds the
  anonymous and logged-in-site roles, and a missing user is treated as the anonymous user. The
  same conditions apply to AC-004.
- **AC-004**: The edit, publish, edit-permissions and add-children flags returned by the folder
  search for an inheriting, uncached folder are the same as the flags the folder gets once it is
  cached. A folder's flags must not depend on whether it happens to be cached. In particular,
  the publish flag keeps following the PUBLISH permission, even though the one-at-a-time check
  would answer with EDIT, and ownership does not grant a flag the cached path would not.
- **AC-005**: The "has subfolders" indicator is true for a folder whose only readable child
  inherits and is uncached.
- **AC-006**: A folder created by Content Drive's bulk duplicate, as a non-admin, appears in the
  folder search as soon as the duplicate finishes.
- **AC-007**: When every folder already has a cached reference row or permissions of its own,
  the batch filter still resolves them in one query, with no per-folder permission query. Only
  items with neither a cached row nor permissions of their own take the one-at-a-time check;
  an item that is simply denied does not. Meeting this requires telling uncached items apart
  from denied ones, which today's batch query does not do.
- **AC-008**: A folder the user cannot read is still left out, whether or not it has a cached
  row.
- **AC-009**: CMS Administrators and the system user see the same results as before. A Liferay
  admin without the CMS Administrator role is still filtered by role, as today.
- **AC-010**: After a request that took the one-at-a-time check for an uncached item, a later
  request for the same item resolves it in the batch query, once the cache row is written.
- **AC-011**: During content import, a related item the importing user can read, which inherits
  its permissions and has no cached row, is accepted as related content rather than rejected.
- **AC-012**: For a low-privilege user who is denied most folders on a site, repeated recursive
  folder searches with a filled cache run no one-at-a-time checks. A denied folder with no
  cached row walks up to its parent at most once; requests that arrive before its row is saved
  are answered from the in-memory permission cache.
- **AC-013**: In the Publishing Queue's Pending tab, a non-admin sees a bundle when
  `doesUserHavePermission` says they can publish its first asset (or edit it, if it is a folder),
  even when that asset inherits its permissions and has no cached row, apart from the known
  differences in Non-goals. A bundle the one-at-a-time check denies stays hidden.
- **AC-014**: The folder bulk duplicate accepts a source folder the user can read and a
  destination they can add children to, even when either has no cached row, apart from the known
  differences in Non-goals (a copy readable only through ownership stays refused). It still reports
  `PERMISSION_DENIED` or `PARENT_PERMISSION_DENIED` when the one-at-a-time check denies.
- **Verification method**:
  - Integration tests (registered in a `MainSuite*` so CI runs them) that delete the reference
    rows for an inheriting subfolder, then call the folder search and the batch filter as a
    non-admin user. Covers AC-001, AC-003, AC-004, AC-005, AC-008.
  - An integration test that compares the batch filter with `doesUserHavePermission` over a mixed
    set (own permissions, cached inheriting, uncached inheriting, denied). Covers AC-003, AC-008,
    AC-009.
  - Rows in that test for the three known differences (an owner-only grant, a Liferay admin
    without the CMS Administrator role, a folder with EDIT but not PUBLISH). Each row asserts
    that the uncached answer equals the cached answer, and documents where it differs from
    `doesUserHavePermission`. Covers AC-003, AC-004, AC-009.
  - An integration test for the related-content check on import with an uncached related item.
    Covers AC-011.
  - An integration test that runs the batch filter twice on an uncached item and confirms the
    second run finds it through the cache. Covers AC-010.
  - An uncached case added to `PublisherAPIImplTest`, which already compares
    `PublishQueuePermissionFilter` with the one-at-a-time check on each bundle. Covers AC-013.
  - An uncached source and destination case added to `FolderBulkDuplicateProcessorIT`. Covers
    AC-014.
  - Manual: repeat the issue's demo steps and the duplicate variant on a build with the fix.
    Covers AC-002, AC-006.
  - AC-007 and AC-012 checked by a test that counts one-at-a-time checks across two requests
    from a user denied most folders, or by review if counting is not practical; the plan
    decides.
  - Tests for AC-010 and AC-012 set `PERMISSION_REFERENCES_UPDATE_ASYNC=false`, or wait for the
    background write, before the second request. Otherwise the second request can arrive before
    the row exists and the test fails or passes only sometimes.
  - The worst-case measurement described under Regression Risk → Performance, recorded in the
    plan with the latency it is judged against.

## Assumptions

- For inherited permissions, the one-at-a-time check (`doesUserHavePermission`) is the correct
  answer, and the batch filter must agree with it. The three known differences in Non-goals are
  the exception until a follow-up issue settles them.
- Folders are not the only items that can inherit without a cached row, so the fix belongs in
  the shared batch filter, not in the folder search alone.
- The permission-change trigger in AC-002 has not been reproduced directly. It comes from
  reading the code, and the integration tests will confirm it.
