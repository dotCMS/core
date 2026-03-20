import type { ExecutionContext, SandboxConfig, SandboxResult } from '../types';

export interface ISandbox {
    execute<T = unknown>(code: string, context: ExecutionContext): Promise<SandboxResult<T>>;
    getConfig(): SandboxConfig;
    configure(config: Partial<SandboxConfig>): void;
    dispose(): void;
}

export type SandboxFactory = (config?: SandboxConfig) => ISandbox;
