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
3. **Given** a streaming request, **When** the stream completes, **Then** token usage for the exchange is available to the client.
4. **Given** a client that disconnects mid-stream, **When** the disconnection is detected, **Then** the request is abandoned without leaving work running on the caller's behalf.

---

### User Story 3 - A site's AI credentials are spent only on that site's behalf (Priority: P2)

An operator running several sites on one dotCMS instance configures dotAI separately per site, or once at system level for all of them. A request arriving for one site uses that site's provider and credentials, falling back to the system-level configuration exactly as the rest of dotAI does; a request that names a site the caller cannot see is refused; and a request whose host name matches no site at all is told so plainly rather than being quietly served by the default site's account. Rotating one site's key takes effect without disturbing the others.

**Why this priority**: Per-site credential governance is the reason to route AI through dotCMS at all — an integration that can silently spend the wrong site's budget is worse than no integration, because the misbilling is invisible. It is second only because Story 1 must exist before it can be governed.

**Independent Test**: On an instance with two sites holding different provider configurations, drive interleaved requests at both and confirm each is answered by its own site's provider; then request an unresolvable host name and confirm an explicit error rather than a borrowed answer from the default site.

**Acceptance Scenarios**:

1. **Given** a request whose host name identifies a configured site, **When** no site override is supplied, **Then** that site's provider and credentials serve the request.
2. **Given** a request carrying an explicit site override — by identifier or host name, as a header or as a query parameter, **When** the caller has read access to that site, **Then** the override wins over the host name.
3. **Given** a request carrying an explicit site override for a site the caller cannot read, **When** the request is made, **Then** it is refused as forbidden.
4. **Given** a request whose host name matches no site or alias, **When** the request is made, **Then** it fails with an explicit error and no other site's credentials are used.
5. **Given** a resolved site with no AI configuration of its own on an instance that has a system-level configuration, **When** a completion is requested, **Then** it is served from the system-level configuration, as every other dotAI endpoint would serve it.
6. **Given** an instance where neither the resolved site nor the system level has any AI configuration, **When** a completion is requested, **Then** it is refused with an explanatory standard-shaped error.
7. **Given** a caller who is not an administrator names a model the resolved site has not configured, **When** the request is made, **Then** it is refused with a standard-shaped "no such model" error rather than the model name being passed through to the provider.
8. **Given** a request naming the reserved model alias, **When** the request is made, **Then** it is served by the resolved site's primary model and its fallback chain, whichever vendor that currently is.
9. **Given** two sites configured with different providers under interleaved load, **When** one site's credentials are rotated, **Then** subsequent requests for that site use the new credentials and requests for the other site are unaffected.
10. **Given** legacy request parameters that elsewhere in dotCMS act as a de-facto site override, **When** they are supplied to this endpoint family, **Then** they are ignored.

---

### User Story 4 - Discover what a site can actually run (Priority: P3)

A developer or an operator asks dotCMS which models are available for a site before wiring anything up, and gets the list the site's own configuration defines, including the fallback chain, so they can pick a model or confirm a site is ready.

**Why this priority**: Useful for setup, tooling, and diagnosis, and required for clients that enumerate models — but an integration can be completed and demonstrated with a known model name, so it follows the exchange itself.

**Independent Test**: Query the model list for a configured site and confirm it matches that site's configuration including fallback entries; query it for an unconfigured site and confirm an empty list.

**Acceptance Scenarios**:

1. **Given** a site with a configured provider and a fallback chain, **When** the model list is requested, **Then** every configured model including the fallback entries is listed in the standard model-list shape, alongside the reserved alias that stands for the site's governed default.
2. **Given** an instance where neither the resolved site nor the system level has any AI configuration, **When** the model list is requested, **Then** an empty list is returned — not another site's models.

---

### User Story 5 - Embeddings and image generation through the same standard door (Priority: P3)

The same client credentials and base URL also serve text embeddings and image generation in their standard shapes, so a developer using a framework's embedding or image helper against dotCMS does not fall back to a bespoke call for those two operations.

**Why this priority**: Completes the surface so an application never needs a second, dotCMS-shaped client for part of its AI work, but neither is on the critical path for an agent loop.

**Independent Test**: Call the embeddings and image-generation operations through a standard client against a configured site and confirm both responses deserialize into the framework's own result types.

**Acceptance Scenarios**:

1. **Given** input text, **When** embeddings are requested, **Then** the vector is returned in the standard embeddings shape with the model and token usage reported.
2. **Given** an image prompt, **When** image generation is requested, **Then** the result is returned in the standard image-generation shape.

---

### Edge Cases

