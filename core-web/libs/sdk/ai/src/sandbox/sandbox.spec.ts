import { Executor } from './executor';

import type { Adapter } from './types';

/**
 * Confinement + routing tests for the worker sandbox. These exercise a REAL Node
 * `worker_threads` worker (the hardest-to-reason-about part of the runtime, and the one
 * with zero coverage before). They prove the network blocks hold, the timeout terminates,
 * abort propagates to in-flight adapter work, and adapter routing works end to end.
 */
describe('sandbox confinement', () => {
    function run(code: string, opts?: { timeout?: number; adapters?: Adapter[] }) {
        const executor = new Executor({
            config: {
                adapters: opts?.adapters ?? [],
                sandbox: { timeout: opts?.timeout ?? 5000 }
            }
        });
        return executor.execute(code, {
            adapters: (opts?.adapters ?? []).map((a) => a.name)
        });
    }

    it('blocks direct fetch from inside the sandbox', async () => {
        const result = await run(`return await fetch('https://example.com');`);
        expect(result.success).toBe(false);
        expect(result.error?.message).toMatch(/Network access is disabled/i);
    });

    it('blocks XMLHttpRequest / WebSocket / EventSource', async () => {
        const result = await run(`
            const blocked = [];
            for (const name of ['XMLHttpRequest', 'WebSocket', 'EventSource']) {
                try { new globalThis[name](); blocked.push(name + ':ALLOWED'); }
                catch (e) { blocked.push(name + ':blocked'); }
            }
            return blocked;
        `);
        expect(result.success).toBe(true);
        expect(result.value).toEqual([
            'XMLHttpRequest:blocked',
            'WebSocket:blocked',
            'EventSource:blocked'
        ]);
    });

    it('removes require', async () => {
        const result = await run(`return typeof require;`);
        expect(result.success).toBe(true);
        expect(result.value).toBe('undefined');
    });

    it('blocks dynamic import() so node builtins cannot be re-opened', async () => {
        const result = await run(
            `const fs = await import('node:fs'); return typeof fs.readFileSync;`
        );
        expect(result.success).toBe(false);
        expect(result.error?.message).toMatch(/dynamic import\(\) is disabled/i);
    });

    it('empties process.env', async () => {
        const result = await run(`return Object.keys(process.env).length;`);
        expect(result.success).toBe(true);
        expect(result.value).toBe(0);
    });

    it('returns a value from executed code', async () => {
        const result = await run(`return 1 + 2;`);
        expect(result.success).toBe(true);
        expect(result.value).toBe(3);
    });

    it('captures console logs', async () => {
        const result = await run(`console.log('hello'); console.warn('careful'); return 'ok';`);
        expect(result.success).toBe(true);
        expect(result.logs).toContain('hello');
        expect(result.logs).toContain('[WARN] careful');
    });

    it('terminates on timeout and reports a TIMEOUT error', async () => {
        const result = await run(`while (true) {}`, { timeout: 200 });
        expect(result.success).toBe(false);
        expect(result.error?.code).toBe('TIMEOUT');
    }, 10000);

    it('surfaces a thrown error from sandbox code', async () => {
        const result = await run(`throw new Error('boom');`);
        expect(result.success).toBe(false);
        expect(result.error?.message).toBe('boom');
    });
});

