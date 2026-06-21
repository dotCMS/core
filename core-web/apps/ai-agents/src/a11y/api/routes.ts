import { Hono } from 'hono';
import { streamSSE } from 'hono/streaming';

import { ActiveRunRegistry } from './active-run';
import { parseBearer } from './auth';
import { parseFixRequest } from './request-parser';

import { type FixReport } from '../domain/contract';
import { DotcmsClient } from '../dotcms/dotcms-client';
import { runFix, type RunFixDeps } from '../fix/run-fix';

/**
 * The a11y agent HTTP surface (plan §8.2 / §8.7), mounted under /a11y:
 *   POST /a11y/fix         → run the loop, return the §6 report (plain JSON)
 *   POST /a11y/fix/stream  → run the loop, stream SSE `step` events + final `done`
 *   GET  /a11y/active-run  → the calling user's in-flight or finished run
 *
 * The token rides in `Authorization: Bearer` (never the body). The request body
 * is validated against the locked FixRequestSchema — IDENTICAL for json + stream;
 * only the response media type differs (plan §8: request shape never changes).
 */

export interface A11yRoutesDeps {
    registry?: ActiveRunRegistry;
    /**
     * Build the runFix deps for a request. Injectable so tests can supply a fake
     * client + stubbed triage/fix. Default wires a real DotcmsClient from the
     * forwarded token.
     */
    makeRunDeps?: (token: string, dotcmsBaseUrl: string) => RunFixDeps;
}

function defaultMakeRunDeps(token: string, dotcmsBaseUrl: string): RunFixDeps {
    const deps: RunFixDeps = { client: new DotcmsClient({ dotcmsBaseUrl, authToken: token }) };
    // Dev affordance: A11Y_AGENT_RESEARCH=off runs PASS 1 (deterministic) only,
    // so UI/stream smoke tests don't spend model credits. Default = research on.
    if (process.env.A11Y_AGENT_RESEARCH === 'off') {
        deps.research = false;
    }
    return deps;
}

export function createA11yRoutes(deps: A11yRoutesDeps = {}): Hono {
    const registry = deps.registry ?? new ActiveRunRegistry();
    const makeRunDeps = deps.makeRunDeps ?? defaultMakeRunDeps;
    const app = new Hono();

    // Plain JSON: run the loop, return the §6 report. (Used by the proxy / tests.)
    app.post('/fix', async (c) => {
        const p = await parseFixRequest(c.req.header('Authorization'), () => c.req.json());
        if (p.ok === false) {
            return c.json(p.error, p.status);
        }
        const { bearer, request } = p;
        const signal = registry.start(bearer.userId, request.runId);
        try {
            const report: FixReport = await runFix(request, {
                ...makeRunDeps(bearer.token, request.dotcmsBaseUrl),
                signal
            });
            registry.finish(bearer.userId, request.runId, report);
            // Partial report when the user stopped mid-run; flag it so callers can tell.
            return c.json({ ...report, aborted: signal.aborted }, 200);
        } catch (e) {
            registry.fail(bearer.userId, request.runId);
            const message = e instanceof Error ? e.message : 'run failed';
            return c.json({ error: 'Agent run failed', message, runId: request.runId }, 502);
        }
    });

    // SSE: run the loop, streaming `step` events live + a final `done` event with
    // the §6 report (or `error`). Same request shape as /fix (plan §8.4).
    app.post('/fix/stream', async (c) => {
        const p = await parseFixRequest(c.req.header('Authorization'), () => c.req.json());
        if (p.ok === false) {
            return c.json(p.error, p.status);
        }
        const { bearer, request } = p;
        const signal = registry.start(bearer.userId, request.runId);
        return streamSSE(c, async (stream) => {
            const send = (event: string, data: unknown) =>
                stream.writeSSE({ event, data: JSON.stringify(data) });
            try {
                const runDeps = makeRunDeps(bearer.token, request.dotcmsBaseUrl);
                const report = await runFix(request, {
                    ...runDeps,
                    signal,
                    onStep: (phase, message) => {
                        // fire-and-forget; SSE writes are ordered by the stream
                        void send('step', { phase, message });
                    }
                });
                registry.finish(bearer.userId, request.runId, report);
                // If the user stopped, the report is partial — emit `aborted` (a
                // terminal event distinct from `done`) carrying what got applied.
                await send(signal.aborted ? 'aborted' : 'done', { report });
            } catch (e) {
                registry.fail(bearer.userId, request.runId);
                await send('error', {
                    message: e instanceof Error ? e.message : 'run failed',
                    runId: request.runId
                });
            }
        });
    });

    // Stop the caller's in-flight run (cooperative — runFix stops at the next safe
    // checkpoint and the stream emits a terminal `aborted` event with the partial
    // report; fixes already applied stay). 202 if a run was signalled, 404 if none.
    app.post('/stop', async (c) => {
        const bearer = parseBearer(c.req.header('Authorization'));
        if (!bearer) {
            return c.json({ error: 'Missing or malformed Authorization: Bearer token' }, 401);
        }
        const runId = registry.requestStop(bearer.userId);
        if (!runId) {
            return c.json({ error: 'No active run to stop' }, 404);
        }
        return c.json({ stopping: true, runId }, 202);
    });

    app.get('/active-run', (c) => {
        const bearer = parseBearer(c.req.header('Authorization'));
        if (!bearer) {
            return c.json({ error: 'Missing or malformed Authorization: Bearer token' }, 401);
        }
        return c.json(registry.get(bearer.userId), 200);
    });

    return app;
}
