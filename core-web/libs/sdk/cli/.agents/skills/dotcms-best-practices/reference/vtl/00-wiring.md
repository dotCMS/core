# 00 · VTL wiring

**Every file below is yours to author** — VTL has no app scaffold to inherit. In an existing site,
list `/application/themes/` and `/application/containers/` first and extend what's there.

## Contents

- [What VTL adds](#what-vtl-adds) — the three requirements on top of the core contract
- [The tree you are creating](#the-tree-you-are-creating) — files, and the prescribed theme partials
- [Author locally, then upload](#author-locally-then-upload) — workflow and upload order
- [Then](#then) — where to go next

## What VTL adds

In addition to [core/00](../core/00-what-must-exist.md), a VTL-rendered page needs:

1. **Template → theme.** The theme's `template.vtl` must loop `$dotThemeLayout`;
   without that loop the page is a bare shell whatever you place ([02](02-themes.md)).
2. **Container → non-empty `preloop.vtl`/`postloop.vtl` + one `<Var>.vtl` per content
   type**, filename = the type's `Var`, case-exact ([03](03-containers.md)).
3. **Each item renders through the right mechanism** — content section, widget or
   detail page ([01](01-choose-mechanism.md)). Wrong mechanism renders empty.

Verify in LIVE ([05](05-verify-and-debug.md)). A VTL error is swallowed into an empty
string with HTTP 200, so "no error" is not evidence that anything rendered.

## The tree you are creating

Everything lives under `/application/` on the site. Nothing goes in `/assets` — that path is
reserved ([core/03](../core/03-content.md)).

```
/application/
  themes/<name>/
    template.vtl          REQUIRED — html shell + the $dotThemeLayout loop
    head.vtl              <head> — meta, canonical, OG/Twitter, the title chain
    header.vtl            skip link, <header>, <nav> landmark
    footer.vtl            <footer> landmark
    scripts.vtl           deferred JS, kept out of the shell
    styles.css            the site's CSS ([02](02-themes.md))
  containers/<name>/
    container.vtl         REQUIRED — metadata only, $dotJSON.put
    preloop.vtl           REQUIRED — must be non-empty, a comment is enough
    postloop.vtl          REQUIRED — must be non-empty; an empty one breaks assembly
    <Var>.vtl             REQUIRED — one per accepted content type, case-exact filename
  vtl/<name>.vtl          optional — shared includes, listing/detail bodies
```

**Use those partials — don't author one long `template.vtl`.** Only `template.vtl` is required by
dotCMS; the split is a house rule. Include them with `#dotParse` via `${dotTheme.path}` — never a
hardcoded path ([02](02-themes.md)).

**`head.vtl` must carry:**

- `<title>` from the fallback chain — `$URLMapContent` → `$dotPageContent` → a site default, as
  one `#macro` ([02](02-themes.md))
- `<meta name="description">` from the same chain
- `<link rel="canonical">`, absolute
- OG and Twitter tags, reusing the title/description values
- the robots directive, read as `.selectedValues.contains('index')` — it is multi-select

**The shell must carry:**

- `lang` on `<html>`, from the page's language, not hardcoded
- a skip link as the **first focusable element**, targeting the main landmark
- `<main>` wrapping the `$dotThemeLayout` loop — exactly one per page
- `<header>`, `<nav>`, `<footer>` as real landmarks
- one `<h1>` per page, no skipped heading levels
- `alt` on every content-field image — `alt=""` deliberately when the field is empty, never
  omitted

Container markup inherits the heading rule: a `<Var>.vtl` may open at `<h3>` under an `<h2>`
section; it must not invent its own `<h1>`.

Three are required, easy to omit, and fail silently:

| Missing | What you see |
|---|---|
| the `$dotThemeLayout` loop | theme, header and footer render; **every content slot is missing** |
| a non-empty `postloop.vtl` | container assembly breaks |
| a `<Var>.vtl` for a placed type | that slot renders empty, HTTP 200, no error anywhere |

Templates reference containers by **host-qualified path** —
`//<site>/application/containers/<name>/`. A relative path resolves against whatever site is
current; one that doesn't resolve gives an empty slot with no error
([core/06](../core/06-containers.md)).

## Author locally, then upload

Write these files on disk first and push them with the asset-upload tool — never inline bytes,
and don't hand-edit them in the instance once they're under version control.

**Mirror `/application/...` in your local tree**, so a file's upload path is its own relative path.

Upload order matters — a template's POST names container paths that must already resolve, plus a
theme id:

```
theme files → container folders → create + publish the template → pages → content → placement
```

Same dependency order as [reference/README.md](../README.md).

## Then

- Choosing how each thing renders → [01-choose-mechanism.md](01-choose-mechanism.md)
- The theme and its layout loop → [02-themes.md](02-themes.md)
- Per-type container markup → [03-containers.md](03-containers.md)
- Listings and URL-mapped detail pages → [04-listings-and-details.md](04-listings-and-details.md)
- Verifying, and triaging a blank render → [05-verify-and-debug.md](05-verify-and-debug.md)
- VTL syntax while you write → [velocity.md](velocity.md)