- The upstream provider rejects the call with a rate-limit or server error: the caller receives a standard-shaped error marked retryable, carrying no raw provider envelope, so a framework's own back-off logic engages.
- Neither the resolved site nor the system level has any AI configuration and a completion is requested: an explicit standard-shaped error, never a borrowed configuration from an unrelated site.
- The caller is anonymous or presents no token: refused as unauthorized.
- A conversation contains a tool-result turn with no preceding tool call, or malformed tool declarations: refused with a standard-shaped validation error naming the offending field.
- The request is made from browser JavaScript: no cross-origin headers are emitted, so the browser blocks it — these endpoints are server-side only by design, because the token can do everything its owner can.
- Many concurrent streams: each stream occupies a request thread for the life of the completion, which is the dominant capacity risk on this family and must be understood as such rather than treated as request-count load.
- A caller sends a request field this family does not support: unsupported fields are ignored rather than failing the request, so a client's defaults do not break the call.

## Requirements *(mandatory)*

### Functional Requirements

#### Endpoint family and wire format

- **FR-001**: The system MUST expose a new inference endpoint family whose path carries no vendor name and whose trailing version segment denotes the wire protocol version, distinct from dotCMS's own resource versioning, so a future protocol lands beside it without disturbing dotCMS versioning.
- **FR-002**: The system MUST accept chat completions as an ordered list of messages carrying system, user, assistant, and tool roles, with no single-prompt length ceiling substituting for the model's own context limit.
- **FR-003**: The system MUST return chat completions in the standard chat-completion response shape, populating the response identifier, object type, creation time, model, choice index, finish reason, and token usage.
- **FR-004**: The system MUST accept tool declarations and a tool-choice preference on a chat-completion request, and MUST return the model's tool calls in the standard tool-call shape with a finish reason indicating a tool call.
- **FR-005**: The system MUST accept tool results as subsequent messages, correlated to the tool call that requested them, so a multi-step agent loop completes.
- **FR-006**: The system MUST accept a response-format preference on a chat-completion request.
- **FR-007**: The system MUST emit streaming responses as standard chat-completion chunk events, each carrying the response identifier, object type, creation time, and model, with a finish reason on the final content event and the conventional terminal marker closing the stream.
- **FR-008**: The system MUST stream tool calls incrementally, emitting each tool call's index, identifier, name, and argument fragments so a standard client can reassemble the arguments as they arrive.
- **FR-009**: The system MUST report token usage for streamed exchanges.
- **FR-010**: The system MUST expose a model-listing operation returning the models the resolved site has configured, including every entry of its fallback chains, in the standard model-list shape.
- **FR-011**: The system MUST expose an embeddings operation returning vectors in the standard embeddings shape.
- **FR-012**: The system MUST expose an image-generation operation returning results in the standard image-generation shape.
- **FR-013**: The system MUST ignore request fields it does not support rather than rejecting the request, so a standard client's default payload succeeds.
- **FR-014**: The legacy non-chat completion operation is explicitly out of scope and MUST NOT be added; it is deprecated in the format being adopted.

#### Authentication, authorization, and site resolution

- **FR-015**: The system MUST authenticate callers by dotCMS API token presented as a bearer credential, and MUST refuse anonymous callers as unauthorized.
- **FR-016**: The system MUST accept any authenticated user, backend or frontend, matching the behavior of the existing dotAI endpoints, because a site calling AI on behalf of a visitor is a supported use case.
- **FR-017**: The system MUST resolve the target site from the request's host name by default, so that a base URL alone identifies the site with no client-specific configuration.
- **FR-018**: The system MUST accept an explicit site override, given as either a site identifier or a host name, supplied as a request header, and MUST also accept it as a query parameter for parity with the existing dotAI endpoints. A header is required because every standard client can set headers, whereas an extra query parameter cannot generally be injected.
- **FR-019**: The system MUST enforce read access on the target site whenever an explicit override is supplied, and MUST refuse the request as forbidden when the caller lacks it.
- **FR-020**: The system MUST NOT fall back to a default site when the request's host name matches no site or alias; it MUST fail with an explicit error instead.
- **FR-021**: A resolved site with no AI configuration of its own MUST continue to inherit the system-level configuration, as every other dotAI caller does — this is standard dotCMS Apps inheritance, and many installations configure dotAI once at system level rather than per site. Only when neither the resolved site nor the system level has any AI configuration MUST a completion request be refused with a standard-shaped error. What is removed is the *unmatched-host* fallback of FR-020, which is where the cross-site credential risk actually lives: a client pointed at an unaliased host name, or sitting behind a proxy that rewrites it, must never be silently served by the default site.
- **FR-022**: The model-listing operation MUST return an empty list when neither the resolved site nor the system level has any AI configuration, rather than any other site's models.
- **FR-023**: The system MUST resolve a requested model against the models the resolved site has configured. A non-administrator naming a model the site has not configured MUST be refused with a standard-shaped "no such model" error; administrators MAY select any model the site has configured.
- **FR-024**: The system MUST additionally accept a reserved model alias meaning "whatever this site is configured to use", which resolves to the site's primary model and its fallback chain. This is what lets a site's provider be swapped from one vendor to another with the client left unchanged — a client that hardcodes a vendor-specific model name is necessarily broken by such a swap, and the alias is the way to ask for the governed default rather than a specific vendor's model.
- **FR-025**: The system MUST ignore the legacy request parameters that elsewhere act as a de-facto site override, so a pre-existing override path does not silently apply to a token-authenticated API.
- **FR-026**: Site resolution together with its authorization check MUST live in one shared component, returning the caller, the resolved site, and that site's AI configuration as a single result, and MUST be the only way an endpoint in this area obtains an AI configuration — because two existing dotAI endpoints have already drifted to opposite model-passthrough policies, and a third copy of the decision guarantees a third divergence.

