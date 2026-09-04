---
name: dotcms-best-practices
description: Reference for doing any single thing in dotCMS correctly — create a content type, page, template or container; place content; author VTL; wire a headless Next.js frontend; or debug a page that renders blank. Indexed by intent, so you load one file for the task at hand. Use whenever working with dotCMS content types, fields, pages, templates, containers, content placement, VTL/Velocity, the Universal Visual Editor, or a headless dotCMS frontend — and especially when a page renders empty, a slot shows "no component", or a change doesn't appear in LIVE.
---

# dotCMS Best Practices

Load the file for the task at hand — never the folder. Building end to end? The ordered sequence
is [reference/README.md](reference/README.md). Planning a whole site? Start with
`dotcms-create-sites`.

## Read this first

**A missing piece renders blank or shell-only with HTTP 200, never an error.** A successful
response is not evidence that anything rendered — which is why every build path ends in a verify
step.

**Establish the delivery mode before any work that touches rendering** — themes, containers,
templates, components, or diagnosing a blank page:

| Mode | dotCMS renders? | You need |
|---|---|---|
| **VTL-rendered** | yes — theme + container VTL | `core/` + `vtl/` |
| **Headless** | no — it serves the page, an app renders it | `core/` + `nextjs/` |

If the task touches rendering and the user hasn't said, ask. **Content-model work — types,
fields, content — is identical in both modes; don't ask.**

Building a page for the first time? Start with the wiring contract —
[reference/core/00-what-must-exist.md](reference/core/00-what-must-exist.md).

## I want to…

**Content model**

| Task | File |
|---|---|
| Create or publish a site | [core/01-site.md](reference/core/01-site.md) |
| Create or change a content type, add fields | [core/02-content-types.md](reference/core/02-content-types.md) |
| Create content | [core/03-content.md](reference/core/03-content.md) |

**Pages and rendering scaffold**

| Task | File |
|---|---|
| Create a page | [core/04-pages.md](reference/core/04-pages.md) |
| Build or change a template — layout, rows, columns | [core/05-templates.md](reference/core/05-templates.md) |
| Set up a container (a slot content goes into) | [core/06-containers.md](reference/core/06-containers.md) |
| Put content onto a page | [core/09-placement.md](reference/core/09-placement.md) |

**VTL-rendered delivery**

| Task | File |
|---|---|
| Know what VTL mode adds, and see the whole tree you must author | [vtl/00-wiring.md](reference/vtl/00-wiring.md) — **start here for VTL** |
| Decide how something should render — content type vs widget vs detail page | [vtl/01-choose-mechanism.md](reference/vtl/01-choose-mechanism.md) |
| Build a theme — HTML shell, grid, SEO | [vtl/02-themes.md](reference/vtl/02-themes.md) |
| Write container markup | [vtl/03-containers.md](reference/vtl/03-containers.md) |
| Build a listing page or a URL-mapped detail page | [vtl/04-listings-and-details.md](reference/vtl/04-listings-and-details.md) |
| Verify a VTL page renders — a required build step, not just debugging | [vtl/05-verify-and-debug.md](reference/vtl/05-verify-and-debug.md) |
| Look up VTL/Velocity syntax while writing `.vtl` | [vtl/velocity.md](reference/vtl/velocity.md) |

**Headless delivery (Next.js)**

| Task | File |
|---|---|
| Connect an app to dotCMS and render a page | [nextjs/00-connect.md](reference/nextjs/00-connect.md) — §A is the dotCMS side; §B is the app wiring, with how to tell what already exists |
| Register a component for a content type | [nextjs/01-component-contract.md](reference/nextjs/01-component-contract.md) |
| Configure `next.config` | [nextjs/02-next-config.md](reference/nextjs/02-next-config.md) |
| Set up routing | [nextjs/03-routing.md](reference/nextjs/03-routing.md) |
| Build a listing or detail route | [nextjs/04-listings-and-details.md](reference/nextjs/04-listings-and-details.md) |
| Verify a headless page renders — a required build step, not just debugging | [nextjs/05-verify.md](reference/nextjs/05-verify.md) |

Next.js is the only framework branch here. Angular, Vue and Astro have upstream examples in
`dotCMS/core` but no branch in this skill. The SDK APIs belong to the `@dotcms/client`,
`@dotcms/react` and `@dotcms/uve` READMEs — these files cover the seam, never the API surface.

## Something is wrong

**First, split the symptom.** Ask the user to view source: is there *any* markup, or is the body
genuinely empty? The two have almost disjoint causes.

**Then establish the delivery mode** — most rows below apply to one mode only.

### Shell renders, content slots are missing

