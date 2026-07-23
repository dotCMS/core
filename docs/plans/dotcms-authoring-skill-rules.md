# dotCMS content-authoring skill — rules & conditions

Draft content for the future `dotcms-content-authoring` skill. This is the *rule list*, not the
skill itself — each rule is a trap seen in a real build session (Awazon / Carlos Swager / Golden
Gate Coverage) plus the correct move.

> **Spec status (updated):** The backend OpenAPI annotations for these traps landed on
> `fmontes/dotcms-openapi-authoring-fixes`. Rules that the spec now documents fully have been
> **removed from this doc** — the live `search` text is the source of truth for them (once the demo
> instance that sources the spec is upgraded). What remains is what the curated spec *can't* express:
> traps on endpoints not included in the spec, or behavioral details no annotation captures. A few
> rules were also **corrected** where the original guidance turned out to be inaccurate.

Legend: 🔴 blocks the build (needs a real recovery) · 🟡 quick retry / discovery · 🔵 caller hygiene.

---

## 0. Tool-first routing (the top of the skill)

- **Pages** → use the `page_create` tool. It already handles: URL-collapse (creates the parent
  folder first), required-field validation before firing, `hostFolder`, `cachettl` casing, the
  two-step blank-page trap, and (as of this PR) site hostname→UUID resolution incl. the root page.
  Only hand-fire a page via `execute` when `page_create` genuinely can't express what you need.
- **Files** (themes, VTL, CSS, JS, images) → use `upload_assets`. Never inline file bytes into
  `execute`.
- **JSON/API work** the dedicated tools don't cover → `execute`.
- **Endpoint discovery** → `search`. The spec is a CURATED allow-list of supported authoring
  endpoints (not every dotCMS endpoint) — treat it as the set you should use. Guard `spec.paths[x]?.get`.
  If a path is absent, look for a supported one that does the job before reaching off-list; the
  curated set is what these tools are designed and tested around.

---

## 1. Content types

### 1.2 🔴 Avoid reserved / colliding content-type variable names
Names like `Hero`, `Contact`, `SectionTitle`, `Category`, `Tag`, `Host`, `Folder`, `File`,
`Template`, `Container` collide with system vars → `Invalid content type variable: Hero`.
- ✅ Prefix with a project token: `AwazonHero`, `AwazonContact`, `AwazonSectionTitle`.
- `Newsletter`, `Book`, etc. are fine — only prefix the collision-prone ones. When in doubt, prefix.

### 1.3 🔴 Add fields inline on the type, or via the v3 field API — NOT the v1 `/fields` endpoints
The v1 `POST/PUT /api/v1/contenttype/{typeId}/fields` endpoints are **deprecated and removed from
the curated spec** — you won't see them, and you shouldn't use them. (Historically `POST .../fields`
was a trap anyway: it saved only ONE field, returning 200 while silently dropping the rest of an
array.) The current field API is v3.
- ✅ Include all fields inline as `fields[]` in the content-type POST (best — one call, and what
  the fire/content-type docs steer you toward). This is the way to create a type *with* its fields.
- ✅ To add or move a field on an **existing** type: `PUT /api/v3/contenttype/{typeIdOrVarName}/fields/move`
  (`MoveFieldsForm` — the layout array; a field not already present is created where you place it).
  This is what the dotCMS admin UI itself calls.
- ✅ To update one field: `PUT /api/v3/contenttype/{typeIdOrVarName}/fields/{id}`.
- ✅ To delete fields: `DELETE /api/v3/contenttype/{typeIdOrVarName}/fields` (body: `{ "fieldsID": [...] }`).
- ✅ Field *variables* still live under v1: `.../fields/id/{fieldId}/variables` (and the `var/{fieldVar}` form).

### 1.4 🔵 `type=` query filter takes a BASE-TYPE enum, not a variable name
`GET /api/v1/contenttype?type=webPageContent` → 400 `BaseContentType webPageContent does not Exist`.
- ✅ `type` ∈ `ANY, CONTENT, WIDGET, FORM, FILEASSET, HTMLPAGE, PERSONA, VANITY_URL, KEY_VALUE, DOTASSET`.
- ✅ To fetch one type by its variable: `GET /api/v1/contenttype/id/{idOrVar}`.

### 1.5 🔵 `workflow` (singular) in the request, `workflows` (plural) in the response
On the CT POST/PUT body the key is `workflow: ["<schemeId>"]` (array of scheme UUIDs). GET responses
return `workflows` (plural, array of objects). Round-tripping a fetched CT? Rename the key.

