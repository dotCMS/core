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
 * Configuration for the sandbox execution environment
 */
export interface SandboxConfig {
    timeout?: number;
    memoryLimit?: number;
    allowAsync?: boolean;
    globals?: Record<string, unknown>;
}

/**
 * Result from sandbox code execution
 */
export interface SandboxResult<T = unknown> {
    success: boolean;
    value?: T;
    error?: {
        name: string;
        message: string;
        stack?: string;
    };
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
