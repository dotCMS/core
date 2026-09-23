# Migrating GraphQL queries over Image and File fields

**Applies to**: any GraphQL client that selects an Image or File field. **Issue**: dotCMS/core#34540.

An Image or File field used to resolve to `DotFileasset`, a flat type with six properties. It now
resolves to an **interface of the same name**, whose possible types are every content type derived
from the DOTASSET or FILEASSET base types. That is what makes a customer's own fields readable —
they were unreachable at any depth before.

**Your queries keep working.** All six properties are still selectable and still return the same
values. Nothing was removed and nothing was renamed. Two things do change, neither of them a query
you have to rewrite: what `__typename` reports, and the kind of `DotFileasset` for anyone
generating types from the schema. Both are below.

---

## What you gain

Properties on your own asset content types, which had no workaround:

```graphql
image {
  ... on BannerImages { campaignName adSize }
  ... on PDFDocuments { category downloadCount }
}
```

Plus the asset's own identity and tags, none of which were selectable before — `identifier`,
`inode`, `host`, `urlMap`, `live`, `title`, `tags` — and the binary's properties without descending
a level:

```graphql
image { name size mime versionPath idPath path sha256 isImage width height }
```

A content type you create later is reachable immediately, with no administrative step.

---

## Change 1 — `__typename`

**The one value that changes.** On an asset-pointing field it used to be the constant
`"DotFileasset"`. It is now the concrete content type: `"Images"`, `"FileAsset"`,
`"BannerImages"`, whatever the field actually points at.

`__typename` did not change its rule — it has always reported the runtime type of the resolved
value. What changed is that the runtime type is no longer a constant, because the field is now
described by an interface. This is inseparable from the feature: the same resolution that makes
`... on BannerImages { campaignName }` work is what `__typename` reports. Pinning it back to
`"DotFileasset"` would mean naming a type the value does not have.

Who this actually reaches:

| If you | Then |
|---|---|
| Never select `__typename` under an asset field | Nothing to do |
| Use Apollo Client, or any normalized cache | **Check this.** Normalized caches key entries on `__typename` + id, so cache keys for these objects change. Entries written by an older build will not be read back |
| Assert `__typename` in snapshot tests | Update the expected value to the concrete type |
| Branch on `__typename` in application code | Re-read the branch. It was comparing against a constant, so it was always taking the same path; now it discriminates, which is probably what you wanted |

---

## Change 2 — if you generate types from the schema

`DotFileasset` changes **kind**, from object to interface. Query text does not notice; code
generators do. Apollo, Relay, graphql-codegen and similar toolchains produce different types for an
interface than for an object, so **regenerate** against the new schema. A build against the old
generated types will not match the runtime schema.

This is about **your** toolchain, not the dotCMS SDK. The `@dotcms/*` packages do not generate
types from the schema — they send the fragment text you write, so they are unaffected and no
minimum SDK version changes because of this.

---

## Change 3 — an asset field pointing at content that is not an asset

An Image or File field stores a bare identifier, and nothing prevents it from pointing at ordinary
content — a Blog Post, say, put there by an import or a script. That used to resolve to the flat
view, which happily answered with the Blog Post's name; it now resolves to `null`.

This is forced rather than chosen: a contentlet outside the interface cannot be handed on, and
handing it on raises `UnresolvedTypeException`, which fails the **entire request** — one
mis-pointed field taking every other collection in your query down with it. Returning nothing for
that one field is the containable outcome.

If you see a field that used to return data now returning `null`, check what it points at. It is a
data problem, not a query problem, and it was a data problem before too — the old answer just
looked valid.

---

## If one of your asset types has a field named like an asset property

The asset field now offers the binary's properties directly — `name`, `size`, `mime`,
`versionPath`, `idPath`, `path`, `sha256`, `isImage`, `width`, `height` — alongside the six it
always had. One of your own asset types may already have a field with one of those names.

**Your field always wins its name on your type.** It existed first, and answering with anything
else would silently change what your queries return. The asset property stays reachable through
the binary: `asset { size }` on image-style content, `fileAsset { size }` on file-style content.

What happens beyond that depends on whether the two agree on the kind of value:

| Your field | Example | Effect |
|---|---|---|
| Same kind of value | a text field `sha256` | `image { sha256 }` returns **your** value for assets of that type |
| Different kind of value | a text field `width`, where the asset's is a number | `width` is no longer offered directly on the asset field anywhere on the instance; `image { width }` is rejected with an error naming `width` |

