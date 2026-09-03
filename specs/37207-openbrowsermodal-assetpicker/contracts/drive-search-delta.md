# Contract 2 — `POST /api/v1/drive/search` (additive delta)

**Audience**: internal REST. **Change**: one optional request field.
**Requirements**: FR-032 – FR-035. **Binding ADR**: 0018.

---

## Delta

### Request — `DriveRequestForm`

```diff
+ "extensions": ["jpg", "png"]     // optional; null/empty ⇒ no filtering
```

| Property | Type | Required | Default | Semantics |
|---|---|---|---|---|
| `extensions` | `string[]` | no | `null` | Narrow file assets to these extensions. Matching mirrors the existing `BrowserQuery.extensions` semantics (substring match on the asset's `extension`). |

**No other request or response field changes.** `showLinks`, `linkCursor`, `hasMoreLinks` and
`nextLinkCursor` already exist — shipped in #37112. The frontend models simply had not adopted them
(R3); that is a client-side gap, not an endpoint change.

---

## Backward compatibility (FR-033)

| Scenario | Required behavior |
|---|---|
| `extensions` absent | Byte-identical to today, including the SQL plan |
| `extensions: []` | Treated as absent |
| `extensions` present | Only assets with a matching extension returned |

---

## Implementation constraints

These are the whole point of the contract — see R4.

### C1 — Resolve in SQL, on the cursor path *(ADR-0018, binding)*

`BrowserQuery.extensions` today is applied **only** by `filterReturnList`
(`BrowserAPIImpl.java:1957`), whose sole caller is the legacy offset path (line 1663).
`getPaginatedContents(...)` (line 1692) — the method behind this endpoint — never calls it.

So plumbing the field through `ContentDriveHelper` alone would produce a **silently ignored
parameter**. The predicate must be added to the SQL builder on the cursor path, mirroring
`appendMIMETypeQuery` (line 2602).

ADR-0018 routes asset name / file name predicates to the database and states they "must **never**
be silently re-routed to the index." An extension predicate is an asset-name predicate.

### C2 — Never post-filter a cursor page

Dropping rows after the page slice shrinks pages below `maxResults` and desynchronizes
`hasMoreContent` / `nextContentCursor`. Violates SC-005.

### C3 — Bind parameters *(constitution III)*

`appendMIMETypeQuery` interpolates caller strings via `String.format`. **Do not copy that pattern.**
`extensions` is caller-supplied REST input; the new predicate binds parameters.

### C4 — Regenerate the API description *(constitution IV, FR-034)*

Describe in the Java annotations, then:

```bash
./mvnw compile -pl :dotcms-core -DskipTests
```

Commit the regenerated `openapi.yaml` alongside the Java change. CI verifies the committed file
matches the build output.

---

## Contract test checklist (FR-035, SC-012)

- [ ] `extensions: ["jpg"]` returns only `.jpg` assets
- [ ] `extensions` absent returns exactly what it returns today (regression baseline)
- [ ] `extensions: []` behaves as absent
- [ ] Paging with `extensions` set: pages are full, cursors advance correctly, no duplicates or
      omissions across at least three pages *(proves C1/C2)*
- [ ] A value containing SQL metacharacters is inert *(proves C3)*
- [ ] `openapi.yaml` regenerated and committed *(C4)*

**Test layer**: integration (`dotcms-integration`) and/or Postman (`dotcms-postman`) — chosen at
`/speckit-tasks`. Note ADR-0013: this Java change puts the PR on the full merge-queue validation
path, so these suites will run (R9).
