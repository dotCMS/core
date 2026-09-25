# Contract: `$ai` viewtool failure payload

The dotAI viewtool is registered under the key `ai` in `dotCMS/src/main/webapp/WEB-INF/toolbox.xml`
(request scope, class `com.dotcms.ai.viewtool.AIViewTool`). This contract covers what a template
receives when a call to one of its methods fails. Success results are out of scope and unchanged.

## Methods covered

| Template call | Java method | Failure behaviour after this change |
|---|---|---|
| `$ai.completions.summarize(prompt)` | `CompletionsTool.summarize(String)` | Returns the failure payload |
| `$ai.completions.summarize(prompt, indexName)` | `CompletionsTool.summarize(String, String)` | Returns the failure payload |
| `$ai.completions.raw(jsonString)` | `CompletionsTool.raw(String)` | Returns the failure payload, including for malformed JSON input |
| `$ai.completions.raw(jsonObject)` | `CompletionsTool.raw(JSONObject)` | Returns the failure payload |
| `$ai.completions.raw(map)` | `CompletionsTool.raw(Map<String,Object>)` | Returns the failure payload |
| `$ai.search.query(query)` | `SearchTool.query(String)` | Returns the failure payload |
| `$ai.search.query(query, indexName)` | `SearchTool.query(String, String)` | Returns the failure payload |
| `$ai.search.query(map)` | `SearchTool.query(Map<String,Object>)` | Returns the failure payload |
| `$ai.search.related(contentMap, indexName)` | `SearchTool.related(ContentMap, String)` | Returns the failure payload |
| `$ai.search.related(contentlet, indexName)` | `SearchTool.related(Contentlet, String)` | Returns the failure payload |
| `$ai.generateImage(prompt)` | `AIViewTool.generateImage(String)` | Returns the failure payload |
| `$ai.generateImage(map)` | `AIViewTool.generateImage(Map<String,Object>)` | Returns the failure payload |
| `$ai.generateText(prompt)` / `$ai.generateText(map)` | `AIViewTool.generateText(...)` | **Unchanged.** The exception propagates to the Velocity engine. For live and preview requests the engine logs it and renders the reference as `null`; the page shows no exception text. Pinned by `AIViewToolTest.test_generateText_providerFailure_liveRender_showsNoExceptionDetail`. |
| `$ai.embeddings.*` | `EmbeddingsTool` | Unchanged. No exception handling exists there today. |

## Failure payload

```json
{ "error": "AI request failed. Check the dotCMS log for details." }
```

- Type: `JSONObject` (implements `Map`). In Velocity, `$result.error` reads the value.
- Exactly one key. The exact wording of the value is the implementation's choice and may be
  refined; it will never contain content derived from the exception.

### Guarantees

- The key name `error` is stable. Templates may test `#if($result.error)` and rely on it.
- The value is a fixed string. Templates must **not** branch on its text; it is not part of the
  contract and may change wording between releases.
- The payload never contains: a `stackTrace` key, a Java class or package name, a source file
  name, a line number, a stack frame, a provider response body, or the prompt.
- Every covered method returns the same payload shape for every failure cause.

### Server side

Each failure is logged once at `ERROR` under the logger of the tool class that caught it
(`com.dotcms.ai.viewtool.CompletionsTool`, `...SearchTool` or `...AIViewTool`) with the full
exception and stack trace. Operators find the cause there.

## What changes for existing templates

| Before | After |
|---|---|
| `$result.stackTrace` held a multi-line trace string (completions) or a list of frames (search) | Key absent. Renders as the literal reference text, or as nothing with `$!result.stackTrace`. |
| `$result.error` held the raw exception message, e.g. `java.net.ConnectException: Connection refused` | Fixed string. |
| `$ai.generateImage(...)` failure: `$result.error` held the raw message, e.g. `Error generating image:dev.langchain4j...` | Fixed string. |
| `#if($result.error)` | Works unchanged. |

No template that worked before this change stops rendering. Templates that displayed the trace or
the raw message to visitors now display a generic sentence instead, which is the purpose of the
change. No release note is required (epic #37255 lists only #37151 and #37153 as needing one).

## Out of scope

- REST endpoints under `/api/v1/ai/*`. They have their own error handling and do not return
  traces today.
- Escaping of success payloads (#37153).
- `AppConfig.debugLogger` prompt/response logging behind `DEBUG_LOGGING` / `AI_DEBUG_LOGGING`.
