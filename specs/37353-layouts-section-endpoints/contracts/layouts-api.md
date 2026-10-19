# Contract: /api/v1/layouts

All endpoints: authenticated back-end user holding `tools` or `tools-beta` in a granted
section, or CMS Administrator; otherwise **401**. Writes additionally require CMS Administrator;
otherwise **403**. Unknown `{layoutId}` on a write: **404**. Every response is wrapped in the
standard `ResponseEntityView` envelope (`entity`, `errors`, `messages`, `i18nMessagesMap`,
`permissions`, `pagination`).

## GET /api/v1/layouts

Response `200`: `ResponseEntitySectionListView` — `entity` is an array of sections in
navigation order.

```json
{ "entity": [
  { "id": "2df9f117-…", "name": "Getting Started", "icon": "whatshot", "tabOrder": -320000,
    "portletIds": ["starter"], "portletTitles": ["Welcome"] },
  { "id": "…", "name": "Site", "icon": "language", "tabOrder": 1,
    "portletIds": ["site-browser", "templates"], "portletTitles": ["Browser", "Templates"] }
], "errors": [], "messages": [], "i18nMessagesMap": {}, "permissions": [], "pagination": null }
```

## POST /api/v1/layouts

Body: `{ "name": "Marketing", "icon": "campaign" }`
Response `200`: `ResponseEntitySectionView` with the saved section (`tabOrder` = last + 1,
`portletIds` = `[]`). `400` on blank/over-long name, over-long icon, duplicate name (also when the
duplicate is caught by the database under a race).

## PUT /api/v1/layouts/{layoutId}

Body: `{ "name": "Marketing & Growth", "icon": "trending_up" }`
Response `200`: `ResponseEntitySectionView` with the saved section; `tabOrder` and
`portletIds` unchanged. `400` as for POST. `404` unknown id.

## DELETE /api/v1/layouts/{layoutId}

Response `200`: `ResponseEntitySectionListView` — the remaining sections in navigation order.
`400` when `{layoutId}` is the Getting Started section. `404` unknown id.

## PUT /api/v1/layouts/_reorder

Body: `{ "layoutIds": ["id-a", "id-b", "id-c"] }` — every existing section id exactly once.
Response `200`: `ResponseEntitySectionListView` in the new order, `tabOrder` = 1..n, written atomically with a single menu-refresh event.
`400` when an id is missing, unknown or repeated; nothing written.

## PUT /api/v1/layouts/{layoutId}/portlets

Body: `{ "portletIds": ["site-browser", "templates", "c_Products"] }` — full ordered list;
may be empty except for the Getting Started section (`400`).
Response `200`: `ResponseEntitySectionListView` in navigation order, the target section
carrying the sent list in the sent order.
`400` naming the offending id when it is unregistered, not placeable or repeated; section
unchanged. `404` unknown section id.

## Error body

Standard dotCMS error envelope from the exception mappers, e.g.

```json
{ "errors": [ { "errorCode": "bad-request-exception", "message": "Portlet id 'nope' is not a registered tool", "fieldName": null } ],
  "entity": null, "messages": [], "i18nMessagesMap": {}, "permissions": [], "pagination": null }
```

## Security log

Every successful write and every 403 refusal writes one `SecurityLogger.logInfo` line with
the acting user id, the operation (`create`, `update`, `delete`, `reorder`, `set-tools`) and the
section id where one exists. The 401 gate refusal is not logged (shared `WebResource`
behaviour).
