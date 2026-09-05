# Data Model: dotAI Portlet Rebuild

**Feature**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md) | **Contracts**: [contracts/](./contracts/) | **Date**: 2026-09-04

Client-side model only. **No server-side entity is created or changed.** Every type here is a view of an existing wire shape, and every conversion from wire to view has exactly one owner — the service that fetches it. Stores and components never see a wrapper map, a CSV string, or nested JSON-in-a-string.

New types go in `core-web/libs/dotcms-models/src/lib/dot-ai.model.ts`, which **already exists** (164 lines). Types listed under "Already present" must not be redeclared. Use `as const` objects rather than `enum` for new unions — the file's existing `DotAiCapability` and `DotAiProviderFieldType` are `enum`s from an older convention and are not the pattern to follow.

---

## Already present in `dot-ai.model.ts` — do not redeclare

`PromptType`, `AiPluginResponse`, `DotAIImageResponse`, `AIImagePrompt`, `DotAIImageContent`, `DotGeneratedAIImage`, `DEFAULT_IMAGE_SIZE`, `DotAIImageOrientation`, `DotAICompletionsConfig`, `DotAiProviderConfig`, `DotAiError`, `DotAiCapability`, `DotAiProviderFieldType`, `DotAiProviderField`, `DotAiProviderMetadata`, `DotAiTestConnectionResult`.

## Not reused, deliberately

`libs/sdk/types/src/lib/ai/` (`DotCMSAISearchQuery`, `DotCMSAISearchResponse`, `DotCMSAISearchMatch`, `DotCMSAISearchContentletData`, `DISTANCE_FUNCTIONS`, `DotErrorAISearch`) is the **published external SDK contract** for a `GET /api/v1/ai/search` query-param call style. This portlet uses `POST` with a body, so the request side is not shared at all, and the response overlaps only because it is the same resource. Coupling an internal admin screen to a versioned external contract means an SDK change breaks the portlet and vice versa.

`AgentMessage` from `@dotcms/ai-ui` is not reused for chat — it is `{id, icon, text, sub?, tone}`, a timeline log entry. A dotAI assistant turn is prose with a streaming lifecycle.

---

## Entities

### `DotAiIndex`

One embeddings index. **Spec**: US3, FR-025 – FR-028.

| Field | Type | Source | Notes |
|---|---|---|---|
| `name` | `string` | the **map key** of `indexCount` | Folded into the object by the service. The store never sees a map. |
| `fragments` | `number` | `fragments` (`Long`) | "chunks" in the UI |
| `contents` | `number` | `contents` (`Long`) | |
| `tokenTotal` | `number` | `tokenTotal` (`Long`) | |
| `tokensPerChunk` | `number` | `tokensPerChunk` (`Long`) | |
| `contentTypes` | `string[]` | `contentTypes` (**comma-joined `String`**) | Split by the service. Comma-joined at the SQL level via `STRING_AGG(distinct(content_type), ',')`. |

**Derived, client-side** (`utils/dot-ai-index.utils.ts`, pure and unit-tested):

- `estimatedCost: number` = `(tokenTotal / 1000) * 0.0001`. Applied to **every** row. The legacy screen computed this but applied it only to the index literally named `cache` — a bug. Labelled as an estimate (FR-026) because it hardcodes one provider's published pricing and is already wrong for the others the platform supports.
- `status: DotAiIndexStatus` — see below.

**Validation / invariants**:
- `indexCount` is a `TreeMap`, so the array arrives sorted by name; the UI does not re-sort by default.
- The pseudo-index literally named `cache` appears in the Embeddings table but is **excluded** from the retrieval index picker (`indexOptions`), preserving legacy behavior.
- Picker label format: `` `${name} - (contents:${contents})` ``.

### `DotAiIndexStatus`

```ts
export const DOT_AI_INDEX_STATUS = { READY: 'READY', BUILDING: 'BUILDING' } as const;
export type DotAiIndexStatus = (typeof DOT_AI_INDEX_STATUS)[keyof typeof DOT_AI_INDEX_STATUS];
```

**There is no status column in `dot_embeddings`.** This is derived per index, not portlet-wide as the legacy screen had it.

**State transitions**:

| From | Trigger | To |
|---|---|---|
| `READY` | a build response returns `{indexName}` for this index | `BUILDING` (seeded immediately — an authoritative "a build just started here" event, so the first poll does not have to guess) |
| `BUILDING` | a 5s poll shows `fragments` **changed** vs the previous snapshot | `BUILDING` (still moving) |
| `BUILDING` | a 5s poll shows `fragments` **unchanged** | `READY` |
| — | an index appears for the first time with no prior snapshot | `READY` |

