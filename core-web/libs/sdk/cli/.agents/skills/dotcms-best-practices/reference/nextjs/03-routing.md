# 03 · Routing

How a Next.js app resolves dotCMS page paths. There is no VTL counterpart to this
file — dotCMS resolves paths itself when it renders.

## The catch-all route

One route resolves every dotCMS page path:

```
src/app/[[...slug]]/page.tsx
const path = slug?.length ? `/${slug.join('/')}` : '/';
```

Every dotCMS-backed route — catch-all or not — repeats the same five steps, and the
order matters:

```tsx
const pageContent = await getDotCMSPage(path);
const insideUVE = isRequestFromUVE(sp);          // @dotcms/uve

// 1. error → inside UVE, still render, passing `graphql`
if (isPageError(pageContent)) {
  if (insideUVE) {
    const { graphql } = pageContent;
    return <Page pageContent={graphql ? { graphql } : undefined} />;
  }
  return <ErrorPage error={{ status: getErrorStatus(pageContent.error) }} />;
}

// 2. vanity URL
const vanityUrl = pageContent.pageAsset?.vanityUrl;
if ((vanityUrl?.action ?? 0) > 200 && vanityUrl?.forwardTo) redirect(vanityUrl.forwardTo);

// 3. genuinely missing
if (!pageContent.pageAsset && !insideUVE) return <NotFound />;

// 4. render
return <Page pageContent={pageContent} />;
```

**The UVE-on-error branch is the non-obvious one.** An unpublished or draft page
fails the normal fetch. Outside UVE that is a real 404; inside the editor it must
still render, so you pass the attempted `graphql` query on to
`useEditableDotCMSPage`, which retries with edit-mode permissions and returns the
draft. Skip this and editors see an error page instead of the content they are editing.

`generateMetadata` fetches the same page again — wrap the fetch in React `cache()` so
both calls share one round-trip.

Wiring listings and URL-mapped detail pages on top of these routes:
[04-listings-and-details.md](04-listings-and-details.md).
