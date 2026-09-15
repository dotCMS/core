# Data Model: #37154 AI viewtool stack-trace leak

This change persists nothing. The only structured values it defines are the template-facing
failure payload and the server-side log record. Both are described here so the tests and the
contract have one source.

## Viewtool failure payload

Returned by every `$ai` viewtool method that catches an exception (see
[contracts/viewtool-error-payload.md](contracts/viewtool-error-payload.md) for the method list).

| Field | Type | Value | Notes |
|-------|------|-------|-------|
| `error` | `String` | Fixed constant, proposed `AI request failed. Check the dotCMS log for details.` | Key name is `AiKeys.ERROR`. The constant is defined once on `AIViewToolErrorHandler`. |

- **Container type**: `com.dotmarketing.util.json.JSONObject`, which implements `Map`, so
  Velocity property access (`$result.error`) and Java `Map`/`JSONObject` access both work.
- **Invariants**:
  - Exactly one key. No `stackTrace`, no `message`, no `cause`, no `type`.
  - The value is never derived from the caught exception. It contains no class name, no
    package name, no file name, no line number, no provider response text, no prompt text.
  - Identical for every exception type and every calling method.
- **Validation rule used by the tests**: no string value in the payload contains any of
  `Exception`, `at ` followed by a fully qualified method, `.java:`, or `com.dotcms`.
- **State transitions**: none. The payload is created and returned; nothing is stored.

### Relationship to success payloads

Success payloads are unchanged and are also `JSONObject`s. A template distinguishes the two by
the presence of the `error` key, exactly as today. One success payload also carries an `error`
key today: `CompletionsAPIImpl.summarize` returns
`{"error": "no matching content found in the index for your query"}` when the index has no
results. That is a normal, non-exception result produced below the viewtool and is not touched
by this change. Templates that test `$result.error` treat both as "no usable answer", which is
the existing behaviour.

## Server-side log record

Emitted once per handled failure by `AIViewToolErrorHandler.handle(source, cause)`.

| Attribute | Value |
|-----------|-------|
| Logger name | Fully qualified name of the `source` class passed in (`CompletionsTool`, `SearchTool` or `AIViewTool`) |
| Level | `ERROR` |
| Message | Fixed text, proposed `AI viewtool call failed`, plus the ` @ <thread name>` suffix that `Logger.velocityError` appends for viewtool classes |
| Throwable | The caught exception, unwrapped as received, with its full stack trace and cause chain |

- **Invariants**: the message never includes the prompt, the request JSON, the query text, the
  user id or the exception message. All diagnostic detail travels in the throwable, which log4j2
  renders with the trace. This follows Constitution III and the repository's secure logging
  pattern.
- **Routing note**: `com.dotmarketing.util.Logger` treats any class whose name contains
  `viewtool` as a Velocity class and routes it through `velocityError`, which writes to the same
  named logger at the same level. Consumers filtering on logger name see the source class as
  expected.

## Entities not changed

`CompletionsForm`, `EmbeddingsDTO`, `AppConfig`, the embeddings table, `EMBEDDING_CACHE`, and
all REST view objects are untouched.
