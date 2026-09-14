# Issue Resolution Specification: UVE lets users modify contentlets they have no edit permission on

**Feature Branch**: `37376-uve-edit-pencil-permission`

**Created**: 2026-09-04

**Last Updated**: 2026-09-04 (rewritten after clarification — scope widened from the pencil alone to every contentlet-modifying affordance)

**Status**: Draft

**Type**: Issue / Bug Resolution

**Related GitHub Issue**: dotCMS/core#37376

**Input**: User description: "In the Page Editor (UVE), a user without edit permission on a contentlet can still click the edit pencil and open the contentlet editor, even though they shouldn't be able to. The edit pencil for a contentlet should be disabled/greyed out when the current user does not have edit permission on that contentlet. The backend implementation is ready and this is a behavior that we lost in current updates so it should only require frontend (UVE Lib) or SDK changes"

## Problem Statement *(mandatory)*

In the Page Editor (UVE), none of the affordances that modify a contentlet check whether the
logged-in user is allowed to modify *that* contentlet. A user who holds edit permission on the
page — and therefore legitimately opens the editor — but only view permission on a contentlet
placed on it can still open the full content editor from the hover pencil, open and fill the Quick
Edit form, and click into inline-editable fields.

The permission itself is not missing. It is computed server-side during page render and written
into the page markup on every contentlet, and has been for years. What is missing is any consumer:
nothing in the editor reads it, so every gate defaults to "allowed".

The reported symptom is the edit pencil. Investigation found the same gap on two further paths that
also write contentlet fields — the Quick Edit form and both inline-editing flows — so fixing only
the pencil would close the front door and leave the side ones open. The gate this spec defines
therefore covers every affordance that **modifies the contentlet**, and deliberately leaves
structural page actions alone.

The immediate harm is a misleading interface rather than a data breach. The user already has read
access to the contentlet — it is rendered on a page they may open — so opening an editor discloses
nothing new, and the persistence layer applies its own permission checks on save. But the affordance
is wrong, it invites work that cannot be completed, and it makes the product's permission model look
unenforced to the people who configured it.

**Severity / Impact**: Medium. Affects every environment that uses contentlet-level permissions to
restrict editing below the content-type level — typically editorial teams with shared, reusable
content blocks placed across many pages. Reproducible on demand for any such user; not intermittent,
not data-dependent beyond the permission setup. It does not corrupt data and does not self-heal, and
it is visible on every page the affected user opens in the editor.

## Clarifications

### Session 2026-09-04

