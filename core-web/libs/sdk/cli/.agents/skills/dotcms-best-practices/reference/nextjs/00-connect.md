# 00 · Next.js: connect and render

> **This branch documents the seam only.** The SDK APIs are owned by their own
> READMEs on npm — show the basics here, link for the detail, never restate the API
> surface. `@dotcms/client` · `@dotcms/react` · `@dotcms/uve`.
> Working reference app: `examples/nextjs` in `dotCMS/core`.

In addition to [core/00](../core/00-what-must-exist.md), a headless page needs a
React component registered for every content type it renders.

**Two halves.** §A is the dotCMS side — always yours, nothing else does it. §B is the app
wiring. **Check the repo before you write any of it**: a dotCMS Next.js app may already
have all five pieces, some of them, or none.

## Contents

- [A. What headless changes on the dotCMS side](#a-what-headless-changes-on-the-dotcms-side) — no theme, containers without markup
- [B. The app wiring](#b-the-app-wiring) — env, client, render

---

## A. What headless changes on the dotCMS side

The SDK fetches a dotCMS page and renders it — it does not replace it. So the whole of `core/` still applies: site, content types, content,
pages, template, containers, placement, publish. What changes is that **nothing in dotCMS
produces HTML**, which strips two things out of the scaffold:

| Scaffold piece | VTL-rendered | Headless |
|---|---|---|
| **Theme** | create the folder, author `template.vtl` + partials | **don't create one.** Omit `theme` from the template POST; the server assigns `SYSTEM_THEME` ([core/05](../core/05-templates.md)) |
| **Template layout** | required | **required** — the SDK reads `layout.body.rows` |
| **Container folder** | required | **required** — it's the slot content is placed into |
| **`container.vtl` / `preloop` / `postloop`** | required | **required**, same as VTL ([core/06](../core/06-containers.md)) |
| **Per-type `<Var>.vtl`** | the actual markup | **a comment-only registration stub** — the filename registers the type so the page editor and UVE offer it to an author; React does the rendering |

Everything else in `vtl/` is VTL-rendering mechanics and does not apply.

---

## B. The app wiring

Five roles make up the wiring. **Filenames vary between projects** — what matters is that
each role is filled once, centrally. Read the repo first and work out which already exist;
recreating one that's there gives you two clients or two component maps, and the bug that
follows is hard to see.

| Role | What it does |
|---|---|
| **Config** | the env values, read once and exported |
| **Client** | a single `createDotCMSClient` instance |
| **Catch-all route** | maps a URL to a dotCMS page and renders it |
| **Component map** | the type's `Var` → React component, plus a fallback for unmapped types |
| **`next.config`** | four dotCMS-specific settings — see [02](02-next-config.md) |

**How to tell what's already there:**

| Look for | Signal |
|---|---|
| `@dotcms/client`, `@dotcms/react`, `@dotcms/uve` in `package.json` | the app is already a dotCMS front end |
| a `createDotCMSClient(...)` call | the client exists — import it, don't make a second |
| a components object passed to `DotCMSLayoutBody` | the map exists — add keys to it |
| `NEXT_PUBLIC_DOTCMS_*` in `.env*` | config exists — read the values, don't overwrite the file |
| `reactStrictMode: false` plus a `/dA/` rewrite in `next.config` | the dotCMS settings are in place |
| a catch-all route such as `app/[[...slug]]` | routing exists |

Fill only the gaps. In a working app the usual job is **adding to the component map**: write
the component and register it under the content type's `Var`, case-exact —
[01](01-component-contract.md).

### 1. Configure

Three values at minimum, centralised rather than read from `process.env` at each call site:

```ts
NEXT_PUBLIC_DOTCMS_HOST       // instance URL
NEXT_PUBLIC_DOTCMS_AUTH_TOKEN // API token — see the warning below
NEXT_PUBLIC_DOTCMS_SITE_ID    // the site you built
```

A project may carry more than these — read what's there rather than assuming this list is
complete.

**The token ships to the browser.** `NEXT_PUBLIC_*` inlines a value into the client
bundle. That is deliberate — the UVE bridge runs client-side and needs it.

So the right token here is one minted for a **restricted user** — anyone who loads the site
can read it out of the JavaScript.

**Don't assume an existing project's token is restricted.** Tokens are commonly minted from
whatever admin credentials were at hand, which puts full permissions in a public bundle. If
you can't confirm the token belongs to a restricted user, treat it as development-only and
say so before anything ships.

### 2. Connect

```ts
import { createDotCMSClient } from '@dotcms/client';

export const dotCMSClient = createDotCMSClient({
  dotcmsUrl: dotCMSHost,
  authToken: dotCMSAuthToken,
  siteId: dotCMSSiteId,
  // UVE needs fresh data so in-context edits appear immediately
  requestOptions: { cache: 'no-cache' },
});
```

Fetch a page with `dotCMSClient.page.get(path)`. Options, GraphQL enrichment,
collections and Lucene queries: **@dotcms/client README → How-to Guides**.

### 3. Render

```tsx
'use client';
import { DotCMSLayoutBody, useEditableDotCMSPage } from '@dotcms/react';

export function Page({ pageContent }) {
  const { pageAsset } = useEditableDotCMSPage(pageContent) ?? {};
  return <DotCMSLayoutBody page={pageAsset} components={pageComponents} mode={mode} />;
}
```

`useEditableDotCMSPage` is what makes the page editable inside UVE; `DotCMSLayoutBody`
walks `layout.body.rows` and renders each contentlet through your component map.
Props, editable fields and block-editor rendering: **@dotcms/react README → SDK Reference**.

---

Next, in order: the component contract ([01](01-component-contract.md)), `next.config`
([02](02-next-config.md)), then routing ([03](03-routing.md)).
