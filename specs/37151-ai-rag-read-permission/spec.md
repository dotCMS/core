# Issue Resolution Specification: dotAI: align RAG retrieval with the READ permission model (search + completions)

**Feature Branch**: `37151-ai-rag-read-permission`

**Created**: 2026-10-02

**Status**: Draft

**Type**: Issue / Bug Resolution

**Related GitHub Issue**: [dotCMS/core#37151](https://github.com/dotCMS/core/issues/37151) — part of epic [dotCMS/core#37255](https://github.com/dotCMS/core/issues/37255) (dotAI Security Hardening)

**Input**: User description: "37151"

<!--
  This is the dotCMS ISSUE-RESOLUTION spec (used by /speckit-specify-fix). Unlike the
  feature spec, it is framed around a defect: what is wrong, how to reproduce it, and how
  we will know it is fixed. It still flows into /speckit-plan, where the Legacy Impact and
  ADR Alignment gates apply. Keep this technology-light — root-cause and fix details are
  refined in the plan.
-->

## Clarifications

### Session 2026-10-02

- Q: How should retrieval treat a request with no user? → A: Check it as the Anonymous user,
  with no bypass (FR-006). The issue's AC proposed "no user means no check" for internal
  automation, but no retrieval caller is internal automation. The only no-user callers are the
  viewtools on anonymous pages, which is exactly the case the fix must cover. Internal callers
  that need everything pass the system user.
- Q: When filtering drops chunks, should the page still be filled to `limit`? → A: Yes. The
  first answer was "no, accept shorter pages". It was reversed after spec review: a few long
  restricted documents can fill the whole page, and the caller then gets "no matching
  content" even though readable matches rank lower. Retrieval now filters first and applies
  `offset`/`limit` to the readable results, over a capped set of candidates, in one query
  (FR-008, FR-012).
- Q: What does `count` report? → A: The distinct readable content items in the response
  (FR-009). An unfiltered count would reveal that restricted matches exist, and a filtered
  count over every match would need a permission check over the whole index on every request.
- Q: Which permission check does the filter use? → A: The per-item check that `find` and
  content search already use, not the batch SQL check (FR-001). The batch check skips the
  live-only rule for anonymous and front-end users, which matters because the embeddings
  workflow actionlet can embed draft versions. It also skips the owner rule, and it can drop
  readable content because it depends on a lazily rebuilt reference table.
- Q: What happens if the content or permission lookup fails? → A: The error propagates and
  nothing is returned (FR-011). Returning an empty result would show "no matching content"
  and hide a real failure.

## Problem Statement *(mandatory)*

dotAI answers search and completion requests from its embeddings index: chunks of text taken
from content an administrator chose to embed. When it retrieves chunks for a request, it
narrows them by site, content type, index name and language. It does not check whether the
caller has READ permission on the content each chunk came from.

Two things follow:

- **Search** returns titles and text snippets from content the caller cannot open anywhere
  else in dotCMS. This covers `/api/v1/ai/search` (GET and POST), `/api/v1/ai/search/related`
  (GET and POST), and the `$ai.search` viewtool (`query`, `related`).
- **Completions** places those chunks in the prompt sent to the AI model, so the generated
  answer can restate content the caller cannot read, even when no snippet is shown. This covers
  `/api/v1/ai/completions` (streaming and non-streaming) and `$ai.completions.summarize`.

Everywhere else, dotCMS content retrieval (`find`, content search, GraphQL, the page API)
returns only what the caller can READ. Embedding a content type makes it available to AI
features for the users who may read it. It does not grant read access to everyone who can
call those features.

**Severity / Impact**: High (P1) per the issue. Any authenticated user, front-end users
included, can call the REST endpoints. Visitors to a page that uses the viewtools are affected
too. The exposure exists wherever an embedded content type, or individual content in it, has
READ narrower than "everyone who can call dotAI". Nothing is modified and no privileges are
gained; the impact is disclosure of content.

## Reproduction *(mandatory)*

**Environment**: `main` as of 2026-10-02. Any dotCMS instance with the dotAI app configured,
an embeddings index populated, and a provider reachable (a mock provider is enough).

**Steps to Reproduce**:

1. As an admin, create a content type whose content only a specific role can READ. Create and
   publish content in it containing a distinctive phrase.
2. Embed that content into the `default` index (content-type indexing config, the embeddings
   workflow actionlet, or `POST /api/v1/ai/embeddings`).
3. Create a back-end or front-end user who does **not** hold that role and log in as them.
4. Call `GET /api/v1/ai/search?query=<distinctive phrase>`.
5. Call `POST /api/v1/ai/completions` with `{"prompt": "<distinctive phrase>"}`.
6. Render a page as an anonymous visitor that calls `$ai.search.query("<distinctive phrase>")`
   and `$ai.completions.summarize("<distinctive phrase>")`.

**Expected Behavior**: Search returns no result for the restricted content: it does not appear
in `dotCMSResults`, and no title or snippet from it appears anywhere in the response.
Completions returns the existing `no matching content found in the index for your query` error
when nothing the caller can read matches. If other, readable content matches, the answer is
built only from that content. A user who **does** hold the role gets the restricted content in
both.

**Actual Behavior**: Search returns the restricted content in `dotCMSResults`, with title and
`matches[].extractedText` snippets. Completions sends the restricted chunks to the model and
returns an answer drawn from them, with the restricted content listed in `dotCMSResults`. The
viewtools behave the same way for anonymous visitors.

**Reproducibility**: Always, whenever restricted content is in the index and matches the query.

## Scope of Investigation *(mandatory)*

- **Affected area**: dotAI retrieval. Covers the REST endpoints `/api/v1/ai/search`,
  `/api/v1/ai/search/related` and `/api/v1/ai/completions` (the `summarize` path), and the
  viewtools `$ai.search` and `$ai.completions.summarize`. All of them obtain chunks through a
  single retrieval method in the embeddings API, which is where the issue requires the fix.
  The raw-prompt, text and image endpoints do not retrieve from the index and are not affected.
- **Suspected surface**: Modern, `com.dotcms.ai.*` (embeddings API, the two viewtools, the
  search and completions resources). Permission checks are done by the existing permission and
  content APIs (`com.dotmarketing.business.PermissionAPI`, `ContentletAPI`), which are called
  but not modified. Legacy impact is expected to be nil; the plan confirms.
- **Related known decisions**: dotCMS's READ model is inheritance-based (individual
  permissions, then folder, content type, site), with an owner rule. Anonymous and front-end
  roles can read only live content. The filter must reach the same READ decision as content
  search and `find`. The plan formally consults `dotCMS/platform-adrs`.

