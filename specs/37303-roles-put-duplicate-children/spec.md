# Issue Resolution Specification: PUT /v1/roles/{roleId} returns the role's children duplicated 4x, with childCount inflated to match

**Feature Branch**: `37303-roles-put-duplicate-children`

**Created**: 2026-09-02

**Status**: Draft

**Type**: Issue / Bug Resolution

**Related GitHub Issue**: [dotCMS/core#37303](https://github.com/dotCMS/core/issues/37303)

**Input**: User description: "https://github.com/dotCMS/core/issues/37303"

## Problem Statement *(mandatory)*

`PUT /api/v1/roles/{roleId}` returns the updated role with its `roleChildren` list duplicated
four times, and `childCount` inflated to match: a role with one child comes back reporting
four. Only the response body is wrong — the persisted rows are correct, and an immediate `GET`
on the same role returns the real children.

Two consequences:

1. **Any client that trusts the PUT response gets wrong data.** The Roles (Beta) portlet
   splices the PUT response into its tree instead of refetching (the normal way to avoid a
   round-trip after a save), so reparenting a role paints every child of that role four times
   until the admin reloads. An integration reading `childCount` to decide whether to recurse
   gets the same wrong answer. (The portlet has since shipped a client-side guard —
   dotCMS/core#37260 — that masks the visible symptom there; the endpoint remains wrong.)
2. **The reparent cascade does redundant write work.** The `DBFQN` rewrite in role save walks
   the subtree seeded from the same duplicated list
   (`roleIdsToProcess.addAll(parentRole.getRoleChildren())`, `RoleFactoryImpl.java:379`), so
   descendants are visited and re-saved once per duplicate. The list holds 2 copies per direct
   child at the moment the queue is seeded (the 4 copies accumulate later in `save()`), so
   direct children are re-saved 2x and the redundancy compounds with depth — grandchildren 3x,
   and so on.

The duplication is not cumulative across requests: a second `PUT` also returns four, not eight.

**Severity / Impact**: Medium — some functionality impacted. Affects any authenticated client
of `PUT /v1/roles/{roleId}` (admin UI, integrations), on every update of a role that has
children. No data corruption; damage is limited to the response body plus redundant writes
during the cascade.

## Reproduction *(mandatory)*

**Environment**: Local instance built from `main`; reproduced via REST only (no UI needed).
Any authenticated CMS Admin against a clean instance.

**Steps to Reproduce**:

1. Create a role `TmpTarget` (no parent).
2. Create a role `TmpParent` (no parent).
3. Create a role `TmpChild` with `parentRoleId = TmpParent`.
4. `GET /api/v1/roles/{TmpParent}?loadChildrenRoles=true` — sanity check.
5. `PUT /api/v1/roles/{TmpParent}` with `parentRoleId = TmpTarget` (a reparent), keeping the
   other fields the same. Inspect `entity.childCount` and `entity.roleChildren`.
6. `GET /api/v1/roles/{TmpParent}?loadChildrenRoles=true` again.

Runnable script:

```bash
A=admin@dotcms.com:admin
H='Content-Type: application/json'
BASE=http://localhost:8080

mk() { curl -s -u $A -H "$H" -X POST $BASE/api/v1/roles -d "$1" | jq -r '.entity.id'; }

TARGET=$(mk '{"roleName":"TmpTarget","canEditUsers":true,"canEditPermissions":true,"canEditLayouts":true}')
PARENT=$(mk '{"roleName":"TmpParent","canEditUsers":true,"canEditPermissions":true,"canEditLayouts":true}')
mk "{\"roleName\":\"TmpChild\",\"parentRoleId\":\"$PARENT\",\"canEditUsers\":true,\"canEditPermissions\":true,\"canEditLayouts\":true}" >/dev/null

curl -s -u $A "$BASE/api/v1/roles/$PARENT?loadChildrenRoles=true" \
  | jq '{childCount: .entity.childCount, children: (.entity.roleChildren|map(.name))}'

curl -s -u $A -H "$H" -X PUT "$BASE/api/v1/roles/$PARENT" \
  -d "{\"roleName\":\"TmpParent\",\"parentRoleId\":\"$TARGET\",\"canEditUsers\":true,\"canEditPermissions\":true,\"canEditLayouts\":true}" \
  | jq '{childCount: .entity.childCount, children: (.entity.roleChildren|map(.name))}'

curl -s -u $A "$BASE/api/v1/roles/$PARENT?loadChildrenRoles=true" \
  | jq '{childCount: .entity.childCount, children: (.entity.roleChildren|map(.name))}'
```

**Expected Behavior**: The PUT response reports each child exactly once —
`{ "childCount": 1, "children": ["TmpChild"] }` — matching what a subsequent `GET` returns.

**Actual Behavior**:

```
GET  before  ->  { "childCount": 1, "children": ["TmpChild"] }
PUT  move    ->  { "childCount": 4, "children": ["TmpChild","TmpChild","TmpChild","TmpChild"] }
GET  after   ->  { "childCount": 1, "children": ["TmpChild"] }
```

The reparent itself is applied correctly; `parent` points at `TmpTarget` afterwards.

**Reproducibility**: Always, on any role with at least one child. Reproduced on `main`; the
underlying accumulate-instead-of-replace behavior is long-standing legacy code, so released
versions (including current LTS) are expected to be affected — worth confirming.

## Scope of Investigation *(mandatory)*

- **Affected area**: Roles REST API (`PUT /api/v1/roles/{roleId}`) response assembly, and the
  role persistence/save path (child-list population and the `DBFQN` reparent cascade).
- **Suspected surface**: **Legacy** — `com.dotmarketing.business.RoleFactoryImpl`
  (`populatChildrenForRolesHelper`, `save`), with the symptom surfacing through the **modern**
  REST layer (`com.dotcms.rest.api.v1.roles`: `RoleHelper.updateRole`, `toRoleViews`,
  `RoleView.childCount`). The fix is expected to land on the legacy side; the plan confirms.
- **Related known decisions**: None known at spec time. The plan phase formally consults
  `dotCMS/platform-adrs`.

## Root-Cause Hypothesis

Verified against the code on `main` (file:line references below). The issue's analysis is
correct in mechanism; the pass count is refined here.

`RoleFactoryImpl.populatChildrenForRolesHelper` (`RoleFactoryImpl.java:859-870`) reads the
role's existing `roleChildren` list and **appends** to it rather than replacing it:

```java
List<String> childrenList = roleMap.get(row.get("parentid")) != null?
        roleMap.get(row.get("parentid")).getRoleChildren(): null;
if (childrenList == null) { childrenList = new ArrayList<>(); }
childrenList.add(row.get("childid"));
```

A `Role` instance that arrives already populated therefore accumulates children on every pass.
The PUT flow passes one instance through it **four times**, matching the observed 4x exactly:

1. **Copy 1 — arrives populated.** `RoleHelper.updateRole` (`RoleHelper.java:86,122-124`)
   loads the cache-resident instance (already carrying its children; the role cache stores
   instances by reference, `RoleCacheImpl.java:33-46`) and `BeanUtils.copyProperties` it onto
   a detached `roleToSave`. `RoleFactoryImpl.save` then copies that onto the freshly loaded
   Hibernate instance (`RoleFactoryImpl.java:325-331`). Both copies transfer the **same
   `List` reference** — the cached instance, `roleToSave`, and the session instance now
   *share one list* (aliasing that matters for fix design).
2. **Copy 2 — `DBFQN` cascade loop.** `populatChildrenForRoles` at
   `RoleFactoryImpl.java:374`; within one Hibernate session, `hu.load()` returns the same
   instance, so the append lands on the same list. The queue is then seeded with the
   now-doubled list (line 379).
3. **Copy 3 — `setFQNForDB` on each child** (`RoleFactoryImpl.java:872-885`) walks up via
   `getRoleById(current.getParent(), false)`. The updated role was `rc.remove`d at the top
   of `save()` (line 326), so this cache-misses, reloads the same session instance, and
   `getRoleById` populates it again (lines 64-97). Side effect: this also re-`add`s the
   mid-save role to the cache (lines 74/93).
4. **Copy 4 — end of `save()`.** `populatChildrenForRoles` again at
   `RoleFactoryImpl.java:395-398`, then `rc.add(r)` caches the 4-copy instance.

`toRoleViews` (`RoleHelper.java:526-570`) then resolves every id in the list with no dedup,
and `RoleView.childCount` is `getRoleChildren().size()` (`RoleView.java:45`), so both fields
carry the duplication to the wire. `RoleAPIImpl.save` and `loadRoleById` add no extra passes
in this repro (validation only / cache delegation). The exact pass count can shift with
topology (deeper trees add `setFQNForDB` walks), which is why acceptance is framed as
"exactly once", not "not four".

**Open question for the plan phase**: `rc.add(r)` caches the polluted instance by reference
at the end of `save()`, yet the repro's subsequent `GET` returns clean data — some
post-commit invalidation must repair it. The plan must confirm the mechanism and that no
window exists where `GET` serves the duplicated list.

The plan decides the exact fix point(s) — making the helper idempotent (replace/dedup) vs.
also hardening the callers and the shared-list aliasing.

## Fix Scope & Non-Goals *(mandatory)*

**In scope**:

- `populatChildrenForRolesHelper` (or its enclosing population path) becomes **idempotent**:
  populating children on an already-populated `Role` instance yields the correct distinct
  list, not an accumulated one.
- `PUT /api/v1/roles/{roleId}` returns each child exactly once, with `childCount` equal to
  the number of distinct children — for plain field updates, reparents, and moves to root
  (`parentRoleId: null`).
- The `DBFQN` cascade in `RoleFactoryImpl.save` visits each descendant once per reparent
  (N updates for a subtree of N roles — today direct children are visited 2x and the
  redundancy compounds with depth) — while still reaching every descendant.
- Integration tests covering the above (see Acceptance & Verification).

**Explicitly out of scope / non-goals**:

- No rewrite or broad refactor of `RoleFactoryImpl` or the role caching layer — this is a
  bounded legacy fix with progressive enhancement only where touched.
- No change to the REST contract or response shape of any `/v1/roles` endpoint (field names,
  hydration depth — the response only hydrates two levels today, and that stays).
- No frontend changes. The Roles (Beta) portlet's client-side guard (#37260) stays as-is; it
  was needed anyway for lazy-loaded grandchildren.
- No repair/migration of persisted data — none is needed; the persisted rows were never wrong.
- No performance work on the cascade beyond removing the duplication multiplier.

## Regression Risk *(mandatory)*

- **Blast radius**: `populatChildrenForRoles` is a shared population path with 8 call sites
  in `RoleFactoryImpl` (`getRoleById`, `getRolesByNameFiltered`, `getRolesByKeyFiltered`,
  `findRoleByName`, `loadRoleByKey`, `findRootRoles`, and twice in `save`). A dedup/replace
  change there affects every caller — the key risk is a fix that *skips* legitimate children
  (the method calls the helper once per 200-role chunk, so a naive "clear then add" inside
  the helper would drop children populated by earlier chunks) or that mutates cache-resident
  `Role` instances differently than callers expect. Two further risk surfaces: the
  shared-`List` aliasing (cached instance, `roleToSave`, and session instance share one list
  via `BeanUtils.copyProperties`, so in-place mutation pollutes the cache), and the `DBFQN`
  cascade — deduplication must not cause it to miss grandchildren and below.
- **Backward compatibility**: Response shape is unchanged — the fix corrects values, not
  contract. No DB schema, ES mapping, or API contract change; **rollback-safe**.
- **Data considerations**: None. Persisted role data was always correct; no migration or
  repair needed.

## Acceptance & Verification *(mandatory)*

- **AC-001**: The reproduction above no longer shows duplication: the `PUT` response reports
  `childCount: 1` and `roleChildren` containing `TmpChild` exactly once, matching the
  subsequent `GET`.
- **AC-002**: The same holds for a plain field update (no reparent) and for a reparent to
  root (`parentRoleId: null`) on a role with children.
- **AC-003**: `populatChildrenForRolesHelper` is idempotent — calling
  `populatChildrenForRoles` twice on the same `Role` instance leaves the same child list, not
  a doubled one.
- **AC-004**: The `DBFQN` cascade visits each descendant exactly once per reparent: a subtree
  of N roles produces N updates (today: 2x for direct children, compounding for deeper
  descendants).
- **AC-005**: The cascade still rewrites `DBFQN` correctly for grandchildren and below after
  the fix — deduplication must not skip descendants.
- **Verification method**:
  - New integration test(s) mirroring the reproduction: create parent + one child, `PUT` a
    reparent, assert the response carries one child and `childCount == 1`; plus a
    grandchild-depth case asserting `DBFQN` correctness after reparent. Run targeted, e.g.
    `./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dit.test=<TestClass>`.
  - An idempotency test at the factory/API level for `populatChildrenForRoles` (AC-003).
  - Manual re-run of the curl reproduction against a local build. Do **not** use the Roles
    (Beta) portlet as evidence — its client-side guard (#37260) masks the symptom.

## Assumptions

- Released versions including current LTS are assumed affected (long-standing legacy code —
  the helper predates the v1 PUT endpoint); confirming against LTS is desirable but not a
  gate for this fix.
- The exact multiplier (4x) is an artifact of how many times the instance passes through the
  population path on `main` today; acceptance is defined as "exactly once", not "not four",
  so the fix remains correct if call counts change.
