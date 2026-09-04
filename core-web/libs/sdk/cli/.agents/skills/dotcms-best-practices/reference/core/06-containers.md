# 06 · Containers — creating the slot

**Both delivery modes need this.** A container is the slot content is placed into.
The template ([05](05-templates.md)) arranges containers into rows and columns;
Content placement ([09](09-placement.md)) addresses the slot it creates. Without
a container there is nowhere to put content, in either mode.

What differs is only what renders the slot: VTL runs a per-type `<Var>.vtl`
([vtl/03](../vtl/03-containers.md)), headless maps the content type to a React
component ([nextjs/01](../nextjs/01-component-contract.md)).

## Create it as a Container-as-File

A container is a **folder** under `/application/containers/<name>/`, uploaded with
the asset-upload capability. The folder is the container; there is no create endpoint to call.

| File | Required | What it's for |
|---|---|---|
| `container.vtl` | yes | metadata only — `$dotJSON.put` |
| `preloop.vtl` | yes | markup before the loop |
| `postloop.vtl` | yes | markup after the loop |
| `<Var>.vtl` | yes — one per accepted type | VTL: renders one contentlet of that type. Headless: a comment-only registration stub (see below) |

`container.vtl` holds metadata, not markup:

```velocity
## Metadata only.
$dotJSON.put("title", "Main")
$dotJSON.put("description", "Flexible section container.")
$dotJSON.put("max_contentlets", 25)
```

Set `max_contentlets` above the most sections any one page will place — it caps the
slot, and content over the cap is silently dropped.

**`preloop.vtl` and `postloop.vtl` must be non-empty** even when there is nothing to
wrap — an empty postloop breaks container assembly. A comment line is enough.

A headless build still creates all three of these. Its `<Var>.vtl` files are comment-only
registration stubs — see below.

## Which content types a container accepts

Determined by **which `<Var>.vtl` files exist in the folder**, not by anything declared
in `container.vtl`. Verified: a container holding `Hero.vtl`, `Section.vtl` and
`Book.vtl` reports exactly those three in the page API's `containerStructures`.

That list is what the **page editor and UVE offer an author** when they add content to
the slot. It does *not* gate the API — placement will accept a type that isn't
on the list, and it renders fine — but a container with no `<Var>.vtl` files offers an
author nothing, so the slot is uneditable in UVE.

**So a headless build still writes one `<Var>.vtl` per accepted type** — as a
**comment-only registration stub**, never an empty file. The filename is the entire point; a
React component does the rendering:

```velocity
## Registration only — <Type> is rendered by its React component.
```

Skip these only if no one will ever author this slot through dotCMS.

## Prefer one flexible container

A container is **not** one-per-type. Put one `<Var>.vtl` per type in the *same*
folder and dotCMS renders each placed contentlet through its match; in headless the
component map does the same job. **Default to one container in one slot**
([05](05-templates.md)) and place a page's sections into it in order — no fixed
per-type slots, nothing empty to hide on pages that skip a section.

Split into a second container only for a genuinely different render path: a widget
([vtl/01](../vtl/01-choose-mechanism.md)) or a URL-mapped detail
([vtl/04](../vtl/04-listings-and-details.md), [nextjs/04](../nextjs/04-listings-and-details.md)).
Needing a wrapper is not a reason to split.

## The path must resolve on your site

The template references the container by a host-qualified path —
`//<site>/application/containers/<name>/`. A relative path resolves against the
*current* site, which may not be yours. A value that doesn't resolve produces an
empty slot at render time with no error ([05](05-templates.md)).
