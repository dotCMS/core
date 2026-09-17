# Feature Specification: OpenAI-Compatible Inference Endpoints

**Feature Branch**: `fmontes/inference-37431`

**Created**: 2026-09-10

**Status**: Draft

**Type**: New Feature

**Input**: GitHub issue [dotCMS/core#37431](https://github.com/dotCMS/core/issues/37431) — "Add OpenAI-compatible inference endpoints at /api/inference/v1"

## Why

dotAI is already a multi-provider AI gateway: seven providers, per-site credentials held in App secrets, model fallback chains, and connection testing. What it lacks is a door the ecosystem can open. Its endpoints take a single capped prompt string rather than a conversation, expose no tool calling at any layer, emit stream events that standard clients cannot parse, and answer `/completions` with a retrieval-augmented envelope instead of a chat response. Any team wanting to route AI through dotCMS must therefore write and maintain a bespoke client — so nobody does. They put provider keys directly in their own application instead, and the per-site credential governance dotAI provides is thrown away along with the audit point, the ability to swap vendors centrally, and the guarantee that a site's keys never leave the server.

This feature puts the wire format the ecosystem already speaks in front of the gateway that already exists, so that a base URL plus a dotCMS API token is the entire integration.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Drive dotCMS as a model provider from a standard AI client (Priority: P1)

An application developer building an agent points their existing AI framework at dotCMS by setting two configuration values — a base URL and an API token — and nothing else. They send a multi-turn conversation with a system instruction, declare the tools their agent can execute, receive the model's request to call one of those tools, execute it, and send the result back on the next turn. No dotCMS-specific client code is written, and the framework's own types and helpers are used unchanged.

**Why this priority**: This is the feature. Multi-turn conversations plus a tool-calling round trip are the minimum for an agent loop to run more than one step, and today the loop stops after one turn. Everything else in this spec is either a refinement of this exchange or an additional surface alongside it.

**Independent Test**: Configure a standard OpenAI-compatible client with only the dotCMS base URL and an API token, run a two-step tool-calling conversation against a site with dotAI configured, and confirm the agent completes a task that requires executing a tool and reasoning over its result.

**Acceptance Scenarios**:

1. **Given** a site with dotAI configured and a valid API token, **When** the client posts a conversation containing system, user, and assistant turns, **Then** the response carries the assistant's reply in the standard chat-completion shape, with the identifier, object type, creation time, model, finish reason, and token-usage fields all populated.
2. **Given** the same setup, **When** the client posts a conversation together with a set of tool declarations, **Then** the response's finish reason indicates a tool call and the response carries the tool name and its arguments in the standard tool-call shape.
3. **Given** a prior response that requested a tool call, **When** the client posts the conversation again with the assistant's tool-call turn and a tool-result turn appended, **Then** the model's answer incorporates the tool result and the conversation continues.
4. **Given** a conversation that exceeds the length the legacy prompt field allowed, **When** it is posted as a conversation, **Then** it is accepted — there is no single-prompt character ceiling standing in for a context limit.

---

### User Story 2 - Stream tokens and tool calls in a form standard clients can parse (Priority: P2)

The same developer enables streaming so their user interface renders the answer as it arrives. Their framework's stream reader consumes the events without custom parsing, shows tokens as they land, surfaces tool-call arguments as they accumulate, and knows cleanly when the response has finished and why.

**Why this priority**: Streaming is how essentially every AI application presents output, so in practice this ships alongside Story 1 — but a non-streaming tool-calling loop is already a working, demonstrable integration, which makes this separable and second.

**Independent Test**: Enable streaming in a standard client against a site with dotAI configured and confirm the client's own stream reader assembles the full message, the tool-call arguments, and the terminal state without any dotCMS-specific handling.

**Acceptance Scenarios**:

1. **Given** a streaming request, **When** the response is read by a standard client, **Then** every event carries the identifier, object type, creation time, and model fields, the final event carries a finish reason, and the stream is terminated by the conventional done marker.
2. **Given** a streaming request that results in a tool call, **When** the stream is read, **Then** the tool call's index, identifier, name, and argument fragments arrive incrementally and reassemble into the complete arguments.
3. **Given** a streaming request that asks for usage through the standard streaming option, **When** the stream completes, **Then** token usage for the exchange reaches the client; **and given** a streaming request that does not ask for it, **Then** no usage event is emitted.
4. **Given** a client that disconnects mid-stream, **When** the disconnection is detected, **Then** the request is abandoned without leaving work running on the caller's behalf.
5. **Given** a stream that has already delivered content, **When** the provider fails or the completion timeout fires, **Then** the client receives a standard error object as the final event and the stream closes without the done marker, so a failed response is never indistinguishable from a complete one.

---

### User Story 3 - A site's AI credentials are spent only on that site's behalf (Priority: P2)

An operator running several sites on one dotCMS instance configures dotAI separately per site, or once at system level for all of them. A request arriving for one site uses that site's provider and credentials, falling back to the system-level configuration exactly as the rest of dotAI does; a request that names a site the caller cannot see is refused; and every response states which site's credentials served it, so an operator can always tell who paid. Rotating one site's key takes effect without disturbing the others.

**Why this priority**: Per-site credential governance is the reason to route AI through dotCMS at all, and its weak point is not resolution but visibility — spend that cannot be attributed to a site is spend an operator cannot manage. It is second only because Story 1 must exist before it can be governed.

**Independent Test**: On an instance with two sites holding different provider configurations, drive interleaved requests at both and confirm each is answered by its own site's provider and that every response names the site that served it; then request an unresolvable host name and confirm the default site serves it, the fallback is logged, and the response says so.

**Acceptance Scenarios**:

1. **Given** a request whose host name identifies a configured site, **When** no site override is supplied, **Then** that site's provider and credentials serve the request.
2. **Given** a request carrying an explicit site override — by identifier or host name, as a header or as a query parameter, **When** the caller has read access to that site, **Then** the override wins over the host name.
3. **Given** a request carrying an explicit site override for a site the caller cannot read, **When** the request is made, **Then** it is refused as forbidden.
4. **Given** a request whose host name matches no site or alias, **When** the request is made, **Then** it is served by the default site exactly as every other request in dotCMS resolves, the fallback is logged with the unmatched host name, and the response header names the site that served it.
5. **Given** a resolved site with no AI configuration of its own on an instance that has a system-level configuration, **When** a completion is requested, **Then** it is served from the system-level configuration, as every other dotAI endpoint would serve it.
6. **Given** an instance where neither the resolved site nor the system level has any AI configuration, **When** a completion is requested, **Then** it is refused with an explanatory standard-shaped error.
7. **Given** a caller who is not an administrator names a model the resolved site has not configured, **When** the request is made, **Then** it is refused with a standard-shaped "no such model" error rather than the model name being passed through to the provider.
8. **Given** a request that omits the model, **When** the request is made, **Then** it is refused with a standard-shaped validation error naming the missing field — the model-listing operation, not an implicit default, is how a caller discovers what the site runs.
9. **Given** two sites configured with different providers under interleaved load, **When** one site's credentials are rotated, **Then** subsequent requests for that site use the new credentials and requests for the other site are unaffected.
10. **Given** legacy request parameters that elsewhere in dotCMS act as a de-facto site override, **When** they are supplied to this endpoint family, **Then** they are ignored.

---

### User Story 4 - Discover what a site can actually run (Priority: P3)

A developer or an operator asks dotCMS which models are available for a site before wiring anything up, and gets the list the site's own configuration defines, including the fallback chain, so they can pick a model or confirm a site is ready.

**Why this priority**: Useful for setup, tooling, and diagnosis, and required for clients that enumerate models — but an integration can be completed and demonstrated with a known model name, so it follows the exchange itself.

**Independent Test**: Query the model list for a configured site and confirm it matches that site's configuration including fallback entries; query it for an unconfigured site and confirm an empty list.

**Acceptance Scenarios**:

1. **Given** a site with a configured provider and a fallback chain, **When** the model list is requested, **Then** every configured model including the fallback entries is listed in the standard model-list shape, and nothing else — no synthetic or reserved entry that does not correspond to a model the site configured.
2. **Given** an instance where neither the resolved site nor the system level has any AI configuration, **When** the model list is requested, **Then** an empty list is returned — not another site's models.

---

### User Story 5 - Embeddings and image generation through the same standard door (Priority: P3)

The same client credentials and base URL also serve text embeddings and image generation in their standard shapes, so a developer using a framework's embedding or image helper against dotCMS does not fall back to a bespoke call for those two operations.

**Why this priority**: Completes the surface so an application never needs a second, dotCMS-shaped client for part of its AI work, but neither is on the critical path for an agent loop.

**Independent Test**: Call the embeddings and image-generation operations through a standard client against a configured site and confirm both responses deserialize into the framework's own result types.

**Acceptance Scenarios**:

1. **Given** input text and a model the site has configured for embeddings, **When** embeddings are requested, **Then** the vector is returned in the standard embeddings shape with the model and token usage reported; an embeddings model the site has not configured is refused as for chat.
2. **Given** an image prompt, **When** image generation is requested, **Then** the image is returned inline as base64 in the standard image-generation shape, with no separately-addressable URL created.

---

### Edge Cases

- The upstream provider rejects the call with a rate-limit or server error before anything is sent: the caller receives the matching HTTP status (429 or 5xx) with a standard-shaped body carrying no raw provider envelope, so a framework's own back-off logic engages.
- Neither the resolved site nor the system level has any AI configuration and a completion is requested: an explicit standard-shaped error, never a borrowed configuration from an unrelated site.
- The host name matches no site or alias — a container service name, internal DNS, or `localhost`: the default site serves the request, as it would anywhere else in dotCMS; the fallback is logged and the response names the serving site.
- The upstream provider fails, or the completion timeout fires, **after** streaming has begun: the caller receives a standard error object as the final stream event and the connection closes with no done marker, so the failure is never mistaken for a finished answer.
- A request omits the model, or names one the site has not configured: refused with a validation error or a "no such model" error respectively. There is no implicit default and no reserved alias.
- A request exceeds the configured maximum size: refused with a standard-shaped error naming the limit, before the payload is forwarded to a provider.
- The caller is anonymous or presents no token: refused as unauthorized.
- A conversation contains a tool-result turn with no preceding tool call, or malformed tool declarations: refused with a standard-shaped validation error naming the offending field.
- The request is made from browser JavaScript: no cross-origin headers are emitted, so the browser blocks it — these endpoints are server-side only by design, because the token can do everything its owner can.
- Many concurrent streams: each stream occupies a request thread for the life of the completion, which is the dominant capacity risk on this family and must be understood as such rather than treated as request-count load.
- A caller sends an incidental field this family does not act on: it is ignored rather than failing the request, so a client's default payload does not break the call. A field that would change what the caller gets or pays for is rejected instead of disregarded — see FR-013.

## Requirements *(mandatory)*

### Functional Requirements

#### Endpoint family and wire format

- **FR-001**: The system MUST expose a new inference endpoint family rooted at `/api/inference/v1`. The path carries no vendor name, and the trailing segment is the **wire-protocol** version, deliberately distinct from dotCMS's own `/api/v1` resource versioning. A second wire format is a **sibling under the same version**, not a version bump: were the Responses format added later it would land at `/api/inference/v1/responses`, alongside `/api/inference/v1/chat/completions`, which is how every comparable gateway serves both. `/api/inference/v2` is reserved for a genuinely incompatible revision of this family, not for an additional format.
- **FR-002**: The system MUST accept chat completions as an ordered list of messages carrying system, user, assistant, and tool roles, with no single-prompt length ceiling substituting for the model's own context limit.
- **FR-003**: The system MUST return chat completions in the standard chat-completion response shape, populating the response identifier, object type, creation time, model, choice index, finish reason, and token usage.
- **FR-004**: The system MUST accept tool declarations and a tool-choice preference on a chat-completion request, and MUST return the model's tool calls in the standard tool-call shape with a finish reason indicating a tool call.
- **FR-005**: The system MUST accept tool results as subsequent messages, correlated to the tool call that requested them, so a multi-step agent loop completes.
- **FR-006**: The system MUST accept a response-format preference on a chat-completion request.
- **FR-007**: The system MUST emit streaming responses as standard chat-completion chunk events, each carrying the response identifier, object type, creation time, and model, with a finish reason on the final content event and the conventional terminal marker closing the stream.
- **FR-008**: The system MUST stream tool calls incrementally, emitting each tool call's index, identifier, name, and argument fragments so a standard client can reassemble the arguments as they arrive.
- **FR-009**: The system MUST report token usage for streamed exchanges **when the client requests it through the standard streaming option**, and MUST NOT emit it otherwise. In the adopted format a usage event carries an empty choices array, which some stream readers do not tolerate — emitting it unconditionally would break clients SC-003 and SC-008 promise to support.
- **FR-010**: The system MUST expose a model-listing operation returning the models the resolved site has configured, including every entry of its fallback chains, in the standard model-list shape.
- **FR-011**: The system MUST expose an embeddings operation returning vectors in the standard embeddings shape. Because a site's embeddings model is configured separately from its chat models, the model validation of FR-023 MUST apply to this operation against the site's configured **embeddings** models rather than its chat models.
- **FR-012**: The system MUST expose an image-generation operation returning results in the standard image-generation shape, delivering the image as a **base64 payload** rather than a hosted URL. A URL would require deciding storage, authentication and lifetime for content generated from a possibly sensitive prompt; returning the bytes inline avoids creating a durable, separately-addressable artifact at all. Should a URL form be offered later, it MUST be authenticated and time-limited.
- **FR-013**: The system MUST ignore request fields that do not change output semantics — the incidental fields a standard client sends by default — so a client's default payload succeeds. It MUST NOT silently ignore a field that changes what the caller gets or pays for. Specifically, the common sampling parameters (`temperature`, `max_tokens`, `top_p`, `stop`) MUST be passed through to the provider, and a field that changes output semantics but cannot be honored (for example a request for several choices, which this family does not support) MUST be rejected with a standard-shaped validation error naming it, rather than accepted and disregarded. Silently dropping a caller's token ceiling is a cost and correctness failure, not a compatibility courtesy.
- **FR-014**: The legacy non-chat completion operation is explicitly out of scope and MUST NOT be added; it is deprecated in the format being adopted.

#### Authentication, authorization, and site resolution

- **FR-015**: The system MUST authenticate callers by dotCMS API token presented as a bearer credential, and MUST refuse anonymous callers as unauthorized. The session cookies and basic authentication that surrounding dotCMS filters generally accept MUST NOT be accepted on this family: it is server-side only by design (FR-030), and admitting ambient browser credentials is what would make the absence of cross-origin headers a formality rather than a control.
- **FR-016**: The system MUST accept any authenticated user, backend or frontend, matching the behavior of the existing dotAI endpoints, because a site calling AI on behalf of a visitor is a supported use case.
- **FR-017**: The system MUST resolve the target site from the request's host name by default, so that a base URL alone identifies the site with no client-specific configuration.
- **FR-018**: The system MUST accept an explicit site override, given as either a site identifier or a host name, supplied as a request header, and MUST also accept it as a query parameter for parity with the existing dotAI endpoints. A header is required because every standard client can set headers, whereas an extra query parameter cannot generally be injected. The query parameter MUST NOT reuse the name of any legacy de-facto site override, since FR-025 requires those to be ignored and a shared name would make the two requirements contradict.
- **FR-019**: The system MUST enforce read access on the target site whenever an explicit override is supplied, and MUST refuse the request as forbidden when the caller lacks it.
- **FR-020**: Site resolution MUST follow standard dotCMS host resolution, including the fallback to the default site when the host name matches no site or alias — this family introduces no resolution semantics of its own, because an endpoint family that resolves differently from the rest of the product is its own source of surprise, and because the server-side callers this family exists for routinely present a host name that is not a site alias (internal DNS, container service names, `localhost`). The risk the fallback carries is that nobody can tell which site paid, so the system MUST answer that directly: a fallback resolution MUST be logged with the unmatched host name, and every response MUST identify the site whose configuration served it. That identification MUST be a **response header**, not a body field — a body field would have to appear in a payload that SC-008 requires to deserialize into a standard client's own types with no adapter, whereas a header is invisible to those types, identical for streamed and non-streamed responses, present on errors as well as successes, and readable by the proxy or log pipeline that would perform the reconciliation SC-005 describes. The header MUST carry the site identifier, and MAY additionally carry the hostname.
- **FR-021**: A resolved site with no AI configuration of its own MUST continue to inherit the system-level configuration, as every other dotAI caller does — this is standard dotCMS Apps inheritance, and many installations configure dotAI once at system level rather than per site. Only when neither the resolved site nor the system level has any AI configuration MUST a completion request be refused with a standard-shaped error.
- **FR-022**: The model-listing operation MUST return an empty list when neither the resolved site nor the system level has any AI configuration, rather than any other site's models.
- **FR-023**: The system MUST resolve a requested model against the models the resolved site has configured, for **every** caller regardless of role. Naming a model the site has not configured MUST be refused with a standard-shaped "no such model" error. There is deliberately no administrator exemption: a role-conditional passthrough would reintroduce exactly the branching FR-026 exists to eliminate, and would make SC-007 untestable as a blanket invariant. An administrator who needs another model configures it on the site.
- **FR-024**: The model MUST be supplied by the caller. A request without one MUST be refused with a standard-shaped validation error naming the field; the system MUST NOT substitute an implicit default, and MUST NOT reserve any alias or sentinel model name of its own. This matches every comparable API — the format being adopted, the Anthropic Messages API, OpenRouter and Vercel AI Gateway all require the field — and it keeps the set of accepted model names exactly equal to the set the site configured, with no reserved word that a provider's own naming could collide with. The model-listing operation (FR-010) is the discovery mechanism: a caller wanting whatever the site currently runs reads the list and takes the first entry, which is the site's primary model. **This deliberately reverses the source issue's expectation that a provider swap leaves clients unchanged** — a caller pinning a vendor model must re-read the list after a swap, which is how every other gateway behaves and what developers already expect.
- **FR-025**: The system MUST ignore the legacy request parameters that elsewhere act as a de-facto site override, so a pre-existing override path does not silently apply to a token-authenticated API.
- **FR-026**: Site resolution together with its authorization check MUST live in one shared component, returning the caller, the resolved site, and that site's AI configuration as a single result, and MUST be the only way an endpoint in this area obtains an AI configuration — because two existing dotAI endpoints have already drifted to opposite model-passthrough policies, and a third copy of the decision guarantees a third divergence.

#### Per-site isolation

- **FR-027**: Per-site provider selection, credential resolution, and model fallback MUST reuse the existing site-configuration path, behaving exactly as the existing dotAI endpoints do. This family defines no exceptions to it.
- **FR-028**: Two sites with different provider configurations under interleaved requests MUST each be served by their own provider instance, and rotating a site's credentials MUST invalidate that site's cached instances without affecting other sites.
- **FR-029**: Provider clients MUST NOT be constructed anywhere outside the existing single client component, because the instance cache is keyed on the site's configuration rather than on the request, and constructing clients elsewhere to obtain tool support would defeat both the cross-site isolation and the credential-rotation invalidation that keying provides.

#### Operational and forward compatibility

- **FR-030**: The system MUST NOT emit cross-origin headers on this family, and the endpoints MUST be documented as server-side only, because the credential is a long-lived token with the full authority of its owner and cross-origin support would invite placing it in browser JavaScript.
- **FR-031**: The system MUST translate upstream provider rate-limit and server errors into the standard error shape without leaking the provider's raw envelope. Retryability MUST be conveyed by the **HTTP status code** — 429 for rate limiting, 5xx for upstream failure — because that is what a standard client's back-off keys off; the standard error shape carries no retryable field, and inventing one would put this requirement in tension with SC-008. The response body MUST stay conformant to the standard error shape.
- **FR-032**: Each new path MUST register a request cost in the band used for operations that make a remote network round trip, so the existing instance-wide rate-limit backstop prices them correctly. A per-site or per-token AI spend quota is out of scope.
- **FR-033**: No existing dotAI endpoint's behavior may change. Retrieval-augmented completions, semantic search, the vector corpus operations, and provider administration remain first-class, as no standard verb covers them.
- **FR-034**: The three existing operations superseded by this family — text generation, image generation, and raw-prompt completion — MUST be documented as superseded while remaining fully functional.
- **FR-035**: The generated API description document MUST be regenerated from the new endpoint annotations and committed alongside them.
- **FR-036**: The system MUST record only **metadata** about an exchange — resolved site, model, token counts, status, duration — and MUST NOT write request or response bodies to any log or durable store. Prompts and tool arguments routinely carry customer data, and FR-020 mandates logging on the fallback path, so silence here would leave an implementer to guess. Conversation content is not retained after the response is delivered; this family is stateless.
- **FR-037**: The system MUST bound capacity explicitly, with a maximum number of concurrent streams, a hard completion timeout, and a maximum request size, all configurable and all with stated defaults. An over-size request MUST be refused with a standard-shaped error naming the limit, rather than left to a container-level rejection the caller cannot interpret. Removing the legacy 4096-character prompt cap (FR-002) was correct — it was a character count standing in for a model's context window — but it left request size unbounded, and dotCMS parses and forwards a payload before any provider rejects it, so the memory cost lands on the node and the token cost on the site's bill first. Streaming parks a request thread for the life of a completion, which the Edge Cases identify as the dominant capacity risk on this family, and an unbounded risk is not a managed one.
- **FR-038**: The internal representation a request is mapped into MUST NOT be a direct binding of the chat-completions JSON, and tool-call identity MUST be first-class within it rather than reconstructed from a streaming index. The chat-completions surface is then one serialization of that representation. This is what keeps a future `/api/inference/v1/responses` (FR-001) a second serializer rather than a re-plumb: an internal model shaped directly by chat-completions semantics discards the item identity a richer format needs before that format is ever written. This requirement constrains the internal shape only; it does not add any Responses-format behavior to this scope.
- **FR-039**: When a streamed response fails after the stream has begun — an upstream error mid-generation, or the completion timeout of FR-037 firing — the system MUST emit the standard error object as a final stream event and then close the connection **without** the terminal done marker. A status code is no longer available once the first chunk is sent, so the error event is what lets a client distinguish failure from a short answer; withholding the done marker means a client that does not parse the event still sees an incomplete stream rather than a cleanly finished one. Closing silently, or closing with the done marker, are both prohibited: the first leaves the caller unable to tell an outage from a timeout, and the second reports a truncated answer as complete.

### Key Entities *(include if feature involves data)*

- **Conversation**: The ordered sequence of turns submitted for completion. Each turn carries a role — instruction, user, assistant, or tool result — and its content; assistant turns may additionally carry tool calls, and tool-result turns reference the call they answer.
- **Tool declaration**: A callable the client declares available, described by name, purpose, and the shape of its arguments, so the model can request its execution.
- **Tool call**: The model's request to execute a declared tool, identified so its result can be correlated back, and carrying the arguments to pass. In a stream it arrives as an indexed sequence of fragments.
- **Completion**: The model's answer, carrying an identifier, the model that produced it, a creation time, one or more choices each with a finish reason, and the token usage for the exchange.
- **Model descriptor**: One entry in a site's available-model list, including entries that exist only as fallback links in a chain. The first entry is the site's primary model.
- **Embedding**: A numeric vector for a piece of input text, reported with the model that produced it and the tokens it consumed.
- **Site inference configuration**: The provider, credentials, model, and fallback chain a single site has configured. Read server-side only; never returned to a caller.
- **Error**: A refusal in the standard error shape, carrying a type, a message, and where applicable the offending field. Retryability is not a field of it — it is carried by the HTTP status.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A developer integrates dotCMS as their agent's model provider by setting exactly two values — a base URL and a token — writing zero lines of dotCMS-specific client code.
- **SC-002**: A multi-step agent running against dotCMS through an unmodified standard client completes a task requiring at least two tool executions and one reasoning step over the tool results.
- **SC-003**: A streamed response is consumed end to end by an unmodified standard stream reader, which assembles the full message, the complete tool-call arguments, and the terminal state without custom parsing.
- **SC-004**: Swapping a site's AI provider from one vendor to another in the site's configuration takes effect for every caller without redeploying, rotating a credential, or touching any application: the model list reports the new vendor's models, and a caller that reads it gets working completions from the new vendor. Callers pinning a model from the previous vendor receive an explicit "no such model" error rather than a silent substitution.
- **SC-005**: Every response — success, error, streamed or not — carries a header naming the site whose configuration served it, so an operator reconciling provider spend can attribute 100% of requests to a site without reading server logs.
- **SC-006**: A request against an instance with no AI configuration at either the resolved site or the system level receives an explanatory error 100% of the time, and never a successful completion.
- **SC-007**: No caller, of any role, can cause a model outside the resolved site's configuration to be invoked, under any request payload.
- **SC-008**: Every one of the four supported operations returns payloads that deserialize into a standard client library's own result types with no adapter.
- **SC-009**: All existing dotAI operations accept the same requests and return the same response contract before and after this change, evidenced by the existing integration and Postman suites passing unchanged. (Provider output is non-deterministic, so the invariant is the contract, not the bytes.)
- **SC-010**: An upstream rate-limit or outage surfaces to the caller as an HTTP status a standard client's default back-off handles without developer intervention.
- **SC-011**: Concurrent streams beyond the configured maximum are refused with a standard-shaped error rather than degrading the instance, and no single completion holds a request thread past the configured timeout.

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: The dotAI area — its REST surface, its per-site App configuration lookup, and its provider client. The endpoint family is new and additive, but two shared pieces are modified in place: the site-resolution helper grows into the single authorization-carrying entry point, and the provider client gains tool support and standards-conformant stream assembly. Site resolution itself reaches into the older site/host resolution surface, whose two silent fallbacks — an unmatched host name resolving to the default site, and a site without its own AI configuration inheriting the system-level one — are both **kept**, so this family resolves exactly as the rest of the product does. The source issue asked for both to be removed here; that is declined deliberately, because per-family resolution semantics are their own source of surprise and because the concern behind the request is attribution rather than resolution. FR-020 answers attribution by naming the serving site on every response; a per-site spend quota answers it fully and is out of scope. **This family now diverges from #37431 in three recorded places: both silent site fallbacks are kept rather than removed, and the model is required with no alias, so a provider swap is not invisible to callers. The fallback decision also conflicts with the second acceptance criterion of #37491, which must be amended to match — otherwise the one shared resolver of FR-026 would need a per-family strictness flag, which is the drift FR-026 exists to prevent.**
- **Backward-compatibility expectations**: No existing dotAI endpoint changes behavior, including the two that share the silent-fallback and model-passthrough weaknesses described here; hardening those is deliberately deferred to a separate change so it gets its own review, release note, and rollback classification. Three existing operations become superseded — documented as such, still functional, not removed. Because this adds a public API contract, it falls in a rollback-sensitive category and should be labeled accordingly.
- **Known related decisions**: One existing dotAI endpoint already pins non-administrators to the site's configured model — this family carries that precedent forward, rather than repeating the unchecked model passthrough of the sibling endpoint that lacks it. Reading a site's AI configuration as the system user is the standard dotCMS Apps design and keeps the secret server-side, so the question here is authorization to *use* a site's credentials, not secret exposure. Related work: [#37491](https://github.com/dotCMS/core/issues/37491) hardens the existing dotAI endpoints and adopts this shared component there; [#37433](https://github.com/dotCMS/core/issues/37433) is the client-side counterpart and is blocked by this. The plan phase will formally consult `dotCMS/platform-adrs`.

## Assumptions

- The adopted wire format is the OpenAI chat-completions format. This is a scope decision, not an implementation detail: it is the de-facto protocol served by OpenRouter, LiteLLM, Vercel AI Gateway, Ollama, vLLM, LM Studio, Groq, and Together, so adopting it is what makes dotCMS work out of the box with the major AI frameworks and official SDKs.
- **Resolved 2026-09-14 — the Responses format was weighed and deliberately deferred, not overlooked.** OpenAI has not deprecated chat completions and has published no sunset date, while recommending Responses for new projects. Responses is the better-designed protocol — it carries explicit tool-call item identity where chat-completions streaming requires index-based reassembly (FR-008), and it preserves reasoning items across turns. It was still declined for this scope on four grounds. Its exclusive capabilities — hosted web search, file search, code interpreter, computer use — are OpenAI-hosted and cannot be offered across seven providers, so the implementation would be partial in a way clients could not detect. The pinned provider abstraction is message-based; only the OpenAI providers speak Responses upstream, so for the other six dotCMS would not be proxying the protocol but inventing it on top of messages. Responses stores conversations by default, which would require per-site conversation persistence, retention and deletion that this spec does not carry (see FR-036). And the selection criterion here is what clients emit when pointed at a custom base URL, which remains chat completions. Comparable multi-provider gateways reached through a custom base URL — Vercel AI Gateway, Portkey, LiteLLM — expose both formats as separate surfaces rather than choosing, and each added chat completions first. **So this is sequencing, not exclusion**: FR-001 reserves the sibling path and FR-038 constrains the internal representation so that adding it later is a serializer rather than a rewrite.
- Conformance is judged against what standard clients actually require, not against every field the upstream format documents. Fields no mainstream client depends on may be omitted; the acceptance bar is that an unmodified standard client works.
- The underlying provider abstraction already supports every concept needed — tool declarations, tool choice, response format, tool calls in responses, tool-result turns, incremental tool-call streaming, and token usage — at the version already pinned in the build. No dependency upgrade is assumed, and the work concentrates in the provider client rather than the HTTP layer.
- Existing per-site provider support, credential storage, fallback chains, and connection testing are reused as-is; this feature adds a standard front door, not a second gateway.
- The host name is a sufficient default site signal because the HTTP specification requires the header, so no client-specific configuration is needed for the common case. Deployments behind a proxy that rewrites it use the explicit override.
- **Resolved 2026-09-10** — the source issue asked this family to remove two silent fallbacks: system-level configuration inheritance, and the unmatched-host default-site fallback. Both are kept. Inheritance is ordinary dotCMS Apps behaviour, and the default-site fallback is how every request in the product resolves; making one endpoint family resolve differently would break the server-side callers this family is built for, since internal DNS, container service names and `localhost` are not site aliases. The concern the issue raised is real, but it is about attribution, not resolution — so FR-020 requires the serving site to be named on every response and the fallback to be logged, and the out-of-scope spend quota answers the rest.
- **Resolved 2026-09-14** — the source issue's requirement that an unconfigured model name be refused conflicts with its own demo expectation that a site's provider can be swapped with the client left unchanged. The conflict is resolved in favour of the refusal, and **the demo expectation is deliberately not met**: the model is required, there is no alias or implicit default, and a caller pinning a vendor model must re-read the model list after a swap. A reserved sentinel was considered and rejected — it is unconventional for this class of API, it must be defended against collision with provider model names that are arbitrary vendor strings, and it is unreachable from clients whose SDK types the model field as required. What the gateway governs is credentials, provider routing, allowlisting and spend attribution; it does not claim that a vendor change is invisible to callers, and no comparable gateway claims that either.
- Errors follow the same standard shape as successful responses, since that is what standard clients parse for messages and retry decisions.
- Token-usage reporting depends on the upstream provider returning it; where a provider omits it, usage is reported as unavailable rather than fabricated.
- Streaming holds a request thread for the life of a completion. Capacity planning for this family is about concurrent streams, not request rate.
- A per-site or per-token AI spend budget is out of scope and needs its own design. The existing instance-wide rate limiter is off by default, prices local resource time rather than provider spend, and cannot express a per-site quota; the honest position is that an authenticated site member can consume a site's AI budget until that follow-up ships.
- No user-interface work is in scope. This is an API-only change.
