/**
 * Contract spec for `src/uve/configure-uve.ts` (tasks T019/T020/T021, dotCMS #37262).
 *
 * Written BEFORE the implementation, so this file DEFINES the API the implementation must
 * satisfy. The module does not exist yet — the failing import is the deliberate Red state
 * of TDD.
 *
 * ---------------------------------------------------------------------------------------
 * API PINNED BY THIS SPEC
 * ---------------------------------------------------------------------------------------
 *
 *   export type UveMode = 'local' | 'remote';
 *
 *   export interface ConfigureUveOptions {
 *       host: string;              // e.g. 'http://localhost:8082' — no trailing slash
 *       siteId: string;            // resolved default site identifier
 *       token: string;             // API token, sent as `Authorization: Bearer <token>`
 *       mode: UveMode;             // 'local' = CLI-owned Docker stack, 'remote' = --dotcms-url
 *       frontendUrl: string;       // value written into configuration.value
 *       maxRetries?: number;       // POST attempts, spent on 5xx only
 *       retryDelayMs?: number;     // backoff between 5xx retries; 0 in tests
 *       report?: (message: string) => void;  // X4: caller-supplied reporter, not console.log
 *   }
 *
 *   export type UveFailurePhase  = 'probe' | 'write';
 *   export type UveFailureReason = 'forbidden' | 'server-error' | 'unreachable' | 'unknown';
 *
 *   export type UveOutcome =
 *       | { readonly kind: 'configured' }
 *       | {
 *             readonly kind: 'failed';
 *             readonly phase: UveFailurePhase;
 *             readonly reason: UveFailureReason;
 *             readonly status: number | null;   // HTTP status; null for transport failures
 *             readonly message: string;         // ready-to-print, MODE-DEPENDENT
 *         };
 *
 *   export function configureUVE(options: ConfigureUveOptions): Promise<UveOutcome>;
 *
 * Why a `kind` discriminant rather than `Result<T, E>`: `Err()` is `{ok: false, val}`, which
 * is TRUTHY — exactly the trap contract X7 documents at `src/index.ts:597`, where
 * `if (!result)` never fires. `outcome.kind === 'configured'` cannot be misread that way, and
 * the caller sets `RunState.uveConfigured` from it.
 *
 * Why a STRING discriminant specifically: this workspace compiles with `strict: false`
 * (`tsconfig.base.json`), and without `strictNullChecks` TypeScript does not narrow a union
 * on a boolean-literal discriminant — `{configured: true} | {configured: false, message}`
 * leaves `outcome.message` unreachable at every call site. A string literal narrows
 * correctly, which is also why `ComposeSource` (same feature) discriminates on `kind`.
 *
 * `frontendUrl` is a fifth field beyond the four the contract text abbreviates
 * (`{ host, siteId, token, mode }`): the POST body cannot be built without it. Both existing
 * call sites already compute it as `http://localhost:${getPortByFramework(selectedFramework)}`.
 * It is the RAW origin — `configureUVE` wraps it with `getUVEConfigValue` itself, so the
 * serialized shape the endpoint expects lives in one place rather than at every call site.
 *
 * ---------------------------------------------------------------------------------------
 * BEHAVIOUR PINNED (contract X2 + X3, acceptance criteria AC-003 + AC-005)
 * ---------------------------------------------------------------------------------------
 *
 *   1. NON-FATAL (X2). No path calls `process.exit`. Asserted directly against a spy, for a
 *      403, a 500 and a network error. This module replaces the two duplicated fatal blocks
 *      at `src/index.ts:226-228` and `src/index.ts:369-371`.
 *   2. PROBE ONCE (X3). Exactly one `GET` of `<host>/api/v1/apps/dotema-config-v2/<siteId>`
 *      precedes the `POST` to the same resource. It is a probe, NOT a poll — one call, even
 *      when it fails. A failed probe means no `POST` at all.
 *   3. RETRY 5xx ONLY (X3). Never retries a `403` or any other `4xx`. Measured: after an
 *      interrupted starter import the endpoint returned 403 on 193 consecutive attempts over
 *      ~7 minutes with zero successes — a poll would spin forever.
 *   4. MODE-DEPENDENT 403 MESSAGE (X3). `'local'` = the bricked first boot: unrecoverable,
 *      recreate with `docker compose down -v`, reference #37268, and DO NOT offer the manual
 *      UVE setup guide (it fails identically). `'remote'` = the user's own server: there is
 *      no stack to recreate, so `docker compose down -v` MUST NEVER be emitted; name the
 *      site ID and the app key `dotema-config-v2`, and DO offer the manual guide.
 *
 * Contract: specs/37262-create-app-docker-uve/contracts/cli-exit-contract.md — X2, X3.
 * Data model: specs/37262-create-app-docker-uve/data-model.md — §3 `UVEAppConfig`.
 */

