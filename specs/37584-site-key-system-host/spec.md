# Issue Resolution Specification: New Edit Content shows every Site's Site Key as "System Host"

**Feature Branch**: `37584-site-key-system-host`

**Created**: 2026-09-16

**Status**: Draft

**Type**: Issue / Bug Resolution

**Related GitHub Issue**: dotCMS/core#37584 (from Freshdesk #39459)

**Input**: User description: "New Edit Content shows every Site's Site Key as `System Host`.
Also include the secondary defect found while investigating: `GET
/api/v1/workflow/tasks/history/comments/{identifier}` returns 500 (NPE on a null
`WorkflowTask`) when a Host is opened in the new editor."

<!--
  Two defects, one spec: both are hit on the same screen (a Host opened in the new Edit
  Content editor), both are one-line-class fixes in different files, and neither is
  independently schedulable in a way that would make separate specs cheaper. They are kept
  as distinct defects (A and B) with distinct acceptance criteria throughout.
-->

## Problem Statement *(mandatory)*

**Defect A — the Site Key field shows the wrong value.** When a Host is opened in the new
Edit Content editor, the **Site Key** field renders `System Host` for every Site, regardless
of the Site's real name. The correct name still shows in the page title, the breadcrumb, the
Site selector and the Site Browser — the wrong value appears only inside the editor and in
the JSON that editor serves. The editor is therefore not showing stored state on a required
field of a core Content Type.

The same wrong value reaches every other consumer of the hydrated Contentlet, not just the
form: `Host.getHostname()` reads the very key that is being overwritten, so any code that
wraps a Contentlet hydrated on this path in a `Host` object gets `System Host` as the Site's
own name.

**Defect B — opening a Host in the new editor throws a 500.** The editor's sidebar loads the
workflow comment timeline via `GET /api/v1/workflow/tasks/history/comments/{identifier}`.
For a Host that request returns HTTP 500 with
`Cannot invoke "com.dotmarketing.portlets.workflows.model.WorkflowTask.getId()" because
"task" is null`, and the editor raises a blocking **"Unknown Error"** modal over the form on
every load.