## Root-Cause Hypothesis

Retrieval is a similarity query against the `dot_embeddings` table, which sits on its own
pgvector connection. The query's filters cover inode, identifier, exclusions, language, content
type, site and index name. There is no user or permission condition, and nothing after the
query applies one. The caller's user is carried on the request object but only reaches the
display step: there, each matched content item is loaded with the caller's permissions. If
that load fails (for example because the caller lacks READ), the item's entry is left empty.
The chunk's stored title is then added to it, and its snippet text is added regardless. The
prompt built for completions uses the raw chunk list and never reaches that step.

The permission data lives in the main database and resolves through inheritance, so a SQL join
from the embeddings table is not practical. The fix filters the chunk list after the query,
keyed on the inode each chunk carries, before any consumer sees the list. Today the database
applies `limit` and `offset`, so a filter added after the query would page over unreadable
chunks too. Paging therefore moves after the filter. That is affordable because the table
has no vector index (the HNSW index is commented out and the IVFFlat definition is never
used), so every query already computes the distance for every row. Fetching more rows only
adds transfer, not database work.

**User resolution today**: The REST resources always resolve a user, and an unauthenticated
call is rejected with 401 before retrieval, because none of them opt into anonymous access.
The viewtools resolve the user from the request, which yields **no user** for an anonymous
visitor. No other retrieval caller passes no user. The platform's permission filter already
checks a missing user as the Anonymous user.

## Fix Scope & Non-Goals *(mandatory)*

**In scope**:

- **FR-001**: Retrieval returns only chunks whose source content the caller has READ permission
  on, using `respectFrontendRoles = true`. The filter uses the per-item
  `PermissionAPI.filterCollection(List, PERMISSION_READ, true, user)`, the same decision
  `ContentletAPI.find(inode, user, true)` makes today when AI results are displayed. It
  covers inheritance, the owner rule, live-only content for anonymous and front-end users,
  and category permissions. The batch `filterCollection(Collection, …, user, boolean)` must
  not be used: it skips the live-only and owner rules, so anonymous visitors would get draft
  text that the embeddings workflow actionlet can embed. It also depends on a lazily rebuilt
  reference table, so it can drop readable content.
- **FR-002**: Chunks with no content behind them are dropped: deleted content, inodes that
  cannot be loaded, and the cache rows the embeddings table holds for past query text
  (`inode = 'cache'`). The filter fails closed.
- **FR-003**: Search, related, completions (`summarize` and `summarizeStream`) and both
  viewtools inherit the filter by going through the single retrieval method,
  `EmbeddingsAPI.getEmbeddingResults`. No consumer performs a separate check, and no consumer
  reads unfiltered chunks. The method's Javadoc states the filter as part of the
  `EmbeddingsAPI` contract, so alternative `EmbeddingsAPI` providers must apply it too.
