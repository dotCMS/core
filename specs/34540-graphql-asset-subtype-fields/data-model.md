# Phase 1 Data Model: GraphQL Asset Subtype Access

**Feature**: `specs/34540-graphql-asset-subtype-fields/` · **Issue**: dotCMS/core#34540

No persistent data model changes: no database table, no column, no index mapping, no serialized
state. The entities below are **GraphQL schema elements** — the shape the delivery API presents.
Working names; the tasks phase settles the final ones.

---

## Entities

### `DotFileasset` (existing — extended, never altered)

The type every `ImageField` and `FileField` resolves to today. Built in the static block of
`CustomFieldType`.

| Property | Type | Change | Notes |
|---|---|---|---|
| `fileName` | String | **deprecated**, behavior frozen | Synthesized: falls back to `contentlet.getName()` when the base type is not FILEASSET. Must keep doing so (R4). |
| `description` | String | **deprecated**, behavior frozen | Synthesized: falls back to `contentlet.getTitle()` when the base type is DOTASSET. Returns the file name for image-style content. Must keep doing so (R4). |
| `fileAsset` | `DotBinary` | **deprecated**, behavior frozen | |
| `metaData` | `[DotKeyValue]` | **deprecated**, behavior frozen | |
| `showOnMenu` | `[String]` | **deprecated**, behavior frozen | |
| `sortOrder` | Int | **deprecated**, behavior frozen | |
| **`content`** | **`DotAssetContent`** | **NEW** | The referenced asset, described by its real content type. |

**Invariants**

- Every existing property keeps its name, its type, and the exact value it returns today
  (FR-012, SC-008). "Frozen" includes the two synthesized values — correcting them is forbidden.
- Each deprecated property carries a reason naming its replacement path (FR-012a, SC-009).
- `@deprecated` marks **fields**; GraphQL cannot deprecate an object type, so the type itself
  carries no marking.
- The customer's own `image` / `file1` field is **not** deprecated — it stays the way in.

---

### `DotAssetContent` (new — interface)

The referenced asset, described by what it actually is.

**Possible types**: every content type whose base type is DOTASSET or FILEASSET — the system ones
and every customer-defined one, including types created after the schema was last built. Derived
from the content types present at schema-build time; **never enumerated** (FR-001a). Schema rebuild
is already triggered by `ContentTypeAndFieldsModsListeners` on content type and field changes
(FR-005).

**Fields**: the common content fields — `identifier`, `inode`, `title`, `host`, `folder`, `live`,
`working`, `archived`, `locked`, `urlMap`, `modDate`, `modUser`, `owner`, `publishDate`,
`publishUser`, `creationDate`, `conLanguage`, `contentType`, `baseType`, `titleImage`,
`dotStyleProperties`, `_map`.

Nothing base-type-specific: the DOTASSET binary is `asset` and the FILEASSET binary is `fileAsset`,
different names, so the binary is reached through a narrowing clause — either on the concrete type
or on the existing base-kind interface.

**Invariants**

- Spans **both** asset base types, so an Image field that points at file-style content, and a File
  field that points at image-style content, both narrow correctly (FR-001b — verified: an Image
  field resolved a `.vtl` FileAsset).
- Its fields are selectable directly **and** inside a narrowing clause (FR-015).
- Resolves to the concrete content type, so `__typename` distinguishes two asset types (FR-003).
- Interface, not union — a union has no fields of its own and would fail FR-015.

---

### `DotAssetBaseType`, `FileBaseType` (existing — unchanged)

The two base-kind interfaces. Already in the schema, already attached by
`ContentAPIGraphQLTypesProvider.createType()`. Untouched by this feature; they remain available as
narrowing targets inside `DotAssetContent` and their clauses merge with concrete-type clauses
(FR-018 — verified live).

---

### Per-content-type object types (existing — one interface added)

Every generated content type object already declares `DotContentlet` plus its base-kind interface.
Those whose base type is DOTASSET or FILEASSET additionally declare `DotAssetContent`. No field
changes; declaring the interface is what places them in its possible-type set.

---

### Response warning (new — not a schema type)

Non-fatal information about narrowing clauses that matched nothing, carried in the response's
`extensions` object. Not part of the type system, so it changes no selection set and cannot break a
client that ignores it.

| Attribute | Content |
|---|---|
| Which clause | The type name the client wrote, and the field path it appeared under |
| Why | It matched none of the assets returned at that path |

**Invariants**

- Carries only type names the client wrote in its own query and paths it chose. **Never asset
  content** — a warning must not become a channel for a value the caller could not otherwise read
  (Constitution III).
- Emitted per unmatched clause, not as one opaque flag (FR-016a).
- Absent when every clause matched, and costs nothing when a query has no clauses.
- A warning never changes the status of the request: the data is still delivered (FR-016).

---

## State transitions

Only one, and it spans releases rather than runtime — the ADR-0022 lifecycle of the superseded
surface:

```
present (today)
  → marked superseded in the schema, still fully functional   ← this feature (FR-012a)
  → clients adopt `content`, old surface still functional     ← after this feature
  → bake: supported-version floor passed AND zero use observed ← gated, not scheduled here
  → removed                                                    ← separate work (FR-012b)
```

This feature delivers the first arrow only. The retirement tracking item is opened when it ships,
naming the surface to be retired — not deferred to a later cleanup pass.
