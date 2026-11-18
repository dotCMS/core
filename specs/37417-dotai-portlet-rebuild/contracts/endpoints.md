# Consumed endpoint contract

Every endpoint below **already ships**. This records the shapes as verified in the resource classes so the frontend service specs can assert against them. Nothing here is added or modified by this feature.

Base path: `/api/v1/ai` (`AI_API_ENDPOINT`, shared by all five services rather than repeated as a literal).

| # | Verb | Path | Auth | Used by |
|---|---|---|---|---|
| 1 | POST | `/search` | backend + frontend user | Search tab |
| 2 | POST | `/completions` (`stream: true`) | backend + frontend user | Chat tab |
| 3 | POST | `/image/generate` | backend + frontend user | Image tab |
| 4 | POST | `/api/v1/workflow/actions/default/fire/PUBLISH` | standard | Image → Save |
| 5 | GET | `/embeddings/indexCount` | **`CMS_ADMINISTRATOR_ROLE`** | Embeddings table + settings index picker |
| 6 | POST | `/embeddings` | backend user | New Index dialog (add mode) |
| 7 | DELETE | `/embeddings` | backend user | Delete index / delete from index |
| 8 | DELETE | `/embeddings/db` | **`CMS_ADMINISTRATOR_ROLE`** | Rebuild DB |
| 9 | GET | `/completions/config` | backend user | Config Values + the configured gate |

**No endpoint in `com.dotcms.ai.rest` checks license level.** A `grep -riE "license"` over the whole `com/dotcms/ai/` package returns zero hits. This is why the route uses no `DotEnterpriseLicenseResolver`, unlike `es-search` and `velocity-playground`.

**Exactly two endpoints are role-gated** (`.requiredRoles(Role.CMS_ADMINISTRATOR_ROLE)`): #5 and #8. This is the entire source of the dead end FR-049 closes — a user whose layout carries `api_playground` gets the portlet, but a non-administrator gets 403 on #5, which empties both the Embeddings table *and* the index picker, silently disabling Search and Chat.

---

## 1. `POST /search` — semantic search

