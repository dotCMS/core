/**
 * The sandbox worker harness — the code that runs INSIDE the worker.
 *
 * It is identical for both backends (Node `worker_threads` and Bun Web Workers); the only
 * difference is the transport, so each backend prepends a tiny bootstrap that defines two
 * functions the harness calls — `__post(msg)` and `__onMessage(handler)` — and (for Node)
 * neutralizes `require`. Keeping the harness in ONE place means a protocol change (e.g. the
 * error round-tripping below) is written once, not copy-pasted into two ~150-line strings.
 *
 * Hoisted to a module constant (it is fully static) so it is not re-allocated per execution.
 */
const HARNESS_BODY = `
      // Block direct network access — all calls must go through adapters
      const __networkError = () => { throw new Error('Network access is disabled in sandbox'); };
      const __disableGlobalApi = (name) => {
        try {
          Object.defineProperty(globalThis, name, { value: __networkError, writable: false, configurable: false });
        } catch { /* ignore if already frozen */ }
      };
      __disableGlobalApi('fetch');
      __disableGlobalApi('XMLHttpRequest');
      __disableGlobalApi('WebSocket');
      __disableGlobalApi('EventSource');
      if (globalThis.navigator && typeof globalThis.navigator === 'object') {
        try {
          Object.defineProperty(globalThis.navigator, 'sendBeacon', { value: __networkError, writable: false, configurable: false });
        } catch { /* ignore */ }
      }

      // Clean process environment so sandboxed code cannot read host secrets.
      if (typeof process !== 'undefined') { process.env = {}; }

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

      __onMessage(async (msg) => {
        const { type, data } = msg;

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
                  __post({
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

          __post({ type: 'ready' });
        }

        else if (type === 'adapter_result') {
          const { id, result, error } = data;
          const pending = pendingCalls.get(id);
          if (pending) {
            pendingCalls.delete(id);
            if (error) {
              // 'error' is the serialized DotCMSError shape ({ name, code, message, detail }).
              // Rebuild a plain Error (class identity can't cross the boundary) but keep the
              // code/name/detail so model code can branch on them.
              const e = new Error(error.message);
              if (error.name) e.name = error.name;
              if (error.code) e.code = error.code;
              if (error.detail) e.detail = error.detail;
              pending.reject(e);
            }
            else pending.resolve(result);
          }
        }

        else if (type === 'execute') {
          try {
            const AsyncFunction = Object.getPrototypeOf(async function(){}).constructor;
            const fn = new AsyncFunction(data.code);
            const result = await fn();
            __post({ type: 'result', success: true, value: result, logs });
          } catch (err) {
            __post({
              type: 'result',
              success: false,
              error: { name: err.name, message: err.message, code: err.code, detail: err.detail, stack: err.stack },
              logs
            });
          }
        }
      });
`;

/**
 * Build the full worker source for a backend. `bootstrap` defines `__post` / `__onMessage`
 * (and, on Node, neutralizes `require`) before the shared harness body runs.
 */
function buildWorkerSource(bootstrap: string): string {
    return `${bootstrap}\n${HARNESS_BODY}`;
}

/** Node `worker_threads` bootstrap: transport via `parentPort`, plus `require` removal. */
const NODE_WORKER_CODE = buildWorkerSource(`
      const { parentPort } = require('worker_threads');
      const __post = (m) => parentPort.postMessage(m);
      const __onMessage = (handler) => parentPort.on('message', handler);
      try {
        Object.defineProperty(globalThis, 'require', { value: undefined, writable: false, configurable: false });
      } catch { /* ignore */ }
`);

/** Bun / Web Worker bootstrap: transport via `self`. */
const BUN_WORKER_CODE = buildWorkerSource(`
      const __post = (m) => self.postMessage(m);
      const __onMessage = (handler) => { self.onmessage = (event) => handler(event.data); };
      try {
        Object.defineProperty(globalThis, 'require', { value: undefined, writable: false, configurable: false });
      } catch { /* ignore */ }
`);

/**
 * Default per-execution resource caps, shared by both backends so a runaway/ballooning
 * script can't exhaust the host. (Node applies these via `worker_threads.resourceLimits`;
 * Bun applies what its Worker supports.)
 */
export const DEFAULT_RESOURCE_LIMITS = {
    maxOldGenerationSizeMb: 256,
    maxYoungGenerationSizeMb: 32,
    stackSizeMb: 4
} as const;

export { NODE_WORKER_CODE, BUN_WORKER_CODE };
