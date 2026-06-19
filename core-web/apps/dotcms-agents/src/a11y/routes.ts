import { Hono } from 'hono';

import { ActiveRunRegistry } from './active-run';
import { parseBearer } from './auth';
import { type FixReport, FixRequestSchema } from './contract';
import { DotcmsClient } from './dotcms-client';
import { runFix, type RunFixDeps } from './runFix';

/**
 * The a11y agent HTTP surface (plan §8.2 / §8.7), mounted under /a11y:
 *   POST /a11y/fix         → run the loop, return the §6 report (JSON, no SSE yet)
 *   GET  /a11y/active-run  → the calling user's in-flight or finished run
 *
 * The token rides in `Authorization: Bearer` (never the body). The request body
 * is validated against the locked FixRequestSchema. Streaming (SSE) arrives in
 * S4 without changing this request shape.
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
    return { client: new DotcmsClient({ dotcmsBaseUrl, authToken: token }) };
}

export function createA11yRoutes(deps: A11yRoutesDeps = {}): Hono {
    const registry = deps.registry ?? new ActiveRunRegistry();
    const makeRunDeps = deps.makeRunDeps ?? defaultMakeRunDeps;
    const app = new Hono();

    app.post('/fix', async (c) => {
        const bearer = parseBearer(c.req.header('Authorization'));
        if (!bearer) {
            return c.json({ error: 'Missing or malformed Authorization: Bearer token' }, 401);
        }

        let body: unknown;
        try {
            body = await c.req.json();
        } catch {
            return c.json({ error: 'Request body must be valid JSON' }, 400);
        }

        const parsed = FixRequestSchema.safeParse(body);
        if (!parsed.success) {
            return c.json(
                { error: 'Invalid request', issues: parsed.error.issues },
                422
            );
        }
        const request = parsed.data;

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

    app.get('/active-run', (c) => {
        const bearer = parseBearer(c.req.header('Authorization'));
        if (!bearer) {
            return c.json({ error: 'Missing or malformed Authorization: Bearer token' }, 401);
        }
        return c.json(registry.get(bearer.userId), 200);
    });

    return app;
}
