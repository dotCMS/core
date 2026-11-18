# Contracts: dotAI Portlet Rebuild

**Feature**: [../spec.md](../spec.md) | **Data model**: [../data-model.md](../data-model.md) | **Date**: 2026-09-04

Two contract surfaces matter for this feature. **Neither is authored by it.**

1. **[endpoints.md](./endpoints.md)** — the nine existing `/api/v1/ai` endpoints this portlet consumes, verified against the resource classes. This is a *consumer* contract: it records what the server already returns so the frontend services can be tested against it. **No endpoint is added, removed or changed**, so no `@Schema` annotation changes and `openapi.yaml` is not regenerated (Constitution Principle IV).

2. **[frontend-services.md](./frontend-services.md)** — the TypeScript service interfaces the portlet exposes to its own store, and the one conversion each owns. This is the contract the unit tests assert.

## Why an internal Angular lib has contracts at all

Because the boundary that matters here is not HTTP, it is **who converts the wire shape**. Three places in this codebase independently parse `providerConfig` today. The portlet's whole service layer exists to make that exactly one place per shape. Writing the conversions down as a contract is what lets `/speckit-tasks` order the conversion tests before any UI, and what lets a reviewer check that no component reached around a service.

## Not consumed, deliberately

`/ai/text/generate` · `/ai/completions/rawPrompt` · `/ai/search/related` · `/ai/embeddings/count` · `PUT /ai/completions/config` · `/ai/providers*`

These belong to the dotAI **app configuration** screens at `/apps/dotAI/edit/:id`, which this feature does not touch (spec Out of Scope). `/ai/text/generate` stays with `DotAiContentService` for its existing block-editor callers.