#### Per-site isolation

- **FR-027**: Per-site provider selection, credential resolution, and model fallback MUST reuse the existing site-configuration path, behaving as the existing dotAI endpoints do, with the single deliberate exception of the unmatched-host fallback removed in FR-020.
- **FR-028**: Two sites with different provider configurations under interleaved requests MUST each be served by their own provider instance, and rotating a site's credentials MUST invalidate that site's cached instances without affecting other sites.
- **FR-029**: Provider clients MUST NOT be constructed anywhere outside the existing single client component, because the instance cache is keyed on the site's configuration rather than on the request, and constructing clients elsewhere to obtain tool support would defeat both the cross-site isolation and the credential-rotation invalidation that keying provides.

#### Operational

- **FR-030**: The system MUST NOT emit cross-origin headers on this family, and the endpoints MUST be documented as server-side only, because the credential is a long-lived token with the full authority of its owner and cross-origin support would invite placing it in browser JavaScript.
- **FR-031**: The system MUST translate upstream provider rate-limit and server errors into standard-shaped errors flagged as retryable, without leaking the provider's raw error envelope, so a standard client's back-off logic engages.
- **FR-032**: Each new path MUST register a request cost in the band used for operations that make a remote network round trip, so the existing instance-wide rate-limit backstop prices them correctly. A per-site or per-token AI spend quota is out of scope.
- **FR-033**: No existing dotAI endpoint's behavior may change. Retrieval-augmented completions, semantic search, the vector corpus operations, and provider administration remain first-class, as no standard verb covers them.
- **FR-034**: The three existing operations superseded by this family — text generation, image generation, and raw-prompt completion — MUST be documented as superseded while remaining fully functional.
- **FR-035**: The generated API description document MUST be regenerated from the new endpoint annotations and committed alongside them.

### Key Entities *(include if feature involves data)*

