# 01 · Choose the mechanism

Before building anything, decide *what primitive* each need maps to. Getting this wrong is the #1 cause of "created fine, renders blank" — a widget's code lives in `widgetCode`, a content section's lives in the container `<Var>.vtl`, and a detail page reads `$URLMapContent`. Pick here; the later steps assume you've chosen.

| Need | Build | Renders via |
|------|-------|-------------|
| A repeating data thing (Book/Product/Event) | **Content type** | container `<Var>.vtl` (or a listing widget) |
| Static editable section (hero/title/contact/newsletter) | **Content type** | container `<Var>.vtl` |
| Dynamic listing, no author options | **SimpleWidget** (`#dotParse` a list VTL) | `widgetCode` |
| Dynamic listing with author options (count/category/sort) | **Widget content type** with fields | `widgetCode`, reading `$dotContentMap.<field>` |
| One item rendered from its own URL (`/products/blue-shoe`) | **Detail page** — SimpleWidget reading `$URLMapContent`; set `urlMapPattern` + `detailPage` on the content type | `$URLMapContent` |

Rule of thumb: **queries content → widget; renders a single passed-in item → detail-page VTL; doesn't query → not a widget.**

**If you picked a widget, its code is part of the TYPE.** `widgetCode` is a constant field on the content type, not a per-contentlet value — so you set it when you build the type ([02-content-types.md](../core/02-content-types.md)), not when you create the content. dotCMS never executes a container's `<Var>.vtl` for a WIDGET base type, so a container VTL is not an alternative home for it.

Full listing + detail recipes: [04-listings-and-details.md](04-listings-and-details.md).