### 1.6 🔵 Field `clazz` accepts short type names — no FQCN needed
When you send a field (inline `fields[]`, v3 `/fields/move`, v3 `/fields/{id}`), `clazz` accepts a
case-insensitive short field-type name — `TEXT`, `TEXT_AREA`, `STORY_BLOCK_FIELD`, `WYSIWYG`,
`CHECKBOX`, `RADIO`, `SELECT`, `MULTI_SELECT`, `DATE`, `DATE_TIME`, `BINARY`, `IMAGE`, `FILE`, `TAG`,
`CATEGORY`, `KEY_VALUE`, `JSON_FIELD`, `CONSTANT`, `HIDDEN`, `CUSTOM_FIELD`, `RELATIONSHIP`,
`ROW_FIELD`, `COLUMN_FIELD`, … The FQCN (`com.dotcms.contenttype.model.field.ImmutableTextField`) and
bare simple name (`TextField`) still work. **GET responses still return the FQCN** — so read
`fields[].clazz` as a full class name, but you don't have to write it that way.

### 1.7 🔵 Boolean fields have no dedicated class
Use `clazz: "RADIO"` + `dataType: BOOL` + `values: "True|true\r\nFalse|false"`. Layout rows/cols
are field entries too: `clazz: "ROW_FIELD"` / `clazz: "COLUMN_FIELD"` mark the grid.

---

## 2. Contentlets (fire body)

### 2.1 🔴 Field keys are the exact field VARIABLE — casing matters
The `contentlet` in a fire body is keyed by each field's `variable`. Wrong case is silently ignored,
then the field 400s as "required".
- Read the real names first: `GET /api/v1/contenttype/id/{idOrVar}` → `entity.fields[].variable`.

### 2.2 🔴 Page system fields: `contentHost` (a SITE UUID), `cachettl` (lowercase)
- `contentHost` — the site, NOT `host`. Must be a site **identifier UUID**, not a hostname.
- `cachettl` — all lowercase, NOT `cacheTTL` / `cacheTtl`.
- `hostFolder` — the folder id (omit for a root page).
- Missing/mis-cased → `The field Site is required` / `The field Cache TTL (seconds) is required`.
- ✅ Prefer `page_create`, which sets all of these correctly.

### 2.3 🔴 Root page (`/`) needs a resolved site UUID as `contentHost`
Firing a root page with `contentHost = "<hostname>"` and no `hostFolder` → 500
`Host.getIdentifier() because host is null`.
- ✅ Use `page_create` (now resolves hostname→UUID for the root page).
- ✅ If hand-firing: resolve the site first (`sites.find(s => s.hostname === x).identifier`) and pass
  that UUID as `contentHost`.

### 2.4 🟡 Use `indexPolicy=WAIT_FOR` when chaining or reading right after a fire
On every `/fire`/`/firemultipart` call that another step depends on. `DEFER` (default) may lag; a
follow-up read can return stale data. `FORCE` is expensive — debugging only.

### 2.5 🔵 Story Block (Block Editor) fields take an HTML/Markdown STRING
Send `body: "<h2>Intro</h2><p>…</p>"` — do NOT hand-author ProseMirror JSON. Identify via
`fields[].clazz === "...ImmutableStoryBlockField"` (GET responses return the FQCN; to *create* one,
`clazz: "STORY_BLOCK_FIELD"` — see 1.6).

### 2.6 🔵 Read a contentlet by identifier at the right path
`GET /api/v1/content/{identifier}` (or `/api/content/_search`). NOT `/api/v1/content/id/{id}`.

---

## 3. Templates

### 3.1 🔴 Drawn-template `body` sanity: watch for `/themes/null/` and empty `drawedBody`
(The hard rules — `body` required when `drawed`, `theme` must be a folder id — are now in the spec.
This is the behavioral residue no annotation captures.) If `themeName` isn't resolved when the body
is generated, dotCMS bakes `/application/themes/null/` into the persisted `body` and leaves
`drawedBody` empty — so it saves "successfully" but isn't really a drawn template.
- ✅ After creating a drawn template, verify the persisted `body` doesn't contain `/themes/null/` and
  that `drawedBody` is populated; if not, PUT-update with the correct `body` + `drawedBody`, then
  publish.

### 3.2 🟡 `_publish` / `_unpublish` take a JSON ARRAY of identifier strings
`PUT /api/v1/templates/_publish` body is `["<id>"]`, not `{ "identifier": "<id>" }` (which 400s
`Cannot deserialize … ArrayList from Object value`). Already correct in the spec — just send an array.

### 3.3 🟡 Fetch a template's working layout via `/working` (now in the spec)
`GET /api/v1/templates/{templateId}/working`. This endpoint is now included in the filtered spec, so
`spec.paths['/api/v1/templates/{templateId}/working']?.get` resolves — but still guard with `?.`.

