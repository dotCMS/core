# Issue Resolution Specification: Publishing Queue → Pending tab "Delete" is a silent no-op when an asset is queued in two bundles

**Feature Branch**: `36861-pending-delete-noop`

**Created**: 2026-09-07

**Status**: Draft

**Type**: Issue / Bug Resolution

**Related GitHub Issue**: [#36861](https://github.com/dotCMS/core/issues/36861) (helpdesk ticket 38574)

**Input**: User description: "Defect: Pending tab 'Delete' is a silent no-op when an asset is queued in two bundles. Component: Publishing Queue portlet, Pending tab. Severity: Medium. Current behavior: Delete does nothing, and it seems there are dojo errors on tab load. Expected behavior: No dojo parser errors on load, all checkboxes functional, Delete button removes the selected bundle from the queue."

## Problem Statement *(mandatory)*

In **Publishing Queue → Pending**, selecting a bundle (or an asset row) and clicking **Delete**
does nothing: no error, no dialog, no change to the list, and no server-side log entry. The
selection is silently discarded and the request that is issued carries no delete instruction.

Because the Pending tab is the only UI path to cancel a *scheduled* push publish, an author who
schedules a bundle by mistake has no way to cancel it — the bundle publishes on its due date
regardless. There is no client-side recovery: the failure is deterministic and recurs on every
page load, including a hard refresh.

The trigger is ordinary content reuse. Pending renders up to 10 bundles per page, each showing
its first 20 assets. When the **same asset with the same operation is queued in two different
bundles rendered on the same page** — normal when shared content (a site stripe, a nav tile, a
shared widget) is included in more than one scheduled campaign — the second asset checkbox
tries to register a `dijit.form.CheckBox` under an id that is already in the widget registry.
Dojo's parser throws and **aborts the remainder of the parse pass**, leaving every checkbox
below the collision point as an un-upgraded plain `<input>`.

**Severity / Impact**: Medium. Affects any authenticated back-end user with publish permission
on the Pending tab, on every load, whenever a duplicated asset appears among the bundles on the
current page. Impact is loss of the only cancel path for scheduled push publishes — the
consequence is an unwanted publish to a remote environment, which is user-visible and not
trivially reversible.

There is a visible signature useful for triage: checkboxes render as large styled dijit widgets
down to the first occurrence of the duplicated asset, then switch to small unstyled native
checkboxes from the second occurrence onward. That visual break marks the exact parse-abort
point; nothing below it can be deleted.

## Reproduction *(mandatory)*

**Environment**: dotCMS Evergreen build 26.07.06-3 (reproduces on current `main`). Back-end
admin UI, any browser. Legacy Dojo portlet at
`/html/portlet/ext/contentlet/publishing/view_publish_queue_list.jsp`, loaded by
`view_publish_tool.jsp`. Requires a user with publish permission on the queued assets. No
special configuration; default page size (`limit=10`) and default per-bundle asset cap
(`MAX_BUNDLE_ASSET_TO_SHOW = 20`).

**Steps to Reproduce**:

1. As a CMS Admin, pick any single contentlet (e.g. a shared widget or content item).
2. **Add to Bundle → Bundle A**, then push publish Bundle A with a *future* publish date.
3. **Add to Bundle → Bundle B**, using the *same* contentlet as step 1, then push publish
   Bundle B with a *different* future publish date.
4. Navigate to **Publishing Queue → Pending**. Both bundles are listed on the same page
   (default page size is 10).
5. Open the browser devtools **Console**.
6. Tick the checkbox of a bundle *below* the second occurrence of the duplicated asset and click
   **Delete**.
7. Reload, then tick the **asset row** for the duplicated contentlet *inside Bundle A only* and
   click **Delete**. (Exercises the asset-level path and, post-fix, the bundle-scoping in
   AC-004.)

**Expected Behavior**:

- The Pending tab loads with **no `dojo/parser` errors**.
- **All** checkboxes — bundle-level and asset-level — upgrade to dijit widgets and are
  interactive.
- **Delete** removes the selected bundle(s) from the queue; the list refreshes without them.
- Deleting an individual **asset row** removes that asset from **that bundle only** — the same
  asset queued in another bundle is left alone (see **DEC-001** / AC-004).

**Actual Behavior**:

- On load, the console shows a duplicate-widget-id error thrown from `dojo/parser::parse()` and
  from `_ContentSetter#Setter_queueContent_N`:

  ```
  dojo/parser::parse() error Error: Tried to register widget with
  id==queue_to_delete_c6026087b8b4fea6b96cad8456b1cf50$1 but that id is already registered
  ```

- Checkboxes below the second occurrence of the duplicated asset render as unstyled native
  checkboxes (never upgraded to widgets).
- Ticking any checkbox below that point and clicking **Delete** does nothing — bundle-level
  (step 6) and asset-level (step 7) alike. The Network tab
  shows a request to `view_publish_queue_list.jsp` carrying only `layout`/`offset`/`limit` —
  **no `delete=` and no `deleteBundle=` parameter**. Nothing is logged server-side. The bundle
  stays queued and publishes on its due date.

**Reproducibility**: Always, given the required data state (one asset queued in ≥2 bundles that
render on the same Pending page with the same operation). Independent of browser or session; a
reload reproduces it identically because the collision happens within a single parse pass.

## Scope of Investigation *(mandatory)*

- **Affected area**: Push publishing — Publishing Queue portlet, Pending tab (admin UI). Server
  side, the delete request handling and `PublisherAPI` queue-element deletion.
- **Suspected surface**: **Legacy**. The Pending tab is a Dojo/dijit JSP:
  `dotCMS/src/main/webapp/html/portlet/ext/contentlet/publishing/view_publish_queue_list.jsp`.
  Server-side deletion goes through `com.dotcms.publisher.business.PublisherAPIImpl` (modern
  package, legacy-era code). A modernized Angular replacement exists behind the
  `publishing-queue-beta` route (`core-web/libs/portlets/dot-publishing-queue`, PrimeNG, no
  Dojo) but is **not** the shipped Pending tab and is not affected by this defect.
- **Related known decisions**: The plan phase formally consults `dotCMS/platform-adrs`. The fix
  must respect the constitution's Legacy-Aware Development principle — repair the legacy JSP in
  place with progressive enhancement, do not rewrite it and do not pull the fix into the beta
  portlet.

## Root-Cause Hypothesis

The asset-level checkbox id is built from the **asset identifier and operation only**, omitting
`bundle_id`, so it is not globally unique on a page:

`view_publish_queue_list.jsp:313`

```jsp
id="queue_to_delete_<%=asset.get(PublishQueueElementTransformer.ASSET_KEY) %>$<%=asset.get(PublishQueueElementTransformer.OPERATION_KEY) %>"
```

The bundle-level checkbox 65 lines above does include it (`view_publish_queue_list.jsp:248`):

```jsp
id="bundle_to_delete_<%=bundle.get("bundle_id") %>"
```

When the same `asset$operation` pair renders twice on one page, the second `dijit.form.CheckBox`
collides in the widget registry, `dojo/parser` throws, and the rest of the parse pass is
abandoned.

**Why the abort makes Delete a silent no-op** — `deleteQueue()` and `deleteBundle()`
(`view_publish_queue_list.jsp:153–183`) gate on:

```js
if(dijit.getEnclosingWidget(node).checked && !dijit.getEnclosingWidget(node).disabled){
```

For a node that was never upgraded, `dijit.getEnclosingWidget()` walks up the DOM and returns
the nearest *enclosing* widget — the `queueContent` `ContentPane`. A `ContentPane` has no
`.checked`, so the condition is `undefined` → falsy and the node is skipped. It does not even
throw, because `.disabled` is also `undefined` and `!undefined === true`. `ids` therefore stays
`""` in both functions, neither `&delete=` nor `&deleteBundle=` is appended, and
`refreshQueueList(url)` re-fetches the list with layout/offset/limit alone. The server is never
asked to delete anything, which is why the logs are silent.

Three adjacent defects in the same file are implicated in the same user-visible failure and are
in scope:

1. **`checkAllBundle()` never selects child rows** (`view_publish_queue_list.jsp:134–142`). It
   uses the descendant selector `dojo.query(".b" + x + " input")`, but the `b<bundleId>` class
   is on the `<input>` itself (line 310), so the query always returns an empty list. The correct
   selector is `dojo.query("input.b" + x)`. This means "check the bundle" has never cascaded to
   its asset rows.
2. **No failure feedback.** `deleteQueue()` / `deleteBundle()` do not null-guard
   `dijit.getEnclosingWidget()` and do not tell the user when zero ids were collected — they
   just reload the pane, which is indistinguishable from success.
3. **`permissionMap` NPE for empty bundles** (`view_publish_queue_list.jsp:234`).
   `permissionMap.get(bundle.get("bundle_id")).equals(Boolean.TRUE)` NPEs for a bundle with zero
   queue elements, because `permissionMap` is only populated inside the inner asset loop
   (lines 203–217) — a bundle whose `bundleAssets` list is empty is never put in the map.

## Fix Scope & Non-Goals *(mandatory)*

**In scope**:

- Make the asset-level checkbox id at `view_publish_queue_list.jsp:313` globally unique by
  including `bundle_id`, mirroring the bundle-level checkbox at line 248.
- Carry each asset checkbox's owning `bundle_id` through to the server so the delete can be
  bundle-scoped (**DEC-001**). The plan picks the carrier — an extra `$`-segment on `value`, the
  `b<bundleId>` class already on the `<input>` (line 310), or a new `data-bundle-id` attribute.
  Whichever it picks, the **first `$`-segment of `value` must remain the bare asset id**, because
  `deleteQueue()` derives it with `nodeValue.split("$")[0]`.
- Fix `checkAllBundle()` to use `dojo.query("input.b" + x)` so ticking a bundle checks and
  disables its child asset rows as intended.
- Null-guard `dijit.getEnclosingWidget()` in `deleteQueue()` / `deleteBundle()` and surface a
  user-visible failure when no ids are collected, instead of reloading the pane as if the delete
  succeeded.
- Null-guard the `permissionMap` lookup at line 234 so a bundle with zero queue elements does
  not NPE the whole page render.
- **Scope asset-level delete to the selected bundle** (decision **DEC-001** below). Add a
  bundle-scoped delete to `PublisherAPI` / `PublisherAPIImpl` —
  `DELETE FROM publishing_queue WHERE asset = ? AND bundle_id = ?` — and have the Pending tab
  submit the `bundle_id` alongside the asset id so ticking an asset row in Bundle A removes it
  from Bundle A only. The existing bundle-agnostic
  `deleteElementFromPublishQueueTableAndAuditStatus(String)` is **retained** for
  backward compatibility with any external/plugin caller.
- Progressive enhancement limited to the lines touched (per constitution Principle I).
- Automated regression coverage for the duplicate-asset-across-bundles case (see
  *Acceptance & Verification*).

**Explicitly out of scope / non-goals**:

- **No port to the Angular beta portlet.** `core-web/libs/portlets/dot-publishing-queue`
  (`publishing-queue-beta`) is a separate modernization track and is unaffected.
- **No rewrite or de-Dojo-ing of `view_publish_queue_list.jsp`**, and no restructuring of the
  Pending tab's pagination or asset-cap behavior (`limit=10`, `MAX_BUNDLE_ASSET_TO_SHOW = 20`).
- **No removal or signature change of the existing bundle-agnostic
  `deleteElementFromPublishQueueTableAndAuditStatus(String)`** — the bundle-scoped delete is
  **additive**. No other queue-delete method, and no other `PublisherAPI` behavior, changes.
- **No change to the publish-audit-status cleanup rule** beyond applying it to the new method:
  the audit status for a bundle is deleted when, and only when, that bundle's queue becomes
  empty — same as today.
- **No refactor of the O(bundles) permission loop** at lines 202–218 (which calls
  `doesUserHavePermission` per bundle rather than using batch
  `permissionAPI.filterCollection`). Worth a follow-up issue; not this fix.
- **No fix for other Publishing Queue tabs** (Auditing, History, Bundles) unless they share the
  exact duplicate-id defect, which is not currently believed to be the case.

## Regression Risk *(mandatory)*

- **Blast radius**:
  - `view_publish_queue_list.jsp` is loaded only by `view_publish_tool.jsp`, so the render change
    is contained to the Publishing Queue portlet. The `id` attribute is not referenced anywhere
    else in the codebase — selection is driven by the `.queue_to_delete` / `.bundle_to_delete`
    classes and by `value`, both unchanged.
  - Fixing `checkAllBundle()` is a **behavior change users will notice**: today ticking a bundle
    silently does nothing to its asset rows; after the fix it will check and disable them. This
    is the intended original behavior but must be verified not to interfere with `deleteQueue()`,
    which skips `disabled` nodes by design (so the bundle-level delete path, not the per-asset
    path, handles a fully-selected bundle).
  - Making asset-level Delete actually fire for the first time in this scenario reaches a
    server-side delete path users have effectively not been able to exercise. Per **DEC-001**
    that path now becomes a **new** bundle-scoped method, so the blast radius on the server is
    the new method plus the single JSP call site — the existing bundle-agnostic method keeps its
    behavior and its callers.
  - Known in-repo callers of the queue-delete methods are only
    `view_publish_queue_list.jsp:84,90` and
    `dotcms-integration/src/test/java/com/dotcms/publisher/business/PublisherAPIImplTest.java`
    (lines 74, 111, 149). Nothing else in core calls them, so the new method cannot alter an
    unrelated flow.
  - `deleteElementsFromPublishQueueTableAndAuditStatus(bundleId)` — the whole-bundle delete
    behind `&deleteBundle=` — is **not** touched.
- **Backward compatibility**: No REST endpoint, `openapi.yaml`, DB schema, or ES mapping change,
  and no new SQL migration — `publishing_queue.bundle_id` already exists. Not rollback-unsafe.
  The changed checkbox `id` is presentation-only; no persisted state, bookmark, or integration
  depends on it. The `PublisherAPI` change is **additive** (a new method on the abstract class),
  so a plugin compiled against the current API keeps working; the old bundle-agnostic method
  stays and keeps its exact semantics. Whether it should be `@Deprecated` is a plan-phase call —
  removing it is out of scope.
- **Data considerations**: No migration. `publishing_queue` rows are *pointers* to assets, not
  the assets themselves — deleting a queue entry never touches a page or contentlet. Existing
  queued bundles need no repair: the defect is purely a render/selection failure, so once the
  parse completes correctly the already-queued rows become deletable through the normal UI.
  Existing stuck bundles can be cleared with the documented workaround
  (`refreshQueueList("offset=0&limit=1")` in the browser console — renders one bundle per page so
  the collision cannot occur; it passes no `deleteBundle` parameter, so it deletes nothing on its
  own and lets the user complete the delete through the UI).

## Acceptance & Verification *(mandatory)*

- **AC-001**: With the same asset (same operation) queued in two or more bundles rendered on one
  Pending page, the tab loads with **zero `dojo/parser` errors** in the console, and every
  bundle-level and asset-level checkbox is upgraded to a dijit widget (no unstyled native
  checkboxes anywhere in the list).
- **AC-002**: In that same scenario, ticking a bundle checkbox and clicking **Delete** issues a
  request carrying `&deleteBundle=<bundleId>` and the bundle is removed from the queue. Ticking
  individual asset rows and clicking **Delete** issues a request that identifies **both** the
  asset and its owning bundle — the exact parameter shape is the plan's call per **DEC-001** —
  and only those bundle-scoped queue entries are removed. A request that names an asset without
  its bundle is a failure of this AC.
- **AC-003**: The asset-level checkbox `id` rendered at `view_publish_queue_list.jsp:313` is
  unique per (bundle, asset, operation) and includes `bundle_id`; `deleteQueue()` continues to
  derive the bare asset id from the checkbox `value` and additionally carries the owning
  `bundle_id` to the server.
- **AC-004** *(DEC-001)*: With asset X queued in both Bundle A and Bundle B, ticking X's row
  **inside Bundle A** and clicking **Delete** removes X's queue entry from Bundle A and **leaves
  Bundle B's entry for X intact**. Bundle B still lists X and still publishes it on its due date.
- **AC-005** *(DEC-001)*: The bundle-scoped delete deletes the publish-audit status for a bundle
  only when that delete empties the bundle's queue — matching the existing rule for the
  bundle-agnostic method. A bundle that still has other queue elements keeps its audit status.
- **AC-006**: `checkAllBundle(bundleId)` checks **and** disables every asset row of that bundle
  (previously a no-op due to the descendant selector), and unticking restores them.
- **AC-007**: When **Delete** is clicked with no selection — or when no ids can be collected for
  any reason — the user sees an explicit message rather than a silent pane reload, and
  `dijit.getEnclosingWidget()` returning a non-checkbox widget cannot produce a silent skip. The
  message is raised with a native `alert()` whose text comes from a **new `Language.properties`
  key** read via `LanguageUtil.get(pageContext, …)`, matching `view_publish_tool.jsp:307`. A
  hardcoded English string fails this AC.
- **AC-008**: A Pending page containing a bundle with **zero** queue elements renders without a
  `NullPointerException` at `view_publish_queue_list.jsp:234`.
- **AC-009 (regression)**: Existing Pending-tab behavior is unchanged when no asset is
  duplicated across bundles: pagination (Previous/Next), the "showing first 20 of N" notice,
  per-bundle permission filtering, and single-bundle delete all behave as before.

- **Verification method**:

  Every AC is assigned a gate below. Where a gate is manual, the reason is stated — per
  constitution Principle V, "no automated test" is never a silent default.

  | AC | Gate | Mechanism |
  | --- | --- | --- |
  | AC-004, AC-005 | **Automated — integration** | New test in the existing `PublisherAPIImplTest` (see below) |
  | AC-001, AC-002, AC-003, AC-006, AC-007, AC-008 | **Automated — render/DOM**, *if* the plan picks e2e | See *Render-level gate* below; coverage depends on which mechanism the plan chooses |
  | AC-009 | **Manual regression pass** | Pagination, the "showing first 20 of N" notice, permission filtering and single-bundle delete are pre-existing behavior with no current automated coverage; adding it is out of scope for this fix |

  **The render-level mechanism choice is consequential, not cosmetic.** If the plan picks a
  Playwright e2e against the portlet, it can cover AC-001, AC-002, AC-003, AC-006, AC-007 and
  AC-008 — six of the nine. If it instead only extracts the id construction into testable
  server-side code, that covers AC-003 and part of AC-001, and AC-002, AC-006, AC-007 and AC-008
  fall back to manual. The plan must state which it chose and therefore which ACs end up
  manual-only. Two caveats for the e2e route: AC-008 needs a **zero-queue-element bundle**
  fixture, which is awkward to produce through the UI and may need direct DB or API setup; and
  AC-007 asserts a **native `alert()`**, so the test must register a dialog handler.

  - **Integration (automated gate for AC-004 / AC-005)**: extend the existing
    `dotcms-integration/src/test/java/com/dotcms/publisher/business/PublisherAPIImplTest.java` —
    already registered in `MainSuite1a` (line 89), so no new suite registration is needed and
    the CI silent-skip trap is avoided. It already covers the sibling methods
    (`test_deleteElementFromPublishQueueTable_OneAsset`, `..._TwoAssets`,
    `test_deleteElementsFromPublishQueueTable_TwoAssets`), giving the new bundle-scoped delete a
    direct pattern to follow. Add a test that queues one asset into two bundles, deletes it from
    one, and asserts the other bundle's entry survives and its audit status is untouched:
    `./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dit.test=PublisherAPIImplTest#<newMethod>`
    (the suite holds two same-named classes — `com.dotcms.publishing` and
    `com.dotcms.publisher.business` — so the plan should use the fully-qualified form if
    `-Dit.test=` is ambiguous).
  - **Manual (always run, regardless of the automated gates — this is the reported repro)**:
    execute the reproduction steps above with devtools open; assert by console output (no
    `dojo/parser` error, all checkboxes upgraded) and Network tab parameters (bundle-scoped
    delete parameters present). Covers AC-001, AC-002, AC-006, AC-007 and AC-009 by inspection,
    and is the acceptance evidence for the helpdesk ticket.
  - **Render-level gate (AC-001, AC-002, AC-003, AC-006, AC-007, AC-008)**: the JSP has no
    existing automated coverage and no unit-test harness. The plan must choose and justify the
    mechanism — a Playwright e2e against the portlet, or extracting the id construction into
    testable server-side code — and record the resulting manual-only set per the table above.
    Per constitution Principle V the plan must not silently drop this: either implement it or
    state explicitly why the layer cannot be tested.

## Resolved Decisions

**DEC-001 — asset-level delete is scoped to the selected bundle** *(decided by the developer,
2026-09-07; was the spec's one open clarification)*.

`PublisherAPIImpl.deleteElementFromPublishQueueTableAndAuditStatus(asset)` executes
`DELETE FROM publishing_queue WHERE asset = ?` — it is **bundle-agnostic**. Once the asset-level
checkboxes work, ticking a duplicated asset inside Bundle A would also remove that asset's queue
entry from Bundle B, which is exactly the scenario this defect is about.

| Option | Behavior | Outcome |
| --- | --- | --- |
| A. Preserve current semantics | Asset delete removes the asset from *all* bundles | Rejected — surprising in precisely the duplicate case this fix targets |
| B. **Scope the delete to the selected bundle** | Pass `bundle_id` alongside the asset; delete only that pairing | **Chosen** |
| C. Preserve semantics + warn | Keep A, warn when the asset is in >1 bundle | Rejected — leaves the underlying behavior wrong |

**Consequence**: the fix boundary is wider than a pure render repair. It adds a bundle-scoped
delete to `PublisherAPI` / `PublisherAPIImpl` (`... WHERE asset = ? AND bundle_id = ?`) plus the
`bundle_id` plumbing in the JSP, keeping the existing bundle-agnostic method intact and additive.
This is a **behavior change**, not only a bug fix: an asset delete that today (when reachable at
all) clears the asset from every bundle will afterwards clear it from one. That is the intended
semantics and is covered by AC-004. It also gives the fix a genuinely testable server-side seam,
which resolves the TDD concern for a defect that otherwise lives only in a JSP.

**No open clarifications remain.**

## Assumptions

- The issue's line references (302, 237, 223, 123) are from build 26.07.06-3; the equivalent
  lines on current `main` are **313, 248, 234, 134** respectively. This spec uses the current
  `main` numbers.
- Duplicate ids are the *only* cause of the reported parse abort. If a Pending page produces
  `dojo/parser` errors from another source, that is a separate issue.
- For a bundle missing from `permissionMap` (zero queue elements), the safe default is to treat
  it as **not permitted** and skip rendering it — preserving the existing intent of the
  permission gate rather than newly exposing empty bundles. The plan confirms this is the
  desired UX.
- The user-visible failure message for **AC-007** uses the nearest in-repo convention: a native
  `alert()` carrying a `LanguageUtil.get(pageContext, …)` string, as at
  `view_publish_tool.jsp:307`. Note this is the *sibling* file's pattern —
  `view_publish_queue_list.jsp` has no notification mechanism of its own today, so this is a
  convention borrowed from the portlet around it rather than one already present in the edited
  file. Introducing a dojo dialog, a growl/toast, or any new notification framework is out of
  scope.
- The new language key is added to `Language.properties` only. Verified norm: the portlet's own
  recent key `unpublished.bundles.item.show` exists in `Language.properties` and in **none** of
  the nine `Language_*.properties` locale bundles, so English-only for a new key matches
  established practice; translation is not a gate for this fix.
- The reported helpdesk ticket (38574) needs no separate data repair beyond the documented
  workaround.
