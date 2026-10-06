# Frontend service contract

The five services in `core-web/libs/data-access/src/lib/dot-ai/` are the portlet's only HTTP surface. There is **no portlet-local `services/` directory**: every conversion has exactly one owner, and it is a service.

All five share one exported `AI_API_ENDPOINT = '/api/v1/ai'` rather than repeating the literal.

---

## The split

`DotAiService` today is `providedIn: 'root'` and carries four unrelated jobs. It is split first, as a pure refactor with no new methods and no behavior change, because piling nine more methods onto it makes the problem worse and because the resulting `nx affected` fan-out deserves its own reviewable commit.

| New service | Methods | Endpoints | Existing callers to re-point |
|---|---|---|---|
| **`DotAiConfigService`** | `getConfig`, `saveConfig`, `getProviders`, `testConnection`, `checkPluginInstallation`, **+ `getResolvedConfig`** | `/completions/config`, `/providers*` | 3 files: `dot-ai-config-detail.component.ts`, `dot-ai-config-detail-legacy.component.ts`, `dot-ai-capability-card.component.ts` — plus the 4 `checkPluginInstallation` callers in `dot-block-editor.component.ts`, `dot-bubble-menu.component.ts`, `new-block-editor/.../editor.store.ts`, `dot-file-field.component.ts` |
| **`DotAiContentService`** | `generateContent`, `createAndPublishContentlet`, `generateAndPublishImage`, **+ `generateImage`** | `/text/generate`, `/image/generate`, workflow PUBLISH | `generateContent`: `ai-content-prompt.store.ts`, `ai-content-dialog.component.ts`. `generateAndPublishImage`: the two `libs/ui/.../dot-ai-image-prompt` stores |
| **`DotAiSearchService`** | `semanticSearch` | `/search` | none — new |
| **`DotAiEmbeddingsService`** | `getIndexes`, `buildIndex`, `deleteIndex`, `deleteFromIndex`, `rebuildEmbeddingsDb` | `/embeddings*` | none — new |
| **`DotAiCompletionsStreamService`** | `stream` | `/completions` | none — new |

**9 production files change**, all mechanically (import + `inject()` swap), across 5 libs.

Two things verified while mapping callers, both of which make the refactor safer than assumed:

- **`createAndPublishContentlet` has no caller outside the service** — not even in specs. It is free to reshape.
- The `getConfig`/`saveConfig` hits in `dot-auth`, `app.component.ts` and `dot-my-account` are **different services with the same method names**. Do not touch them.

---

## Signatures

```ts
// DotAiConfigService
getResolvedConfig(siteId?: string): Observable<DotAiResolvedConfig>   // GET /completions/config

// DotAiSearchService
semanticSearch(form: DotAiCompletionsForm): Observable<DotAiSearchResponse>   // POST /search

// DotAiEmbeddingsService
getIndexes(): Observable<DotAiIndex[]>                                 // GET    /embeddings/indexCount
buildIndex(form: DotAiEmbeddingsBuildForm): Observable<DotAiEmbeddingsBuildResult>  // POST /embeddings
deleteIndex(indexName: string): Observable<number>                     // DELETE /embeddings {indexName}
deleteFromIndex(indexName: string, deleteQuery: string): Observable<number>        // DELETE /embeddings {…, deleteQuery}
rebuildEmbeddingsDb(): Observable<boolean>                             // DELETE /embeddings/db

// DotAiContentService
generateImage(prompt: string, size?: string, numberOfImages?: number, model?: string): Observable<DotAIImageResponse>

// DotAiCompletionsStreamService
export type DotAiStreamEvent =
    | { type: 'delta'; content: string }
    | { type: 'error'; message: string };

@Injectable()   // NOT providedIn:'root' — route-provided, so it lives and dies with the screen
stream(form: DotAiCompletionsForm): Observable<DotAiStreamEvent>
```

`DotAiCompletionsStreamService` sits beside `DotAiSearchService` in the same folder — same `/api/v1/ai` family, and it is the completions counterpart of the search call. It follows the "not `providedIn: 'root'`" rule that `DotAgentRunService`'s class comment states, and for the same reason: teardown is what aborts the `fetch`.

---

## The conversions each service owns

This table **is** the contract. Each row is asserted in that service's spec, and no component or store may reimplement any of them.

| Owner | Wire shape | View shape |
|---|---|---|
| `DotAiEmbeddingsService.getIndexes` | `{indexCount: {"<name>": {...}}}` | `DotAiIndex[]` with `name` folded in from the key |
| `DotAiEmbeddingsService.getIndexes` | `contentTypes: "Blog,News"` | `contentTypes: string[]` |
| `DotAiEmbeddingsService.delete*` | `{deleted: N}` | `number` |
| `DotAiEmbeddingsService.rebuildEmbeddingsDb` | `{created: true}` | `boolean` |
| `DotAiConfigService.getResolvedConfig` | `providerConfig` as a JSON **string**, sometimes absent | parsed object, `isConfigured` from its presence |
| `DotAiConfigService.getResolvedConfig` | `chat.model` as a CSV fallback list | `chatModels: string[]`, order preserved, `[0]` is the default |
| `DotAiConfigService.getResolvedConfig` | `"[CONFIG PRESENT — REDACTION FAILED]"` | `redactionFailed: true`, no throw |
| `DotAiSearchService.semanticSearch` | full contentlet JSON + `matches[]` | `DotAiSearchResult[]`, `modDate` optional |
| `DotAiCompletionsStreamService.stream` | bare `data:` lines, JSON possibly split across chunks | ordered `DotAiStreamEvent`s |

---

## The one behavior-preserving refactor

Inside `DotAiContentService`, extract `generateImage()` out of `generateAndPublishImage()`, then:

```ts
generateAndPublishImage = (prompt, size) =>
    this.generateImage(prompt, size).pipe(switchMap((r) => this.createAndPublishContentlet(r)));
```

`generateAndPublishImage`'s existing spec is the regression guard for the two `libs/ui` image-prompt stores and **must pass unchanged**. The Image tab then composes `generateImage` (Generate) and `createAndPublishContentlet` (Save to Assets) separately, which is what FR-037 requires — today every generation, including discarded ones, publishes a live `dotAsset`.

---

## Error handling contract

| Situation | Behavior |
|---|---|
| Any CRUD or load failure | `DotHttpErrorManagerService.handle(error)` → `EMPTY`, and `status` returns to `LOADED` so the list stays usable (FR-051) |
| Chat stream failure | **Not** `handle()`. Sets `chatError` and renders inline (FR-014) — a modal over a stream the user is watching is the wrong UX, and every stream failure is recoverable in place. Precedent: `a11y-run.store.ts`'s `runError`. |
| `403` on `getIndexes` | A **forbidden state**, not an error dialog (FR-049, FR-050) |
| `providerConfig` absent | An **unconfigured state**, not an error dialog (FR-047, FR-050) |
| `404 {error: "Index 'x' not found"}` | A distinguishable error naming the index |

⚠️ `DotHttpErrorManagerService` and `DotGlobalMessageService` are both bare `@Injectable()` — **not** `providedIn: 'root'`. Both must be listed in `providers`. No `p-toast` in this portlet.
