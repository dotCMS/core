/**
 * Contract spec for the readiness probe (task T041, dotCMS #37262, AC-009 / contract X5).
 *
 * Written before the implementation; this file defines the API.
 *
 * WHY THIS CHANGES. Today the CLI's only readiness signal is `/api/v1/appconfiguration` on 8082
 * (`getDotcmsApisByBaseUrl` → `DOTCMS_HEALTH_API`). That endpoint was chosen as a workaround: the
 * purpose-built probes were unreachable because of IP ACL restrictions from the Docker host
 * (issue #34509). The bundled compose file now publishes the management port itself
 * (`127.0.0.1:8090:8090`), so `/dotmgt/readyz` is reachable from the host and the workaround is no
 * longer needed. `readyz` is the right signal: it does not depend on the web application being up.
 *
 * MEASURED, 2026-08-31, against a real stack on released `dotcms/dotcms:latest`:
 *   - Both `/dotmgt/livez` and `/dotmgt/readyz` exist on the released image.
 *   - `docker compose up --wait` reporting "healthy" means LIVE, not READY. The container
 *     healthcheck probes `livez`; `readyz` was still answering 503 for a few seconds AFTER
 *     `--wait` had already returned, and only then flipped to 200 "ready".
 *   - Therefore **a 503 from `readyz` is the normal "still starting" state, not an error**. That is
 *     the single most important case in this file: treating it as a failure would make the CLI
 *     abort on a stack that is booting exactly as designed.
 *
 * API PINNED
 *   export type Readiness =
 *       | { kind: 'ready' }
 *       | { kind: 'not-ready'; detail: string };
 *   export function probeReadiness(options: {
 *       readyzUrl: string;                                  // /dotmgt/readyz on 8090
 *       fallbackUrl: string;                                // /api/v1/appconfiguration on 8082
 *       get: (url: string) => Promise<{ status: number }>;  // injected: no test touches the network
 *   }): Promise<Readiness>;
 *
 * A STRING discriminant, not a boolean: this workspace sets `"strict": false` in
 * tsconfig.base.json, and without `strictNullChecks` TypeScript will not narrow a union on a
 * boolean-literal discriminant.
 *
 * THE STATUS CONTRACT (X5). Today `fetchWithRetry` accepts any 2xx (`validateStatus: status >= 200
 * && status < 300`) while `isDotcmsRunning` re-narrows to `res.status === 200` — so a 204 is
 * "success" to one and failure to the other. One rule from here on: **any 2xx counts as ready**,
 * and callers MUST NOT re-narrow. Pinned below with an explicit 204 case.
 *
 * ONE probe, not a retry loop. `probeReadiness` answers "is it ready *right now*" and returns a
 * value; waiting and retrying belong to the caller.
 *
 * Contract: contracts/cli-exit-contract.md X5 (readiness) and X2 (a result is a value, never an
 * exit). Spec: spec.md AC-009.
 */

import { probeReadiness } from './readiness';

const READYZ_URL = 'http://127.0.0.1:8090/dotmgt/readyz';
const FALLBACK_URL = 'http://localhost:8082/api/v1/appconfiguration';

/** A `get` that answers per-URL, so a test can say "readyz 404, fallback 200" in one line. */
function responder(byUrl: Record<string, number | Error>) {
    return jest.fn(async (url: string) => {
        const answer = byUrl[url];

        if (answer === undefined) {
            throw new Error(`connect ECONNREFUSED (${url})`);
        }

        if (answer instanceof Error) {
            throw answer;
        }

        return { status: answer };
    });
}

function options(overrides: Partial<Parameters<typeof probeReadiness>[0]> = {}) {
    return {
        readyzUrl: READYZ_URL,
        fallbackUrl: FALLBACK_URL,
        get: responder({ [READYZ_URL]: 200 }),
        ...overrides
    };
}

