# REST Contracts: #37584

Two existing endpoints. No new endpoint, no new parameter, no renamed or removed key, no
`@Schema` change. What changes is one **value** and one **status code**.

---

## Contract 1 — `GET /api/v1/content/{inodeOrIdentifier}`

Serves the new Edit Content editor (`dot-edit-content.service.ts:77`). Implemented at
`ContentResource.java:376-426`, hydrated with `contentResourceOptions(false)`.

### Request — unchanged

| Parameter | In | Required | Notes |
|---|---|---|---|
| `inodeOrIdentifier` | path | yes | Contentlet inode or identifier |
| `language` | query | no | Language id |
| `depth` | query | no | Relationship hydration depth; the editor sends `2` |

### Response — key set unchanged, one value corrected

Envelope: the standard `{ entity, errors, i18nMessagesMap, messages, pagination, permissions }`.

For a Contentlet of a Content Type that **declares its own `hostName` field** (today: `Host`):

| Key | Before | After |
|---|---|---|
| `hostName` | `"System Host"` — the parent Site's name, for every Site | The Contentlet's own stored `hostName` value |
| `title` | the Site's name | unchanged |
| `host` | `"SYSTEM_HOST"` | unchanged |
| `hostname` | present only when the stored map carries it | unchanged — **not** synthesised, added or removed |
| every other key | — | unchanged |

For every other Content Type: **no change**. `hostName` remains the name of the Site the
Contentlet lives on.

### Worked example — the demo Site, `48190c8c-42c4-46af-8d1a-0cd5db894797`

Before (reproduced live):

```json
{
  "hostName":     "System Host",
  "hostname":     "demo.dotcms.com",
  "title":        "demo.dotcms.com",
  "host":         "SYSTEM_HOST",
  "folder":       "SYSTEM_FOLDER",
  "isDefault":    true,
  "isSystemHost": false
}
```

After:

```json
{
  "hostName":     "demo.dotcms.com",
  "hostname":     "demo.dotcms.com",
  "title":        "demo.dotcms.com",
  "host":         "SYSTEM_HOST",
  "folder":       "SYSTEM_FOLDER",
  "isDefault":    true,
  "isSystemHost": false
}
```

### Invariants to assert

- `entity.hostName == entity.title` for any Site.
- `entity.hostName == GET /api/v1/site/{id} → entity.hostname`.
- When `entity.hostname` is present, it equals `entity.hostName`.
- For the System Host itself, `entity.hostName == "System Host"` — correct by its own name,
  not by accident of the parent reference.
- For a Blog Contentlet, `entity.hostName` is still the name of the Site it lives on.

### Consumers to keep working

The same hydration (`contentResourceOptions(false)`) backs `ContentHelper.java:191`,
`ContentResource.java:686`/`:744` (lock/unlock), `WorkflowResource.java:3122` (fire response)
and `ContentletUtil.java:164` (the printable map behind CSV export). All see the corrected
value; none see a shape change.

---

## Contract 2 — `GET /api/v1/workflow/tasks/history/comments/{contentletIdentifier}`

Implemented at `WorkflowResource.java:6238-6296`. Called by the editor's sidebar on load.

### Request — unchanged

| Parameter | In | Required | Notes |
|---|---|---|---|
| `contentletIdentifier` | path | yes | Contentlet identifier |
| `language` | query | no | Defaults to `-1` (resolve from request) |

### Response — the 200 shape already declared, now actually returned

Declared today as `ResponseEntityWorkflowHistoryCommentsView`, a list of
`WorkflowTimelineItemView`.

| Case | Before | After |
|---|---|---|
| Contentlet **has** a workflow task | 200, populated timeline | **unchanged** |
| Contentlet has **no** workflow task (always true for a Host) | **500** — `Cannot invoke "…WorkflowTask.getId()" because "task" is null` | **200**, `entity: []` |
| Contentlet does not exist | `DoesNotExistException` | unchanged |

Empty-timeline response:

```json
{
  "entity": [],
  "errors": [],
  "i18nMessagesMap": {},
  "messages": [],
  "pagination": null,
  "permissions": []
}
```

### Invariants to assert

- A Host returns 200 with `entity == []`.
- A Contentlet created and never run through a workflow returns 200 with `entity == []`.
- A Contentlet that *has* a task returns its full timeline, byte-for-byte as before.
- No response body carries a Java exception message.

---

## OpenAPI

Neither contract changes a declared return type, so
`dotCMS/src/main/webapp/WEB-INF/openapi/openapi.yaml` is expected to be unchanged. It is
generated at compile — `./mvnw compile -pl :dotcms-core --am -DskipTests` — and must be
committed alongside the Java change if it does move.

## Backward compatibility

Both changes are additive-or-corrective and rollback-safe:

- No key is added, renamed or removed; no parameter changes; no status code is *removed* from
  the success path.
- Contract 1 corrects a value that was a constant carrying no information. A client that read
  `hostName` off a Host expecting `"System Host"` would see a change — but `Host.getHostname()`
  reads the same key, so that constant was the corruption, not the contract.
- Contract 2 turns a 500 into the 200 the endpoint already advertised. A client that handles
  the documented success shape cannot break; one that special-cased the 500 sees the error
  stop occurring.
