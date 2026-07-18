# dotCMS content-authoring skill — rules & conditions

Draft content for the future `dotcms-content-authoring` skill. This is the *rule list*, not the
skill itself — each rule is a trap seen in a real build session (Awazon / Carlos Swager / Golden
Gate Coverage) plus the correct move. Rules marked **[needs release]** are formalized by a backend
OpenAPI fix (PR 2); until that ships + demo upgrades, trust this doc over the live spec text.

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

### 1.1 🔴 POST the content type BARE — no `contentType` envelope **[needs release]**
`POST /api/v1/contenttype` reads the content-type object directly (or an array of them). Do NOT wrap
it in `{ "contentType": {...} }` — that hides `clazz` and 400s with
`missing type id property 'clazz'`. The `@Schema` currently mis-advertises a wrapper; ignore it.
- ✅ `{ "clazz": "...ImmutableSimpleContentType", "name": "Book", "fields": [...] }`
- ✅ or an array `[ {...}, {...} ]` to create several at once.

### 1.2 🔴 Avoid reserved / colliding content-type variable names
Names like `Hero`, `Contact`, `SectionTitle`, `Category`, `Tag`, `Host`, `Folder`, `File`,
`Template`, `Container` collide with system vars → `Invalid content type variable: Hero`.
- ✅ Prefix with a project token: `AwazonHero`, `AwazonContact`, `AwazonSectionTitle`.
- `Newsletter`, `Book`, etc. are fine — only prefix the collision-prone ones. When in doubt, prefix.

### 1.3 🔴 Add multiple fields with PUT or inline — NOT array-to-POST **[needs release]**
`POST /api/v1/contenttype/{typeId}/fields` saves only ONE field. Passing an array returns 200 but
**silently persists only element [0]** and drops the rest.
- ✅ Include all fields inline as `fields[]` in the content-type POST (best — one call).
- ✅ Or `PUT /api/v1/contenttype/{typeId}/fields` with the array to save many at once.
- ✅ Or loop `POST .../fields` once per field.

### 1.4 🔵 `type=` query filter takes a BASE-TYPE enum, not a variable name
`GET /api/v1/contenttype?type=webPageContent` → 400 `BaseContentType webPageContent does not Exist`.
- ✅ `type` ∈ `ANY, CONTENT, WIDGET, FORM, FILEASSET, HTMLPAGE, PERSONA, VANITY_URL, KEY_VALUE, DOTASSET`.
- ✅ To fetch one type by its variable: `GET /api/v1/contenttype/id/{idOrVar}`.

### 1.5 🔵 `workflow` (singular) in the request, `workflows` (plural) in the response
On the CT POST/PUT body the key is `workflow: ["<schemeId>"]` (array of scheme UUIDs). GET responses
return `workflows` (plural, array of objects). Round-tripping a fetched CT? Rename the key.

### 1.6 🔵 Boolean fields have no dedicated class
Use `ImmutableRadioField` + `dataType: BOOL` + `values: "True|true\r\nFalse|false"`. Layout rows/cols
are field entries too: `ImmutableRowField` / `ImmutableColumnField` mark the grid.

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
`fields[].clazz === "...ImmutableStoryBlockField"`.

### 2.6 🔵 Read a contentlet by identifier at the right path
`GET /api/v1/content/{identifier}` (or `/api/content/_search`). NOT `/api/v1/content/id/{id}`.

---

## 3. Templates

### 3.1 🔴 `drawed: true` requires `body` (and a real theme folder + `drawedBody`) **[needs release]**
A drawn template with no `body` → 400 `body required when drawed`. A `theme` that isn't a theme-
folder id → `theme must be a folder identifier`. And if `themeName` isn't resolved when the body is
generated, dotCMS bakes `/application/themes/null/` into it and leaves `drawedBody` empty (so it's
not really a drawn template).
- ✅ Set `drawed: true`, a non-empty `body`, the theme **folder** id, `themeName`, and a real
  `drawedBody` layout JSON. Verify the persisted `body` doesn't contain `/themes/null/` and that
  `drawedBody` is populated; if not, PUT-update the template with the correct body + drawedBody, then
  publish.

### 3.2 🟡 `_publish` / `_unpublish` take a JSON ARRAY of identifier strings
`PUT /api/v1/templates/_publish` body is `["<id>"]`, not `{ "identifier": "<id>" }` (which 400s
`Cannot deserialize … ArrayList from Object value`). Already correct in the spec — just send an array.

### 3.3 🟡 Fetch a template's working layout via `/working` (now in the spec)
`GET /api/v1/templates/{templateId}/working`. This endpoint is now included in the filtered spec, so
`spec.paths['/api/v1/templates/{templateId}/working']?.get` resolves — but still guard with `?.`.

### 3.4 🔵 Layout containers must reference the host-qualified container PATH, not a DB inode
If a template layout stores the system default container (`//default/application/containers/`),
page-content placement 404s `Container … not found`. Set
`layout.containers[0].identifier = "//<site>/application/containers/<name>/"`, republish the template,
re-read page-json, then place content.

---

## 4. Pages, folders & rendering

### 4.1 🔴 Render a NON-default site with `?host_id=<site UUID>` **[needs release]**
`/api/v1/page/render/{uri}`, `/json/{uri}`, `/renderHTML/{uri}` resolve the DEFAULT site unless you
pass `host_id`. A non-default-site page 404s `Page 'index' not found` without it.
- ✅ `GET /api/v1/page/renderHTML/index?mode=LIVE&host_id=<site-uuid>` (host_id is for backend users).
- ❌ The `//hostname/uri` path form does NOT work here (`//` is rejected); the `Host` header does not
  set the render site context either — use `host_id`.
- `uri` is a plain page path (`index`, `about/team`), no leading host segment.

### 4.2 🔵 Folder `uri` param is a RAW path — don't URL-encode it **[needs release]**
`GET /api/v1/folder/sitename/{siteName}/uri/{uri}` — pass `application/themes/golden-gate/` raw.
Percent-encoding it (`%2Fapplication%2F…`) 404s. Leading slash optional (added if missing);
embedded slashes are fine.

### 4.3 🔵 `hostFolder` = site id for a root page, folder id for a sub-folder page
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
Rules tagged **[needs release]** (1.1, 1.3, 4.1, 4.2, 3.1) are being fixed at the OpenAPI-annotation
level in a separate backend PR. Until that ships and the demo instance (the default spec source) is
upgraded, the live `search` spec text may still be wrong for those endpoints — this skill is the
source of truth for them in the meantime.
