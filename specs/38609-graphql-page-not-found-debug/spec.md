# Issue Resolution Specification: GraphQL page-not-found diagnostics at DEBUG

**Feature Branch**: `issue-38609-graphql-page-not-found-debug-spec`

**Created**: 2026-11-16

**Status**: Draft — awaiting spec PR approval

**Type**: Issue / Bug Resolution

**Related GitHub Issue**: [#38609](https://github.com/dotCMS/core/issues/38609)

**Input**: Repeated GraphQL page-not-found errors flood the server log; move these diagnostics to DEBUG.

## Problem Statement *(mandatory)*

Requests for nonexistent GraphQL pages produce repeated ERROR/WARN entries and stack traces for an expected not-found result. This makes operational logs noisy and obscures actionable failures.

**Severity / Impact**: Repeated missing-page requests create continuous log noise for server operators. This report does not establish an availability failure.

## Reproduction *(mandatory)*

**Environment**: A dotCMS server exposing GraphQL with normal logging enabled. The reported deployment version is unspecified; current main contains the reported logging paths. Supplied production logs are the reproduction evidence; no local runtime reproduction has been performed.

**Steps to Reproduce**:

1. Choose a site and a page URL that does not exist, such as `/meta.json`.
2. Execute a GraphQL `page` query for that URL using the site's normal request context.
3. Repeat the request and inspect both the GraphQL response and server logs.

**Expected Behavior**: The response retains the existing structured not-found error. Missing-page diagnostics and their stack traces are available only when DEBUG logging is enabled.

**Actual Behavior**: `PageDataFetcher` logs `Page not found: /meta.json` at ERROR, including an `HTMLPageAssetNotFoundException` stack trace. Its outer catch logs another ERROR, and GraphQL's `SimpleDataFetcherExceptionHandler` logs a WARN with a `ResourceNotFoundException` stack trace.

**Reproducibility**: Reported repeatedly in production; the affected exception path is present in current main. Requires a nonexistent page, not a malformed query or permission failure.

## Scope of Investigation *(mandatory)*

- **Affected area**: GraphQL page lookup and exception logging during query execution.
- **Suspected surface**: Modern `com.dotcms.graphql` code and the GraphQL library's default exception handler. Page lookup delegates to the active legacy `com.dotmarketing.*` rendering API.
- **Related known decisions**: Structured GraphQL page errors introduced by [#35351](https://github.com/dotCMS/core/issues/35351) must remain compatible. Relevant architectural decisions will be consulted during planning; no ADR alignment is claimed here.

## Root-Cause Hypothesis

An expected page-not-found outcome is logged at multiple exception-handling layers, each treating it as an operational failure. Lowering only the first log statement would leave both the outer ERROR and GraphQL's WARN. Planning must confirm how to lower only the expected missing-page diagnostics while retaining error propagation and normal handling of other exceptions.

## Fix Scope & Non-Goals *(mandatory)*

**In scope**:

- Make diagnostics for a missing GraphQL page DEBUG-only across the page lookup and GraphQL exception-handling paths.
- Preserve the existing not-found response, including its message, error classification, path, locations, and extension metadata.
- Keep unexpected failures and permission failures on their existing logging and response paths.

**Explicitly out of scope / non-goals**:

- Changing `FileViewStrategy` warnings about empty `ogImage` identifiers.
- Silencing GraphQL warnings globally, or lowering other exception categories.
- Changing HTTP status behavior, permissions, page resolution, vanity URL behavior, or SDK handling.
- Adding configuration flags, dependencies, rate limits, migrations, or rewriting legacy rendering code.

## Regression Risk *(mandatory)*

- **Blast radius**: A shared GraphQL handler may affect queries beyond `page`; unexpected exceptions must continue to be reported normally, including when thrown by another fetcher.
- **Backward compatibility**: Existing clients rely on the structured error response. Preserve `code: NOT_FOUND`, `status: 404`, `resourceType: Page`, and the requested/resolved `resourceId`, as well as existing message/path/location behavior. No public API contract change is intended.
- **Data considerations**: No content writes, data repair, database changes, or search-index changes are required.

## Acceptance & Verification *(mandatory)*

- **AC-001**: Repeating a missing-page query at normal logging levels produces no missing-page ERROR/WARN entries or associated stack traces from the page fetcher or GraphQL exception handler.
- **AC-002**: Enabling DEBUG logging exposes a diagnostic identifying the missing page.
- **AC-003**: The GraphQL response for the same missing-page query is unchanged, including message, error classification, path, locations, and not-found extensions.
- **AC-004**: An unexpected exception still uses the existing non-DEBUG logging path and preserves its response behavior. Permission failures are not downgraded by this change.
- **AC-005**: Existing-page queries still succeed, and unrelated empty-image warnings remain unchanged.
- **Verification method**: After spec approval and explicit authorization to write/run tests, propose at most three focused regression tests: (1) a missing page stays DEBUG-only with an unchanged response, (2) an unexpected fetcher exception retains normal logging, and (3) an existing page still resolves. Follow the repository's developer-approval and failing-test gates before implementation. Also repeat the reproduction manually with normal and DEBUG logging and compare responses. No tests are created or run in this spec-only PR.

## Assumptions

- The request concerns expected GraphQL page-not-found outcomes, not the separate empty-image warnings shown in the same log excerpt.
- DEBUG-only logging must not turn a not-found response into a successful page result or hide unexpected server failures.
- The spec PR requires another developer's approval before planning and implementation; approval is not inferred from the original request.