| Mode | Cause | File |
|---|---|---|
| VTL | `template.vtl` isn't looping `$dotThemeLayout` | [vtl/02-themes.md](reference/vtl/02-themes.md) |
| VTL | A hand-rolled `#parseContainer` — it takes three args, and `$container.uuid` is empty because the getter is `getUUID()` | [vtl/02-themes.md](reference/vtl/02-themes.md) |
| VTL | No `<Var>.vtl` for a placed type, or its filename is the wrong case | [vtl/03-containers.md](reference/vtl/03-containers.md) |
| VTL | A VTL error swallowed into an empty string with HTTP 200 | [vtl/05-verify-and-debug.md](reference/vtl/05-verify-and-debug.md) |
| both | An empty `preloop.vtl` or `postloop.vtl` — they must be non-empty | [core/06-containers.md](reference/core/06-containers.md) |
| both | The template's container path doesn't resolve on this site — it must be host-qualified `//<site>/application/containers/<name>/` | [core/06-containers.md](reference/core/06-containers.md) |
| both | Nothing was placed, or placement cleared a slot — placement is a full replacement | [core/09-placement.md](reference/core/09-placement.md) |
| both | Content over the container's `max_contentlets` is **silently dropped** | [core/06-containers.md](reference/core/06-containers.md) |
| headless | The component map has no key for the type, or the case differs | [nextjs/01-component-contract.md](reference/nextjs/01-component-contract.md) |
| headless | No `<Var>.vtl` registration stub, so UVE never offered the type and nothing was ever placed | [core/06-containers.md](reference/core/06-containers.md) |

VTL: [vtl/00-wiring.md](reference/vtl/00-wiring.md) has a missing-thing → what-you-see table.

### Nothing renders at all

| Mode | Cause | File |
|---|---|---|
| both | Site unpublished, page unpublished, or published *before* content and placement so LIVE is stale | [core/00-what-must-exist.md](reference/core/00-what-must-exist.md) |
| both | A layout change without re-publishing the template | [core/05-templates.md](reference/core/05-templates.md) |
| both | The request resolved to a different site than you built on | [core/01-site.md](reference/core/01-site.md) |
| both | Content saved to the wrong site — the type has no Site-or-Folder field, so `contentHost` was ignored silently | [core/02-content-types.md](reference/core/02-content-types.md) |
| headless | The page fetch failed and the route's error branch rendered nothing | [nextjs/05-verify.md](reference/nextjs/05-verify.md) |

### "I added a content type and it doesn't show up"

Three causes look identical in a browser. Check all three:

1. **The `Var` isn't what you think** — read it back from dotCMS rather than assuming it matches
   the type name ([core/02](reference/core/02-content-types.md)).
2. **The renderer isn't registered under that variable** — a `<Var>.vtl` file in VTL
   ([vtl/03](reference/vtl/03-containers.md)), a component-map key in headless
   ([nextjs/01](reference/nextjs/01-component-contract.md)). Case-exact.
3. **The container has no registration stub for the type**, so UVE never offered it and nothing was
   ever placed — [core/06](reference/core/06-containers.md).

A fallback component makes cause 2 **invisible** — no "no component" text reaches the screen, so
don't rule it out on that basis.

### Also worth checking, either mode

| Check | Why |
|---|---|
| **Language / locale** | Content saved under one language and requested under another can render blank. This skill doesn't document dotCMS's resolution behavior — **fetch it and verify** |
| **Page cache** | A stale cached page serves old output; page verification reports this as a cache verdict — [vtl/05](reference/vtl/05-verify-and-debug.md) |
| **Verify properly** | VTL: [vtl/05](reference/vtl/05-verify-and-debug.md) — note `/api/vtl/dynamic` runs outside a request context and cannot see `$CONTENTLETS`, `$dotContentMap`, `$URLMapContent` or `$dotTheme`, so it validates only the parts that don't matter for a container. Headless: [nextjs/05](reference/nextjs/05-verify.md) — page verification does **not** apply |

## Rules that bite

1. **`Var` matching is case-exact.** A container's `<Var>.vtl` filename and a component-map key
   must both equal the type's `Var` exactly — read it back, never assume.
2. **Containers are folders.** There is no create endpoint — you build the folder and its files.
3. **Placement replaces.** Omitted slots are cleared, not left alone.
4. **Publishing is explicit and ordered.** LIVE changes only on publish, and the page publishes
   last — after content, placement, or a template edit.

And one that isn't a build failure but matters: a headless project's
a headless project's `NEXT_PUBLIC_DOTCMS_AUTH_TOKEN` **ships to the browser**. It should belong to a
restricted user, and often doesn't — never assume an existing project's is safe for production. See
[nextjs/00-connect.md §B.1](reference/nextjs/00-connect.md).

## Working through the dotCMS MCP server

The server exposes dotCMS as a **code API you write against**, not a fixed menu of operations.
The normal loop is: **search the spec to discover the endpoint, then write code that calls it.**
That is the general path for anything, not a fallback.

Alongside it, some operations have **purpose-built tools that absorb known traps** — page
creation, content placement, page verification, asset transfer. **Prefer those where they exist**,
not because writing code is worse, but because those tools already handle the failure modes these
files would otherwise have to teach you.

**Enumerate the MCP server's tools and read their descriptions — each description is the source of
truth** for how to call it. These files name no tools, so they don't go stale when one is renamed
or added. What spec search returns is authoritative for anything the curated OpenAPI spec
expresses; these files keep only what it can't. Run raw VTL through `POST /api/vtl/dynamic`.

## Index by build step

If you're working through a full build rather than a single task, the same files are indexed in
dependency order in [reference/README.md](reference/README.md).