- **FR-004**: Completions uses the filtered chunks both for the prompt sent to the model and for
  the `dotCMSResults` it returns.
- **FR-005**: When filtering leaves no chunks, completions returns the existing
  `no matching content found in the index for your query` error and search returns an empty
  `dotCMSResults`. Neither response reveals that restricted matches existed.
- **FR-006**: A request with no user is checked as the Anonymous user, so anonymous viewtool
  callers see only content the Anonymous role can READ (live content only). There is no
  "no user means no check" bypass. A caller that needs unrestricted retrieval passes the
  system user, which the permission API already lets read everything.
- **FR-007**: The REST search and completions resources always pass a non-null user into
  retrieval. They already reject unauthenticated callers with 401, and FR-006 makes a null
  user safe regardless.
- **FR-008**: `offset` and `limit` apply to the chunks the caller can read, after filtering.
  A page holds `limit` readable chunks whenever enough readable matches exist within the
  candidate cap (FR-012). Readable matches ranked below restricted ones are still returned.
  Each caller's ranking is the same on every request, so consecutive pages have no gaps or
  duplicates. Chunks at equal distance are ordered by row id so that order is stable.
  `limit` still counts chunks, not content items.
- **FR-009**: The response `count` field reports the number of distinct content items in
  that response, which the caller can READ, matching `dotCMSResults`. It no longer reports an
  index-wide count of every match, because a count that includes unreadable content would
  tell a restricted caller that restricted matches exist (contradicting FR-005). The
  retrieval API's Javadoc documents the change.
- **FR-010**: The in-memory embedding cache keeps keying on hashed text → embedding vector.
  A code comment there states that it holds no result sets and that caching results in future
  needs a per-user dimension.
- **FR-011**: If loading the content or checking permissions throws, the error propagates and
  retrieval returns nothing. The REST resources then return an error response, and the
  viewtools return the generic error payload.
- **FR-012**: For a caller other than a CMS admin or the system user, retrieval runs **one**
  embeddings query with `offset 0` and `limit = max(offset + limit, cap)`. The cap is a
  configurable property, default 1000, which is today's default page size for GET `/search`.
  Candidates are filtered in rank order, in batches of distinct content items. Each batch is
  one content load plus one per-item permission check (FR-001). Filtering stops as soon as
  `offset + limit` readable chunks are collected, and retrieval returns that slice. It never
  runs a second embeddings query.
- **FR-013**: CMS admins and the system user keep today's query, with `offset` and `limit`
  applied in the database, because the permission check returns everything for them.

**Explicitly out of scope / non-goals**:

