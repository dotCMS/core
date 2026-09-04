# 05 · Templates

The template is the layout that ties containers into a page. **Both delivery modes need
it**: in VTL the theme renders the layout, in headless the SDK reads
`layout.body.rows` and renders it with components.

**Create the containers it references first** ([06](06-containers.md)) — a path that
doesn't resolve yields an empty slot with no error.

`theme` is **optional** (verified): omit it and the server assigns `SYSTEM_THEME`.

- **VTL** — create the theme first ([vtl/02](../vtl/02-themes.md)) and pass its folder id.
- **Headless** — don't create a theme at all; omit the key and take `SYSTEM_THEME`.
  Nothing renders through it, because the SDK never asks dotCMS for HTML.

Same split for containers: both modes create them ([06](06-containers.md)), but only VTL
authors markup in the per-type `<Var>.vtl` ([vtl/03](../vtl/03-containers.md)).

**Don't over-decompose the layout.** A content-driven page usually wants **one row, one column, one flexible container** (a single container rendering many content types — see [06-containers.md](06-containers.md)), with the page placing its sections into that one slot in order. Reach for multiple rows/slots only for a real 2-D layout (side-by-side columns) or a genuinely separate render path (a widget slot, a URL-mapped detail). A stack of N fixed per-type slots (one row each) is the common anti-pattern: it's rigid and leaves blank slots to hide on pages that skip a section.

## Creating a template — one POST, then publish
Send `layout`; never send `drawed` or `body`. The server sets `drawed` for you, and `layout` is what supplies `$dotThemeLayout` (the theme's row/column loop) and the slots the placement tool addresses.

```json
{ "title": "Main", "friendlyName": "Main layout",
  "theme": "<theme folder id>",   // omit for headless → SYSTEM_THEME
  "siteId": "<site id>",
  "layout": { "header": true, "footer": true, "sidebar": null, "title": "", "width": "",
    "body": { "rows": [ { "styleClass": null, "columns": [
      { "containers": [ { "identifier": "//<site>/application/containers/<name>/", "uuid": "1" } ],
        "leftOffset": 1, "width": 12, "styleClass": "" } ] } ] } } }
```

Then publish. `identifier` is ignored on POST. The result has `body: null` and the layout in `drawedBody` — that is what renders. Sending `drawed` instead triggers a legacy generator that persists a broken scaffold.

## `_publish` / `_unpublish` take a JSON ARRAY of identifier strings
`PUT /api/v1/templates/_publish` body is `["<id>"]`, not `{ "identifier": "<id>" }` (which 400s `Cannot deserialize … ArrayList from Object value`). Already correct in the spec — just send an array.

## Fetch a template's working layout via `/working`
`GET /api/v1/templates/{templateId}/working`. Included in the filtered spec, so `spec.paths['/api/v1/templates/{templateId}/working']?.get` resolves — but still guard with `?.`.

## Layout container `identifier` must RESOLVE — path or DB id both valid
`layout.containers[].identifier` (`ContainerUUID.identifier`) legitimately accepts **either** a database identifier (a full UUID or a dotCMS shorty id) for a DB container, **or** a host-qualified file path (`//<site>/application/containers/<name>/`) for a Container-as-File — the spec now documents all accepted forms. The real trap is a value that **doesn't resolve on the target site**: it produces no container at render time (the slot renders empty / placement can't find the slot), with no hard error. It does NOT silently fall back to the system container.
- For a file container, use the full host-qualified path for the site you're building on: `"//<site>/application/containers/<name>/"` (a relative path resolves against the *current* site, which may not be yours), with `uuid:"1"`.
- For a DB container, pass its exact identifier.
- After setting the layout, republish the template, then place content ([09-placement.md](09-placement.md)).
