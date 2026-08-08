import { BunWorkerSandbox } from './bun-worker';
import { NodeWorkerSandbox } from './node-worker';

import type { ISandbox } from './interface';
import type { SandboxConfig } from './types';

/**
 * Low-level worker-sandbox factory. Auto-detects the runtime: native Web Workers on Bun,
 * `worker_threads` on Node. Returns the raw {@link ISandbox} whose `execute(code, context)`
 * the {@link Executor} drives. Most callers want the higher-level `createSandbox` barrel
 * export (which returns a `run(code)` surface) instead of this.
 *
 * Backends are statically imported (not `require`d) so this works in both the ESM and CJS
 * builds — `require` is undefined in an ESM module. Importing both is cheap: each file only
 * declares a class, and `node:worker_threads` (used by the Node backend) is available on Bun too.
 */
export function createWorkerSandbox(config?: SandboxConfig): ISandbox {
    if (typeof (globalThis as Record<string, unknown>).Bun !== 'undefined') {
        return new BunWorkerSandbox(config);
    }

    return new NodeWorkerSandbox(config);
}
