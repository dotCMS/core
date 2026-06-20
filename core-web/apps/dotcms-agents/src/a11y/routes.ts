import { Hono } from 'hono';
import { streamSSE } from 'hono/streaming';

import { ActiveRunRegistry } from './active-run';
import { parseBearer, type BearerInfo } from './auth';
import { type FixReport, type FixRequest, FixRequestSchema } from './contract';
import { DotcmsClient } from './dotcms-client';
import { runFix, type RunFixDeps } from './runFix';

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

/** Auth + validate the /fix request body; returns the bearer + parsed request or a 4xx error. */
type ParseFailure = { ok: false; status: 401 | 400 | 422; error: object };
type ParseSuccess = { ok: true; bearer: BearerInfo; request: FixRequest };
type ParseResult = ParseSuccess | ParseFailure;

async function parseFixRequest(
    header: string | undefined,
    rawBody: () => Promise<unknown>
): Promise<ParseResult> {
    // Dev fallback: when no Authorization header arrives but A11Y_AGENT_DEV_TOKEN
    // is set, treat that env token as the bearer. This lets the Angular dev server
    // call the agent same-origin without the browser holding a token (the WDS dev
    // proxy can't reliably inject headers on streamed requests). Gated on the env
    // var, so production — which has no such var and always sees the proxy-injected
    // JWT — keeps requiring a real bearer.
    const devToken = process.env.A11Y_AGENT_DEV_TOKEN;
    const bearer = parseBearer(header) ?? (devToken ? parseBearer(`Bearer ${devToken}`) : null);
    if (!bearer) {
        return { ok: false, status: 401, error: { error: 'Missing or malformed Authorization: Bearer token' } };
    }
    let body: unknown;
    try {
        body = await rawBody();
    } catch {
        return { ok: false, status: 400, error: { error: 'Request body must be valid JSON' } };
    }
    const parsed = FixRequestSchema.safeParse(body);
    if (!parsed.success) {
        return { ok: false, status: 422, error: { error: 'Invalid request', issues: parsed.error.issues } };
    }
    return { ok: true, bearer, request: parsed.data };
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
        registry.start(bearer.userId, request.runId);
        try {
            const report: FixReport = await runFix(
                request,
                makeRunDeps(bearer.token, request.dotcmsBaseUrl)
            );
            registry.finish(bearer.userId, request.runId, report);
            return c.json(report, 200);
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
        return streamSSE(c, async (stream) => {
            const send = (event: string, data: unknown) =>
                stream.writeSSE({ event, data: JSON.stringify(data) });
            registry.start(bearer.userId, request.runId);
            try {
                const runDeps = makeRunDeps(bearer.token, request.dotcmsBaseUrl);
                const report = await runFix(request, {
                    ...runDeps,
                    onStep: (phase, message) => {
                        // fire-and-forget; SSE writes are ordered by the stream
                        void send('step', { phase, message });
                    }
                });
                registry.finish(bearer.userId, request.runId, report);
                await send('done', { report });
            } catch (e) {
                registry.fail(bearer.userId, request.runId);
                await send('error', { message: e instanceof Error ? e.message : 'run failed', runId: request.runId });
            }
        });
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