### 3.4 🔵 Layout container `identifier` must RESOLVE — path or DB id both valid (corrected)
Correction to the earlier "path, not inode" framing: `layout.containers[].identifier`
(`ContainerUUID.identifier`) legitimately accepts **either** a database identifier (a full UUID or a
dotCMS shorty id) for a DB container, **or** a host-qualified file path
(`//<site>/application/containers/<name>/`) for a Container-as-File — the spec now documents all
accepted forms. The real trap is a value that **doesn't resolve on the target site**: it produces no
container at render time (the slot renders empty / placement can't find the slot), with no hard
error. It does NOT silently fall back to the system container.
- ✅ For a file container, use the full host-qualified path for the site you're building on:
  `"//<site>/application/containers/<name>/"` (a relative path resolves against the *current* site,
  which may not be yours).
- ✅ For a DB container, pass its exact identifier.
- After setting the layout, republish the template, re-read page-json, then place content.

---

## 4. Pages, folders & rendering

### 4.1 🔵 `hostFolder` = site id for a root page, folder id for a sub-folder page
When hand-firing (vs `page_create`): root page anchors on the site; `/books`, `/contact` anchor on
their folder id. `page_create` derives this for you.

---

## 5. Assets & uploads

### 5.1 🔴 `assets` is a RESERVED top-level folder
Uploading to `//<host>/assets/...` → `reserved folder name: assets`.
- ✅ Put themes/VTL/containers under `/application` (e.g. `//<host>/application/themes/<name>`), or use
  `/images`, or another non-reserved path.

### 5.2 🔵 `upload_assets` booleans accept string forms now
`verify` / `publish` accept `true/false` (bool) and `"true"/"false"/"1"/"0"` (string) — the tool
coerces them. Prefer real booleans; omit `verify` to accept its default.

---

## 6. Workflow

### 6.1 🔵 There is NO endpoint to associate a scheme to a content type
`POST /workflow/schemes/{id}/contenttypes/...` etc. all 404/405 — no such public endpoint.
- ✅ Associate via the `workflow: ["<schemeId>"]` array on the content-type POST/PUT.
- The **System Workflow applies globally** — you can usually just fire `PUT
  /api/v1/workflow/actions/default/fire/PUBLISH` on any content without associating anything.

### 6.2 🔵 Fire endpoints that take `{actionId}` need a workflow action UUID
Not the system-action enum (NEW/EDIT/PUBLISH). Get the UUID from
`GET /api/v1/workflow/contentlet/{inode}/actions` (or `.../contenttypes/{var}/system/actions`
without an inode). The enum values are only valid on the `/default/fire/{systemAction}` endpoints.

---

## 7. `execute` sandbox hygiene (JS, not VTL)

### 7.1 🔵 It's JavaScript — Velocity vars don't exist
`$dotcontent`, `$dotcontent.pull(...)`, `$date`, `#foreach` → `ReferenceError`. Query content via
`POST /api/content/_search`. Run VTL only through `POST /api/vtl/dynamic`.

### 7.2 🔵 `await` everything; return only JSON-serializable values
An un-awaited Promise (or a function/class) in the return → `DataCloneError`. Return plain
objects/arrays/strings.

### 7.3 🔵 Watch string literals
A raw apostrophe in a single-quoted JS string (`'grandchild's'`) → `SyntaxError`. Use double quotes
or escape. Don't use non-ASCII/CJK variable names by accident.

### 7.4 🔵 `.rendered` on a container is an object keyed by `uuid-N`, not a flat string
Index into `rendered["uuid-1"]` before string ops like `.slice`.

---

## Cross-reference note for the skill
The OpenAPI-annotation fixes for these traps landed on `fmontes/dotcms-openapi-authoring-fixes`.
Rules the spec now documents fully were **removed** from this doc (bare content-type POST,
`host_id` render, raw folder `uri`); the live `search` spec is the source of truth once the demo
instance that sources the spec is upgraded. What remains is deliberately spec-*un*expressible:
- **1.3** — steering to inline `fields[]` / the v3 field API. The deprecated v1 `/fields` CRUD is
  now excluded from the curated spec and the active v3 field endpoints were added, so the endpoint
  choice is largely enforced by the spec; the "prefer inline on create" guidance stays skill-side.
- **3.1** — the `/themes/null/` + empty-`drawedBody` behavioral check (the hard 400s are in the spec).
- **3.4** — corrected: layout container `identifier` accepts a DB id *or* a host-qualified path; the
  real trap is a value that doesn't resolve (empty render), not "path vs inode."

Everything else here is a build-session trap on tooling or behavior that no annotation covers.