**Severity / Impact**: High. Every Site, on every install where the Host Content Type uses
the new editor, on every open. No data is corrupted and no data is lost (see "Fix Scope &
Non-Goals" for why the save path cannot write the bad value back), but a required field on a
core Content Type does not reflect stored state, and the screen greets the user with an
error modal. Defect A also cost real investigation time on a customer escalation by
presenting as data corruption on a production Site.

## Reproduction *(mandatory)*

**Environment**: dotCMS `26.09.14-01` container (`dotcms/dotcms:latest`), demo starter data,
PostgreSQL + OpenSearch, admin user. Code references verified against `origin/main` @
`d69f054143`. Instance config already had `DOT_CONTENT_EDITOR2_ENABLED=true` and
`DOT_CONTENT_EDITOR2_CONTENT_TYPE=*`; the Host Content Type's
`CONTENT_EDITOR2_ENABLED` metadata was flipped to `true` for the UI run and reverted after.

**Steps to Reproduce**:

*Defect A, via API (no UI or feature flag needed):*

1. `GET /api/v1/content/{siteIdentifier}` — the endpoint the new editor calls
   (`dot-edit-content.service.ts:77`).
2. Compare `entity.hostName` against `entity.title` and the `hostname` returned by
   `GET /api/v1/site/{siteIdentifier}`.

*Defects A and B, via UI:*

3. Set `CONTENT_EDITOR2_ENABLED: true` in the Host Content Type's metadata.
4. Open `#/content/{siteInode}` (Settings → Sites still opens the legacy JSP editor, which is
   unaffected).
5. Read the **Site Key** field, and observe the modal that opens over the form.

**Expected Behavior**:

- Site Key shows the Site's own name (`demo.dotcms.com`).
- `entity.hostName` equals `entity.title` and the Site's stored name.
- The editor opens with no error modal; a Host with no workflow task yields an empty
  timeline.

**Actual Behavior**:

- Site Key reads `System Host`.
- ```json
  "hostName":     "System Host",     // wrong
  "hostname":     "demo.dotcms.com", // right
  "title":        "demo.dotcms.com", // right
  "host":         "SYSTEM_HOST",
  "isSystemHost": false
  ```
- `GET /api/v1/workflow/tasks/history/comments/48190c8c-...` → HTTP 500,
  `{"message":"Cannot invoke \"...WorkflowTask.getId()\" because \"task\" is null"}`, surfaced
  as an "Unknown Error" modal.

**Reproducibility**: Always, for every Site. Verified on the demo Site and on a Site created
from scratch during the investigation (`test-sitekey.local`, since deleted) — it is not
data-dependent. Defect B is reproducible in isolation: the same endpoint returns 200 with a
populated timeline for a Blog Contentlet and 500 for a Host.

## Scope of Investigation *(mandatory)*

- **Affected area**: Content editing (new Edit Content editor) and the REST layer that
  serves it. Defect A is in the shared Contentlet-to-map transform used by
  `GET /api/v1/content/{inodeOrIdentifier}`; defect B is in the Workflow REST resource.
  Neither is a frontend defect — the editor renders exactly what the API hands it.
- **Suspected surface**: Legacy (`com.dotmarketing.*`) for defect A — the transform
  strategies live under `com.dotmarketing.portlets.contentlet.transform.strategy`. Mixed for
  defect B — a modern resource (`com.dotcms.rest.api.v1.workflow`) calling a legacy API
  (`com.dotmarketing.portlets.workflows.business`). The callers of both are modern.
- **Related known decisions**: `Host.getMap()` has aliased `hostname` to `hostName` since the
  initial trunk import (`e8ef584ec9`, 2012) and is `@JsonIgnore`, so the modern JSON path
  never runs it. `SiteViewStrategy` already exists and does the right thing for Sites, but is
  only wired to the `SITE_VIEW` transform option. Workflow actions are deliberately
  prohibited on the Host Content Type (`WorkflowAPIImpl.fireContentWorkflow`) — that
  restriction is treated as given here, not revisited.

## Root-Cause Hypothesis

**Defect A — a derived property collides with a real field variable.**
`DefaultTransformStrategy.addCommonProperties` writes a *derived* convenience property
`hostName` — the name of the Site the Contentlet lives on — into the Contentlet's map
(`DefaultTransformStrategy.java:125-126`):

```java
final Host site = toolBox.hostAPI.find(contentlet.getHost(), APILocator.systemUser(), true);
map.put(HOST_NAME, site != null ? site.getHostname() : NOT_APPLICABLE);   // HOST_NAME == "hostName"
```

`hostName` is also a real field variable on the Host Content Type — the required TextField
labelled **Site Key**. Every Host lives on the System Host by definition
(`host: "SYSTEM_HOST"`), so the derived value is always the literal `System Host`, and it
overwrites the stored field value for every Site.

This is visible only on this endpoint because `ContentResource.java:426` hydrates with
`contentResourceOptions(false)`, which includes `COMMON_PROPS` (hence
`DefaultTransformStrategy`) but **not** `SITE_VIEW` (hence not `SiteViewStrategy`, which
would restore the field from `Host.getMap()`). Endpoints that do not run this strategy —
`/api/content/id/...`, `/api/v1/content/_search`, `/api/v1/site/...` — return the correct
value, which is why the Site selector and Site Browser look fine.

Confidence: confirmed by reproduction, not a hypothesis. The issue's original theory (an
Angular binding resolving the parent host reference) described the right mechanism in the
wrong layer.

**Defect B — an unguarded null.** `WorkflowResource.getWorkflowTasksHistoryComments`
(`WorkflowResource.java:6238`) passes the result of
`WorkflowAPI.findTaskByContentlet(contentlet)` straight into
`WorkflowAPI.getCommentsAndChangeHistory(task)`. `WorkFlowFactoryImpl.findTaskByContentlet`
returns `null` when no `workflow_task` row exists (`WorkFlowFactoryImpl.java:1098-1128`), and
`getCommentsAndChangeHistory` dereferences `task.getId()` without a null check
(`WorkflowAPIImpl.java:4110`). A Host can never have a workflow task, because workflow
actions are prohibited on that Content Type — so for Hosts the null path is guaranteed
rather than incidental. The defect is not Host-specific in principle: any Contentlet with no
workflow task row reaches the same NPE.

