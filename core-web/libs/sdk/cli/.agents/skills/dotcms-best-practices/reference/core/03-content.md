# 03 · Content (contentlets)

Creating and publishing content items.

**Content is created by firing a workflow ACTION on a contentlet** — there is no create-content
call. The action is the verb (`NEW`, `EDIT`, `PUBLISH`), the contentlet is the payload:

```
PUT /api/v1/workflow/actions/default/fire/PUBLISH?indexPolicy=WAIT_FOR
{ "contentlet": { … } }
```

- **`PUT`, not `POST`** — `POST` may return no identifier even when it half-creates the record.
- **`indexPolicy=WAIT_FOR` whenever a later step depends on this one.** The default `DEFER` may
  lag, and a follow-up read returns stale data. `FORCE` is expensive — debugging only.

Pages are the same pattern (a page is an HTMLPAGE contentlet) but go through the page flow —
[04-pages.md](04-pages.md).

## Content is stored per site — pass `contentHost` on every contentlet

Set `contentHost: <siteId>` via the Site-or-Folder field the type must have ([02-content-types.md](02-content-types.md)). Skip it and the contentlet saves onto the wrong site with `errors: []` and `live: true`, then URL-mapped detail routes 404 even though the content clearly exists.

**Content shared across every site goes on `SYSTEM_HOST`** — pass that as the id. Use it for
genuinely global items; anything site-specific gets the site's own id, or it will surface where
it shouldn't.

Host-scope listing queries too, or another site's content renders as yours:

```velocity
#set($awzHost = $host)
#set($q = "+contentType:Book +live:true +conHost:${awzHost.identifier}")
```

## Field keys are the exact field VARIABLE — casing matters
The `contentlet` in the action body is keyed by each field's `variable`. Wrong case is silently ignored, then the field 400s as "required". Read the real names first: `GET /api/v1/contenttype/id/{idOrVar}` → `entity.fields[].variable`.

## Read a contentlet back by identifier at the right path
`GET /api/v1/content/{identifier}` (or `POST /api/content/_search`). NOT `/api/v1/content/id/{id}`.

## Workflow

### There is NO endpoint to associate a scheme to a content type
`POST /workflow/schemes/{id}/contenttypes/...` etc. all 404/405. The only way in is the
`workflow: ["<schemeId>"]` array on the **content-type create body** (spec:
`ContentTypeRequestView`) — so associate the scheme when you create the type
([02-content-types.md](02-content-types.md)); retrofitting it means recreating the type.

The System Workflow is often reachable without an explicit association, but don't build
on that: associate it in the create body and `PUT /api/v1/workflow/actions/default/fire/PUBLISH`
is guaranteed to have an action to fire.

### Firing a specific action by UUID vs. a system action by name
The `/default/fire/{systemAction}` endpoints take the system-action *name* (`NEW`, `EDIT`, `PUBLISH`, …). The other fire endpoints take a workflow *action UUID* — not the enum name. Get the UUID from `GET /api/v1/workflow/contentlet/{inode}/actions` (or `.../contenttypes/{var}/system/actions` without an inode).

## `assets` is a RESERVED top-level folder
Uploading to `//<host>/assets/...` fails with `reserved folder name: assets`. Put themes, VTL and
containers under `/application` (e.g. `//<host>/application/themes/<name>`), or use `/images`, or
another non-reserved path. `/dA/` URLs are host-portable by identifier.
