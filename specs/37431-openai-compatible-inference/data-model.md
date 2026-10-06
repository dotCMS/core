# Data Model: OpenAI-Compatible Inference Endpoints

**Feature**: [spec.md](./spec.md) · **Plan**: [plan.md](./plan.md) · **Date**: 2026-09-14

Two layers, deliberately separate (FR-038): the **internal representation**, which is provider-neutral and carries tool-call identity as a first-class field, and the **wire views**, which are one serialization of it. The wire layer depends on the internal layer; never the reverse. Nothing here is persisted — this family is stateless (FR-036).

---

## Internal representation — `com.dotcms.inference.model`

### `InferenceRequest`

Immutable; `Serializable` so it can travel as the payload of `AIRequest<InferenceRequest>` (research R1).

| Field | Type | Required | Validation |
|---|---|---|---|
| `model` | `String` | **yes** | Non-blank. Must appear in the resolved site's configured models for the operation's section (FR-023, FR-024, R7); unknown → `NoSuchModelError` 404; absent → 400 naming the field |
| `messages` | `List<InferenceMessage>` | yes | Non-empty. A `TOOL` message must follow an assistant turn whose `toolCalls` contains its `toolCallId` |
| `tools` | `List<InferenceToolSpec>` | no | Each entry needs a non-blank `name` and a valid JSON-schema `parameters` object |
| `toolChoice` | `ToolChoice` | no | `AUTO`, `REQUIRED`, `NONE`, or a named function |
| `responseFormat` | `ResponseFormat` | no | Text, JSON object, or JSON schema |
| `stream` | `boolean` | no | Default `false` |
| `includeUsageInStream` | `boolean` | no | Default `false`; set from the standard streaming option (FR-009, R5) |
| `temperature` | `Double` | no | Passed through (FR-013) |
| `maxOutputTokens` | `Integer` | no | Passed through (FR-013) |
| `topP` | `Double` | no | Passed through (FR-013) |
| `stopSequences` | `List<String>` | no | Passed through (FR-013) |

**Rejected rather than ignored** (FR-013): any field that changes output semantics but cannot be honored — notably a request for several choices — fails with a validation error naming it.

### `InferenceMessage`

| Field | Type | Required | Notes |
|---|---|---|---|
| `role` | `Role` | yes | `SYSTEM`, `USER`, `ASSISTANT`, `TOOL` |
| `content` | `String` | conditional | Required except on an assistant turn that carries only `toolCalls` |
| `toolCalls` | `List<InferenceToolCall>` | no | `ASSISTANT` only |
| `toolCallId` | `String` | conditional | Required on `TOOL`; must match a `toolCalls[].id` from an earlier assistant turn (FR-005) |
| `name` | `String` | no | Tool name on a `TOOL` turn |

### `InferenceToolCall`

The type FR-038 exists to protect. `id` is carried from the provider, **never** derived from a streaming index.

| Field | Type | Required | Notes |
|---|---|---|---|
| `id` | `String` | yes | Stable identity; correlates the later `TOOL` result |
| `name` | `String` | yes | Tool to execute |
| `arguments` | `String` | yes | JSON text as produced by the model; not parsed by dotCMS |
| `index` | `int` | no | Position within the turn. Serialization detail for the wire format only — the internal model never uses it for identity |

### `InferenceToolSpec`

| Field | Type | Required |
|---|---|---|
| `name` | `String` | yes |
| `description` | `String` | no |
| `parameters` | `JsonNode` | yes — a JSON Schema object |

### `InferenceResponse`

| Field | Type | Notes |
|---|---|---|
| `id` | `String` | Generated per response |
| `model` | `String` | The model that actually served it — may be a fallback-chain entry rather than the requested one (FR-027) |
| `createdEpochSeconds` | `long` | |
| `message` | `InferenceMessage` | Assistant turn; carries `toolCalls` when the model requested tools |
| `finishReason` | `FinishReason` | `STOP`, `LENGTH`, `TOOL_CALLS`, `CONTENT_FILTER`, `ERROR` |
| `usage` | `InferenceUsage` | Absent when the provider does not report it — never fabricated |

