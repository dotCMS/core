# Reference — the build sequence

**Load only the file for the step you're on** — don't read the folder up front.

This file owns the **order**. For a single task or a diagnosis, `SKILL.md`'s intent and symptom
indexes are the better entry point.

## Contents

- [Establish the delivery mode](#establish-the-delivery-mode) — do this before anything else
- [The sequence](#the-sequence) — 11 steps in dependency order
- [Branch notes](#branch-notes)
- [Tools and spec status](#tools-read-once)

## Establish the delivery mode

It changes which files apply, and guessing wrong wastes the build. If you came from
`dotcms-create-sites`, Phase 1 recorded it in PLAN.md §3b; otherwise ask.

| Mode | You build | Branch |
|------|-----------|--------|
| **VTL-rendered** | dotCMS renders the HTML: theme + container VTL | `core/` + `vtl/` |
| **Headless** | dotCMS serves the page; a front-end app renders it | `core/` + a framework branch (`nextjs/`) |

## The sequence

**Dependency order, not preference.** A template's payload names container paths that must
already resolve, plus a theme id in VTL; a page needs the template. So the scaffold is built
bottom-up.

```
Build Progress — copy this and track it:
- [ ]  1. Read the wiring contract (core/00)
- [ ]  2. Choose the mechanism per need — DESIGN ONLY, nothing is built yet
- [ ]  3. Create and publish the SITE — everything below needs its id
- [ ]  4. Create content types — and READ BACK each type's Var
- [ ]  5a. Theme        (VTL only)
- [ ]  5b. Containers   (both modes)
- [ ]  5c. Template     (both modes) — after 5a and 5b resolve
- [ ]  5d. App wiring   (headless only) — after step 4, because map keys are the Vars
- [ ]  6. Create pages — needs the template from 5c
- [ ]  7. Wire listings and detail pages
- [ ]  8. Create content
- [ ]  9. Place content into slots — needs the container from 5b
- [ ] 10. Publish everything, page LAST
- [ ] 11. Verify every page type — then fix and re-verify until all pass
```

**1. The wiring contract** — [core/00-what-must-exist.md](core/00-what-must-exist.md). Four
requirements that hold in both modes. Read this before building.

**2. Choose the mechanism** — design only, and choosing wrong renders empty. VTL: content type
vs widget vs detail page ([vtl/01-choose-mechanism.md](vtl/01-choose-mechanism.md)). Headless:
one component per content type that can appear on a page
([nextjs/01-component-contract.md](nextjs/01-component-contract.md)).

**3. Site** — [core/01-site.md](core/01-site.md). Create and publish it; everything below is
scoped to its id.

**4. Content types** — [core/02-content-types.md](core/02-content-types.md), with their fields.
**Read back each type's `Var`** — everything downstream matches on that exact value.

**5. The rendering scaffold, in this order.** The template's payload names container paths that
must already resolve, plus a theme id if VTL — so the template goes last of the three.

- **5a. Theme** — VTL only. [vtl/00-wiring.md](vtl/00-wiring.md) for the file tree, then
  [vtl/02-themes.md](vtl/02-themes.md). Headless creates no theme.
- **5b. Containers** — [core/06-containers.md](core/06-containers.md), plus
  [vtl/03-containers.md](vtl/03-containers.md) for the markup in VTL mode.
- **5c. Template** — [core/05-templates.md](core/05-templates.md). After 5a and 5b resolve.
- **5d. App wiring** — headless only, and **after step 4**, because the component map's keys are
  the `Var`s read back there. [nextjs/00-connect.md](nextjs/00-connect.md) §B,
  [02-next-config](nextjs/02-next-config.md), [03-routing](nextjs/03-routing.md), and one
  component per content type ([01-component-contract](nextjs/01-component-contract.md)).

**6. Pages** — [core/04-pages.md](core/04-pages.md). Needs the published template from 5c.

**7. Listings and detail pages** — [vtl/04-listings-and-details.md](vtl/04-listings-and-details.md)
or [nextjs/04-listings-and-details.md](nextjs/04-listings-and-details.md). A URL-mapped type needs
`urlMapPattern` and `detailPage` patched onto it **after** its detail page exists.

**8. Content** — [core/03-content.md](core/03-content.md). Fire an action on each contentlet.

**9. Placement** — [core/09-placement.md](core/09-placement.md). Needs the container from 5b.

**10. Publish, page last.** Re-publish the page after content, placement, or a template edit.

**11. Verify, then loop.** VTL: [vtl/05-verify-and-debug.md](vtl/05-verify-and-debug.md).
Headless: [nextjs/05-verify.md](nextjs/05-verify.md). **A failure is not the end of the build:**
classify it, return to the step that owns it, fix, re-publish what depended on it, re-verify.
Repeat until every page type passes.

## Branch notes

**The branches share a spine:** `00` wire up · `01` type↔renderer contract · `02`–`03` mode
plumbing · `04` listings & detail · `05` verify. `02`–`03` differ because the modes do.

**SDK APIs belong to their npm READMEs** — `@dotcms/client`, `@dotcms/react`, `@dotcms/uve` —
and to `examples/nextjs` in `dotCMS/core`. Never restate the API surface here.

**Next.js is the only framework documented.** dotCMS also ships `@dotcms/angular` and
`@dotcms/vue`, with `angular`, `angular-ssr`, `astro` and `vuejs` examples in
`dotCMS/core/examples`. Do **not** apply this branch's routing, `next.config` or `next/image`
guidance to them — only `core/` and the shape (connect → render → component contract) carry over.

## Tools (read once)

Authoring goes through the **dotCMS MCP server**. Enumerate its tools and read their
descriptions — **they are the source of truth**, and they already document their own traps and
parameters. These files don't restate them; they cover order, and the dotCMS behavior that sits
outside any single call.

Run raw VTL through `POST /api/vtl/dynamic`.

## Spec status (read once)

Many traps ship in the curated OpenAPI spec, so what the spec-search tool returns is the source
of truth for them. These files keep what the spec **can't** express: traps on endpoints not in
the spec, and behavioral details no annotation captures.