import axios from 'axios';

import { configureUVE } from './configure-uve';

import { getUVEConfigValue } from '../utils';

jest.mock('axios', () => {
    const get = jest.fn();
    const post = jest.fn();
    const isAxiosError = (err: unknown): boolean =>
        Boolean(err && typeof err === 'object' && (err as { isAxiosError?: boolean }).isAxiosError);

    const instance: Record<string, unknown> = { get, post, isAxiosError };
    instance['create'] = jest.fn(() => instance);

    return { __esModule: true, default: instance, ...instance };
});

const mockedAxios = axios as unknown as { get: jest.Mock; post: jest.Mock };

const HOST = 'http://localhost:8082';
const SITE_ID = '48190c8c-42c4-46af-8d1a-0cd5db894797';
const TOKEN = 'eyJhbGciOiJIUzI1NiJ9.test-token.signature';
const FRONTEND_URL = 'http://localhost:3000';

/** The app key is part of the resource path AND of the remote-mode guidance. */
const UVE_APP_KEY = 'dotema-config-v2';
/** Stable slug of the headless UVE guide the CLI links as the "manual steps" (X2.2). */
const MANUAL_STEPS_SLUG = 'uve-headless-config';

type Outcome = Awaited<ReturnType<typeof configureUVE>>;

const baseOptions = {
    host: HOST,
    siteId: SITE_ID,
    token: TOKEN,
    frontendUrl: FRONTEND_URL,
    // Keep the 5xx retry loop instantaneous; the delay itself is not under test.
    retryDelayMs: 0,
    maxRetries: 3
};

/** An axios-shaped rejection carrying an HTTP status, as axios produces for non-2xx. */
function httpError(status: number, statusText = 'Error') {
    return Object.assign(new Error(`Request failed with status code ${status}`), {
        isAxiosError: true,
        code: status >= 500 ? 'ERR_BAD_RESPONSE' : 'ERR_BAD_REQUEST',
        config: {},
        toJSON: () => ({}),
        response: { status, statusText, data: {}, headers: {}, config: {} }
    });
}

/** A transport-level failure: no HTTP response at all. */
function networkError(code = 'ECONNREFUSED') {
    return Object.assign(new Error(`connect ${code} 127.0.0.1:8082`), {
        isAxiosError: true,
        code,
        config: {},
        toJSON: () => ({}),
        response: undefined
    });
}

function okResponse(data: unknown = { entity: 'Ok' }) {
    return { status: 200, statusText: 'OK', data, headers: {}, config: {} };
}

