# Contract: GraphQL schema for asset-pointing fields

**Feature**: `specs/34540-graphql-asset-subtype-fields/` · **Issue**: dotCMS/core#34540

The GraphQL schema **is** the contract for this feature. Names below are working names; the tasks
phase settles them. SDL is illustrative of shape, not of the generated output byte-for-byte.

---

## 1. What exists today (must not change)

```graphql
type DotFileasset {
  fileName:    String
  description: String
  fileAsset:   DotBinary
  metaData:    [DotKeyValue]
  showOnMenu:  [String]
  sortOrder:   Int
}
```

Every `ImageField` and `FileField` on every content type resolves to this type.

**Frozen behaviors** — these are contract, not accidents, and correcting them is forbidden:

| Selection | Returns, for image-style content | Returns, for file-style content |
|---|---|---|
| `fileName` | the contentlet's name | the stored file name |
| `description` | the contentlet's **title** (i.e. the file name) | the stored description |

Verified live: `image { fileName description }` returned the same string for both. Against the
asset's own type, the *stored* description is populated for 2 of 57 images. Correcting
`description` here would change what a live query returns without failing it.

---

## 2. What is added

```graphql
type DotFileasset {
  fileName:    String        @deprecated(reason: "Use `content { title }`, or `content { ... on FileAsset { fileName } }` for file-style assets.")
  description: String        @deprecated(reason: "Returns the asset title, not its description. Use `content { ... on Images { description } }` for the stored value.")
  fileAsset:   DotBinary     @deprecated(reason: "Use `content { ... on DotAssetBaseType { asset } }`, or `... on FileBaseType { fileAsset }`.")
  metaData:    [DotKeyValue] @deprecated(reason: "Use `content { ... on FileBaseType { metaData } }`.")
  showOnMenu:  [String]      @deprecated(reason: "Use `content { ... on FileAsset { showOnMenu } }`.")
  sortOrder:   Int           @deprecated(reason: "Use `content { ... on FileAsset { sortOrder } }`.")

  "The referenced asset, described by its real content type."
  content:     DotAssetContent
}

interface DotAssetContent {
  identifier: ID
  inode: String
  title: String
  host: Site
  folder: String
  live: Boolean
  working: Boolean
  archived: Boolean
  locked: Boolean
  urlMap: String
  modDate: String
  modUser: String
  owner: String
  publishDate: String
  publishUser: String
  creationDate: String
  conLanguage: Language
  contentType: String
  baseType: String
  titleImage: DotBinary
  dotStyleProperties: JSON
  _map: JSON
}
```

Every content type whose base type is DOTASSET or FILEASSET additionally declares
`implements DotAssetContent`:

```graphql
type Images       implements DotAssetContent & DotAssetBaseType & DotContentlet { ... }
type FileAsset    implements DotAssetContent & FileBaseType     & DotContentlet { ... }
type BannerImages implements DotAssetContent & DotAssetBaseType & DotContentlet { campaignName: String  adSize: String  ... }
```

**The possible-type set is derived, never listed.** It is whatever content types exist when the
schema is built, and the schema is already rebuilt when a content type or field changes. A type a
customer creates after deployment is reachable with no administrative step (FR-001a, FR-005).

---

## 3. Required client-visible behavior

### 3.1 Shared properties at both levels (FR-015)

```graphql
image {
  content {
    identifier                      # directly on the interface
    ... on Images { identifier      # and again inside a clause
                    tags }
  }
}
```

Both are valid. A client is never forced to choose one level.

### 3.2 Narrowing (FR-016, FR-016a, FR-017, FR-018)

| Client writes | Result | Status |
|---|---|---|
| a clause on a type the asset **is** | its properties are returned | 200, data |
| a clause on a possible type the asset **is not** | contributes nothing, rest of the response delivered, **warning** names the clause | 200, data + `extensions` |
| a clause on a type that **does not exist** | request fails | validation error, no data |
| clauses on the base kind **and** the concrete type | properties **merge** into one object | 200, data |

Across a result set of mixed types, every matching asset is populated, every non-matching asset is
still returned, and one non-match never suppresses the matches (FR-016a).

There is no cast to fail here: a clause is a condition, not a coercion.

### 3.3 Warning shape

```json
{
  "data": { "...": "delivered normally" },
  "extensions": {
    "warnings": [
      { "path": "BannerCollection.image.content",
        "typeCondition": "PDFDocuments",
        "message": "No asset at this path was of type PDFDocuments." }
    ]
  }
}
```

Warnings name only the type the client itself wrote and the path it chose — **never asset content**
(Constitution III). A client that ignores `extensions` is unaffected.

---

## 4. Worked example

**Before** — works today, and must keep working unchanged:

```graphql
{ BannerCollection { title image { fileName description fileAsset { versionPath size mime } } } }
```

**After** — the same query still valid, plus what was unreachable:

```graphql
{
  BannerCollection {
    title
    image {
      content {
        identifier
        title
        __typename
        ... on DotAssetBaseType { asset { versionPath size mime } }
        ... on Images           { tags description }
        ... on BannerImages     { campaignName adSize }
      }
    }
  }
}
```

`... on Images { description }` returns the asset's **stored** description — a different value from
the top-level `image { description }`, which keeps returning the title. Two names, two meanings,
neither surprising the other. That separation is the point of FR-009a.

---

## 5. Compatibility guarantees

| Guarantee | Requirement |
|---|---|
| Every selection valid today is still valid | FR-012, SC-008 |
| Every such selection returns the same value | FR-012, SC-008 |
| Superseded properties are marked in the schema itself, with replacements named | FR-012a, SC-009 |
| Removal happens in no release introduced by this feature | FR-012b |
| Rolling back removes only the added field; a client on the old selection set is unaffected | Legacy Impact |
