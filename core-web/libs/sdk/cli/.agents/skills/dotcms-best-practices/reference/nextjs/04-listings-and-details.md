# 04 · Listings and detail pages

Routing itself is [03-routing.md](03-routing.md); this file is what you render on
those routes. The dotCMS-side setup is identical to the VTL build — only the rendering
differs ([vtl/04](../vtl/04-listings-and-details.md) is the Velocity equivalent).

## Contents

- [Listings](#listings)
- [Detail pages](#detail-pages) — urlmaps and `urlContentMap`

## Listings

Two sources, used for different jobs:

**Enrich the page request** when the list is part of the page and should render
server-side. Pass GraphQL alongside the page fetch; results arrive on `content`:

```ts
dotCMSClient.page.get(path, {
  graphql: { content: { blogs: blogQuery }, fragments: [fragmentNav] },
});
// -> const { blogs } = content as PageExtraContent
```

**Query the collection** for interactive lists — search, filters, pagination:

```ts
dotCMSClient.content.getCollection<Blog>('Blog')
  .limit(3)
  .query((qb) => qb.field('title').equals(`${term}*`))
  .sortBy([...]);
```

Query builder, GraphQL options and typing: **@dotcms/client README → How to Query
Content Collections / How to Work with GraphQL**.

## Detail pages

A detail page is a **real dotCMS page driven by a urlmap** — the same mechanism as the
VTL build, only the rendering differs. Two things must be set in dotCMS on the content
type ([core/02](../core/02-content-types.md)):

- `urlMapPattern` — e.g. `/blog/post/{urlTitle}`
- `detailPage` — the identifier of the page that renders it

Then the resolved contentlet arrives on the page response as **`urlContentMap`** —
the headless equivalent of VTL's `$URLMapContent`:

```tsx
const { pageAsset } = useEditableDotCMSPage(pageContent) ?? {};
const item = pageAsset?.urlContentMap;

// urlContentMap is absent whenever the URL did not resolve to a contentlet —
// a bad slug, a direct hit on the renderer page, or a UVE error response.
// Guard it: item.<field> on undefined throws and takes the whole route down.
if (!item) return notFound();   // or an in-UVE placeholder, see below

<DotCMSEditableText contentlet={item} fieldName="title" />
<DotCMSBlockEditorRenderer blocks={item.blogContent} customRenderers={customRenderers} />
```

**Inside UVE, don't `notFound()`** — an editor opening the renderer page directly has no
slug to resolve, so a 404 makes the page uneditable. Render a placeholder instead when
`isRequestFromUVE` is true, the same split the five-step sequence above makes for errors.

Give the detail its own route rather than leaning on the catch-all when the path shape
is known and it renders a different view (`app/blog/post/[[...slug]]`). Next resolves
the more specific segment first, so both can coexist.

Setting `urlMapPattern` / `detailPage` on the content type, the slug-field rules, and the
page-before-URL-map ordering: [core/02](../core/02-content-types.md).

`DotCMSEditableText`, `DotCMSBlockEditorRenderer` and custom renderers:
**@dotcms/react README → SDK Reference**.