**Request**: a `CompletionsForm` **body** (not query params — the SDK's `GET` variant uses query params, which is why the SDK types are not reused).

| Field | Default | Constraint |
|---|---|---|
| `prompt` | — | 1..4096. Not `@NotNull`; "required" is enforced imperatively and a blank returns `400 {error: "query required"}` |
| `searchLimit` | `50` | 1..1000 |
| `searchOffset` | `0` | ≥ 0 |
| `indexName` | `"default"` | |
| `threshold` | `.25` | |
| `operator` | `"cosine"` | **only** `distance` \| `cosine` \| `innerProduct` — anything else silently becomes cosine |
| `temperature` | provider config | 0..2 |
| `responseLengthTokens` | `0` | declared `@Min(128)` |
| `contentType` | — | comma-split on `\s?,\s?` |
| `site`, `model`, `language`, `stream`, `fieldVar` | | |

⚠️ **Bean validation is decorative.** There is no `@Valid` anywhere in the package; form params are plain method arguments. So `@Min(128)` is not enforced — and the builder's own default for that field is `0`, violating its own annotation. The client is the only place the declared 128 minimum can be honored (FR-023).

**Response**: `{ timeToEmbeddings, total, query, threshold, dotCMSResults[], operator, offset, limit, count }`. Each `dotCMSResults` entry is a **full contentlet JSON** plus a forced `title` and `matches[{distance, extractedText}]`.

⚠️ One server fallback path emits only `{inode, identifier, title, language, index, contentType}` — **no `modDate`**. Guard it (FR-008, US1 scenario 3).

**Error**: `404 {error: "Index 'x' not found"}` — must surface as a distinguishable error naming the index, not a generic failure.

## 2. `POST /completions` with `stream: true`

Same `CompletionsForm` body. Returns a `StreamingOutput` of bare `data:` lines:

```
data: {"choices":[{"delta":{"content":"…"}}]}
data: [DONE]
```

**Parsing rules the service must implement** (all covered by its spec):

- A JSON object **can be split across two chunks** — a failed `JSON.parse` must retain the fragment and prepend it to the next chunk.
- Normalize `\r\n` → `\n` across the whole buffer, so a CRLF pair split across two reads still resolves.
- Skip blank lines and `:comment` lines.
- `[DONE]` completes the observable.
- Flush any trailing frame after the stream ends.
- `AbortController.abort()` on teardown, so unsubscribing genuinely cancels (FR-012, FR-015).

⚠️ These are **bare** `data:` frames with no event name. `DotAgentRunService` parses **named** frames (`event:` + `data:`) onto a closed union and drops unknown names — it would drop every dotAI frame. Its technique is reused; the service is not.

⚠️ Reading a stream with `fetch` bypasses Angular's `HttpClient` interceptor chain. Same-origin with credentials, as the legacy screen relied on. Inherent to the technique.

## 3–4. Image generate, then publish

`POST /image/generate` → `{ tempFileName, response, tempFile, originalPrompt, revised_prompt, url }`. The asset is at `/dA/{response}/asset.png`.

Publishing is a **separate** call to `POST /api/v1/workflow/actions/default/fire/PUBLISH` with a `dotAsset` contentlet.

⚠️ The existing `DotAiService.generateAndPublishImage` chains these two, so it publishes at generate time. FR-037 requires them separated: `generateImage()` is extracted, and `generateAndPublishImage = generateImage().pipe(switchMap(createAndPublishContentlet))` preserves the old behavior for its two existing callers.

## 5. `GET /embeddings/indexCount`

⚠️ Response is **wrapped one level**: `{ indexCount: { "<name>": {...} } }`. Inside, a `TreeMap` keyed by index name (so already sorted):

```json
{ "fragments": 0, "contents": 0, "tokenTotal": 0, "tokensPerChunk": 0, "contentTypes": "Blog,News" }
```

The four numerics are `Long`. `contentTypes` is comma-joined at the SQL level (`STRING_AGG(distinct(content_type), ',')`).

**No paging, no filtering, no query parameters** — the whole dataset arrives in one response. This is what makes the index search, the status filter and the column sort pure client-side `computed()` work with no `[lazy]` table and no debounced HTTP.

**Conversion owned by `DotAiEmbeddingsService`**: unwrap `indexCount`, fold each key into a `name` field, split `contentTypes`. Returns `DotAiIndex[]`.

## 6. `POST /embeddings`

⚠️ Takes an **`EmbeddingsForm`, not a `CompletionsForm`**: `{ indexName, query, fields?, velocityTemplate?, limit?, offset? }`. `query` non-null is enforced imperatively → `400`. The build dialog therefore does not reuse the retrieval payload.

**Response**: `{ timeToEmbeddings, totalToEmbed, indexName }`. `indexName` is the authoritative "a build just started for this index" signal that seeds `BUILDING`.

## 7. `DELETE /embeddings`

Body `{indexName}` to delete an index, or `{indexName, deleteQuery}` to delete matching content from one. Response `{deleted: N}` → the service returns `N`.

The DELETE **must actually carry a body** — asserted in the spec, since it is an easy thing to get silently wrong.

## 8. `DELETE /embeddings/db`

Admin-gated. Response `{created: true}` → the service returns `boolean`. Destructive; behind a confirmation stating so (FR-032). The legacy screen used a browser `confirm`.

## 9. `GET /completions/config`

**Response**: `{ configHost, providerConfig, settings }`.

- `configHost` — ⚠️ a **display string**: `hostname + " (falls back to system host)"`. Render as given.
- `providerConfig` — a **JSON string**, and ⚠️ **only present when non-blank**. Its absence is the "not configured" signal (FR-047). Inside it, `chat.model` is a **comma-separated fallback list whose first element is the default**.
- `settings` — a flat `LinkedHashMap<String,String>` of every `AppKeys` entry with a non-null `settingsKey`. Keys are camelCase (`temperature`, `embeddingsSearchThreshold`, `imageSize`, …), not the design's dotted names (FR-043).

⚠️ Credential keys (`API_KEY`, `API_URL`, `API_IMAGE_URL`, `API_EMBEDDINGS_URL`, `PROVIDER_CONFIG`) have a null `settingsKey` and are **absent from `settings` entirely**. Secret rows derive from `providerConfig`, whose credential fields — `apiKey`, `secretAccessKey`, `accessKeyId`, `credentialsJson` — the server has already rewritten to `"*****"`. The client renders its own `••••••••` and never echoes the server's mask (FR-042).

⚠️ If redaction itself fails the server returns the literal `"[CONFIG PRESENT — REDACTION FAILED]"` (**em dash**). It is not JSON. Guard it and say so (FR-046) rather than rendering it as a value.

**Conversion owned by `DotAiConfigService.getResolvedConfig`** — the single place `providerConfig` is parsed. It is parsed independently in three places today.
