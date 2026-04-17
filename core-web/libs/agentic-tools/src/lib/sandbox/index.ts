import type { SandboxConfig } from '../types';
import type { ISandbox } from './interface';

export { type ISandbox } from './interface';

export function createSandbox(config?: SandboxConfig): ISandbox {
    if (typeof (globalThis as Record<string, unknown>).Bun !== 'undefined') {
        const { BunWorkerSandbox } = require('./bun-worker');
        return new BunWorkerSandbox(config);
    }

    const { NodeWorkerSandbox } = require('./node-worker');
    return new NodeWorkerSandbox(config);
}
