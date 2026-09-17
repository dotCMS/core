# OpenAI-Compatible Inference API (`/api/inference/v1`)

`/api/inference/v1` is a REST endpoint family that speaks the OpenAI chat-completions wire format — the request and response JSON shapes published by OpenAI and served by most AI gateways (OpenRouter, LiteLLM, Vercel AI Gateway, Ollama, vLLM). Because the format is the one standard client libraries and agent frameworks already emit, pointing such a client at dotCMS takes exactly two settings: a base URL and a dotCMS API token. No dotCMS-specific client code is written.

The endpoints sit in front of dotAI, which is already a multi-provider gateway with per-site credentials held in App secrets, model fallback chains, and provider selection. What the family adds is the door: an application that previously had to embed a provider key of its own can route through dotCMS instead, so the site's credentials stay server-side, the provider can be swapped centrally, and every call can be attributed to a site.

**This is not a replacement for `/api/v1/ai/*`.** Those endpoints stay and keep their behavior. Retrieval-augmented completions, semantic search, the vector-corpus operations and provider administration have no equivalent verb in the OpenAI format and remain first-class there. Three `/api/v1/ai/*` operations are superseded by this family and are now `@Deprecated` — see [Superseded endpoints](#superseded-endpoints).

Source: `dotCMS/src/main/java/com/dotcms/inference/`. Design intent lives in `specs/37431-openai-compatible-inference/`; the published contract is the generated `openapi.yaml`.

---

## Operations

| Operation | Path | Notes |
|---|---|---|
| Chat completion | `POST /api/inference/v1/chat/completions` | Multi-turn messages, tool calling, optional SSE streaming |
| Model listing | `GET /api/inference/v1/models` | Every model the resolved site configured — chat, embeddings and images — chat first |
| Embeddings | `POST /api/inference/v1/embeddings` | One string or an array of strings |
| Image generation | `POST /api/inference/v1/images/generations` | Always base64, never a URL |

The trailing `v1` is the **wire-protocol** version, not dotCMS's own `/api/v1` resource versioning. A second wire format would be added as a sibling under the same version (`/api/inference/v1/responses`), not as `/api/inference/v2`. OpenAI's legacy non-chat `POST /completions` is deliberately absent.

---

## Authentication

**Bearer token only.** Every endpoint requires `Authorization: Bearer <dotCMS API token>`. Session cookies and basic authentication are refused with a `401`, even though the surrounding dotCMS filters would otherwise accept them — `BearerOnlyAuthFilter` enforces this for the whole family, and each resource calls the same check directly so it also holds when a resource method is invoked outside the JAX-RS chain.

Any authenticated user is accepted, backend or frontend, matching the existing dotAI endpoints.

**These endpoints are server-side only. Never put the token in browser JavaScript.** The token carries the full authority of the user it was issued for, so a token that reaches a browser is a token that can be read out of it. This is why **no CORS headers are emitted**: each resource carries the `@NoCors` marker (`com.dotcms.rest.annotation.NoCors`), which `com.dotcms.rest.api.CorsFilter` honors by skipping cross-origin headers entirely, so a browser blocks a cross-origin call before it reaches the endpoint. Refusing session and basic credentials is what keeps that a real control rather than a formality — if an ambient browser credential authenticated here, any page the user had open would be one fetch away from spending the site's AI budget.

---

## Model selection

`model` is **required on every request**. There is no implicit default, no reserved alias and no sentinel name — a request that omits it is refused with a `400` naming the field.

The value is validated against what the resolved site has configured **for that capability**, for every caller including administrators:

| Endpoint | Validated against the site's |
|---|---|
| `chat/completions` | `chat` section of `providerConfig` (including every fallback-chain entry) |
| `embeddings` | `embeddings` section |
| `images/generations` | `image` section |

A model the site has not configured for that capability is refused with a `404`, so a chat model sent to the images endpoint is refused even though the same site configured it perfectly well for chat. Nothing is passed through to the provider.

`GET /api/inference/v1/models` is the discovery mechanism: it lists every model the resolved site has configured, in configured order, chat section first — so a caller who wants "whatever this site runs" reads the list and takes the first entry. Embeddings and image models are listed alongside chat models because every operation here requires an exact model name and this is the only place to learn one; listing chat alone would leave two of the four operations undiscoverable. The format has nowhere to record what a model is for — its model object carries no type, mode or modality field, which is why OpenAI's own listing mixes chat, embedding and image models the same way — so picking an entry the operation does not serve is refused by that operation with a `404` naming `model`. Nothing is added to the entries to signal capability: adding a field the format does not define is what the no-adapter promise of this family exists to avoid.

Because the model is required and validated, swapping a site's provider is **not** invisible to callers: a caller pinning the old vendor's model gets an explicit `404` and has to re-read the model list. That is deliberate.

Within the chat capability, the site's own configuration and fallback chain decide which model actually runs. The `model` field of the response names the model that served the request, which may be a later entry in the chain than the one requested.

---

## Site resolution

Site resolution is **standard dotCMS resolution** — this family invents no semantics of its own:

1. An explicit override, given as the `X-dotCMS-Site` request header or the `siteId` query parameter (a site identifier or a host name). The header wins when both are present. An override is resolved **as the caller**, so a site the caller cannot read is refused with a `403`.
2. Otherwise the request's host name.
3. Otherwise the default site, exactly as everywhere else in dotCMS. Server-side callers routinely present a host name that is no site alias — internal DNS, a container service name, `localhost` — so refusing those would break the deployments this family exists for.

A default-site fallback is **logged** with the unmatched host name, at warn level, stating that the default site's credentials will be spent.

The legacy `host` and `host_id` request parameters, which act as a de-facto site override elsewhere in dotCMS, are **ignored** here.

Every response — success, refusal, streamed or not — carries the serving site in the **`X-dotCMS-Resolved-Site`** response header, holding the site identifier. It is a header rather than a body field so that payloads still deserialize into a standard client's own types, and so that it survives on errors and on streams. `ResolvedSiteHeaderFilter` writes it from an attribute the resource publishes the instant the site is known, before anything that can fail.

A site with no dotAI configuration of its own inherits the system-level configuration, as every other dotAI caller does. Only when neither the site nor the system level has any configuration is a completion refused; the model listing returns an empty array in that case, never another site's models.

---

## Configuration

All four keys are read through `Config` at the point of use (`com.dotcms.inference.model.InferenceLimits`), so changing one takes effect without restarting the node.

| Key | Default | What it bounds |
|---|---|---|
| `DOT_INFERENCE_MAX_CONCURRENT_STREAMS` | `50` | Streamed completions in flight on this node. A streamed completion parks a request thread for the whole generation, so concurrency — not request rate — is the scarce resource here. Over the ceiling, a streaming request is refused with `429`. |
| `DOT_INFERENCE_COMPLETION_TIMEOUT_SECONDS` | `300` | Hard ceiling on one streamed completion. A provider that stops producing events ends the stream with an error event instead of parking the thread indefinitely. |
| `DOT_INFERENCE_MAX_REQUEST_BYTES` | `1048576` (1 MiB) | Largest request body accepted. A declared `Content-Length` over the ceiling is refused with `413` before the body is read. |
| `DOT_INFERENCE_MAX_IMAGES_PER_REQUEST` | `10` | Most images one generation request may ask for. Ten is the ceiling the OpenAI images API itself documents for `n`. Over it, a `400` naming `n`. |

> Earlier task notes refer to "the three config keys". There are four: `DOT_INFERENCE_MAX_IMAGES_PER_REQUEST` was added later, because image generation is priced per image and per-site spend quotas are out of scope — without a ceiling, one accepted request is an unbounded bill.

Each endpoint also registers `@RequestCost(Price.HTTP_FETCH)`, the band used for operations that make one remote round trip, so the instance-wide rate-limit backstop prices them. That price is known to under-count a stream; it is not a per-site AI spend quota, which is out of scope.

---

## `POST /chat/completions`

Non-streaming, with tool declarations:

```bash
curl -X POST "https://demo.dotcms.com/api/inference/v1/chat/completions" \
  -H "Authorization: Bearer $DOTCMS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gpt-4o",
    "messages": [
      { "role": "system", "content": "You are a helpful assistant." },
      { "role": "user", "content": "What is the weather in Bogota?" }
    ],
    "tools": [
      { "type": "function", "function": {
          "name": "get_weather",
          "description": "Current weather for a city",
          "parameters": { "type": "object", "properties": { "city": { "type": "string" } }, "required": ["city"] } } }
    ],
    "tool_choice": "auto",
    "temperature": 0.7,
    "max_tokens": 1024
  }'
```

```json
{
  "id": "chatcmpl-…", "object": "chat.completion", "created": 1789000000, "model": "gpt-4o",
  "choices": [ { "index": 0, "finish_reason": "tool_calls",
    "message": { "role": "assistant", "content": null,
      "tool_calls": [ { "id": "call_1", "type": "function",
        "function": { "name": "get_weather", "arguments": "{\"city\":\"Bogota\"}" } } ] } } ],
  "usage": { "prompt_tokens": 82, "completion_tokens": 17, "total_tokens": 99 }
}
```

The next turn appends the assistant's tool-call message and a `tool` message carrying the result, correlated by `tool_call_id`:

```bash
curl -X POST "https://demo.dotcms.com/api/inference/v1/chat/completions" \
  -H "Authorization: Bearer $DOTCMS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gpt-4o",
    "messages": [
      { "role": "system", "content": "You are a helpful assistant." },
      { "role": "user", "content": "What is the weather in Bogota?" },
      { "role": "assistant", "content": null,
        "tool_calls": [ { "id": "call_1", "type": "function",
          "function": { "name": "get_weather", "arguments": "{\"city\":\"Bogota\"}" } } ] },
      { "role": "tool", "tool_call_id": "call_1", "content": "{\"tempC\":19}" }
    ]
  }'
```

`temperature`, `max_tokens`, `top_p` and `stop` are passed through to the provider, because they change what the caller gets and what the site pays. Incidental fields a standard client sends by default are ignored. `n` is the exception among the fields this family does not honor: asking for more than one choice is refused with a `400` rather than silently ignored. `response_format` is accepted.

### Streaming

Set `"stream": true` and the answer is served as `text/event-stream`:

```bash
curl -N -X POST "https://demo.dotcms.com/api/inference/v1/chat/completions" \
  -H "Authorization: Bearer $DOTCMS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gpt-4o",
    "messages": [ { "role": "user", "content": "Write a haiku about caching." } ],
    "stream": true,
    "stream_options": { "include_usage": true }
  }'
```

```
data: {"id":"chatcmpl-…","object":"chat.completion.chunk","created":1789000000,"model":"gpt-4o","choices":[{"index":0,"delta":{"role":"assistant","content":""}}]}

data: {"id":"chatcmpl-…","object":"chat.completion.chunk","created":1789000000,"model":"gpt-4o","choices":[{"index":0,"delta":{"content":"Cold bytes"}}]}

data: {"id":"chatcmpl-…","object":"chat.completion.chunk","created":1789000000,"model":"gpt-4o","choices":[{"index":0,"delta":{},"finish_reason":"stop"}]}

data: [DONE]
```

Every chunk carries `id`, `object`, `created` and `model`, fixed for the life of the stream so a client can correlate the fragments it stitches together. Tool calls stream incrementally: each fragment carries the call's `index`, and `id`, `type` and `function.name` arrive on the first fragment, so a standard reader reassembles the arguments as they land.

Token usage on a stream is emitted **only** when the request asks for it through `stream_options.include_usage`. It arrives as a final chunk with an empty `choices` array, immediately before `[DONE]`. dotCMS builds that chunk itself from the counts the provider abstraction reports and does not forward the option upstream; a usage chunk a provider volunteers unasked is suppressed, because the empty `choices` array is exactly what breaks readers that assume every chunk carries a choice.

**A stream that fails after it has begun** — a provider error mid-generation, or `DOT_INFERENCE_COMPLETION_TIMEOUT_SECONDS` firing — emits the standard error object as its final event and closes **without** `[DONE]`:

```
data: {"error":{"message":"The model provider failed to complete the request","type":"api_error","param":null,"code":null}}
```

Once the first chunk is on the wire the HTTP status can no longer carry a failure, so withholding the done marker is what stops a client that does not parse the error event from reading a truncated answer as a finished one.

---

## `GET /models`

```bash
curl "https://demo.dotcms.com/api/inference/v1/models" \
  -H "Authorization: Bearer $DOTCMS_TOKEN"
```

```json
{ "object": "list", "data": [
  { "id": "gpt-4o",      "object": "model", "created": 1789000000, "owned_by": "dotcms" },
  { "id": "gpt-4o-mini", "object": "model", "created": 1789000000, "owned_by": "dotcms" } ] }
```

`created` is the time the listing was built, the same value on every entry: dotCMS serves a configuration, not a catalogue, and does not know when a vendor published a model.

---

## `POST /embeddings`

`input` accepts a single string or an array of strings; batching is how content is ordinarily embedded.

```bash
curl -X POST "https://demo.dotcms.com/api/inference/v1/embeddings" \
  -H "Authorization: Bearer $DOTCMS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "text-embedding-3-small",
    "input": ["The quick brown fox", "jumps over the lazy dog"]
  }'
```

```json
{ "object": "list", "model": "text-embedding-3-small",
  "data": [ { "object": "embedding", "index": 0, "embedding": [0.0023, -0.0091] },
            { "object": "embedding", "index": 1, "embedding": [0.0512, 0.0034] } ],
  "usage": { "prompt_tokens": 12, "total_tokens": 12 } }
```

The response is always a list with one entry per input, each carrying the `index` of the input it embeds — the caller's only way to correlate a vector back to the text they sent. A batch with one bad element is refused whole rather than cleaned, because dropping an element would shift every index after it.

Refused with a `400` naming `input`: an absent, null or empty `input`; an empty array; an element that is not a string (arrays of token ids are not supported); and a blank or whitespace-only string wherever it appears. Where one element of an array is at fault, the message says which index. There is no separate cap on element count — `DOT_INFERENCE_MAX_REQUEST_BYTES` already bounds what can arrive.

Input text is never logged and never echoed back into an error message.

---

## `POST /images/generations`

```bash
curl -X POST "https://demo.dotcms.com/api/inference/v1/images/generations" \
  -H "Authorization: Bearer $DOTCMS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "dall-e-3",
    "prompt": "A cat in a hammock",
    "n": 2,
    "size": "1024x1024"
  }'
```

```json
{ "created": 1789000000, "data": [ { "b64_json": "iVBORw0KGgo…" }, { "b64_json": "R0lGODlhAQAB…" } ] }
```

**Results are always base64 (`b64_json`), never a hosted URL.** The response shape has no `url` component at all. A URL would mean deciding storage, authentication and lifetime for an artifact generated from a prompt that may carry customer data, so this family declines to create a separately-addressable artifact. Where a provider offers the choice, dotCMS asks it for the inline form so nothing is minted upstream either; where a provider only returns a link, dotCMS fetches and re-encodes it. On those providers an artifact does exist upstream — the guarantee is that a caller never receives one.

`n` is honored; omitting it means one image. It is refused with a `400` naming `n` — never clamped, and never surfaced as a retryable upstream error — when it is:

- below `1`;
- above `DOT_INFERENCE_MAX_IMAGES_PER_REQUEST`;
- above `1` on a site whose configured image model can only produce one. Support is probed from the model implementation rather than a list of provider names, so it cannot rot on a library upgrade. In the current provider set, `GoogleAiGeminiImageModel` is the one that does not support it, while the OpenAI image models do.

A model limitation that can never succeed is a `400` precisely because a `502` would tell a standard client's back-off to keep retrying a request that cannot succeed however long it waits.

`prompt` is required, and is refused with a `400` naming `prompt` when blank. `size` is passed through to the provider as a `WIDTHxHEIGHT` string; where the site carries an `imageSize` setting, the caller's value wins and the site's is the default for a request that omits it, which is what the existing dotAI image endpoint already does.

Prompts are never logged and never echoed back into an error message.

---

## Errors

Every refusal uses the standard OpenAI error envelope, so a client library deserializes a failure into its own types with no adapter. Nothing is wrapped in dotCMS's `ResponseEntityView`.

```json
{ "error": { "message": "The model field is required; there is no implicit default model", "type": "invalid_request_error", "param": "model", "code": null } }
```

**Retryability is carried by the HTTP status, not by a body field** — that is what a standard client's back-off keys off, and the standard error shape has no retryable field.

| Status | `type` | When |
|---|---|---|
| `400` | `invalid_request_error` | Two different things. **Caller mistakes**: missing `model`, empty `messages`, a malformed or uncorrelated tool message, `n > 1` on chat, a bad `input`, a bad `prompt`, an `n` the site's image model or the configured ceiling will not allow, an unresolvable site override. **And a provider refusal a retry cannot fix**: an exhausted provider account, a rejected credential, a model the provider does not serve. The second kind is not the caller's fault and says so — it points at the site's provider account or configuration — but it is a `4xx` because that is the only way to tell a client's back-off to stop |
| `401` | `invalid_request_error` | Anonymous, or a credential that is not a bearer token |
| `403` | `invalid_request_error` | An explicit site override naming a site the caller cannot read |
| `404` | `invalid_request_error` | A model the resolved site has not configured for that capability |
| `413` | `invalid_request_error` | Request body over `DOT_INFERENCE_MAX_REQUEST_BYTES` |
| `429` | `rate_limit_error` | The node is already serving its ceiling of concurrent streamed completions (carries `Retry-After`), **or** the model provider is rate limiting this site (no `Retry-After` — see below) |
| `502` | `api_error` | A genuine upstream fault — the provider had a bad minute. Retryable, and the only provider failure that is |

A `502` carries a fixed, safe message; the provider's own message is logged and never returned, because it can carry the provider's endpoint, account identifiers, or a fragment of the prompt. Retryability is carried by the status alone — the standard error shape has no retryable field, and adding one would break the no-adapter promise these endpoints exist to keep — so a provider rate limit is reported as `429` rather than folded into `502`: the two call for different back-off, and a client that reads `502` treats a busy provider as a broken one. **Which failures are worth retrying is carried entirely by the status.** The standard error shape has no retryable field and adding one would break the no-adapter promise, so a client reads the number: `429` and `502` mean come back, every other `4xx` means do not. That is why a provider account out of credit answers `400` rather than `502` — it will refuse the next attempt identically, and a client that retried it would burn a provider round trip each time for as long as the account stayed empty. Whether a refusal is permanent is read from the provider library's own classification rather than a list of status codes dotCMS maintains, so a new failure kind in a later release is classified without a code change here.

The two `429`s differ in one way that matters to a client. When dotCMS refuses because **this node** is at its streaming ceiling, the response carries `Retry-After: 5` — that is a wait dotCMS knows, so it states it in the form every standard client already honours rather than only in prose. When the refusal comes from the **provider**, no `Retry-After` is sent: the provider knows how long it wants to be left alone and says so in a header the client library discards on its error path, so the value never reaches dotCMS, and inventing one would present a guess to the client as an instruction. Relaying the provider's value needs a custom HTTP client under the provider abstraction and is tracked as follow-up work; until then a client backs off on the status alone for upstream throttling. A site with no usable configuration for the requested capability lands on the same `404` as an unknown model, so a caller cannot use the distinction to learn which sites have dotAI set up.

Only exchange **metadata** is recorded — resolved site, model, token counts, status, duration. Request and response bodies are never written to a log or a durable store, and no conversation is retained after the response is delivered; the family is stateless.

---

## Superseded endpoints

These three `/api/v1/ai/*` operations are now `@Deprecated`. They remain fully functional and no removal date is committed.

| Deprecated | Replaced by |
|---|---|
| `POST /api/v1/ai/text/generate` | `POST /api/inference/v1/chat/completions` |
| `POST /api/v1/ai/image/generate` | `POST /api/inference/v1/images/generations` |
| `POST /api/v1/ai/completions/rawPrompt` | `POST /api/inference/v1/chat/completions` |

Every other `/api/v1/ai/*` endpoint is unaffected and is not superseded.

---

## Related

- [REST API Patterns](REST_API_PATTERNS.md) — JAX-RS, Swagger, `@Schema` rules
- [Configuration Patterns](CONFIGURATION_PATTERNS.md) — `Config.getProperty()` usage
- [Security Patterns](SECURITY_BACKEND.md) — auth, input validation, secure logging
