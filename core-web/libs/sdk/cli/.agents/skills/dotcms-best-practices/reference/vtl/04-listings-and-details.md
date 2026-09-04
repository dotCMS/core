# 04 · Listings & detail pages

The two recipes almost every site needs: a **listing** (many items on one page) and a **detail
page** (one item, keyed off the URL). This file is the *sequence*; siblings hold the gotchas for
each mechanic. VTL syntax: [velocity.md](velocity.md).

## Contents

- [Decide the mechanism first](#decide-the-mechanism-first)
- [Recipe A — Listing page](#recipe-a--listing-page)
- [Recipe B — Detail page (URL-mapped)](#recipe-b--detail-page-url-mapped-urlmapcontent)

## Decide the mechanism first

Do this in [01-choose-mechanism.md](01-choose-mechanism.md) before writing anything — the short
version is **queries content → widget; renders a single passed-in item → detail-page VTL.**

---

## Recipe A — Listing page

A page such as `/products` rendering all (or the newest N) items of a content type.

**Two content types are involved** and are easy to conflate: the **data type** being listed
(`Product`), and the **widget type** whose `widgetCode` runs the listing VTL.

1. **The data content type exists** with the fields you list ([02-content-types.md](../core/02-content-types.md) — check variable names against the reserved list, send all fields inline). Note its **`Var`** (case-exact) — you'll query by it.

2. **Write the listing VTL.** A SimpleWidget's code, or a standalone VTL you `#dotParse`. Pull, loop, render a card per item. Query by the type's `Var`:

   ```velocity
   #set($items = $dotcontent.pull("+contentType:Product +live:true +conHost:${host.identifier}", 12, "modDate desc"))
   <ul class="product-grid">
   #foreach($item in $items)
     <li class="product-card">
       <a href="/products/${item.urlTitle}">
         <img src="/dA/${item.identifier}/image/400w/80q/webp" alt="$esc.html($item.title)" />
         <h3>$esc.html($item.title)</h3>
         <p class="price">$number.currency($item.price)</p>
       </a>
     </li>
   #end
   </ul>
   ```

   Notes:
   - `+live:true` on the listing so you only render published items.
   - `+conHost:${host.identifier}` scopes the query to the current site. Without it the
     pull spans every site on the instance, so a shared content type leaks another
     site's items into your listing.
   - `pull(query, limit, sort)` — limit `0` means "up to 10,000". For real paging use `$dotcontent.pullPerPage(query, currentPage, perPage, sort)` (see viewtools cheat sheet).
   - Image URL is `/dA/${item.identifier}/<fieldVar>/<width>w/<quality>q/webp` — the `w`/`q` suffixes are required, and never `${item.image}` ([velocity.md](velocity.md) §4).
   - Detail link uses the same slug the urlmap uses (see Recipe B) so listing→detail is consistent.

3. **Upload the VTL** with the asset-upload tool (never inline bytes). Put it under `/application/...`, not `/assets` (reserved — [03-content.md](../core/03-content.md) assets).

4. **Create the WIDGET content type** — a different type from step 1. Use the built-in
   SimpleWidget for a fixed list, or your own Widget type with fields if the author picks
   count/category/sort (read those via `$dotContentMap.<field>` instead of hardcoding).
   Widgets render through **`widgetCode`**, not a container `<Var>.vtl`
   ([01-choose-mechanism.md](01-choose-mechanism.md)).

   `widgetCode` is a **constant field on the widget type**, so it is set here, in this
   type's create body — `#dotParse` the VTL you uploaded in step 3
   ([02-content-types.md](../core/02-content-types.md)). It cannot be set later on the
   contentlet; the author-set fields (heading, limit, …) *are* per-contentlet and behave
   normally.

5. **Create a widget contentlet and place it** — this is the build's normal content and
   placement work ([03-content.md](../core/03-content.md),
   [09-placement.md](../core/09-placement.md)), not extra steps: do it with the rest of
   the site's content. **Re-publish the page** after placing; LIVE won't change until you do.

6. **Verify** ([05-verify-and-debug.md](05-verify-and-debug.md)): a `verdict` of `empty-vtl-error` means the listing VTL failed — run it through `/api/vtl/dynamic` for a real stack trace.

---

## Recipe B — Detail page (URL-mapped, `$URLMapContent`)

Goal: `/products/blue-shoe` renders the single "blue-shoe" Product. One page template serves every item; dotCMS resolves the URL to a contentlet and hands it to the VTL as `$URLMapContent`.

**Order matters here**: `detailPage` is the identifier of a page that must already
exist, so the page is created *before* the URL-map is set — not after.

1. **The content type needs a slug field.** A real field whose value is the slug
   (`urlTitle` is the convention), **unique per item** and indexed. The URL-map token
   in step 4 refers to it by name.

2. **Write the detail VTL.** It reads the passed-in item from **`$URLMapContent`** — NOT `$dotcontent.pull` (nothing to query; the item is already resolved from the URL):

   ```velocity
   ## copy the context object before doing anything — never reassign $URLMapContent (see 05 VTL traps)
   #set($item = $URLMapContent)
   #if($UtilMethods.isSet($item))
     <article class="product-detail">
       <h1>$esc.html($item.title)</h1>
       <img src="/dA/${item.identifier}/image/1200w/85q/webp" alt="$esc.html($item.title)" />
       <p class="price">$number.currency($item.price)</p>
       <div class="body">$item.description</div>  ## Story Block renders as HTML
     </article>
   #else
     ## no item resolved — render a real 404-ish message, not a blank page
     <p>Product not found.</p>
   #end
   ```

3. **Create the detail page** at a stable URL — often a placeholder like
   `/products/detail`. Users never visit it directly; `urlMapPattern` is what they hit,
   and this page is the renderer. **Note its identifier** — step 4 needs it.

4. **Now set the URL-map on the content type**, in a PUT/PATCH on the existing type:
   - `urlMapPattern` — the URL shape with the slug token, e.g. `/products/{urlTitle}`.
   - `detailPage` — the **identifier of the page you created in step 3**.

   This is why it comes fourth: there is no page identifier to reference until step 3 has
   run. (If you happen to be creating the type from scratch after the page already exists,
   both keys can go straight into the content-type POST body.)

5. **Put the detail VTL in a SimpleWidget and place it** on the detail page, then publish
   ([09-placement.md](../core/09-placement.md)).

   **It must be a widget, not the container's `<Var>.vtl`.** A `<Var>.vtl` only runs for a
   contentlet actually *placed* in that slot, read via `$dotContentMap` — and the
   URL-mapped item is never placed; dotCMS resolves it from the URL and exposes it as
   `$URLMapContent`. Putting the detail VTL in a `<Var>.vtl` renders nothing. The widget's
   `widgetCode` runs on every request to the page, which is what you want here.

6. **Link listing → detail.** The listing card's `href` must match `urlMapPattern` with the item's slug value substituted: pattern `/products/{urlTitle}` → `href="/products/${item.urlTitle}"`.

7. **Verify** a real slug (path `/products/<slug>`) — its `urlMap` reports whether `$URLMapContent` resolved. Confirm a real slug renders (title/price present) and that an unknown slug hits your `#else` branch, not a blank 200. Details: [05-verify-and-debug.md](05-verify-and-debug.md).

---
