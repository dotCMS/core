# Velocity (VTL) — the reference for authoring `.vtl`

The viewtools, context objects, and language gotchas a site build actually reaches for. The viewtool sections are curated from the dotCMS docs (dev.dotcms.com/docs/velocity-viewtools) — **not the full list**, just what a listing/detail/template build uses, with documented signatures and the traps that bite (full per-tool docs linked inline). §5 covers VTL-language behavior that isn't tied to any one tool.

> These run in **VTL** (`.vtl` files, container/widget code, `#dotParse` partials, and `POST /api/vtl/dynamic`). They do **NOT** exist in the JS code-execution sandbox — see [05-verify-and-debug.md](05-verify-and-debug.md). When a `#dotParse` render comes back blank with HTTP 200, POST the VTL to `/api/vtl/dynamic` to get a real line/column error.

## Contents

1. [Content querying](#1-content-querying) — `$dotcontent`
2. [Page / site / navigation context](#2-page--site--navigation-context) — `$URLMapContent`, `$dotContentMap`, and friends
3. [Utility & formatting](#3-utility--formatting) — `$UtilMethods`, `$date`, `$number`, `$math`, `$text`
4. [Escaping, images & links](#4-escaping-images--links) — `$esc`, `/dA/` resize URLs, urlmap hrefs
5. [VTL gotchas](#5-vtl-gotchas-not-tied-to-one-viewtool) — language behavior, not viewtools
- [What's intentionally left out](#whats-intentionally-left-out)

---

## 1. Content querying

### `$dotcontent` — pull content by Lucene query · [docs](https://dev.dotcms.com/docs/dotcontent-viewtool)
The workhorse for listings. Query strings are Lucene, keyed by `contentType` + field variables.

```velocity
## pull(query, limit, sort)   — limit 0 = up to 10,000
$dotcontent.pull("+contentType:Product +live:true", 12, "modDate desc")

## pull(query, offset, limit, sort)  — offset -1 = default
$dotcontent.pull("+contentType:Blog", -1, 0, "modDate desc")

## pullPerPage(query, currentPage, perPage, sort)  — 1-indexed page; use for real paging
$dotcontent.pullPerPage("+contentType:Product +live:true", 2, 12, "title asc")

## find(idOrInode)  — one contentlet by identifier or inode
$dotcontent.find("<identifier or inode>")

## count(query)  — integer count, for "N results" / paging math
$dotcontent.count("+contentType:Product +live:true")
```

Query patterns: `+contentType:Product`, `+live:true`, `+conHost:${host.identifier}`, `+categoryField:someCategory`, `+RelatedType.field:value`, `+title:blue*`. Combine with spaces; `+` = required, `-` = exclude.

- **Scope listings to the site** with `+conHost:${host.identifier}` — without it a pull spans every site on the instance.

- **Custom field terms must be TYPE-QUALIFIED** — `+Product.featured:true`, `+Product.slug:blue-shoe`, NOT `+featured:true` / `+slug:...`. An unqualified custom-field term matches nothing and returns **zero results with no error** (a silent empty listing). `contentType`, `live`, `title`, `modDate` and other system fields work unqualified; your own fields need the `<Var>.` prefix. Same rule in `POST /api/content/_search` `query` strings.

- **Listings query; detail pages do NOT** — a detail page reads `$URLMapContent` (§2), it doesn't pull.
- `pullRelated` (legacy relationships) is superseded — prefer a Lucene query against the relationship field instead.
- On a Widget content type, read the author-set field values with `$dotContentMap.<field>` and feed them into the query, rather than hardcoding the query.

---

## 2. Page / site / navigation context

These are **context objects** dotCMS puts in scope — not something you call to fetch. [05-verify-and-debug.md](05-verify-and-debug.md) (VTL traps): never reassign one; copy to a new var first (`#set($item = $URLMapContent)`).

- **`$URLMapContent`** — on a URL-mapped **detail page**, the single contentlet resolved from the URL. Guard it: `#if($UtilMethods.isSet($URLMapContent))`. Absent on non-detail pages.
- **`$dotContentMap`** — the current contentlet's fields when rendering a **widget or container** (a widget reads its own author fields here). Not the same as `$URLMapContent`.
- **`$dotPageContent`** — the current HTMLPage's own fields (title, SEO fields, friendly name). Used in the theme's SEO fallback macro: `$URLMapContent` → `$dotPageContent` → default.
- **`$host`** — the current site. `$host.identifier` (the UUID) is the host-portable way to build `/dA/` and API references — never hardcode a hostname. Copy before calling methods on it.
- **`$navtool`** — build menus/breadcrumbs from the folder tree. [docs](https://dev.dotcms.com/docs/navtool-viewtool)
  ```velocity
  ## getNav(path, depth)  — items under a path, N levels deep
  #set($nav = $navtool.getNav("/", 2))
  #foreach($item in $nav)
    <a href="$item.href"#if($item.active) class="is-active"#end>$esc.html($item.title)</a>
  #end
  ```
  Each nav item exposes `.href`, `.title`, `.active`, `.children`. Prefer `$navtool` over hardcoding nav so new pages/folders appear automatically.

---

## 3. Utility & formatting

### `$UtilMethods` — null-safety & helpers · [docs](https://dev.dotcms.com/docs/utilmethods)
The one you'll use constantly is **presence**: `#if($UtilMethods.isSet($x))`. It handles null AND empty-string in one check — the correct guard before rendering any optional field ([05-verify-and-debug.md](05-verify-and-debug.md) VTL traps).
```velocity
#if($UtilMethods.isSet($item.subtitle))<p class="sub">$esc.html($item.subtitle)</p>#end
$UtilMethods.capitalize($item.category)
$UtilMethods.truncatify($item.description, 160)   ## trim with ellipsis for cards/meta
```
> Method name is capitalized: **`$UtilMethods`** (not `$utilmethods`) — match the codebase convention.

### `$date` — format dates · [docs](https://dev.dotcms.com/docs/date-viewtool)
Content date fields arrive as date objects; format for display, don't print raw.
```velocity
$date.format('medium', $item.publishDate)          ## Oct 7, 2003
$date.format("EEE, MMMM d, yyyy", $item.eventDate)  ## Fri, July 3, 2026
#set($d = $date.toDate("yyyy-MM-dd'T'HH:mm:ss'Z'", $someIsoString))  ## parse a string
```
Patterns: `yyyy` year · `MMMM`/`MMM` month · `dd` day · `HH`/`hh` 24/12-hr · `mm` min · `EEE` weekday.

### `$number` — number & currency formatting · [docs](https://dev.dotcms.com/docs/numbertool)
```velocity
$number.currency($item.price)     ## localized currency
$number.format("#,##0.00", $item.rating)
```
(For a raw dollar string, `$UtilMethods.dollarFormat($item.price)` also works.)

### `$math` — arithmetic & int coercion · [docs](https://dev.dotcms.com/docs/mathtool)
Velocity does have `+ - * /` in `#set`, but they are strict about types — a string or null operand yields no result rather than an error. `$math` coerces first, so prefer it for anything derived from content. `$math.toInteger($x)` is the safe int coercion [05-verify-and-debug.md](05-verify-and-debug.md) calls out. `$math.add`, `.sub`, `.mul`, `.div` for paging math, grid columns, etc.

### `$text` — language/i18n variables · [docs](https://dev.dotcms.com/docs/texttool-language-viewtool)
`$text.get("some.key")` pulls a Language-variable value. Use for any label you want translatable instead of hardcoding a string. Only reach for this if the plan says i18n is in scope.

---

## 4. Escaping, images & links

### `$esc` (EscapeTool) — output escaping · [docs](https://dev.dotcms.com/docs/escapetool-class)
**Escape every user/content string you drop into HTML** — titles, descriptions, alt text.
```velocity
<h3>$esc.html($item.title)</h3>
<a href="/products/$esc.html($item.slug)">…</a>   ## attribute → $esc.html, not $esc.javascript
```
Use `$esc.javascript` only inside an actual `<script>` block or JS string — it is not an HTML-attribute escaper.

(Registered as `$escape`; `$esc` is the common alias. Story Block / WYSIWYG body is already HTML — render it raw, don't `$esc.html` it, or you'll show tags as text.)

### Images — the `/dA/` resize URL (NOT a viewtool, but the rule that pairs with them)
Image/Binary fields render through the `/dA/` endpoint by **identifier**, never `${field}` ([05-verify-and-debug.md](05-verify-and-debug.md) VTL traps). Host-portable, no hostname. **Width and quality need their `w` / `q` suffixes:**
```
/dA/${item.identifier}/<fieldVariable>/<width>w/<quality>q/webp
/dA/${item.identifier}/image/800w/80q/webp
```
`webp` is the recommended format. Same shape in listing thumbnails and detail heroes — just change the width.

Drop the suffixes (`/800/80/webp`) and the segments are not recognized as resize params — with **no error**. A JPEG source is then served at full size, and a VP8X WebP source comes back as a corrupt 76-byte 256×6 image. Verify a resize actually happened: `curl` the URL and check the returned pixel dimensions, not just the HTTP status.

### Links — build hrefs from the URL-map pattern
There's no special link viewtool for content detail URLs: construct the href from the content type's `urlMapPattern` with the item's slug substituted (see [04-listings-and-details.md](04-listings-and-details.md)):
```velocity
<a href="/products/${item.urlTitle}">$esc.html($item.title)</a>
```
For nav/menu links use `$item.href` from `$navtool` (§2). For a link to a file/asset by identifier, the `/dA/` URL above is already host-portable.

---

## 5. VTL gotchas (not tied to one viewtool)

These are how VTL itself behaves against dotCMS data — the usual causes of a field that renders empty or wrong even when the code parses.

- **Select / Checkbox / Radio / Multi-Select values are Maps, not strings.** Read `.selectValue` / `.selectLabel`; for a checkbox test `.selectedValues.contains("true")`. (The REST API serializes them as plain strings — don't infer the VTL type from an API response.)
  **Never string-coerce one and pattern-match it.** `"$!{item.layoutStyle}"` stringifies the *whole map*, and its `toString()` contains **every** option — so `.contains("imageRight")` is true even when the value is `imageLeft`. Mutually-exclusive branches then all fire at once (an image renders three times, a "featured only" flag is always on). This is worse than a blank: the page looks populated and is quietly wrong. Safe idiom, which also survives a value that genuinely is a string:
  ```velocity
  #set($f = $item.layoutStyle)
  #set($v = "$!{f.selectValue}")
  #if($v.length() == 0)#set($v = "$!{f}")#end   ## fallback if already a plain string
  #if($v.equals("imageRight")) … #end
  ```
  Same shape for a boolean RadioField: `$v.equals("true")`, never `.contains("true")`.
- **Never call a method on a quoted literal** — `"$!{x}".trim()` fails. Assign to a var first, then call the method.
- **Never reassign a context object** (`$host`, `$dotContentMap`, `$URLMapContent`) — copy to a new var first: `#set($item = $URLMapContent)`.
- **Unknown method refs fail open** — they render as literal text rather than erroring, so a typo'd method silently prints its own source. Coerce ints with `$math.toInteger($x)`; test presence with `$UtilMethods.isSet($x)` (§3).

---

## What's intentionally left out

The full viewtool list also has `$elasticsearch`, `$sql`, `$json`, `$xml`, `$dotcache`, `$mailer`, `$workflowtool`, `$categories`, `$persona`, `$sitesearch`, and more. They're real but out of scope for a standard content-driven site build — reach for the docs index (dev.dotcms.com/docs/velocity-viewtools) if the plan calls for search, external data, email, or personalization, and prefer a supported REST endpoint (look it up with the spec-search tool) where one exists.