- Q: Does the backend return `data-dot-can-edit` in the Page API for both `render` and `json`? → A: `render` yes, `json` no. Traditional UVE pages read their DOM from `page.rendered` returned by `GET /api/v1/page/render/...` (the store selects `render` when no `clientHost` is set, and `$iframeURL` is empty for `PageType.TRADITIONAL`, so the editor writes that HTML into the iframe). That HTML is produced by the Velocity container templates from `ContainerLoader`, so the attribute is present. `GET /api/v1/page/json/...` — used for headless pages — returns contentlet maps built by `DotTransformerBuilder`, which carry no permission field; headless iframes also build their own wrappers via `getDotContentletAttributes()`, which does not emit the attribute. This confirms the fix covers traditional pages and does not cover headless.
- Q: Should the restricted pencil be disabled only, or disabled plus an explanatory tooltip? → A: Both — disabled/greyed **and** a tooltip explaining why.
- Q: Which UVE affordances should the contentlet-permission gate cover? → A: Every affordance that **modifies the contentlet** — the edit pencil, the Quick Edit (⚡) form, and inline field editing. Structural page actions (delete, move/drag, add) stay ungated: a user who can enter edit mode on the page may change the page's composition; what they may not do is alter a contentlet they lack permission on.
- Q: Does the Quick Edit gate cover only the ⚡ button, or the side panel form too? → A: Both. The ⚡ button is disabled in the toolbar and overflow menu, **and** the quick-edit form itself renders read-only with a permission notice when the selected contentlet is restricted. The panel follows the current selection once open, so gating the button alone would leave a two-click bypass. The permission must therefore survive `$contentletEditData()`, which today replaces the DOM payload contentlet with the page-asset object and drops it. Refusing the save call itself was considered and left out — the server already rejects it.
- Q: How should a missing `data-dot-can-edit` attribute be treated? → A: **Fail open** — absent, empty or malformed means allowed. Headless/SDK-rendered pages never emit the attribute, so failing closed would disable every gate on every headless page for every user, including administrators. ADR-0019's date-lockstep versioning also makes mismatched SDK/CMS pairs a normal state that must degrade to current behavior.
- Q: How many message strings should explain the missing permission? → A: **One shared key** — `uve.contentlet.no.edit.permission` — used by the pencil tooltip, the Quick Edit tooltip, the read-only panel notice and the inline-edit toast. Keeps a single translation item and one consistent wording; the copy must therefore read acceptably in all four placements.
- Q: Should a Playwright e2e test be part of this change? → A: **No.** Unit and component tests cover every acceptance criterion, plus the manual scenario in `quickstart.md`. No limited-permission user fixture exists and building one (user, role, content type / instance permission split, page) exceeds the fix. Recorded as the explicit developer decision required by Constitution Principle V; a reusable permissions e2e fixture should be tracked separately.
- Q: Should a refused inline-edit click be silent? → A: **No — show a toast.** A click that does nothing reads as a broken editor and turns into a support ticket. The refusal must say why, reusing the shared permission message.
- Q: Should the style editor be gated too? → A: **Yes — the side panel must not permit editing *or* styling a contentlet the user has no permission on.** Style properties describe how *this contentlet* looks, so they belong with the contentlet rather than the page's composition. This moves style editing out of the "structural page actions" carve-out; delete, move/drag and add content stay ungated because those change which contentlets the page uses, not the contentlet itself.
- Q: Should headless / SDK-rendered pages be covered after all? → A: **Yes — scope amended 2026-09-04, after the spec was approved.** Two findings changed the calculus. First, the permission is *already computed* per contentlet at `PageRenderUtil:338` for the Velocity `$EDIT_CONTENT_PERMISSION` variable and simply discarded for JSON, so surfacing it costs no extra permission checks. Second, GraphQL exposes contentlets through the `_map` JSON scalar (`ContentFields:89` → `ContentMapDataFetcher`), which the client SDK spreads verbatim — so one server-side line reaches **both** `/api/v1/page/json` and GraphQL with no schema change and no SDK query change. The developer chose to keep this in the same PR rather than a follow-up, because review capacity is the scarce resource. **This spec amendment requires re-approval.**
- Q: Does inline editing include the Block Editor? → A: **Yes, and it is a separate event.** Two distinct paths must both be gated: plain/WYSIWYG inline editing, a direct click on a `[data-mode]` element handled in the editor component; and Block Editor inline editing, where the SDK binds a click on `[data-block-editor-content]` and posts `INIT_INLINE_EDITING` with type `BLOCK_EDITOR`, handled in the UVE actions handler. Gating only the first would leave block-editor fields editable.

## Reproduction *(mandatory)*

**Environment**: dotCMS `main` (verified against `788795e915`). Default UVE configuration —
`FEATURE_FLAG_UVE_LEGACY_SCRIPT_INJECTION` unset, so the modern `/ext/uve/dot-uve.js` client is in
use. A traditional, VTL-rendered page opened in the Page Editor in EDIT mode. Any supported browser.

**Setup**:

1. As an administrator, create a page and place a contentlet on it (for example a content block or
   banner) in a container. Use a content type with at least one inline-editable field and, ideally,
   a Block Editor field.
2. On that contentlet **instance**, grant a role **View** permission only — explicitly not Edit.
3. On its **content type**, grant the same role Publish (which implies edit at the type level), so
   that type-level and instance-level permission disagree. This disagreement is the whole point.
4. Grant that role edit access to the page itself, so the user can legitimately open the editor.
5. Log in as a back-end user holding only that role.

**Steps to Reproduce** — four independent paths, all currently ungated:

*Path A — hover toolbar pencil (the reported symptom):*

6. Open the page in the Page Editor and hover the restricted contentlet.
7. Click the edit pencil. If the contentlet is on more than one page, choose **Edit All Pages**.
8. Repeat via the collapsed overflow (`⋯`) menu shown on narrow contentlets.

*Path B — Quick Edit button:*

9. Hover the restricted contentlet and click the Quick Edit (⚡) button.

*Path C — Quick Edit panel following the selection:*

10. Open the Quick Edit panel on a contentlet the user *can* edit, then click the restricted one.

*Path D — inline editing:*

11. Click an inline-editable field inside the restricted contentlet.
12. Click a Block Editor field inside the restricted contentlet.

**Expected Behavior**: The pencil and the Quick Edit button render disabled and greyed out, with a
tooltip explaining that the user lacks edit permission; clicking them does nothing. The Quick Edit
panel, if it follows the selection onto the restricted contentlet, shows a read-only form with the
same explanation and no save. Inline-edit clicks do not start an editing session and raise a toast
saying why. Delete, move, add and style actions stay available throughout.