- **Conversation**: The ordered sequence of turns submitted for completion. Each turn carries a role — instruction, user, assistant, or tool result — and its content; assistant turns may additionally carry tool calls, and tool-result turns reference the call they answer.
- **Tool declaration**: A callable the client declares available, described by name, purpose, and the shape of its arguments, so the model can request its execution.
- **Tool call**: The model's request to execute a declared tool, identified so its result can be correlated back, and carrying the arguments to pass. In a stream it arrives as an indexed sequence of fragments.
- **Completion**: The model's answer, carrying an identifier, the model that produced it, a creation time, one or more choices each with a finish reason, and the token usage for the exchange.
- **Model descriptor**: One entry in a site's available-model list, including entries that exist only as fallback links in a chain, plus the reserved alias standing for the site's governed default.
- **Embedding**: A numeric vector for a piece of input text, reported with the model that produced it and the tokens it consumed.
- **Site inference configuration**: The provider, credentials, model, and fallback chain a single site has configured. Read server-side only; never returned to a caller.
- **Error**: A refusal in the standard error shape, carrying a type, a message, and where applicable the offending field and whether the condition is retryable.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A developer integrates dotCMS as their agent's model provider by setting exactly two values — a base URL and a token — writing zero lines of dotCMS-specific client code.
- **SC-002**: A multi-step agent running against dotCMS through an unmodified standard client completes a task requiring at least two tool executions and one reasoning step over the tool results.
- **SC-003**: A streamed response is consumed end to end by an unmodified standard stream reader, which assembles the full message, the complete tool-call arguments, and the terminal state without custom parsing.
- **SC-004**: Swapping a site's AI provider from one vendor to another in the site's configuration and re-running the same agent, unchanged — asking for the reserved alias rather than a vendor-specific model name — produces a working completion from the new vendor, demonstrating dotCMS as the governed gateway.
- **SC-005**: In 100% of requests, the credentials spent belong to the site the request resolved to; no request is ever served from a different site's configuration.
- **SC-006**: A request against an instance with no AI configuration at either the resolved site or the system level, or for a host name matching no site, receives an explanatory error 100% of the time, and never a successful completion.
- **SC-007**: A non-administrator cannot cause a model outside the resolved site's configuration to be invoked, under any request payload.
- **SC-008**: Every one of the four supported operations returns payloads that deserialize into a standard client library's own result types with no adapter.
- **SC-009**: All existing dotAI operations return byte-identical results before and after this change.
- **SC-010**: An upstream rate-limit or outage surfaces to the caller as a retryable error that a standard client's default back-off handles without developer intervention.

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: The dotAI area — its REST surface, its per-site App configuration lookup, and its provider client. The endpoint family is new and additive, but two shared pieces are modified in place: the site-resolution helper grows into the single authorization-carrying entry point, and the provider client gains tool support and standards-conformant stream assembly. Site resolution itself reaches into the older site/host resolution surface, which carries two distinct silent fallbacks, and this family treats them differently: an unmatched host name resolving to the default site is the cross-site credential risk and is declined here, while a site without its own AI configuration inheriting the system-level one is ordinary dotCMS Apps inheritance and is kept. The declined fallback remains in place for every other caller.
- **Backward-compatibility expectations**: No existing dotAI endpoint changes behavior, including the two that share the silent-fallback and model-passthrough weaknesses described here; hardening those is deliberately deferred to a separate change so it gets its own review, release note, and rollback classification. Three existing operations become superseded — documented as such, still functional, not removed. Because this adds a public API contract, it falls in a rollback-sensitive category and should be labeled accordingly.
- **Known related decisions**: One existing dotAI endpoint already pins non-administrators to the site's configured model — this family carries that precedent forward, rather than repeating the unchecked model passthrough of the sibling endpoint that lacks it. Reading a site's AI configuration as the system user is the standard dotCMS Apps design and keeps the secret server-side, so the question here is authorization to *use* a site's credentials, not secret exposure. Related work: [#37491](https://github.com/dotCMS/core/issues/37491) hardens the existing dotAI endpoints and adopts this shared component there; [#37433](https://github.com/dotCMS/core/issues/37433) is the client-side counterpart and is blocked by this. The plan phase will formally consult `dotCMS/platform-adrs`.

## Assumptions

- The adopted wire format is the OpenAI chat-completions format. This is a scope decision, not an implementation detail: it is the de-facto protocol served by OpenRouter, LiteLLM, Vercel AI Gateway, Ollama, vLLM, LM Studio, Groq, and Together, so adopting it is what makes dotCMS work out of the box with the major AI frameworks and official SDKs.
- Conformance is judged against what standard clients actually require, not against every field the upstream format documents. Fields no mainstream client depends on may be omitted; the acceptance bar is that an unmodified standard client works.
- The underlying provider abstraction already supports every concept needed — tool declarations, tool choice, response format, tool calls in responses, tool-result turns, incremental tool-call streaming, and token usage — at the version already pinned in the build. No dependency upgrade is assumed, and the work concentrates in the provider client rather than the HTTP layer.
- Existing per-site provider support, credential storage, fallback chains, and connection testing are reused as-is; this feature adds a standard front door, not a second gateway.
- The host name is a sufficient default site signal because the HTTP specification requires the header, so no client-specific configuration is needed for the common case. Deployments behind a proxy that rewrites it use the explicit override.
- **Resolved 2026-09-10** — the source issue described system-level configuration inheritance and the unmatched-host default-site fallback as one problem. They are not: only the second can spend a site's credentials on a request that was never meant for it. Inheritance is kept (FR-021), so installations that configure dotAI once at system level keep working on this family exactly as they do on the existing endpoints, and this family stays consistent with the rest of dotAI on that point.
- **Resolved 2026-09-10** — the source issue's requirement that an unconfigured model name be refused conflicts with its own demo expectation that a site's provider can be swapped with the client left unchanged. A client that hardcodes a vendor-specific model name cannot survive a vendor swap, so the reserved alias of FR-024 is the mechanism that makes the demo true, while concrete unknown names are still refused. Choosing the alias's exact spelling is a plan-phase decision.
- Errors follow the same standard shape as successful responses, since that is what standard clients parse for messages and retry decisions.
- Token-usage reporting depends on the upstream provider returning it; where a provider omits it, usage is reported as unavailable rather than fabricated.
- Streaming holds a request thread for the life of a completion. Capacity planning for this family is about concurrent streams, not request rate.
- A per-site or per-token AI spend budget is out of scope and needs its own design. The existing instance-wide rate limiter is off by default, prices local resource time rather than provider spend, and cannot express a per-site quota; the honest position is that an authenticated site member can consume a site's AI budget until that follow-up ships.
- No user-interface work is in scope. This is an API-only change.