The second case is how the schema stays valid: GraphQL requires every type behind an asset field to
agree on what `width` is, and a disagreement would otherwise reject the whole schema. dotCMS logs a
warning at schema build naming the content type and field. To get the direct property back, rename
your field; until then `... on YourType { width }` reads yours and `asset { width }` reads the
binary's.

One long-standing exception, unchanged by this work: on **image-style (DOTASSET)** types, a field
of yours named `name`, `size`, `path`, `type` or `extension` is overwritten with the binary's value
whenever the content is read — through its own collection as much as through an asset field. That
was already true before; renaming the field is the only way to read what was stored in it.

A new field whose variable is generated from its name steers clear of these names on asset types
(a text field called "Width" gets `width1`). A variable you choose explicitly is not changed.

---

## What does not change

All six properties keep their names **and their exact values**:

```graphql
image { fileName  description  fileAsset { size mime }  metaData { key value }  showOnMenu  sortOrder }
```

Including the quirks. `fileName` on image-style content is still the contentlet name, synthesized
rather than stored, exactly as before.

`... on DotFileasset { … }` also keeps working and keeps returning data. The interface deliberately
kept that name, so clauses you already wrote stay valid.

### `description`, and why it returns two different things

This one is worth understanding, because it looks like a bug and is not — and because it behaved
this way **before** this change too.

```graphql
image { description }                 # the contentlet TITLE, which for an asset is the file name
ImagesCollection { description }      # the stored description, usually empty
```

Same asset, same field name, different values. Two meanings have always shared this name: the flat
view derived `description` from the title, while an asset content type's own `description` field
holds what an editor typed. Measured on a real instance, the first form returned a value for 57 of
57 images while only 2 of those 57 have a stored description.

Once the field is described by an interface, the concrete type's own definition would normally take
over — which would have made `image { description }` silently start returning the stored value,
usually empty. It does not: the value is selected by how the asset was reached, so both answers are
exactly what they were.

If your front-end used `description` as alt text or a caption, it was showing the file name, and it
still is. If you want the stored one, query the asset through its own collection.

---

## A whole query, before and after

A page listing banners, where each banner points at an image.

```graphql
query Banners {
  BannerCollection(limit: 10) {
    title
    image {
      fileName
      description
      fileAsset { versionPath size mime width height }
      metaData { key value }
    }
  }
}
```

**This query is unchanged.** It validates and returns the same values it did before. That is the
whole migration for a typical client: nothing.

**Using what is now available** — the same query taking advantage of the change:

```graphql
query Banners {
  BannerCollection(limit: 10) {
    title
    image {
      # the binary, without descending a level
      fileName  versionPath  size  mime  width  height

      # the asset's own identity — none of this was selectable before
      identifier  title  live  urlMap
      __typename                       # which content type this actually is

      # properties defined on the customer's own asset types
      ... on Images       { tags }
      ... on BannerImages { campaignName adSize }
    }
  }
}
```

**A mixed collection**, where the same field points at different kinds of asset:

```graphql
query MixedAssets {
  ArticleCollection(limit: 10) {
    image {
      baseType                         # DOTASSET or FILEASSET, no clause needed
      __typename                       # the concrete type
      ... on DotAssetBaseType { asset     { size mime } }
      ... on FileBaseType     { fileAsset { size mime } }
    }
  }
}
```

Each row answers with only the clause that applies. Reading `baseType` is enough to branch without
writing any clause at all.

---

## Checking your queries

Three things to grep your codebase for:

1. `__typename` under an Image or File field selection, and anything keyed on it — a normalized
   cache, a snapshot, a switch.
2. Generated GraphQL types referencing `DotFileasset` — regenerate them.
3. Any Image or File field you know points at non-asset content.

To see what an asset field can return on your own instance:

```graphql
{ __type(name: "DotFileasset") { kind fields { name } possibleTypes { name } } }
```

`possibleTypes` lists every asset content type you can narrow to, including your own.

---

## Telling a mistyped clause from one that simply did not match

A clause naming a type the asset is not contributes nothing and does **not** fail — which is
correct, but used to be indistinguishable from a typo in the type name. The response now carries a
non-fatal warning in `extensions` naming any clause that matched nothing:

```json
{ "data": { "…": "…" },
  "extensions": { "warnings": [
    { "path": "BannerCollection.image", "typeCondition": "PDFDocuments",
      "message": "No content at this path was of type PDFDocuments." } ] } }
```

A clause naming a type that does **not exist** still fails the request outright. That is a client
mistake with no valid reading.