**Actual Behavior**: Every affordance renders and behaves as if the user were fully entitled. Path A
opens the full content editor with an editable form. Paths B and C present an editable quick-edit
form with a working save. Path D activates the inline editor. All of them ultimately fail at the
server on save, with no prior indication.

**Reproducibility**: Always, given the setup. No timing, load, or cluster conditions required.

## Scope of Investigation *(mandatory)*

- **Affected area**: Page Editor (UVE) — the per-contentlet hover/selection toolbar, the Quick Edit
  side panel, both inline-editing entry points, and the UVE client SDK that feeds them contentlet
  data from the rendered page.
- **Suspected surface**: Modern frontend only. Two libraries: `core-web/libs/sdk/uve` (the client
  script injected into the editor iframe, built into the committed bundle
  `dotCMS/src/main/webapp/ext/uve/dot-uve.js`) and `core-web/libs/portlets/edit-ema/portlet` (the
  Angular editor, its toolbar, its quick-edit panel and its actions handler). The Java producer of
  the permission — `com.dotcms.rendering.velocity.services.ContainerLoader`, a legacy-era rendering
  path in a `com.dotcms.*` package — is read as reference and is correct; no backend change.
- **Related known decisions**: ADR-0013 (frontend-only CI filtering) and ADR-0019 (date-lockstep SDK
  versioning) both apply and are addressed in the plan's ADR Alignment gate. Sibling issue #34215
  and its fix PR #36510 added instance-level WRITE checks to the server's "Copy and Edit" and page
  deep-copy paths; this issue is the user-interface counterpart on the same permission model.

## Root-Cause Hypothesis

The per-contentlet edit permission is already produced and already reaches the browser, but has no
reader.

During page render in EDIT mode, `ContainerLoader` wraps each contentlet in a
`[data-dot-object="contentlet"]` element carrying a set of `data-dot-*` attributes, one of which is
`data-dot-can-edit`, populated from a WRITE-level permission check for the logged-in back-end user
against that contentlet's inode. A repository-wide search for that attribute finds only producers —
`ContainerLoader` for contentlets and `DotParse` for VTL files — and no consumer anywhere in the
Java, Velocity, or TypeScript sources.

The UVE client SDK reads the contentlet's dataset on hover and click, normalizing the other
attributes into the payload it posts to the editor. `data-dot-can-edit` is not among them, so the
editor's contentlet payload model has no permission field at all. Every downstream gate therefore
has nothing to consult:

- the toolbar renders the pencil and the Quick Edit button with no disabled binding, in both the
  icon row and the collapsed overflow menu;
- the Quick Edit panel resolves its contentlet from the page asset, which carries no permission
  either;
- both inline-edit handlers validate only that an inode and field name are present.

This is consistent with the report that the behavior regressed: the attribute predates UVE, and the
UI that consumed it did not survive the editor rewrite.

## Fix Scope & Non-Goals *(mandatory)*

**In scope** — every affordance that modifies the contentlet:

- Carry the per-contentlet edit permission already present in the rendered page markup through the
  UVE client SDK into the editor's contentlet payload, and preserve it wherever that payload is
  transformed downstream.
- **Edit pencil**: disabled with an explanatory tooltip when the permission is denied, in both the
  icon-row toolbar and the collapsed overflow menu, which must stay consistent with each other.
- **Quick Edit (⚡)**: same treatment in both surfaces, *and* the side panel form itself. The panel
  follows the current selection once open, so the permission must survive into the panel's own
  contentlet data; when denied it renders read-only with a permission notice and offers no save.
- **Style editor**: same treatment — the toolbar's palette button is disabled, and the side panel's
  style tab shows the permission notice rather than the style form.
- **Inline field editing — both paths**: activating an inline-editable field on a denied contentlet
  does not start an editing session, and tells the user why with a toast.
  - *Plain / WYSIWYG inline*: a click on a `[data-mode]` element, handled in the editor component.
  - *Block Editor inline*: the SDK binds a click on `[data-block-editor-content]` and posts
    `INIT_INLINE_EDITING` with type `BLOCK_EDITOR`, handled in the UVE actions handler. A separate
    event from the first, needing its own guard.
  Both resolve the owning `[data-dot-object="contentlet"]` wrapper to read the permission.
- Prevent each action from being dispatched even if its disabled control is bypassed (stale toolbar
  bounds, programmatic emit), so no guarantee rests on styling alone.
