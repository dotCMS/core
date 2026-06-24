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