- Per-user caching of search results.
- Prompt-injection mitigation in completions (#37152).
- Escaping viewtool output (#37153).
- Changing the embeddings table, its indexes, or what gets embedded and when.
- A cursor-based paging API, a `hasMore`/`nextOffset` field, and running further embeddings
  queries to fill a page beyond the candidate cap.
- Capping `searchLimit`. Request cost and rate limits belong to the epic's rate-limiting
  work.
- Permission checks on indexing, `deleteByQuery`, `countEmbeddingsByIndex`, the embeddings
  admin endpoints, raw prompt, text or image generation. These are unchanged.
- The embeddings count endpoint (`/api/v1/ai/embeddings/count`) keeps returning an
  unfiltered count. It is a known residual risk: it is limited to back-end users and returns
  only a number.
- Changes to the permission or content APIs themselves.

## Regression Risk *(mandatory)*

- **Blast radius**: Every AI search, related and completions call through REST and the
  viewtools. Users without READ on embedded content will get fewer results and different AI
  answers. That is the intent, and it needs a release note. CMS admins and the system user are
  unaffected, because the permission API already lets them read everything. The largest
  visible change is for headless clients using an API token: they now get only what the
  token's user can READ, so the release note tells customers to grant that user READ on the
  embedded content types. Indexing, deletion, index counts and the embeddings admin endpoints
  do not go through retrieval and must not change. No caller outside dotAI retrieval uses
  the changed method.
- **Restricted matches beyond the cap**: a page comes back short only when the readable
  matches lie beyond the first `max(offset + limit, cap)` candidates. That needs more than
  1000 restricted chunks ranked above them by default. It is an accepted, known limitation,
  and raising the cap property reaches further.
- **Performance**: There is still one embeddings query per request. For a non-admin caller,
  it returns up to the cap (1000 by default) instead of `limit` rows: completions and the
  viewtools go from 50 to at most 1000 rows, while GET `/search` stays at 1000. The database
  work is unchanged, because it already computes the distance for every row. The extra cost
  is transferring rows (about 512 tokens of text each) plus loading content and checking
  permission per batch of candidates, both cache-backed. Because filtering stops once the
  page is full, a caller who can read most matches pays for about one batch. The worst case
  is a caller who can read almost nothing, who pays for checking every candidate up to the
  cap. Admins and the system user add no cost (FR-013). The separate count query over all
  matches is no longer needed (FR-009). Building the response still runs only over the
  returned page. The per-item check logs a warning each time a front-end user is denied a
  draft, which happens only where the workflow actionlet embeds drafts.
- **Backward compatibility**: Response shapes are unchanged. `total` reflects the chunks
  actually returned after filtering. These semantic changes need a release note:
  - `offset` and `limit` now count readable chunks, not all matching chunks (FR-008). At
    most the cap's worth of matching chunks is considered per request (FR-012);
  - `count` is now per response, not index-wide (FR-009). In this repo, only the dot-ai
    portlet's search tab reads it, and only to display "N results". Nothing pages with it.
    The JS SDK declares the field in its response type but does not read it;
  - code (plugins included) that calls retrieval with no user now gets only what Anonymous
    can READ, and must pass the system user to keep unrestricted results (FR-006).

  The public JS SDK types document `limit`, `offset` and `count` only generically, so no SDK
  change is needed. No DB schema, ES mapping or serialized state changes, so the change is
  rollback-safe.
- **Data considerations**: None. Stale embedding rows for deleted content are dropped at read
  time and are not removed from the table.

## Acceptance & Verification *(mandatory)*

- **AC-001**: Search, with readable and restricted content both matching:
  - for the user without the role, `dotCMSResults` holds only the readable content, no
    restricted title or snippet appears anywhere in the response, `count` equals the number
    of items in `dotCMSResults`, and `total` equals the number of chunks returned;
  - for a user with the role, the restricted content is returned too.
- **AC-002**: Completions, with readable and restricted content both matching: for the user
  without the role, the request the mock provider receives contains the readable text and
  not the restricted text.
- **AC-003**: Completions, with only restricted content matching: for the user without the
  role, the response is `no matching content found in the index for your query`, and the
  provider receives no chat request.
- **AC-004**: On an anonymous page, `$ai.search.query` and `$ai.completions.summarize` return
  only published content the Anonymous role can READ.
- **AC-005**: Given chunks from readable, unreadable and missing content (including a cache
  row), the retrieval filter keeps only the readable ones, in their original distance order.
  With no user, the filter still runs: the request is passed to the permission check as is,
  never skipped. Given more readable chunks than `offset + limit`, the filter stops checking
  once it has collected that many, and returns the slice `[offset, offset + limit)`.
- **AC-006**: Indexing, `deleteByQuery` and `countEmbeddingsByIndex` behave as before.
- **AC-007**: Completions, where restricted chunks outrank readable ones and number more than
  `searchLimit`: for the user without the role, the answer is built from the readable
  content, and the response is not `no matching content`. A search with the same setup
  returns a full page of `searchLimit` readable chunks.
- **Verification method** (each AC once; related, the owner and live-only rules and the
  admin shortcut are not tested separately, because they come from the shared retrieval path
  and the unchanged permission API):
  - **Unit**: the retrieval filter, extracted into a small class with injected content and
    permission APIs (AC-005).
  - **Integration, REST**: one new AI REST integration test class using the existing WireMock
    provider setup in `AiTest`, real permissions on a restricted content type, and fixed
    embedding vectors so ordering is deterministic (AC-001 to AC-003, AC-007). Register it at
    the end of `MainSuite2b`'s `@SuiteClasses`, where the AI integration tests share the
    WireMock port and run sequentially.
  - **Integration, viewtool**: `SearchToolTest` and `CompletionsToolTest` currently seed rows
    with fake inodes, which the filter now drops. They move to real, published,
    anonymous-readable content, with one anonymous assertion added (AC-004). #37153's open
    implementation PR (#37756) seeds fake rows the same way, including a cache-index test.
    Whichever of the two merges second updates those fixtures.
  - **Manual performance check**: on an index with more than 1000 matching chunks, as a
    non-admin user, before and after the change, time one search and one completion. Run it
    once with a user who can read almost nothing, which is the worst case. Record the
    response times in PR 2. This is not an automated test.
  - **Regression**: existing `EmbeddingContentListenerTest`, `BulkEmbeddingsRunnerTest` and
    `EmbeddingsToolTest` (AC-006). Run with
    `-Dmaven.build.cache.enabled=false -Dit.test.forkcount=1`, and confirm `Tests run:` in the
    failsafe reports.

## Assumptions

- Permission is decided per content item via the inode on each chunk. All chunks of one inode
  share that item's permissions.
- `respectFrontendRoles = true` is used for every caller, as the issue's AC specifies,
  including back-end users.
- CMS admins and the system user keep unrestricted retrieval, through the permission API's
  existing bypass.
- Customer-facing release notes about reduced AI recall are owned by the epic, not this spec.
