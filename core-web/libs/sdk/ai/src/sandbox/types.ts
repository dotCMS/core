/**
 * Configuration for an adapter method parameter
 */
export interface AdapterMethodParameter {
    name: string;
    type: 'string' | 'number' | 'boolean' | 'object' | 'array';
    description?: string;
    required?: boolean;
    default?: unknown;
}

/**
 * A method exposed by an adapter
 */
export interface AdapterMethod {
    name: string;
    description?: string;
    parameters: AdapterMethodParameter[];
    execute: (...args: unknown[]) => unknown | Promise<unknown>;
}

/**
 * A registered adapter instance
 */
export interface Adapter {
    name: string;
    description?: string;
    version: string;
    methods: Map<string, AdapterMethod>;
    config?: unknown;
}

/**
 * Per-execution resource caps for the worker. Maps onto Node's `worker_threads`
 * `resourceLimits`; ignored fields are simply not applied on runtimes that lack them.
 */
export interface SandboxResourceLimits {
    /** Max old-generation heap (MB). The main memory cap. */
    maxOldGenerationSizeMb?: number;
    /** Max young-generation heap (MB). */
    maxYoungGenerationSizeMb?: number;
    /** Worker stack size (MB). */
    stackSizeMb?: number;
}

/**
 * Configuration for the sandbox execution environment.
 *
 * The boundary this enforces is **capability confinement for trusted code generators, NOT a
 * defense against adversarial code** (see the package threat model). It blocks accidental
 * egress and runaway cost; it is not a hardened isolate.
 */
export interface SandboxConfig {
    /** Wall-clock timeout (ms) for one execution. On expiry the worker is terminated. */
    timeout?: number;
    /** Extra globals injected into the worker (e.g. `spec`). */
    globals?: Record<string, unknown>;
    /** Per-execution memory/stack caps. */
    resourceLimits?: SandboxResourceLimits;
    /**
     * Fired when the execution is torn down (timeout, error, or completion) so the host can
     * abort in-flight adapter work (the threaded `AbortController.abort()`). The runtime wires
     * this to the same signal it gave the adapter, so a sandbox timeout stops an in-flight fetch.
     */
    onTeardown?: () => void;
}

/** The serializable error shape carried back from an execution. Mirrors SerializedDotCMSError. */
export interface SandboxResultError {
    name: string;
    message: string;
    /** Machine-readable error code from the typed hierarchy, when available. */
    code?: string;
    /** Only present when the runtime opts into leaking stacks to executed code. */
    stack?: string;
    /** Type-specific structured detail. */
    detail?: Record<string, unknown>;
}

/** Fully-resolved sandbox config (timeout/globals always present). Shared by both backends. */
export type ResolvedSandboxConfig = SandboxConfig & {
    timeout: number;
    globals: Record<string, unknown>;
};

// ---- Worker host↔worker message protocol (shared by the Node and Bun backends) --------

export interface WorkerMessage {
    type: string;
    [key: string]: unknown;
}
export interface AdapterCallMessage {
    adapter: string;
    method: string;
    args: unknown[];
    id: number;
}
export interface ResultMessage {
    success: boolean;
    value?: unknown;
    error?: SandboxResultError;
    logs?: string[];
}

/**
 * Result from sandbox code execution
 */
export interface SandboxResult<T = unknown> {
    success: boolean;
    value?: T;
    error?: SandboxResultError;
    logs: string[];
    executionTime: number;
}

/**
 * Context passed to sandbox execution
 */
export interface ExecutionContext {
    adapters: Record<string, Record<string, (...args: unknown[]) => unknown | Promise<unknown>>>;
    variables?: Record<string, unknown>;
}