- **Headless / SDK-rendered pages** *(amended scope)*: surface the already-computed permission on
  the contentlet map so it reaches `/api/v1/page/json` and GraphQL `_map`, emit it as
  `data-dot-can-edit` from the SDK's headless attribute builder, and bind it in the framework
  wrappers. All four gates then work on headless pages exactly as they do on traditional ones.
- Regenerate and commit the `dot-uve.js` bundle that ships the SDK change.

**Explicitly out of scope / non-goals**:

- **Structural page actions stay ungated — deliberately.** Delete, move/drag and add content
  change the *page's composition*, not the contentlet's content. A user permitted to open the
  page in edit mode is entitled to do them. Contentlet-level permission governs modifying the
  contentlet, and nothing else. Gating these would misread the permission model.
- **Any backend change.** The permission is already computed and emitted; this fix consumes it.
- **Extending the Page API (`/api/v1/page/*`) contract** to carry per-contentlet permissions. That
  is the durable fix for headless pages, and it is an API-contract change deserving its own issue
  and rollback-safety review.
- **A typed `canEdit` field on the contentlet GraphQL type.** The SDK's page query has two variants
  (`client/page/utils.ts:150`): the `_map` branch carries the permission for free, while the
  named-field branch (`DEFAULT_PAGE_CONTENTLETS_CONTENT`) would need both a new `ContentFields`
  entry and a query edit. Deferred until something actually needs that variant.
- **The legacy script-injection client** (`FEATURE_FLAG_UVE_LEGACY_SCRIPT_INJECTION`, off by
  default), which is not updated.
- **Refusing the save call server-side from the client.** `ESContentletAPIImpl.checkin` already
  requires WRITE on the instance; a client-side pre-check would duplicate it.
- Any rework of the container rendering path or of the permission APIs themselves.

## Regression Risk *(mandatory)*

- **Blast radius**: Wide relative to the diff. The SDK's contentlet-dataset reader is on the path of
  every contentlet interaction in the editor — hover outlines, selection, toolbar, drag. The toolbar
  is the single shared component for all contentlets. The quick-edit form is the default right-hand
  panel. The inline-edit click handler runs on **every click inside the iframe**, and the actions
  handler is the single choke point for all client→editor messages. A mistake in any of these
  affects every page and every user, not only restricted ones.
- **Backward compatibility**: The permission flag must **fail open** when the attribute is absent.
  Headless and SDK-rendered pages do not emit it, and treating "missing" as "denied" would disable
  every gate on those pages — a far worse regression than the defect being fixed. Adding an optional
  field to the editor's contentlet payload model is additive and breaks no existing consumer. No
  REST contract, serialized state, database schema, or search-index mapping is touched, so the
  change is **not rollback-unsafe**. Per ADR-0019 the SDK ships in date-lockstep with the CMS, so
  mismatched client/server pairs are normal and must degrade to today's behavior in both directions.
- **Data considerations**: None. No stored data is read, written, migrated, or repaired.

## Acceptance & Verification *(mandatory)*

### The gate — denied user

- **AC-001**: The edit pencil renders disabled and visually greyed out.
- **AC-002**: Clicking the disabled pencil — icon row or collapsed overflow menu — opens neither the
  content editor nor the "Edit All Pages / Copy and Edit" decision dialog.
- **AC-003**: The disabled pencil exposes a tooltip stating the user lacks edit permission, rather
  than the generic "Edit" label. The same message key (`uve.contentlet.no.edit.permission`) is
  reused by the Quick Edit tooltip, the read-only panel notice and the inline-edit toast, so all
  four read identically.
- **AC-008**: The Quick Edit (⚡) affordance is disabled with that tooltip in both the icon-row
  toolbar and the collapsed overflow menu, and activating it does not open the quick-edit form.
- **AC-008b**: With the quick-edit panel already open on an editable contentlet, selecting a
  restricted contentlet renders the form read-only with a permission notice — fields cannot be
  changed and no save is offered.
- **AC-014**: The style editor is gated on the same permission. Its toolbar button is disabled with
  the permission tooltip, and the side panel's style tab renders the permission notice instead of
  the style form — so neither panel tab offers a way to modify a restricted contentlet.
- **AC-009**: Clicking an inline-editable field inside a restricted contentlet does not start an
  inline editing session, and surfaces a toast explaining the missing permission. Silent refusal is
  not acceptable — it reads as a broken editor.