### `InferenceUsage`

`inputTokens`, `outputTokens`, `totalTokens` — all `Integer`, all nullable, mapped from LangChain4j `TokenUsage`.

### `InferenceStreamEvent`

A sealed set; the SSE serializer is a total function over it.

| Variant | Carries | Emitted as |
|---|---|---|
| `ContentDelta` | text fragment | chunk with `delta.content` |
| `ToolCallDelta` | tool-call `index`, `id`, `name`, partial arguments | chunk with `delta.tool_calls[]` (FR-008) |
| `Finish` | `finishReason` | chunk with `finish_reason` |
| `Usage` | `InferenceUsage` | chunk with empty `choices` — **only** when `includeUsageInStream` (FR-009) |
| `Error` | `InferenceError` | error event, then close **without** `[DONE]` (FR-039) |

### `InferenceError`

| Field | Type | Notes |
|---|---|---|
| `type` | `String` | e.g. `invalid_request_error`, `not_found_error`, `rate_limit_error` |
| `message` | `String` | Never carries the provider's raw envelope (FR-031) |
| `param` | `String` | Offending field where applicable |
| `httpStatus` | `int` | **Retryability lives here, not in the body** (FR-031): 429 and 5xx are retryable |

### `InferenceLimits`

Config-backed (research R4). `maxConcurrentStreams`, `completionTimeoutSeconds`, `maxRequestBytes` — read via `Config.getIntProperty`.

### `ResolvedAiContext` — `com.dotcms.ai.rest`

Returned by the shared resolver (FR-026, research R9). Immutable triple: `User user`, `Host host`, `AppConfig config`. Obtaining these separately is what the type exists to prevent.

---

## Wire views — `com.dotcms.inference.rest.view`

Concrete DTOs so `@Schema(implementation = ...)` matches the real return type and the generated `openapi.yaml` stays truthful (research R6). These mirror the OpenAI shapes field-for-field and deliberately do **not** use the dotCMS `ResponseEntityView` envelope — see the plan's Complexity Tracking.

| View | Serializes | Endpoint |
|---|---|---|
| `ChatCompletionView` | `InferenceResponse` | `POST /chat/completions` (non-streaming) |
| `ChatCompletionChunkView` | `InferenceStreamEvent` | `POST /chat/completions` (SSE event payload) |
| `ModelListView` | the site's configured models for a section | `GET /models` |
| `EmbeddingListView` | vectors + model + usage | `POST /embeddings` |
| `ImageGenerationView` | base64 image data (FR-012) | `POST /images/generations` |
| `InferenceErrorView` | `InferenceError` minus `httpStatus` | all — status carries retryability |

---

## Relationships

```
InferenceRequest 1──* InferenceMessage
InferenceMessage 0──* InferenceToolCall        (assistant turns only)
InferenceMessage 0──1 toolCallId ──▶ InferenceToolCall.id   (tool turns; correlation, FR-005)
InferenceRequest 0──* InferenceToolSpec
InferenceResponse 1──1 InferenceMessage
InferenceResponse 0──1 InferenceUsage
ResolvedAiContext 1──1 AppConfig ──▶ ProviderConfig sections: chat | embeddings | image  (R7)
```

## Validation summary

| Rule | Source | Failure |
|---|---|---|
| Model present | FR-024 | 400, field named |
| Model configured for the operation's section | FR-023, R7 | 404 `NoSuchModelError` |
| At least one message | FR-002 | 400 |
| Tool result correlates to a prior tool call | FR-005 | 400 |
| Request body within `maxRequestBytes` | FR-037 | 413, limit named |
| Concurrent streams within `maxConcurrentStreams` | FR-037 | 429 |
| Site READ when an explicit override is supplied | FR-019 | 403 |
| Authenticated, non-anonymous, bearer token only | FR-015, FR-016 | 401 |

## Commit-worthiness

**Commit this file.** It carries field-level shapes a future developer would otherwise have to reconstruct from the mappers — specifically the internal representation FR-038 mandates and the correlation rules between tool calls and tool results. It is not a restatement of `spec.md`, which stays above field level.
