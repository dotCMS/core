import { serializeError } from './errors';
import { BUN_WORKER_CODE, DEFAULT_RESOURCE_LIMITS } from './worker-harness';

import type { ISandbox } from './interface';
import type {
    AdapterCallMessage,
    ExecutionContext,
    ResolvedSandboxConfig,
    ResultMessage,
    SandboxConfig,
    SandboxResult
} from './types';

const DEFAULT_CONFIG: ResolvedSandboxConfig = {
    timeout: 5000,
    globals: {},
    // Kept for config parity with the Node backend; Bun's Web Worker cannot enforce these
    // (no `resourceLimits` option), so they are advisory here — documented, not silently dropped.
    resourceLimits: DEFAULT_RESOURCE_LIMITS
};

export class BunWorkerSandbox implements ISandbox {
    private config: ResolvedSandboxConfig;

    constructor(config?: SandboxConfig) {
        this.config = { ...DEFAULT_CONFIG, ...config };
    }

    getConfig(): SandboxConfig {
        return { ...this.config };
    }

    configure(config: Partial<SandboxConfig>): void {
        this.config = { ...this.config, ...config };
    }

    async execute<T = unknown>(code: string, context: ExecutionContext): Promise<SandboxResult<T>> {
        const startTime = performance.now();

        return new Promise((resolve) => {
            const adapterMethods: Record<string, string[]> = {};
            for (const [name, methods] of Object.entries(context.adapters)) {
                adapterMethods[name] = Object.keys(methods);
            }

            const workerCode = BUN_WORKER_CODE;

            const blob = new Blob([workerCode], { type: 'application/javascript' });
            const url = URL.createObjectURL(blob);
            const worker = new Worker(url);

            let resolved = false;
            const cleanup = () => {
                if (!resolved) {
                    resolved = true;
                    worker.terminate();
                    URL.revokeObjectURL(url);
                    // Let the host abort any in-flight adapter work (threaded AbortSignal).
                    this.config.onTeardown?.();
                }
            };

            const timeoutId = setTimeout(() => {
                if (!resolved) {
                    cleanup();
                    resolve({
                        success: false,
                        error: {
                            name: 'TimeoutError',
                            code: 'TIMEOUT',
                            message: `Execution timed out after ${this.config.timeout}ms`
                        },
                        logs: [],
                        executionTime: performance.now() - startTime
                    });
                }
            }, this.config.timeout);

            worker.onmessage = async (event: MessageEvent) => {
                const { type, ...data } = event.data as { type: string } & Record<string, unknown>;

                if (type === 'ready') {
                    worker.postMessage({ type: 'execute', data: { code } });
                } else if (type === 'adapter_call') {
                    const { adapter, method, args, id } = data as unknown as AdapterCallMessage;
                    // Posting to a worker that was already terminated (e.g. by a
                    // timeout while this adapter call was in flight) throws and
                    // would escape as an unhandled rejection, crashing the host.
                    const postResult = (payload: Record<string, unknown>) => {
                        if (resolved) {
                            return;
                        }
                        try {
                            worker.postMessage({ type: 'adapter_result', data: payload });
                        } catch {
                            /* worker gone — nothing to deliver the result to */
                        }
                    };
                    try {
                        const adapterObj = context.adapters[adapter];
                        if (!adapterObj || !adapterObj[method]) {
                            throw new Error(`Adapter method not found: ${adapter}.${method}`);
                        }
                        const result = await (adapterObj[method] as (...a: unknown[]) => unknown)(
                            ...args
                        );
                        postResult({ id, result });
                    } catch (err) {
                        // Send the serialized error (typed shape round-trips); drop `stack`
                        // so host stacks don't cross into the sandbox.
                        const error = serializeError(err);
                        delete error.stack;
                        postResult({ id, error });
                    }
                } else if (type === 'result') {
                    clearTimeout(timeoutId);
                    cleanup();
                    const r = data as unknown as ResultMessage;
                    resolve({
                        success: r.success,
                        value: r.value as T,
                        error: r.error,
                        logs: r.logs || [],
                        executionTime: performance.now() - startTime
                    });
                }
            };

            worker.onerror = (error: ErrorEvent) => {
                clearTimeout(timeoutId);
                cleanup();
                resolve({
                    success: false,
                    error: {
                        name: 'WorkerError',
                        code: 'RUNTIME',
                        message: error.message || 'Unknown worker error'
                    },
                    logs: [],
                    executionTime: performance.now() - startTime
                });
            };

            worker.postMessage({
                type: 'init',
                data: {
                    variables: context.variables || {},
                    adapterMethods,
                    globals: this.config.globals
                }
            });
        });
    }
    dispose(): void {
        // Workers are created per execution, nothing to dispose
    }
}
