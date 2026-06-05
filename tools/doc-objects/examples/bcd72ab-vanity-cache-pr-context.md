# Example: Real PR context for doc-object quality testing
# Source: dotCMS/core PR #35704 — fix(page-cache): vanity URL cache key collision
# Hand-written reference doc object: doc-object-git-idea/samples/bcd72ab-fix-page-cache-vanity-collision.md
#
# This file exists to give the doc-object generation workflow a substantive
# real-world PR to analyze — a high-severity backend bugfix with a customer
# report, a linked issue, and a security-adjacent classification decision.

## PR #35704 — fix(page-cache): include original request URI in cache key for vanity URL 200-forwards

**Type:** Bug fix — high severity, multi-tenant content bleed  
**Module:** page-cache / VelocityLiveMode  
**Customer report:** Freshdesk #37004  
**Related PR:** #34879 (same collision class, different code path)

### What was wrong

`VelocityLiveMode.buildCacheParameters()` composed the cache key from `pageUrl`,
`pageInode`, and `vanityUrlId` — none of which differ between `/store/123/acme/catalog/`
and `/store/456/globex/catalog/` when both URLs are forwarded to the same detail page
via the same vanity URL rule. First request warms the cache; every subsequent request
receives that cached response regardless of which URL was actually requested.

On multi-node clusters with round-robin load balancing, different nodes cached different
affiliates' content under the same key — wrong content served on every other request for
up to the full 1-hour TTL.

### The fix

When `VANITY_URL_OBJECT` is present and `isForward() == true`, include
`RequestDispatcher.FORWARD_REQUEST_URI` (the original browser URL) as an `originalUri:`
component in the cache key. Non-vanity pages and 301/302 redirects are unaffected.

```java
String originalRequestUri = (request.getAttribute(VANITY_URL_OBJECT) != null
        && ((CachedVanityUrl) request.getAttribute(VANITY_URL_OBJECT)).isForward())
        ? (String) request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI)
        : null;
```

### Why this is security-adjacent

The impact — Tenant A sees Tenant B's content — is a data isolation failure. Not a
traditional CVE, but phrasing in release notes should be coordinated with support/security.
PR #34879 addressed the same collision class for URL-mapped contentlets; this PR closes
the vanity-URL 200-forward path that #34879 did not cover.

### Diff summary

- `VelocityLiveMode.java`: visibility change on `buildCacheParameters()` (private → package-private,
  for testability); `originalUri:` component added to cache key when forwarding via vanity URL
- `VelocityLiveModeTest.java`: new integration test
  `vanityForwardDifferentOriginalUriProducesDifferentCacheKeys` verifying different incoming
  URLs produce different cache keys