## Fix Scope & Non-Goals *(mandatory)*

**In scope**:

- **A**: Stop `DefaultTransformStrategy` from writing the derived `hostName` property when
  the Contentlet's Content Type declares its own field with that variable. The stored field
  value wins. Applies generally (a custom Content Type with a `hostName` field is protected
  the same way), not as a `Host`-only special case.
- **B**: Make `GET /api/v1/workflow/tasks/history/comments/{identifier}` return an empty
  timeline (HTTP 200) instead of throwing when the Contentlet has no workflow task, so the
  editor renders an empty history rather than an error modal.
- Record on the issue the outcome of the save test described in its "Open question" section
  (see below) and confirm severity stays **High** rather than rising to Critical.

**Explicitly out of scope / non-goals**:

- **Removing or reworking the 2012 `hostname` / `hostName` alias in `Host.getMap()`.** The
  issue says so explicitly and it is right: fourteen years of Java, Velocity and integrations
  may read the lowercase key. Note this means `hostname` will stay absent from the payload
  for Contentlets whose stored map never carried it (observed on a freshly created Site) —
  after the fix the two keys will not *disagree*, but the alias will not be synthesised
  either. If the issue's AC-002 is read as requiring the alias to always be present, that is
  a separate change to `SiteViewStrategy` and should be its own issue.
- **Adding `SITE_VIEW` to `contentResourceOptions()`.** It would fix the symptom but
  `SiteViewStrategy` does `map.putAll(host.getMap())` with the full raw map, which would undo
  the other transforms in that option set (binaries rewritten to `/dA/` paths,
  `DATETIME_FIELDS_TO_TIMESTAMP`, categories).
- **Making the new editor able to save a Host.** It currently cannot — see the note below.
  Lifting the Host workflow prohibition is an architectural decision well beyond this defect
  and needs its own issue (and likely its own ADR proposal).
- Changes to the legacy JSP Site editor, which is unaffected by both defects.
- Broader null-safety work across `WorkflowResource`; only the endpoint that is provably
  reachable with a null task is fixed here.

**Resolving the issue's open question — "does saving throw?"**: neither branch. The save
never reaches the `forceExecution` guard at `HostAPIImpl.java:473`. Two barriers stop it
first: (1) the Host Content Type has no workflow scheme, so the editor's "Select a Workflow"
dialog returns *No results found* and no save action is offered at all; (2) forcing it
through the API — `PUT /api/v1/workflow/actions/{id}/fire` with `hostName: "System Host"` —
is rejected by `WorkflowAPIImpl.fireContentWorkflow` (lines 3433 and 3527) with
`Workflow Actions can not be executed on the Host Content Type.` Verified against the live
instance. There is therefore **no silent overwrite of `hostName`** and no
`UpdateContainersPathsJob` / `UpdatePageTemplatePathJob` cascade. Severity stays High.

## Regression Risk *(mandatory)*

- **Blast radius**: `contentResourceOptions(false)` is shared by `ContentHelper.java:191`,
  `ContentResource.java:426` (the editor's GET), `ContentResource.java:686` and `:744` (lock
  / unlock), `WorkflowResource.java:3122` (fire response) and `ContentletUtil.java:164` (the
  printable map behind CSV export). The change alters the payload **only** for Contentlets
  whose Content Type declares its own `hostName` field — in practice, Hosts. For those, the
  value changes from the constant `System Host` to the Site's own name, which is what every
  other surface already reports. Any consumer that today reads `hostName` off a hydrated Host
  expecting the *parent* Site would see a change, but that value is a constant with no
  information in it, and `Host.getHostname()` reads the same key — so today's behavior is the
  corruption, not the contract. `DefaultTransformStrategy` is also exercised by
  `defaultOptions()` and several other option sets; the guard applies uniformly and is a
  no-op for every Content Type that does not declare the field.
- **Backward compatibility**: No DB schema, ES/OpenSearch mapping or serialized-state change.
  The REST response shape is unchanged — same keys, corrected value for one Content Type.
  Defect B changes a 500 into a 200 with an empty list, which widens the set of successful
  responses and cannot break a client that handles the existing success shape. `@Schema` on
  `getWorkflowTasksHistoryComments` already declares
  `ResponseEntityWorkflowHistoryCommentsView`, so `openapi.yaml` should be unaffected — to be
  confirmed by regenerating it during implementation.
