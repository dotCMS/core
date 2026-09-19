# Phase 1 Data Model: GraphQL Asset Subtype Access

**Feature**: `specs/34540-graphql-asset-subtype-fields/` · **Issue**: dotCMS/core#34540

No persistent data model changes: no database table, no column, no index mapping, no serialized
state. The entities below are **GraphQL schema elements** — the shape the delivery API presents.
Reflects what was implemented.

---

## Entities

### `DotFileasset` — object type **replaced by an interface of the same name**

The name is kept deliberately: clients already write `... on DotFileasset { … }`, and a fragment on
the position's own interface always matches, so those clauses stay valid and keep returning data. A
new name would have invalidated every one of them. The kind changes — object → interface — which
query text does not notice but client code generators do.

| Property | Type | Fate | Notes |
|---|---|---|---|
| `fileName` | String | **carried over** | Synthesized: falls back to the contentlet name when the base type is not FILEASSET. Kept behaving exactly so. |
| `fileAsset` | `DotBinary` | **carried over** | Resolved for both base types; the binary is named `asset` on DOTASSET content and the fetcher already maps it. |
| `metaData` | `[DotKeyValue]` | **carried over** | |
| `showOnMenu` | `[String]` | **carried over** | |
| `sortOrder` | Int | **carried over** | |
| `description` | String | **removed** | The one property whose meaning differs: the flat view answered with the contentlet *title*, while the content answering it stores something else. Carrying the name over would have returned different data without failing. |

Plus every common content field — `identifier`, `inode`, `title`, `host`, `live`, `urlMap`,
`baseType`, `folder`, `modDate`, `_map` and the rest — none of which was reachable before.

**Possible types**: every content type whose base type is DOTASSET or FILEASSET — the system ones
and every customer-defined one, including types created after the schema was last built. Derived
from the content types present at schema-build time; **never enumerated**. Schema rebuild is already
triggered by `ContentTypeAndFieldsModsListeners`.

**Invariants**

- Spans **both** asset base types, so an Image field pointing at file-style content, and a File
  field pointing at image-style content, both narrow correctly. Verified: an Image field resolves a
  `.vtl` FileAsset.
- Resolves to the concrete content type, so `__typename` distinguishes two asset types.
- Interface, not union — a union has no fields of its own, so the flat properties would be
  unreachable without a clause.

---

### `DotAssetBaseType`, `FileBaseType` (existing — extended)

The two base-kind interfaces, unchanged in purpose. Each now **also declares the five flat
properties**, because every one of their possible types carries them. Without that, `fileName` is
selectable on the asset interface and on the concrete types but not through these clauses, and a
client's query changes shape depending on how it narrows.

They remain **independent** interfaces: none implements another, and none implements
`DotFileasset`. They share fields because the same fields are declared on each.

---

### Per-content-type object types (existing — one interface added, five properties synthesized)

Every asset content type additionally declares `DotFileasset`, and carries any of the five flat
properties it does not already define — synthesized with the very same fetchers the flat view used,
which is what makes them answer identically. FILEASSET-derived types usually define most already;
a customer-created one carries only the required fields, so `showOnMenu` and `sortOrder` can be
absent and are filled in.

A property the customer already defined always wins. A duplicate definition fails the **whole**
schema build, taking every other content type down with it.

---

### The flat object type (removed)

No longer registered as a schema type. Nothing references it once asset fields are interface-typed,
so registering it would leave an orphan visible in introspection and reachable by nobody.

---

## State transitions

None. The flat view is replaced in one step rather than deprecated and retired over releases — a
product decision, recorded in the spec's "Decision: the flat view is replaced" section, and the
reason an exception to ADR-0022 is requested there.
