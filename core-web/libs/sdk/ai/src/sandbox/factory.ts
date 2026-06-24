import type { ISandbox } from './interface';
import type { SandboxConfig } from './types';

/**
 * Low-level worker-sandbox factory. Auto-detects the runtime: native Web Workers on Bun,
 * `worker_threads` on Node. Returns the raw {@link ISandbox} whose `execute(code, context)`
 * the {@link Executor} drives. Most callers want the higher-level `createSandbox` barrel
 * export (which returns a `run(code)` surface) instead of this.
 */
export function createWorkerSandbox(config?: SandboxConfig): ISandbox {
    if (typeof (globalThis as Record<string, unknown>).Bun !== 'undefined') {
        const { BunWorkerSandbox } = require('./bun-worker');
        return new BunWorkerSandbox(config);
    }

    const { NodeWorkerSandbox } = require('./node-worker');
    return new NodeWorkerSandbox(config);
}
