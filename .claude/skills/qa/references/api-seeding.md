# Seeding test data via API

Always seed through the API — driving forms is slow and flaky. On local dev use
**Basic auth** (`curl -u admin@dotcms.com:admin`) — do NOT mint API tokens: a 1-day token
triggers the "API Tokens about to expire" warning that then photobombs every recording
(see environment.md § API auth). If an endpoint truly requires a JWT, mint one and revoke
it in teardown.

**Naming rule:** prefix every seeded object `qa-<issue#>-<epoch>` and leave it in place
after the run. The video then shows real, traceable names and the data stays inspectable.

## Content type (with fields)

`POST /api/v1/contenttype` — Block Editor field clazz =
`com.dotcms.contenttype.model.field.ImmutableStoryBlockField` (dataType LONG_TEXT).

## Contentlet

`PUT /api/v1/workflow/actions/default/fire/NEW` with `{"contentlet": {...}}`.

- **GOTCHA:** a StoryBlock/Block-Editor field value must be a **JSON string**
  (`JSON.stringify(doc)` / `json.dumps(doc)`), NOT a nested object — otherwise it's stored
  as a Java map `toString()` (`{type=doc, ...}`, no quotes) and the editor can't parse it.

## Folders

`POST /api/v1/folder/createfolders/{siteName}` with a JSON array of paths, e.g.
`["/qa-36369-1720000000/sub-a", "/qa-36369-1720000000/sub-b"]`.

## Image / file asset (real, sized, servable)

1. `POST /api/v1/temp` (multipart, field `file`) → `tempFileId`.
   - **GOTCHA:** rejects 400 "Invalid Origin or referer" unless you send
     `-H "Origin: https://localhost:8443" -H "Referer: https://localhost:8443/dotAdmin/"`.
2. `PUT /api/v1/workflow/actions/default/fire/PUBLISH` with
   `{"contentlet":{"contentType":"dotAsset","asset":"<tempFileId>","hostFolder":"SYSTEM_HOST"}}`.
3. Served at `/dA/<identifier>`. Reference in block-editor images via `attrs.src = "/dA/<id>"`
   (data: URIs break — the UI appends `?language_id=1` → `ERR_INVALID_URL`).

## Tags

`POST /api/v2/tags` body `[{"name": "...", "siteId": "..."}]`.
`siteId: "SYSTEM_HOST"` = global tag; the v2 list endpoint hides global tags unless `global=true`.

## Edge datasets

Seed the shapes the checks need **before** opening the browser:

- Pagination behavior → bulk-create 30+ rows.
- CSV/formula-injection → seed values starting with `=` `+` `-` `@`.
- Selection behavior → at least 2 rows (multi-select checks need index 0 and 1).

## Endpoint discovery

OpenAPI spec of the running instance: `https://localhost:8443/api/openapi.json`
(fallback `https://demo.dotcms.com/api/openapi.json`). Check it before guessing shapes.
