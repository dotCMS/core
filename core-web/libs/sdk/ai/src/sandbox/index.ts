/**
 * `@dotcms/ai/sandbox` — the generic execution engine. ZERO dotCMS code lives here (a
 * lint-enforced module boundary); this subpath is fully generic. The dotCMS wiring lives in
 * `@dotcms/ai/adapter`, and the front door that combines them in `@dotcms/ai`.
 */
import { type AdapterContext, type RequestLike, isDefinedAdapter } from './define-adapter';
import { Executor } from './executor';

import type { Adapter, SandboxResourceLimits, SandboxResult } from './types';

// ---- Generic engine surface ----------------------------------------------------------

export { Executor, createExecutor } from './executor';
export type { ExecutorOptions } from './executor';

export { createWorkerSandbox } from './factory';
export type { ISandbox, SandboxFactory } from './interface';

export {
    defineAdapter,
    isDefinedAdapter,
    describeAdapterForLLM
} from './define-adapter';
export type {
    AdapterContext,
    AdapterDef,
    AdapterMethodDef,
    DefinedAdapter,
    RequestLike
} from './define-adapter';

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
} from './errors';
export type { DotCMSErrorCode, SerializedDotCMSError } from './errors';

export type {
    Adapter,
    AdapterMethod,
    AdapterMethodParameter,
    SandboxConfig,
    SandboxResourceLimits,
    SandboxResult,
    SandboxResultError,
    ExecutionContext
} from './types';

// ---- High-level sandbox ---------------------------------------------------------------

export interface CreateSandboxConfig {
    /** Adapters granted to the sandboxed code — the only doors out. */
    adapters: Adapter[];
    /** Wall-clock timeout (ms). */
    timeout?: number;
    /** Extra globals injected into the sandbox. */
    globals?: Record<string, unknown>;
    /** Per-execution memory/stack caps. */
    resourceLimits?: SandboxResourceLimits;
    /**
     * Host `request` capability injected into `defineAdapter` handlers as `ctx.request`. When
     * provided, defined adapters are bound to it (and the per-run abort signal) before running.
     * For the generic engine this is optional; the dotCMS front door always provides one.
     */
    request?: (opts: RequestLike) => Promise<unknown>;
}

export interface Sandbox {
    /** Run code in the confined worker. Returns the structured result. */
    run<T = unknown>(code: string): Promise<SandboxResult<T>>;
}

/**
 * Create a sandbox over a set of adapters — the generic entry point shown in §6.2.
 *
 * `defineAdapter` results are bound to the injected host `request` (and a per-run abort
 * signal) via `withContext`; plain hand-built adapters are passed through untouched. Each
 * `run()` gets its own `AbortController`, so a timeout aborts in-flight host work.
 */
export function createSandbox(config: CreateSandboxConfig): Sandbox {
    return {
        async run<T = unknown>(code: string): Promise<SandboxResult<T>> {
            const controller = new AbortController();
            const ctx: AdapterContext = {
                request: config.request ?? notProvided,
                signal: controller.signal
            };

            const adapters = config.adapters.map((a) =>
                isDefinedAdapter(a) ? a.withContext(ctx) : a
            );

            const executor = new Executor({
                config: {
                    adapters,
                    sandbox: {
                        timeout: config.timeout,
                        globals: config.globals,
                        resourceLimits: config.resourceLimits,
                        onTeardown: () => controller.abort()
                    }
                }
            });

            return executor.execute<T>(code, {
                adapters: adapters.map((a) => a.name)
            });
        }
    };
}

const notProvided = (): Promise<never> => {
    throw new Error(
        'This adapter needs a host `request` capability. Pass `request` to createSandbox(), ' +
            'or use createDotCMSRuntime() from "@dotcms/ai" which provides one.'
    );
};