- **AC-009b**: AC-009 holds for **both** inline paths: a plain/WYSIWYG field click (`[data-mode]`)
  and a Block Editor field click (`[data-block-editor-content]` → `INIT_INLINE_EDITING` with type
  `BLOCK_EDITOR`).

### No regression

- **AC-004**: A user who holds edit permission sees the pencil and Quick Edit enabled, opens the
  content editor exactly as before — including the multi-page "Edit All Pages / Copy and Edit"
  decision — and can still edit fields inline, in both inline paths.
- **AC-005**: A contentlet whose markup does not carry the permission attribute keeps every
  affordance working. Absent, empty and malformed attribute values all resolve to allowed. This
  still matters after the amended scope: a customer app on an older SDK, or any page the wrappers
  do not stamp, must degrade to today's behavior rather than lock the editor.
- **AC-006**: An empty container still shows its add-content affordances and is unaffected.
- **AC-010**: Structural page actions — delete, move/drag and add content — remain fully available
  on a restricted contentlet for a user who can edit the page. These change the page's composition,
  not the contentlet.

### Headless coverage *(amended scope)*

- **AC-011**: A contentlet returned by `GET /api/v1/page/json` carries a `canEdit` boolean
  reflecting the requesting user's EDIT permission on that contentlet instance.
- **AC-012**: The same value reaches a GraphQL page query through the contentlet's `_map`, with no
  change to the SDK's query.
- **AC-013**: A headless page rendered by the SDK stamps `data-dot-can-edit` on its contentlet
  wrappers, and all four gates behave on it as they do on a traditional page.

### Shipping

- **AC-007**: The committed `dotCMS/src/main/webapp/ext/uve/dot-uve.js` bundle is regenerated from
  the SDK source in the same change, so the shipped client matches the source.

### Verification method

- Jest, UVE SDK — the contentlet-dataset reader maps the permission attribute; absent, empty and
  malformed all resolve to allowed (AC-005):
  `cd core-web && pnpm nx test sdk-uve --testPathPatterns=dom`
- Jest/Spectator, toolbar component — pencil and Quick Edit disabled state, shared tooltip, and
  collapsed-menu parity across permitted / denied / attribute-absent inputs (AC-001, AC-003,
  AC-008); structural actions stay enabled (AC-010):
  `cd core-web && pnpm nx test portlets-edit-ema-portlet --testPathPatterns=dot-uve-contentlet-tools`
- Jest/Spectator, quick-edit panel — read-only form plus permission notice when the selection moves
  to a restricted contentlet (AC-008b):
  `cd core-web && pnpm nx test portlets-edit-ema-portlet --testPathPatterns=dot-uve-contentlet-quick-edit`
- Jest/Spectator, editor component — the edit handler refuses a denied contentlet (AC-002), and the
  plain/WYSIWYG inline path refuses and raises the toast (AC-009):
  `cd core-web && pnpm nx test portlets-edit-ema-portlet --testPathPatterns=edit-ema-editor`
- Jest, UVE actions handler — the `INIT_INLINE_EDITING` / `BLOCK_EDITOR` path refuses and raises the
  toast (AC-009b):
  `cd core-web && pnpm nx test portlets-edit-ema-portlet --testPathPatterns=dot-uve-actions-handler`
- Bundle freshness (AC-007): `pnpm nx run sdk-uve:build:js`, then confirm `git diff` on
  `dotCMS/src/main/webapp/ext/uve/dot-uve.js` is empty.
- Manual, per the reproduction paths above, exercising both the permitted and the denied user.
- **No Playwright e2e** — explicit decision, see Clarifications.

## Assumptions

- The server-side permission emitted as `data-dot-can-edit` is the correct authority: a WRITE/EDIT
  check against the contentlet **instance** for the logged-in back-end user. Confirmed during
  planning (`ContentsWebAPI` → `permissionAPI.doesUserHavePermission`, permission value `2`).
- The content editor's save path already rejects a user without edit permission on the contentlet.
  Confirmed during planning: `ESContentletAPIImpl.checkin` calls `checkPermission`, which requires
  `PERMISSION_WRITE` on the instance whenever an identifier is present. This fix therefore corrects
  an affordance rather than closing a write bypass.
- The report that this behavior existed previously is taken as context rather than a verified git
  history claim. What is verified is that the attribute is emitted today and has no consumer.
- The permission is evaluated at page-render time. A permission changed while the editor is open is
  not reflected until the page reloads — acceptable, and consistent with the rest of the editor's
  page data.
- The Quick Edit panel's read-only mode is a presentation state; the underlying form component is
  not expected to gain server-side validation of its own.