- **Data considerations**: None. No stored data was ever corrupted (see the save analysis
  above); the defect is confined to the read/transform path, so there is no repair migration.

## Acceptance & Verification *(mandatory)*

- **AC-001** *(defect A)*: `GET /api/v1/content/{siteIdentifier}` returns `hostName` equal to
  the Site's own stored name for the default Site, a non-default Site, and the System Host.
  The reproduction steps produce the expected behavior.
- **AC-002** *(defect A)*: `hostName` and `title` agree in that payload, and neither
  disagrees with `hostname` when that key is present. The `hostname` alias is not added,
  removed or otherwise touched.
- **AC-003** *(defect A, UI)*: With the Host Content Type on the new editor, the **Site Key**
  field shows the Site's own name.
- **AC-004** *(defect A, regression)*: For a Contentlet whose Content Type does **not**
  declare a `hostName` field (e.g. a Blog), `hostName` still resolves to the name of the Site
  the Contentlet lives on, on every endpoint listed under Blast radius.
- **AC-005** *(defect B)*: `GET /api/v1/workflow/tasks/history/comments/{identifier}` returns
  HTTP 200 with an empty list for a Contentlet with no workflow task, including every Host.
- **AC-006** *(defect B, regression)*: The same endpoint still returns the full timeline for a
  Contentlet that does have a workflow task (verified against a Blog on demo, which returns a
  populated timeline today).
- **AC-007** *(defect B, UI)*: Opening a Host in the new editor raises no error modal.
- **AC-008**: The save-path finding above is recorded as a comment on dotCMS/core#37584, and
  the severity is confirmed as High.

- **Verification method**:
  - Unit: extend `DefaultTransformStrategyTest`
    (`dotCMS/src/test/java/com/dotmarketing/portlets/contentlet/transform/strategy/DefaultTransformStrategyTest.java`)
    to cover the collision case and the non-collision case (AC-001, AC-004).
  - Integration: extend `ContentletTransformerTest`
    (`dotcms-integration/src/test/java/com/dotmarketing/portlets/contentlet/transform/ContentletTransformerTest.java`)
    with a Host hydrated through `contentResourceOptions(false)` (AC-001, AC-002, AC-004), and
    add a workflow-comments case under
    `dotcms-integration/src/test/java/com/dotcms/rest/api/v1/workflow/` (AC-005, AC-006).
    Run with `-Dcoreit.test.skip=false -Dmaven.build.cache.enabled=false`, select at class
    level, and confirm `Tests run: N` in `target/failsafe-reports/*.txt` — a cached failsafe
    run reports BUILD SUCCESS with zero tests.
  - **Any new integration test class must be registered in the relevant
    `MainSuite*`/`Junit5Suite*` `@SuiteClasses` list**, or it compiles, passes locally via
    `-Dit.test=` and is silently never run in CI.
  - Manual: the reproduction steps above, on the demo Site, a non-default Site and the System
    Host (AC-003, AC-007).
  - Regenerate `openapi.yaml` via `./mvnw compile -pl :dotcms-core --am -DskipTests` and
    confirm it is unchanged (or commit it alongside if the response contract shifts).

## Assumptions

- The Host workflow prohibition in `WorkflowAPIImpl.fireContentWorkflow` is intentional and
  stays. This spec therefore fixes what the editor *displays*, and does not attempt to make
  the editor able to save a Host.
- `ContentType.fieldMap()` is documented to NPE on fields with a null variable
  (`ContentType.java:299`), so the field-collision check should iterate `fields()` defensively
  rather than call `fieldMap()`. Treated as an implementation constraint for the plan, not a
  requirement here.
- No customer-authored Content Type in the wild relies on the derived `hostName` overwriting
  a same-named field it declares itself. Such a type would be getting a constant today and is
  vanishingly unlikely to exist.
- Demo data is representative: both defects reproduced on a stock demo instance and on a
  Site created from scratch, matching the report's own observation on demo.dotcms.com and
  Evergreen 26.08.19-04.
