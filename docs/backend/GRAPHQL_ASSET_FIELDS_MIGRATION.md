# Migrating GraphQL queries over Image and File fields

**Applies to**: any GraphQL client that selects an Image or File field. **Issue**: dotCMS/core#34540.

An Image or File field used to resolve to `DotFileasset`, a flat type with six properties. It now
resolves to an **interface of the same name**, whose possible types are every content type derived
from the DOTASSET or FILEASSET base types. That is what makes a customer's own fields readable —
they were unreachable at any depth before.

Most queries need no change. Two do. Both fail **visibly**: you get a GraphQL validation error, not
different data.

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

## Change 1 — `description`

**This is the one that will break you.** It fails the whole request, not just the field: GraphQL
rejects an unknown field at validation time, so you get `data: null`. In the JavaScript SDK that
surfaces as a thrown `DotErrorPage`, not a missing value.

```graphql
# before
image { description }

# after
image { ... on Images { description } }
```

**Read the value it returns — it is probably not what you think.** On image-style content the old
`description` never returned a stored description: it returned the contentlet's **title**, which
for an asset is the file name. Measured on a real instance, the old field returned a value for
57 of 57 images while only 2 of those 57 have a stored description.

So the mechanical fix above changes what you get. Decide which you actually wanted:

| You wanted | Select |
|---|---|
| The file name, as before | `image { fileName }` — unchanged, still works |
| The contentlet's title | `image { title }` |
| The stored description | `image { ... on Images { description } }` — usually empty |

If your front-end used `description` as alt text or a caption, it was showing the file name. The
first row keeps that behaviour exactly.

---

## Change 2 — an asset field pointing at content that is not an asset

An Image or File field stores a bare identifier, and nothing prevents it from pointing at ordinary
content. That used to resolve to the flat view; it now resolves to `null`.

This is forced rather than chosen: a contentlet outside the interface cannot be handed on, and
handing it on raises `UnresolvedTypeException`, which fails the **entire request** — one
mis-pointed field taking every other collection in your query down with it. Returning nothing for
that one field is the containable outcome.

If you see a field that used to return data now returning `null`, check what it points at. It is a
data problem, not a query problem.

---

## What does not change

Five of the six original properties keep their names **and their exact values**:

```graphql
image { fileName  fileAsset { size mime }  metaData { key value }  showOnMenu  sortOrder }
```

Including the quirks. `fileName` on image-style content is still the contentlet name, synthesized
rather than stored, exactly as before.

`... on DotFileasset { … }` also keeps working and keeps returning data. The interface deliberately
kept that name, so clauses you already wrote stay valid.

---

## If you generate types from the schema

`DotFileasset` changes **kind**, from object to interface. Query text does not notice; code
generators do. Apollo, Relay and similar toolchains will produce different types for an interface
than for an object, so **regenerate** against the new schema. A build against the old generated
types will not match the runtime schema.

This is why the release carries an updated minimum SDK version.

---

## Checking your queries

Three things to grep your codebase for:

1. `description` immediately under an Image or File field selection — the breaking case.
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