describe('probeReadiness', () => {
    let exitSpy: jest.SpyInstance;

    beforeEach(() => {
        exitSpy = jest.spyOn(process, 'exit').mockImplementation(((code?: number) => {
            throw new Error(`process.exit(${code}) must never be called here`);
        }) as never);
    });

    afterEach(() => {
        jest.restoreAllMocks();
    });

    describe('readyz is the preferred signal', () => {
        it('reports ready on a 200 from readyz', async () => {
            const opts = options({ get: responder({ [READYZ_URL]: 200 }) });

            const readiness = await probeReadiness(opts);

            expect(readiness).toEqual({ kind: 'ready' });
        });

        it('does not touch the fallback when readyz answers', async () => {
            const opts = options({
                get: responder({ [READYZ_URL]: 200, [FALLBACK_URL]: 200 })
            });

            await probeReadiness(opts);

            const asked = (opts.get as jest.Mock).mock.calls.map(([url]) => url);

            expect(asked).toEqual([READYZ_URL]);
            expect(asked).not.toContain(FALLBACK_URL);
        });
    });

    describe('503 from readyz — still starting, NOT an error (measured 2026-08-31)', () => {
        it('reports not-ready rather than throwing or erroring out', async () => {
            const opts = options({ get: responder({ [READYZ_URL]: 503 }) });

            const readiness = await probeReadiness(opts);

            expect(readiness.kind).toBe('not-ready');
        });

        it('words the detail as "still starting", not as a failure', async () => {
            const opts = options({ get: responder({ [READYZ_URL]: 503 }) });

            const readiness = await probeReadiness(opts);

            if (readiness.kind === 'not-ready') {
                // `compose up --wait` reporting healthy only means livez passed; readyz stays 503
                // for a few more seconds. The user must be told the stack is booting, not that
                // something went wrong.
                expect(readiness.detail).toMatch(/start/i);
                expect(readiness.detail).not.toMatch(/error|fail/i);
            }
        });

        it('does not fall back on a 503 — readyz answered, and its answer is "not yet"', async () => {
            // The fallback exists for stacks whose compose predates the published 8090 port, not
            // as a second opinion. `/api/v1/appconfiguration` can answer 200 while readyz is still
            // 503, so consulting it here would report a booting stack as ready.
            const opts = options({
                get: responder({ [READYZ_URL]: 503, [FALLBACK_URL]: 200 })
            });

            const readiness = await probeReadiness(opts);

            expect(readiness.kind).toBe('not-ready');
            expect((opts.get as jest.Mock).mock.calls.map(([url]) => url)).not.toContain(
                FALLBACK_URL
            );
        });
    });

    describe('older images without /dotmgt/readyz fall back to appconfiguration', () => {
        it('falls back on a 404 and reports ready when the fallback answers 200', async () => {
            const opts = options({
                get: responder({ [READYZ_URL]: 404, [FALLBACK_URL]: 200 })
            });

            const readiness = await probeReadiness(opts);

            expect(readiness).toEqual({ kind: 'ready' });
            expect((opts.get as jest.Mock).mock.calls.map(([url]) => url)).toEqual([
                READYZ_URL,
                FALLBACK_URL
            ]);
        });

        it('falls back when readyz is unreachable (port 8090 not published)', async () => {
            const opts = options({
                get: responder({
                    [READYZ_URL]: new Error('connect ECONNREFUSED 127.0.0.1:8090'),
                    [FALLBACK_URL]: 200
                })
            });

            const readiness = await probeReadiness(opts);

            expect(readiness).toEqual({ kind: 'ready' });
            expect((opts.get as jest.Mock).mock.calls.map(([url]) => url)).toContain(FALLBACK_URL);
        });

        it('reports not-ready and names both URLs when the fallback fails too', async () => {
            const opts = options({
                get: responder({
                    [READYZ_URL]: new Error('connect ECONNREFUSED 127.0.0.1:8090'),
                    [FALLBACK_URL]: new Error('connect ECONNREFUSED 127.0.0.1:8082')
                })
            });

            const readiness = await probeReadiness(opts);

            expect(readiness.kind).toBe('not-ready');

            if (readiness.kind === 'not-ready') {
                // Whoever reads this line needs to know what was tried, or they will guess.
                expect(readiness.detail).toContain(READYZ_URL);
                expect(readiness.detail).toContain(FALLBACK_URL);
            }
        });

        it('reports not-ready when the fallback answers a non-2xx', async () => {
            const opts = options({
                get: responder({ [READYZ_URL]: 404, [FALLBACK_URL]: 500 })
            });

            const readiness = await probeReadiness(opts);

            expect(readiness.kind).toBe('not-ready');
        });
    });

    describe('the status contract — any 2xx is ready, on either URL', () => {
        it.each([200, 201, 204])('treats %i from readyz as ready', async (status) => {
            const readiness = await probeReadiness(
                options({ get: responder({ [READYZ_URL]: status }) })
            );

            expect(readiness).toEqual({ kind: 'ready' });
        });

        it('treats a 204 as ready — the one status the two current call sites disagree about', async () => {
            // `fetchWithRetry` accepts it (validateStatus: 200-299); `isDotcmsRunning` rejects it
            // (`res.status === 200`). This assertion is the tie-break: 204 is ready.
            const readiness = await probeReadiness(
                options({ get: responder({ [READYZ_URL]: 204 }) })
            );

            expect(readiness).toEqual({ kind: 'ready' });
        });

        it('treats a 204 from the fallback as ready as well', async () => {
            const readiness = await probeReadiness(
                options({ get: responder({ [READYZ_URL]: 404, [FALLBACK_URL]: 204 }) })
            );

            expect(readiness).toEqual({ kind: 'ready' });
        });

        it('treats a 3xx as not ready — a redirect is not an answer', async () => {
            const readiness = await probeReadiness(
                options({ get: responder({ [READYZ_URL]: 302, [FALLBACK_URL]: 302 }) })
            );

            expect(readiness.kind).toBe('not-ready');
        });
    });

    describe('contract X2 — the probe returns a value, it never exits and never throws', () => {
        const everyPath = [
            { [READYZ_URL]: 200 },
            { [READYZ_URL]: 204 },
            { [READYZ_URL]: 503 },
            { [READYZ_URL]: 404, [FALLBACK_URL]: 200 },
            { [READYZ_URL]: 404, [FALLBACK_URL]: 500 },
            { [READYZ_URL]: new Error('ECONNREFUSED'), [FALLBACK_URL]: new Error('ECONNREFUSED') },
            {}
        ];

        it('never throws, whatever the two endpoints do', async () => {
            for (const byUrl of everyPath) {
                await expect(probeReadiness(options({ get: responder(byUrl) }))).resolves.toEqual(
                    expect.objectContaining({ kind: expect.any(String) })
                );
            }
        });

        it('never calls process.exit on any path', async () => {
            for (const byUrl of everyPath) {
                await probeReadiness(options({ get: responder(byUrl) }));
            }

            expect(exitSpy).not.toHaveBeenCalled();
        });
    });
});
