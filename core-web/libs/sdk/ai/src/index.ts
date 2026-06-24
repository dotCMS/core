/**
 * `@dotcms/ai` — the dotCMS agentic runtime.
 *
 * The front door. `createDotCMSRuntime` gives you one runtime with two verbs:
 *   - `request(opts)` — DIRECT, you write the call (no worker).
 *   - `run(code)`     — SANDBOXED, for code a model wrote.
 *
 * This is an execution runtime, NOT an agent: no LLM, no inference, no prompting inside it.
 * The agent is what you build on top.
 *
 * Power users who want the generic engine or the dotCMS building blocks directly should
 * import the subpaths instead:
 *   - `@dotcms/ai/sandbox` — generic sandbox + `defineAdapter` (zero dotCMS)
 *   - `@dotcms/ai/adapter` — `dotcmsAdapter`, request core, context loading/cache
 *   - `@dotcms/ai/spec`    — the OpenAPI spec (opt-in)
 */

export { createDotCMSRuntime } from './runtime';
export type {
    DotCMSRuntime,
    DotCMSRuntimeConfig,
    RuntimeAllow
} from './runtime';

// The typed builder for custom, schema-validated, model-facing operations.
export { defineAdapter, describeAdapterForLLM } from './sandbox/define-adapter';
export type {
    AdapterContext,
    AdapterDef,
    AdapterMethodDef,
    DefinedAdapter
} from './sandbox/define-adapter';

// The one typed error hierarchy, surfaced identically from `request()` and `run()`.
export {
    DotCMSError,
    ValidationError,
    PolicyError,
    HttpError,
    TimeoutError,
    AbortError,
    SandboxError,
    RuntimeError,
    isDotCMSError,
    serializeError
} from './sandbox/errors';
export type { DotCMSErrorCode, SerializedDotCMSError } from './sandbox/errors';

// Result shape returned by `run()`, and the binary-response helpers callers need to decode it.
export type { SandboxResult, SandboxResultError } from './sandbox/types';
export { isBinaryResponseEnvelope } from './adapter/request-core';
export type {
    BinaryResponseEnvelope,
    RequestOptions,
    RequestCallEvent
} from './adapter/request-core';

// Instance-context types injected into `run(code)` as globals.
export type {
    DotCMSContext,
    ContentTypeSummary,
    SiteSummary,
    LanguageSummary,
    CurrentUserSummary
} from './adapter/context';