/** Strip chalk styling so message assertions are about words, not escape codes. */
// eslint-disable-next-line no-control-regex
const ANSI_PATTERN = /\u001b\[[0-9;]*m/g;

/**
 * Everything the user could possibly see for this outcome: the returned message plus
 * anything the module reported or printed. Negative assertions ("`down -v` never appears in
 * remote output") are only meaningful over the union — a leak through `console.warn` is as
 * wrong as one through `message`.
 */
function visibleOutput(outcome: Outcome, reported: string[], printed: string[]): string {
    const message = outcome.kind === 'failed' ? outcome.message : '';

    return [message, ...reported, ...printed].join('\n').replace(ANSI_PATTERN, '');
}

function expectFailure(outcome: Outcome) {
    if (outcome.kind !== 'failed') {
        throw new Error('expected configureUVE to report failure, but it reported success');
    }

    return outcome;
}

describe('configureUVE', () => {
    let exitSpy: jest.SpyInstance;
    let logSpy: jest.SpyInstance;
    let warnSpy: jest.SpyInstance;
    let errorSpy: jest.SpyInstance;
    let reported: string[];
    let printed: string[];

    const report = (message: string) => {
        reported.push(message);
    };

    beforeEach(() => {
        mockedAxios.get.mockReset();
        mockedAxios.post.mockReset();

        reported = [];
        printed = [];

        exitSpy = jest.spyOn(process, 'exit').mockImplementation(((code?: number) => {
            throw new Error(
                `configureUVE called process.exit(${code}) — contract X2 forbids it on every path`
            );
        }) as never);

        const capture = (...args: unknown[]) => {
            printed.push(args.map(String).join(' '));
        };

        logSpy = jest.spyOn(console, 'log').mockImplementation(capture);
        warnSpy = jest.spyOn(console, 'warn').mockImplementation(capture);
        errorSpy = jest.spyOn(console, 'error').mockImplementation(capture);
    });

    afterEach(() => {
        exitSpy.mockRestore();
        logSpy.mockRestore();
        warnSpy.mockRestore();
        errorSpy.mockRestore();
    });

    describe('X2 — the UVE step is non-fatal: it never calls process.exit', () => {
        it('does not exit on a 403 in mode "local"', async () => {
            mockedAxios.get.mockResolvedValue(okResponse());
            mockedAxios.post.mockRejectedValue(httpError(403, 'Forbidden'));

            const outcome = await configureUVE({ ...baseOptions, mode: 'local', report });

            expect(exitSpy).not.toHaveBeenCalled();
            expect(expectFailure(outcome).reason).toBe('forbidden');
        });

        it('does not exit on a 403 in mode "remote"', async () => {
            mockedAxios.get.mockResolvedValue(okResponse());
            mockedAxios.post.mockRejectedValue(httpError(403, 'Forbidden'));

            const outcome = await configureUVE({ ...baseOptions, mode: 'remote', report });

            expect(exitSpy).not.toHaveBeenCalled();
            expect(expectFailure(outcome).reason).toBe('forbidden');
        });

        it('does not exit on a 500 once the retry budget is exhausted', async () => {
            mockedAxios.get.mockResolvedValue(okResponse());
            mockedAxios.post.mockRejectedValue(httpError(500, 'Internal Server Error'));

            const outcome = await configureUVE({ ...baseOptions, mode: 'local', report });

            expect(exitSpy).not.toHaveBeenCalled();
            expect(expectFailure(outcome).reason).toBe('server-error');
        });

        it('does not exit on a network error', async () => {
            mockedAxios.get.mockRejectedValue(networkError());

            const outcome = await configureUVE({ ...baseOptions, mode: 'remote', report });

            expect(exitSpy).not.toHaveBeenCalled();

            const failure = expectFailure(outcome);
            expect(failure.reason).toBe('unreachable');
            expect(failure.status).toBeNull();
        });

        it('does not throw — the caller warns and continues to scaffolding', async () => {
            mockedAxios.get.mockResolvedValue(okResponse());
            mockedAxios.post.mockRejectedValue(httpError(403, 'Forbidden'));

            await expect(
                configureUVE({ ...baseOptions, mode: 'local', report })
            ).resolves.toBeDefined();
        });

        it("reports failure through kind === 'failed', not a truthy Err", async () => {
            mockedAxios.get.mockResolvedValue(okResponse());
            mockedAxios.post.mockRejectedValue(httpError(403, 'Forbidden'));

            const outcome = await configureUVE({ ...baseOptions, mode: 'local', report });

            // X7's trap: `Err()` is `{ok: false, val}` — truthy — so `if (!outcome)` would
            // silently treat this failure as a success. The discriminant must be a field.
            expect(Boolean(outcome)).toBe(true);
            expect(outcome.kind).toBe('failed');
        });

        it('carries a non-empty, printable message on every failure', async () => {
            mockedAxios.get.mockResolvedValue(okResponse());
            mockedAxios.post.mockRejectedValue(httpError(500, 'Internal Server Error'));

            const failure = expectFailure(
                await configureUVE({ ...baseOptions, mode: 'local', report })
            );

            expect(typeof failure.message).toBe('string');
            expect(failure.message.length).toBeGreaterThan(0);
        });
    });

    describe('X3 — the GET is a single probe, not a poll', () => {
        it('probes the UVE app resource exactly once before writing', async () => {
            mockedAxios.get.mockResolvedValue(okResponse());
            mockedAxios.post.mockResolvedValue(okResponse());

            await configureUVE({ ...baseOptions, mode: 'local', report });

            expect(mockedAxios.get).toHaveBeenCalledTimes(1);

            const [probeUrl] = mockedAxios.get.mock.calls[0];
            expect(probeUrl).toContain(HOST);
            expect(probeUrl).toContain(UVE_APP_KEY);
            expect(probeUrl).toContain(SITE_ID);
        });

        it('writes to the same resource it probed', async () => {
            mockedAxios.get.mockResolvedValue(okResponse());
            mockedAxios.post.mockResolvedValue(okResponse());

            await configureUVE({ ...baseOptions, mode: 'local', report });

            const [probeUrl] = mockedAxios.get.mock.calls[0];
            const [writeUrl, body] = mockedAxios.post.mock.calls[0];

            expect(writeUrl).toBe(probeUrl);
            // NOT a bare `FRONTEND_URL`. The endpoint takes the serialized UVE config object
            // that `getUVEConfigValue` produces; posting the raw origin is accepted with a 200
            // and silently leaves the editor misconfigured — a green signal that means nothing,
            // which is the same failure shape as the bug this issue is about.
            expect(body).toEqual({
                configuration: { hidden: false, value: getUVEConfigValue(FRONTEND_URL) }
            });
        });

        it('authenticates both calls with the run token', async () => {
            mockedAxios.get.mockResolvedValue(okResponse());
            mockedAxios.post.mockResolvedValue(okResponse());

            await configureUVE({ ...baseOptions, mode: 'local', report });

            const [, getConfig] = mockedAxios.get.mock.calls[0];
            const [, , postConfig] = mockedAxios.post.mock.calls[0];

            expect(getConfig?.headers?.Authorization).toBe(`Bearer ${TOKEN}`);
            expect(postConfig?.headers?.Authorization).toBe(`Bearer ${TOKEN}`);
        });

        it('proceeds to the POST when the probe returns 200', async () => {
            mockedAxios.get.mockResolvedValue(okResponse());
            mockedAxios.post.mockResolvedValue(okResponse());

            const outcome = await configureUVE({ ...baseOptions, mode: 'local', report });

            expect(mockedAxios.post).toHaveBeenCalledTimes(1);
            expect(outcome.kind).toBe('configured');
        });

        it('never POSTs when the probe is forbidden, and never re-probes', async () => {
            mockedAxios.get.mockRejectedValue(httpError(403, 'Forbidden'));

            const outcome = await configureUVE({ ...baseOptions, mode: 'local', report });

            expect(mockedAxios.get).toHaveBeenCalledTimes(1);
            expect(mockedAxios.post).not.toHaveBeenCalled();

            const failure = expectFailure(outcome);
            expect(failure.phase).toBe('probe');
            expect(failure.reason).toBe('forbidden');
            expect(failure.status).toBe(403);
        });

        it('never polls the probe on a network error', async () => {
            mockedAxios.get.mockRejectedValue(networkError());

            await configureUVE({ ...baseOptions, mode: 'local', report });

            expect(mockedAxios.get).toHaveBeenCalledTimes(1);
            expect(mockedAxios.post).not.toHaveBeenCalled();
        });
    });

    describe('X3 — the POST retries on 5xx only', () => {
        it('retries a 500 and succeeds on the second attempt', async () => {
            mockedAxios.get.mockResolvedValue(okResponse());
            mockedAxios.post
                .mockRejectedValueOnce(httpError(500, 'Internal Server Error'))
                .mockResolvedValueOnce(okResponse());

            const outcome = await configureUVE({ ...baseOptions, mode: 'local', report });

            expect(mockedAxios.post).toHaveBeenCalledTimes(2);
            // Still a single probe — the retry re-POSTs, it does not re-GET.
            expect(mockedAxios.get).toHaveBeenCalledTimes(1);
            expect(outcome.kind).toBe('configured');
        });

        it('gives up after the retry budget on a persistent 5xx', async () => {
            mockedAxios.get.mockResolvedValue(okResponse());
            mockedAxios.post.mockRejectedValue(httpError(503, 'Service Unavailable'));

            const outcome = await configureUVE({
                ...baseOptions,
                mode: 'local',
                maxRetries: 3,
                report
            });

            expect(mockedAxios.post).toHaveBeenCalledTimes(3);

            const failure = expectFailure(outcome);
            expect(failure.phase).toBe('write');
            expect(failure.reason).toBe('server-error');
            expect(failure.status).toBe(503);
        });

        it('NEVER retries a 403 — the second attempt is never made', async () => {
            mockedAxios.get.mockResolvedValue(okResponse());
            // If the implementation retried, this queued 200 would turn the run green and
            // hide the defect. A 403 here is terminal: measured at 193 consecutive
            // failures over ~7 minutes with zero successes.
            mockedAxios.post
                .mockRejectedValueOnce(httpError(403, 'Forbidden'))
                .mockResolvedValueOnce(okResponse());

            const outcome = await configureUVE({ ...baseOptions, mode: 'local', report });

            expect(mockedAxios.post).toHaveBeenCalledTimes(1);
            expect(outcome.kind).toBe('failed');
            expect(expectFailure(outcome).status).toBe(403);
        });

        it('does not retry any other 4xx either', async () => {
            for (const status of [400, 401, 404, 422]) {
                mockedAxios.get.mockReset();
                mockedAxios.post.mockReset();
                mockedAxios.get.mockResolvedValue(okResponse());
                mockedAxios.post
                    .mockRejectedValueOnce(httpError(status))
                    .mockResolvedValueOnce(okResponse());

                const outcome = await configureUVE({ ...baseOptions, mode: 'local', report });

                expect(mockedAxios.post).toHaveBeenCalledTimes(1);
                expect(outcome.kind).toBe('failed');
            }

            expect(exitSpy).not.toHaveBeenCalled();
        });
    });

    describe('X3 — a 403 in mode "local" means the instance is unrecoverable', () => {
        let output: string;

        beforeEach(async () => {
            mockedAxios.get.mockResolvedValue(okResponse());
            mockedAxios.post.mockRejectedValue(httpError(403, 'Forbidden'));

            const outcome = await configureUVE({ ...baseOptions, mode: 'local', report });
            output = visibleOutput(outcome, reported, printed);
        });

        it('tells the user to recreate the stack with `docker compose down -v`', () => {
            expect(output).toMatch(/docker\s+compose\s+down\s+-v/);
        });

        it('says the instance cannot be repaired in place', () => {
            expect(output).toMatch(/unrecoverable|recreate|re-create|start over|from scratch/i);
        });

        it('references the backend defect #37268', () => {
            expect(output).toContain('37268');
        });

        it('does NOT offer the manual UVE setup steps — they fail identically', () => {
            expect(output).not.toContain(MANUAL_STEPS_SLUG);
            expect(output).not.toMatch(/dev\.dotcms\.com/);
        });
    });

    describe('X3 — a 403 in mode "remote" is a token permission problem', () => {
        let output: string;

        beforeEach(async () => {
            mockedAxios.get.mockResolvedValue(okResponse());
            mockedAxios.post.mockRejectedValue(httpError(403, 'Forbidden'));

            const outcome = await configureUVE({ ...baseOptions, mode: 'remote', report });
            output = visibleOutput(outcome, reported, printed);
        });

        it('reports that the API token lacks permission on the resolved site', () => {
            expect(output).toMatch(/permission/i);
            expect(output).toMatch(/token/i);
        });

        it('names the site ID and the app key so the fix is actionable', () => {
            expect(output).toContain(SITE_ID);
            expect(output).toContain(UVE_APP_KEY);
        });

        it('DOES offer the manual UVE setup steps — on this path they work', () => {
            expect(output).toContain(MANUAL_STEPS_SLUG);
        });

        it('NEVER suggests `docker compose down -v` — there is no stack to recreate', () => {
            expect(output).not.toMatch(/down\s+-v/);
            expect(output).not.toMatch(/docker/i);
        });

        it('does not reference #37268 — that defect is about the local first boot', () => {
            expect(output).not.toContain('37268');
        });
    });

    describe('success', () => {
        it('reports success when the probe is 200 and the POST succeeds', async () => {
            mockedAxios.get.mockResolvedValue(okResponse());
            mockedAxios.post.mockResolvedValue(okResponse());

            const outcome = await configureUVE({ ...baseOptions, mode: 'local', report });

            expect(outcome.kind).toBe('configured');
            expect(outcome).not.toHaveProperty('reason');
            expect(exitSpy).not.toHaveBeenCalled();
        });

        it('reports success in mode "remote" the same way', async () => {
            mockedAxios.get.mockResolvedValue(okResponse());
            mockedAxios.post.mockResolvedValue(okResponse());

            const outcome = await configureUVE({ ...baseOptions, mode: 'remote', report });

            expect(outcome.kind).toBe('configured');
            expect(mockedAxios.get).toHaveBeenCalledTimes(1);
            expect(mockedAxios.post).toHaveBeenCalledTimes(1);
        });

        it('emits no failure guidance on the happy path', async () => {
            mockedAxios.get.mockResolvedValue(okResponse());
            mockedAxios.post.mockResolvedValue(okResponse());

            const outcome = await configureUVE({ ...baseOptions, mode: 'local', report });
            const output = visibleOutput(outcome, reported, printed);

            expect(output).not.toMatch(/down\s+-v/);
            expect(output).not.toContain('37268');
        });
    });
});
