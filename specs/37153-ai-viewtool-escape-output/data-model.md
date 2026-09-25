# Data Model: dotAI viewtool output escaping

No schema, index or persisted-state change. The "model" here is the in-memory payload each
`$ai` method hands to Velocity, and the rule for which values are escaped.

## Payload model and escaping rules

Legend: **E** = HTML-escaped by default (OWASP `Encode.forHtml`), **—** = returned as today.
`$ai.unsafe` returns every value as **—**.

### `completions.summarize(prompt[, index])` → `JSONObject`

| Key path | Source | Default |
|---|---|---|
| `query` | echoed caller input | **E** |
| `openAiResponse` (entire subtree, every string leaf) | provider | **E** |
| `dotCMSResults[].matches[].extractedText` | stored search excerpt (may be another visitor's query via the `cache` index) | **E** |
| `dotCMSResults[].matches[].distance` | dotCMS number | — |
| `dotCMSResults[].title` | contentlet field | — |
| `dotCMSResults[].<any other contentlet field>` | contentlet field | — |
| `total`, `count`, `limit`, `offset`, `threshold`, `operator`, `timeToEmbeddings` | dotCMS metadata (`operator` is `<=>`, must stay) | — |
| no-hits shape `{ "error": "no matching content found …" }` | fixed dotCMS string | **E** (no-op) |

### `search.query(...)` / `search.related(...)` → `JSONObject`

Same table as `summarize` without `openAiResponse`. `related` may return an empty
`JSONObject` when the contentlet has nothing to relate; it passes through unchanged.

### `completions.raw(String | JSONObject | Map)` → `JSONObject`

| Key path | Source | Default |
|---|---|---|
| every string leaf at any depth (`choices[].message.content`, `model`, `id`, `object`, …) | provider | **E** |
| numbers, booleans, nulls | provider | — |

### `generateText(String | Map)` → `JSONObject`

Same as `raw`: the whole object is the provider's response.

### `generateImage(String | Map)` → `JSONObject`

| Key path | Source | Default |
|---|---|---|
| `url`, `b64_json` (the client rebuilds the provider response as `data[].url`; no `revised_prompt` reaches dotCMS) | provider `data[0]` | **E** |
| `originalPrompt` | echoed caller input | **E** |
| `tempFileName`, `response`, `tempFile` | dotCMS-added strings inside the same object | **E** (harmless; the object is escaped as one provider payload) |

The image payload therefore has no model-written text; `originalPrompt` is the value that matters
(reflected XSS when the prompt is taken from a request parameter).

### Error payloads (all handled methods)

| Shape | When | Default |
|---|---|---|
| `JSONObject { "error": <fixed string> }` (from `AIViewToolErrorHandler`, #37154) | any handled failure | **E** (no-op on today's fixed message) |
| `generateText` | throws; no payload | n/a |

## Encoder semantics

`Encode.forHtml`: `&`→`&amp;`, `<`→`&lt;`, `>`→`&gt;`, `"`→`&#34;`, `'`→`&#39;`; other
characters unchanged. Idempotence is **not** a property: already-escaped text is escaped again
(`&amp;` → `&amp;amp;`). Safe in HTML body and quoted-attribute context only.

## Class and flag model

```text
AIViewTool                       (public, toolbox key "ai", request scope)
  - escapeOutput: boolean        true via no-arg ctor; false via private copy ctor
  - context, config, user, chatService, imageService   (resolved in init(); copied by copy ctor)
  + getUnsafe(): AIViewTool      escapeOutput ? new AIViewTool(this) : this
  + getCompletions(): CompletionsTool   new CompletionsTool(context, escapeOutput)
  + getSearch(): SearchTool             new SearchTool(context, escapeOutput)
  + getEmbeddings(): EmbeddingsTool     unchanged
  + generateText(..): JSONObject        escapeOutput ? deepEscape(r) : r
  + generateImage(..): JSONObject       escapeOutput ? deepEscape(r) : r   (error payload too)

CompletionsTool / SearchTool     (public class, package-private ctors)
  - escapeOutput: boolean
  ~ (Object initData)                    -> this(initData, true)   [kept for existing tests]
  ~ (Object initData, boolean escapeOutput)
  + summarize / query / related          escapeOutput ? escapeSearchShaped(r) : r ; errors deepEscape
  + raw(..)                              escapeOutput ? deepEscape(r) : r        ; errors deepEscape

AIViewToolOutputEscaper          (package-private final, static)
  ~ deepEscape(Object): Object
  ~ deepEscape(JSONObject): JSONObject
  ~ escapeSearchShaped(Object): Object
```

## Invariants (asserted by tests)

1. Default and unsafe payloads have identical key sets at every depth, identical array
   lengths, identical non-string values (AC-004).
2. The object returned by the API method is not mutated by the default path (AC-006).
3. The default path never throws where the unsafe path does not (copying is total over
   `JSONObject`, `JSONArray`, `Map`, `List`, `NULL`, `null`, scalars).
4. `title`, `operator` and every contentlet field are reference-identical between default and
   unsafe payloads.
5. `getUnsafe()` on an unsafe tool returns the same instance.
