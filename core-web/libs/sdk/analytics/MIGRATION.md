# Migration guide

## The package root is now framework-neutral

**Affects:** anyone importing React APIs from `@dotcms/analytics` directly.

Previously `@dotcms/analytics` and `@dotcms/analytics/react` resolved to exactly the same module — the package root was a single re-export of the React bindings:

```ts
// src/index.ts, before
export * from './lib/react';
```

That meant importing the package at all pulled in `react`, `next/navigation` and `@dotcms/uve`, even for a consumer that only wanted to initialize tracking. It also left the framework-neutral analytics engine — `initializeContentAnalytics`, the plugins, the queue and the trackers — with no entrypoint of its own.

The root now exports only the framework-neutral API. The React bindings stay exactly where they already were, behind the explicit `./react` subpath.

### What to change

If you import `useContentAnalytics` or `DotContentAnalytics` from the package root, add `/react` to the specifier:

```diff
- import { useContentAnalytics, DotContentAnalytics } from '@dotcms/analytics';
+ import { useContentAnalytics, DotContentAnalytics } from '@dotcms/analytics/react';
```

Nothing else changes: the components, hooks and their signatures are untouched, and `@dotcms/analytics/react` already worked before this release, so you can make the change ahead of upgrading.

### What you get for it

A non-React consumer can now use analytics without React in the bundle at all:

```ts
import { initializeContentAnalytics } from '@dotcms/analytics';

const analytics = initializeContentAnalytics({ siteAuth: '…', server: '…' });
analytics?.pageView();
```

### Also in this release

`next` is now declared as an optional peer dependency. The `./react` entry has always imported `next/navigation` (through `useRouterTracker`), but never said so, which left package managers unable to warn about a missing or mismatched Next.js version. It is marked optional because the neutral root does not need it.
