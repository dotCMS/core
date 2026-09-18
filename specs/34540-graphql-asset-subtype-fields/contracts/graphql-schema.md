# Contract: GraphQL schema for asset-pointing fields

**Feature**: `specs/34540-graphql-asset-subtype-fields/` · **Issue**: dotCMS/core#34540

The GraphQL schema **is** the contract for this feature. SDL below is illustrative of shape, not of
the generated output byte-for-byte. Reflects what was implemented, including the two breaking
changes in §5.

---

## 1. What existed before

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

**Two of the six were synthesized, not stored** — contract nonetheless, and the reason the change
takes the shape it does:

| Selection | Returns, for image-style content | Returns, for file-style content |
|---|---|---|
| `fileName` | the contentlet's name | the stored file name |
| `description` | the contentlet's **title** (i.e. the file name) | the stored description |

Verified live: `image { fileName description }` returned the same string for both. Against the
asset's own type, the *stored* description is populated for 2 of 57 images.

`fileName` is carried over unchanged, synthesis and all. `description` is not — see §5.

---

## 2. What changes

An asset-pointing field is no longer typed by the flat object. It is typed by an **interface that
keeps the same name**, so a client narrows to the concrete content type in the same block as the
flat properties:

```graphql
interface DotFileasset {
  # the flat properties, carried over
  fileName:   String
  fileAsset:  DotBinary
  metaData:   [DotKeyValue]
  showOnMenu: [String]
  sortOrder:  Int
  # plus every common content field: identifier, inode, title, host, live, urlMap, baseType, …
}
```

`description` is **not** on it. See §5.

Four independent interfaces exist; none implements another. Each object type declares the ones that
apply:

```graphql
type Images       implements DotContentlet & DotAssetBaseType & DotFileasset
type BannerImages implements DotContentlet & DotAssetBaseType & DotFileasset
type FileAsset    implements DotContentlet & FileBaseType     & DotFileasset
type PDFDocuments implements DotContentlet & FileBaseType     & DotFileasset
```

The five flat properties are declared on `DotFileasset`, on **both** base-type interfaces, and on
every concrete asset type — synthesized where absent, using the very same fetchers the flat view
used. A property present on some surfaces and not others would make a client's query change shape
depending on which clause it narrows through.

**The possible-type set is derived, never listed.** It is whatever content types exist when the
schema is built, and the schema is already rebuilt when a content type or field changes, so a type
a customer creates after deployment is reachable with no administrative step.

**The old flat object type is gone from the schema**, not merely unreferenced: registering it would
leave an orphan visible in introspection and reachable by nobody.

---

## 3. Required client-visible behavior

```graphql
query MixedAssets {
  AssetRefTestCollection(limit: 10) {
    title
    image {
      fileName
      ... on DotFileasset     { fileName }
      ... on DotAssetBaseType { asset     { size mime } }
      ... on FileBaseType     { fileName  fileAsset { size mime } }
      ... on Images           { tags }
      ... on BannerImages     { campaignName adSize }
    }
  }
}
```

`... on DotFileasset` stays valid and always fires: a fragment on the position's own interface
always matches. That is why the interface kept the name — a new one would have invalidated every
such clause a customer has written.

| Client writes | Result |
|---|---|
| a clause on a type the asset **is** | its properties are returned |
| a clause on a possible type the asset **is not** | contributes nothing, rest of the response delivered |
| a clause on a type that **does not exist** | request fails (validation error) |
| clauses on several applicable types | properties **merge** into one object |
| `baseType` | `DOTASSET` or `FILEASSET`, without any clause |

An asset field aimed at content that is not an asset resolves to `null`. Handing such a contentlet
on would raise `UnresolvedTypeException`, which fails the **whole request** rather than that field.

---

## 4. Before and after

**Before** — still works, except `description`:

```graphql
{ BannerCollection { title image { fileName fileAsset { versionPath size mime } } } }
```

**After** — the same, plus what was unreachable:

```graphql
{
  BannerCollection {
    title
    image {
      fileName
      identifier
      __typename
      ... on Images       { tags description }
      ... on BannerImages { campaignName adSize }
    }
  }
}
```

`... on Images { description }` returns the asset's **stored** description — a different value from
what `image { description }` used to return, which was the title. Two meanings, now two places, and
the old one fails rather than lying.

---

## 5. Compatibility guarantees

| Guarantee | Requirement |
|---|---|
| Five of the six flat properties stay selectable and return the same values | FR-012, SC-008 |
| Every removed selection fails **visibly**; none keeps working while returning different data | FR-009a, SC-009 |
| The break ships with announcement and migration guidance | FR-012c |

**What breaks**

| Selection | Why it could not be carried over |
|---|---|
| `image { description }` | The flat view answered with the contentlet *title*; the content answering it stores something else. Measured on a real instance: populated for 57 of 57 images before, 2 of 57 after. Carrying the name over would have returned different data without failing. Reachable through a clause on the concrete type, where it returns the stored value. |
| an asset field aimed at non-asset content | A contentlet outside the interface cannot be handed on; doing so fails the **whole request**. |

**Not a rollback-safe change.** Unlike an additive field, this replaces a type and removes a
selection, so a client that has adopted the new shape breaks on rollback and one that has not
breaks on deploy.