Rendered as `<p-tag>`: `READY` → `success` (exact theme match — the preset maps success to `green.100`/`green.700`), `BUILDING` → `warn`. The preset maps `warn` to yellow rather than the design's orange; accept it, it is the app's in-progress convention and `!`-overriding a severity is banned by the styling standards.

### `DotAiSearchResult` / `DotAiSearchMatch` / `DotAiSearchResponse`

**Spec**: US1, FR-007 – FR-009.

`DotAiSearchResponse`: `{ timeToEmbeddings, total, query, threshold, operator, offset, limit, count, results: DotAiSearchResult[] }` — the header meta (`count`, `timeToEmbeddings`, `threshold`) is all real and satisfies FR-009.

`DotAiSearchResult` is projected from a **full contentlet JSON** entry in `dotCMSResults`:

| Field | Type | Notes |
|---|---|---|
| `identifier`, `inode`, `title` | `string` | `title` is forced by the server |
| `contentType` | `string` | prefer `contentTypeName` when present |
| `modDate` | `string \| undefined` | ⚠️ **may be absent.** One server fallback path emits only `{inode, identifier, title, language, index, contentType}` with no `modDate`. FR-008 and US1 scenario 3 require the row to omit it cleanly and drop the separator. |
| `matches` | `DotAiSearchMatch[]` | |

`DotAiSearchMatch`: `{ distance: number; extractedText: string }`. `extractedText` is server-truncated to 255 chars; rendered with `line-clamp-2`. Snippet = `matches[0].extractedText`; count = `matches.length`; closeness bar = `matches[0].distance`.

**Ordering**: results arrive distance-ordered from the pgvector query and the API exposes no sort parameter. No client re-sort — see spec Out of Scope.

### `DotAiRetrievalPayload` and `DotAiCompletionsForm`

**Spec**: US1 + US2, FR-016 – FR-024. The single most important type here.

`DotAiRetrievalPayload` is a `CompletionsForm` **minus `prompt`**, assembled in exactly one place — the `retrievalPayload` computed in `withRetrievalSettings`. Both Search and Chat do `{ ...store.retrievalPayload(), prompt }`. Same discipline as `apiRequestBody` in `dot-query-tool.store.ts`.

| Field | Type | Default | Rule |
|---|---|---|---|
| `indexName` | `string` | `'default'` | |
| `site` | `string` | `''` | `''` = all sites (FR-021) |
| `contentType` | `string[]` | `[]` | ⚠️ **empty omits the field entirely** — never send `''` (FR-020). Server splits on `\s?,\s?`. |
| `threshold` | `number` | `.25` | seeded from `settings.embeddingsSearchThreshold` |
| `operator` | `DotAiVectorOperator` | `'cosine'` | see below |
| `model` | `string` | first entry of the parsed chat-model list | |
| `temperature` | `number` | provider config | clamped `0..2` (FR-022) |
| `responseLengthTokens` | `number` | — | **minimum 128** (FR-023) |
| `language` | `number` | current | |
| `stream` | `boolean` | per tab | Search `false`, Chat `true` |

### `DotAiVectorOperator`

```ts
export const DOT_AI_VECTOR_OPERATOR = {
    DISTANCE: 'distance',
    COSINE: 'cosine',
    INNER_PRODUCT: 'innerProduct'   // NOT 'product'
} as const;
```

**These three strings are the server's entire accepted set.** Anything else falls back to cosine via `getOrDefault(operator, "<=>")`. The legacy screen's third radio sends `value="product"`, which is not a key — so "Inner Product" has never actually produced `<#>`. FR-024 exists solely because of this. The union type is what prevents it recurring, and `with-retrieval-settings.feature.spec.ts` asserts it explicitly.

### `DotAiEmbeddingsBuildForm` / `DotAiEmbeddingsBuildResult`

**Spec**: US3, FR-029, FR-030.

⚠️ **The build endpoint takes `EmbeddingsForm`, not `CompletionsForm`** — a different shape. The build dialog therefore does **not** reuse `DotAiRetrievalPayload`.

`DotAiEmbeddingsBuildForm`: `{ indexName: string; query: string; fields?: string; velocityTemplate?: string; limit?: number; offset?: number }`. `query` is required — the server rejects a null with 400.

`DotAiEmbeddingsBuildResult`: `{ timeToEmbeddings: string; totalToEmbed: number; indexName: string }`. `indexName` is what seeds `BUILDING`.

**Add vs delete mode** (one dialog, FR-030): add mode posts the form; delete mode sends `{indexName, deleteQuery}` to `DELETE /embeddings`, remapping the same query field. The submit label flips accordingly.

Deletion responses are unwrapped by the service: `{deleted: N}` → `number`, `{created: true}` → `boolean`.

### `DotAiResolvedConfig`

**Spec**: US5, FR-041 – FR-046, and FR-047 (the configured/unconfigured gate).

