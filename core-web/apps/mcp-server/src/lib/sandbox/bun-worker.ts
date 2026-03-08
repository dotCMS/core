import type { ExecutionContext, SandboxConfig, SandboxResult } from '../types';
import type { ISandbox } from './interface';

const DEFAULT_CONFIG: Required<SandboxConfig> = {
    timeout: 5000,
    globals: {}
};

export class BunWorkerSandbox implements ISandbox {
    private config: Required<SandboxConfig>;

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

            const workerCode = this.buildWorkerCode();

            const blob = new Blob([workerCode], { type: 'application/javascript' });
            const url = URL.createObjectURL(blob);
            const worker = new Worker(url);

            let resolved = false;
            const cleanup = () => {
                if (!resolved) {
                    resolved = true;
                    worker.terminate();
                    URL.revokeObjectURL(url);
                }
            };

            const timeoutId = setTimeout(() => {
                if (!resolved) {
                    cleanup();
                    resolve({
                        success: false,
                        error: {
                            name: 'TimeoutError',
                            message: `Execution timed out after ${this.config.timeout}ms`
                        },
                        logs: [],
                        executionTime: performance.now() - startTime
                    });
                }
            }, this.config.timeout);

            worker.onmessage = async (event: MessageEvent) => {
                const { type, ...data } = event.data;

                if (type === 'ready') {
                    worker.postMessage({ type: 'execute', data: { code } });
                } else if (type === 'adapter_call') {
                    const { adapter, method, args, id } = data;
                    try {
                        const adapterObj = context.adapters[adapter];
                        if (!adapterObj || !adapterObj[method]) {
                            throw new Error(`Adapter method not found: ${adapter}.${method}`);
                        }
                        const result = await (adapterObj[method] as (...a: unknown[]) => unknown)(
                            ...args
                        );
                        worker.postMessage({ type: 'adapter_result', data: { id, result } });
                    } catch (err) {
                        const error = err instanceof Error ? err.message : String(err);
                        worker.postMessage({ type: 'adapter_result', data: { id, error } });
                    }
                } else if (type === 'result') {
                    clearTimeout(timeoutId);
                    cleanup();
                    resolve({
                        success: data.success,
                        value: data.value as T,
                        error: data.error,
                        logs: data.logs || [],
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

    private buildWorkerCode(): string {
        return `
      // Block direct network access — all calls must go through adapters
      globalThis.fetch = undefined;
      globalThis.XMLHttpRequest = undefined;
      globalThis.WebSocket = undefined;
      globalThis.EventSource = undefined;

      const logs = [];
      const pendingCalls = new Map();
      let callId = 0;

      const console = {
        log: (...args) => logs.push(args.map(a => typeof a === 'object' ? JSON.stringify(a) : String(a)).join(' ')),
        warn: (...args) => logs.push('[WARN] ' + args.map(a => typeof a === 'object' ? JSON.stringify(a) : String(a)).join(' ')),
        error: (...args) => logs.push('[ERROR] ' + args.map(a => typeof a === 'object' ? JSON.stringify(a) : String(a)).join(' ')),
        info: (...args) => logs.push('[INFO] ' + args.map(a => typeof a === 'object' ? JSON.stringify(a) : String(a)).join(' ')),
      };
      globalThis.console = console;

      globalThis.pick = (arr, fields) => {
        if (!Array.isArray(arr)) return arr;
        return arr.map(item => {
          const result = {};
          for (const field of fields) {
            const parts = field.split('.');
            let value = item;
            let key = parts[parts.length - 1];
            for (const part of parts) value = value?.[part];
            result[key] = value;
          }
          return result;
        });
      };

      globalThis.table = (arr, maxRows = 10) => {
        if (!Array.isArray(arr) || arr.length === 0) return '(empty)';
        const items = arr.slice(0, maxRows);
        const keys = Object.keys(items[0]);
        const header = '| ' + keys.join(' | ') + ' |';
        const sep = '|' + keys.map(() => '---').join('|') + '|';
        const rows = items.map(item => '| ' + keys.map(k => String(item[k] ?? '')).join(' | ') + ' |');
        let result = [header, sep, ...rows].join('\\n');
        if (arr.length > maxRows) result += '\\n... +' + (arr.length - maxRows) + ' more rows';
        return result;
      };

      globalThis.count = (arr, field) => {
        if (!Array.isArray(arr)) return {};
        return arr.reduce((acc, item) => {
          const key = String(item[field] ?? 'unknown');
          acc[key] = (acc[key] || 0) + 1;
          return acc;
        }, {});
      };

      globalThis.sum = (arr, field) => {
        if (!Array.isArray(arr)) return 0;
        return arr.reduce((acc, item) => acc + (Number(item[field]) || 0), 0);
      };

      globalThis.first = (arr, n = 5) => {
        if (!Array.isArray(arr)) return arr;
        return arr.slice(0, n);
      };

      self.onmessage = async (event) => {
        const { type, data } = event.data;

        if (type === 'init') {
          const { variables, adapterMethods, globals } = data;

          for (const [key, value] of Object.entries(variables || {})) {
            globalThis[key] = value;
          }

          for (const [key, value] of Object.entries(globals || {})) {
            globalThis[key] = value;
          }

          globalThis.adapters = {};
          for (const [adapterName, methods] of Object.entries(adapterMethods)) {
            const adapterObj = {};
            for (const methodName of methods) {
              adapterObj[methodName] = async (...args) => {
                const id = ++callId;
                return new Promise((resolve, reject) => {
                  pendingCalls.set(id, { resolve, reject });
                  self.postMessage({
                    type: 'adapter_call',
                    adapter: adapterName,
                    method: methodName,
                    args,
                    id
                  });
                });
              };
            }
            globalThis.adapters[adapterName] = adapterObj;
            globalThis[adapterName] = adapterObj;
          }

          self.postMessage({ type: 'ready' });
        }

        else if (type === 'adapter_result') {
          const { id, result, error } = data;
          const pending = pendingCalls.get(id);
          if (pending) {
            pendingCalls.delete(id);
            if (error) pending.reject(new Error(error));
            else pending.resolve(result);
          }
        }

        else if (type === 'execute') {
          try {
            const AsyncFunction = Object.getPrototypeOf(async function(){}).constructor;
            const fn = new AsyncFunction(data.code);
            const result = await fn();
            self.postMessage({ type: 'result', success: true, value: result, logs });
          } catch (err) {
            self.postMessage({
              type: 'result',
              success: false,
              error: { name: err.name, message: err.message, stack: err.stack },
              logs
            });
          }
        }
      };
    `;
    }

    dispose(): void {
        // Workers are created per execution, nothing to dispose
    }
}

export function createSandbox(config?: SandboxConfig): ISandbox {
    return new BunWorkerSandbox(config);
}