describe('sandbox adapter routing', () => {
    /** A fake adapter that records calls and echoes its args back. */
    function makeEchoAdapter(record: unknown[][]): Adapter {
        return {
            name: 'echo',
            description: 'test',
            version: '1.0.0',
            methods: new Map([
                [
                    'ping',
                    {
                        name: 'ping',
                        parameters: [],
                        execute: (...args: unknown[]) => {
                            record.push(args);
                            return { pong: args[0] };
                        }
                    }
                ]
            ])
        };
    }

    it('routes an adapter call from the sandbox to the host and back', async () => {
        const calls: unknown[][] = [];
        const adapter = makeEchoAdapter(calls);
        const executor = new Executor({
            config: { adapters: [adapter], sandbox: { timeout: 5000 } }
        });
        const result = await executor.execute(`return await echo.ping({ n: 42 });`, {
            adapters: ['echo']
        });
        expect(result.success).toBe(true);
        expect(result.value).toEqual({ pong: { n: 42 } });
        expect(calls).toEqual([[{ n: 42 }]]);
    });

    it('propagates an adapter error code into the sandbox', async () => {
        const adapter: Adapter = {
            name: 'boom',
            version: '1.0.0',
            methods: new Map([
                [
                    'go',
                    {
                        name: 'go',
                        parameters: [],
                        execute: () => {
                            const e = new Error('nope') as Error & { code?: string };
                            e.code = 'POLICY';
                            throw e;
                        }
                    }
                ]
            ])
        };
        const executor = new Executor({
            config: { adapters: [adapter], sandbox: { timeout: 5000 } }
        });
        const result = await executor.execute(
            `try { await boom.go(); } catch (e) { return { msg: e.message, code: e.code }; }`,
            { adapters: ['boom'] }
        );
        expect(result.success).toBe(true);
        expect(result.value).toEqual({ msg: 'nope', code: 'POLICY' });
    });

    it('aborts in-flight adapter work when the sandbox times out', async () => {
        let aborted = false;
        const slowAdapter: Adapter = {
            name: 'slow',
            version: '1.0.0',
            methods: new Map([
                [
                    'wait',
                    {
                        name: 'wait',
                        parameters: [],
                        execute: () =>
                            new Promise((resolve) => {
                                // Resolves only after a long delay; the abort below should fire first.
                                const t = setTimeout(() => resolve('done'), 2000);
                                // Don't keep the event loop alive once the test ends.
                                (t as { unref?: () => void }).unref?.();
                            })
                    }
                ]
            ])
        };

        const controller = new AbortController();
        controller.signal.addEventListener('abort', () => {
            aborted = true;
        });

        const executor = new Executor({
            config: {
                adapters: [slowAdapter],
                sandbox: { timeout: 200, onTeardown: () => controller.abort() }
            }
        });

        const result = await executor.execute(`return await slow.wait();`, { adapters: ['slow'] });
        expect(result.success).toBe(false);
        expect(result.error?.code).toBe('TIMEOUT');
        // The onTeardown hook fired, so a runtime wiring this to its adapter signal would
        // have aborted the in-flight fetch.
        expect(aborted).toBe(true);
    }, 10000);
});

describe('resolveRef sandbox helper', () => {
    // A tiny fake spec injected as the `spec` global (as the search runtime does via includeSpec).
    const fakeSpec = {
        components: {
            schemas: {
                Page: {
                    type: 'object',
                    properties: {
                        title: { type: 'string' },
                        template: { $ref: '#/components/schemas/Template' }
                    }
                },
                Template: {
                    type: 'object',
                    properties: { theme: { $ref: '#/components/schemas/Theme' } }
                },
                Theme: { type: 'object', properties: { name: { type: 'string' } } },
                // self-referential — bounded depth must terminate
                Node: { type: 'object', properties: { child: { $ref: '#/components/schemas/Node' } } }
            }
        }
    };

    function runWithSpec(code: string) {
        const executor = new Executor({ config: { adapters: [], sandbox: { timeout: 5000 } } });
        return executor.execute(code, { variables: { spec: fakeSpec } });
    }

    it('expands nested $refs up to the given depth', async () => {
        const result = await runWithSpec(`return resolveRef('Page', 2);`);
        expect(result.success).toBe(true);
        const value = result.value as {
            properties: { template: { properties: { theme: { $ref?: string; properties?: unknown } } } };
        };
        // depth 2: Page → Template → Theme all expanded (Theme has no further refs)
        expect(value.properties.template.properties.theme.properties).toBeDefined();
        expect(value.properties.template.properties.theme.$ref).toBeUndefined();
    });

    it('leaves $ref strings in place beyond the depth bound', async () => {
        const result = await runWithSpec(`return resolveRef('Page', 1);`);
        expect(result.success).toBe(true);
        const value = result.value as {
            properties: { template: { properties: { theme: { $ref?: string } } } };
        };
        // depth 1: Page → Template expanded, but Template's theme ref is left unresolved
        expect(value.properties.template.properties.theme.$ref).toBe(
            '#/components/schemas/Theme'
        );
    });

    it('terminates on a self-referential schema', async () => {
        const result = await runWithSpec(`return resolveRef('Node', 5);`);
        expect(result.success).toBe(true);
        // Should complete without infinite recursion; the deepest child stays a $ref.
        expect(result.value).toBeDefined();
    });

    it('throws a friendly error for an unknown schema name', async () => {
        const result = await runWithSpec(`return resolveRef('Nope');`);
        expect(result.success).toBe(false);
        expect(result.error?.message).toMatch(/Unknown schema "Nope"/);
    });

    it('throws a friendly error when the spec global is absent', async () => {
        const executor = new Executor({ config: { adapters: [], sandbox: { timeout: 5000 } } });
        const result = await executor.execute(`return resolveRef('Page');`);
        expect(result.success).toBe(false);
        expect(result.error?.message).toMatch(/only available in the search sandbox/i);
    });
});
