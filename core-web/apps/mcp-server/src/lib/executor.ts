import { createSandbox } from './sandbox/bun-worker';

import type { ISandbox } from './sandbox/interface';
import type { Adapter, ExecutionContext, SandboxConfig, SandboxResult } from './types';

export interface ExecutorOptions {
    config?: {
        adapters?: Adapter[];
        sandbox?: SandboxConfig;
    };
    sandboxFactory?: (config?: SandboxConfig) => ISandbox;
}

export class Executor {
    private adapters: Map<string, Adapter> = new Map();
    private sandboxConfig: SandboxConfig;
    private sandboxFactory: (config?: SandboxConfig) => ISandbox;

    constructor(options: ExecutorOptions = {}) {
        this.sandboxConfig = options.config?.sandbox ?? {
            timeout: 5000,
            memoryLimit: 128,
            allowAsync: true,
            globals: {}
        };
        this.sandboxFactory = options.sandboxFactory ?? createSandbox;

        if (options.config?.adapters) {
            for (const adapter of options.config.adapters) {
                this.registerAdapter(adapter);
            }
        }
    }

    registerAdapter(adapter: Adapter): void {
        this.adapters.set(adapter.name, adapter);
    }

    unregisterAdapter(name: string): boolean {
        return this.adapters.delete(name);
    }

    getAdapter(name: string): Adapter | undefined {
        return this.adapters.get(name);
    }

    getAdapterNames(): string[] {
        return Array.from(this.adapters.keys());
    }

    async execute<T = unknown>(
        code: string,
        options: {
            sandbox?: Partial<SandboxConfig>;
            adapters?: string[];
            variables?: Record<string, unknown>;
        } = {}
    ): Promise<SandboxResult<T>> {
        const sandboxConfig = {
            ...this.sandboxConfig,
            ...options.sandbox
        };

        const sandbox = this.sandboxFactory(sandboxConfig);

        try {
            const context = this.buildExecutionContext(options.adapters, options.variables);
            return await sandbox.execute<T>(code, context);
        } finally {
            sandbox.dispose();
        }
    }

    configureSandbox(config: Partial<SandboxConfig>): void {
        this.sandboxConfig = {
            ...this.sandboxConfig,
            ...config
        };
    }

    private buildExecutionContext(
        adapterNames?: string[],
        variables?: Record<string, unknown>
    ): ExecutionContext {
        const adaptersToInclude = adapterNames
            ? adapterNames.filter((name) => this.adapters.has(name))
            : Array.from(this.adapters.keys());

        const adapterMethods: ExecutionContext['adapters'] = {};

        for (const name of adaptersToInclude) {
            const adapter = this.adapters.get(name);
            if (!adapter) continue;

            adapterMethods[name] = {};
            for (const [methodName, method] of adapter.methods) {
                adapterMethods[name][methodName] = method.execute.bind(method);
            }
        }

        return {
            adapters: adapterMethods,
            variables
        };
    }
}

export function createExecutor(options?: ExecutorOptions): Executor {
    return new Executor(options);
}