Parsed **once**, in `DotAiConfigService.getResolvedConfig`. Today `providerConfig` is parsed independently in three places — the legacy script, the dot-apps config screen, and the image-prompt component. This ends that.

| Field | Type | From |
|---|---|---|
| `configHost` | `string` | ⚠️ a **display string**, not a hostname — the server returns `hostname + " (falls back to system host)"`. Render as given; do not parse. |
| `settings` | `Record<string, string>` | the flat resolved map |
| `providerConfig` | `object \| null` | the JSON **string**, parsed |
| `chatModels` | `string[]` | `chat.model` inside `providerConfig` is a **comma-separated fallback list whose first element is the default**. Split in order. |
| `isConfigured` | `boolean` | ⚠️ derived from **the presence of `providerConfig`** — the server omits the key entirely when blank. That absence is the "not configured" signal for FR-047. |
| `redactionFailed` | `boolean` | the literal sentinel `"[CONFIG PRESENT — REDACTION FAILED]"` (**em dash**, not a hyphen) is not JSON. Guard it, set this flag, and satisfy FR-046 — never render it as a value. |

Malformed JSON must yield `chatModels: []` and must not throw.

### `DotAiConfigValueRow`

The Config Values table row. **Spec**: FR-041 – FR-043.

`{ key: string; value: string; source: 'App Config' | 'Default' | 'Secret' }`

**Derivation mirrors the backend's own resolution rule** (`AppConfig.getConfig` returns the set value when non-blank, else the enum default):

- `providerConfig[key]` non-blank → **App Config**
- otherwise → **Default**
- plus one **Secret** row per credential field found in `providerConfig`: `apiKey`, `secretAccessKey`, `accessKeyId`, `credentialsJson`. Rendered as `••••••••` — **never echo the server's `"*****"`**.

⚠️ Two constraints the design got wrong:
1. Keys are the **camelCase `settingsKey` values** — `rolePrompt`, `imageSize`, `temperature`, `embeddingsSearchThreshold`, `embeddingsSplitAtTokens`, `debugLogging`, … — not the design's illustrative dotted names. FR-043 requires the real keys so a value found here can be searched for elsewhere.
2. The credential keys (`API_KEY`, `API_URL`, `API_IMAGE_URL`, `API_EMBEDDINGS_URL`, `PROVIDER_CONFIG`) have a **null `settingsKey` and are absent from `settings` altogether**. The Secret rows must therefore be derived from `providerConfig`, not from `settings`. The client never receives a real credential — which is what makes FR-042 structurally true rather than a rendering promise.

### `DotAiChatMessage`

**Spec**: US2, FR-011 – FR-015.

`{ id: string; role: 'user' | 'assistant'; content: string; state: 'streaming' | 'complete' | 'stopped' | 'error'; error?: string }`

**State transitions** for an assistant turn:

| From | Trigger | To |
|---|---|---|
| — | send | `streaming` (empty content) |
| `streaming` | each `{type:'delta'}` | `streaming`, content appended in order |
| `streaming` | `[DONE]` | `complete` |
| `streaming` | Stop pressed, or tab left | `stopped` — partial content **retained** (FR-012) |
| `streaming` | a new question sent | `stopped` (the earlier turn is abandoned, FR-013) |
| `streaming` | `{type:'error'}` or a transport failure | `error`, rendered inline, `handle()` **not** called (FR-014) |

`stopped` is a distinct state from `complete` so the UI can say the answer was cut short rather than implying it finished.

### `DotAiGeneratedImage`

**Spec**: US4, FR-036 – FR-040.

`{ tempFileName: string; response: string; originalPrompt: string; revisedPrompt: string; url: string; published: boolean }`

Asset is viewable at `/dA/{response}/asset.png` — same-origin, already proxied in dev, and the legacy screen already linked it. Download is a plain anchor to that URL; **Save** is the separate `createAndPublishContentlet` call.

`published` starts `false` and only flips on an explicit save. This is the whole point of FR-037: the existing `generateAndPublishImage` publishes at generate time, so every discarded generation becomes a live `dotAsset`. The new flow must not inherit that.

---

## Relationships

```
DotAiResolvedConfig ──seeds──> DotAiRetrievalPayload.threshold, .model, .temperature
                    └─derives─> isConfigured ──gates──> Search / Send / Generate / Build   (FR-047)

DotAiIndex[] ──feeds──> Embeddings table                  (FR-025)
             └─feeds──> DotAiRetrievalPayload.indexName picker, minus `cache`
             ▲
             └── every mutation calls loadIndexes() ──> both readers update  (FR-033)

DotAiRetrievalPayload ──+ prompt──> Search  ──> DotAiSearchResponse
                       └─+ prompt──> Chat   ──> DotAiChatMessage stream
```

One owner, two readers — this is why the store is composed on the shell rather than per tab. It is also what makes FR-033 free rather than a cross-store notification.
