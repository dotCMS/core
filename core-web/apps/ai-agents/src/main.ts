import { serve } from '@hono/node-server';
import { config as loadDotenv } from 'dotenv';
import { Hono } from 'hono';
import { cors } from 'hono/cors';

import { existsSync } from 'node:fs';
import { resolve } from 'node:path';

import { createA11yRoutes } from './a11y/api/routes';

// Dev: load the repo-root .env (DOTCMS_URL/DOTCMS_AUTH/OPENROUTER_KEY/…) so
// `nx serve ai-agents` works without sourcing env by hand. The .env lives at the
// monorepo root (core/.env), one level above the Nx workspace (core-web), which is
// the cwd under `nx serve`. Real env vars already set always win (override:false).
// In production these come from the deployment env, not a file — a missing .env
// here is silently fine.
for (const candidate of ['../.env', '.env']) {
    const p = resolve(process.cwd(), candidate);
    if (existsSync(p)) {
        loadDotenv({ path: p });
        break;
    }
}

/**
 * ai-agents — host service for dotCMS AI agent capabilities.
 *
 * First capability: the accessibility-fix agent (S1). Routes are grouped by
 * capability (e.g. `/a11y/...`) so additional agents can be added later without
 * a new service. Plain JSON for now; SSE streaming arrives in a later phase
 * (plan §8) without changing the request shape.
 */
const app = new Hono();

// Dev: the Studio (localhost:4200 / the Nx dev server) calls the agent directly
// (the dev shortcut — plan §10 Phase 2). In production the same-origin dotCMS
// proxy fronts the agent, so CORS is a dev convenience. Allowed origins are
// env-configurable; default to the local Angular dev server.
app.use(
    '/a11y/*',
    cors({
        origin: (process.env.A11Y_AGENT_CORS_ORIGINS ?? 'http://localhost:4200').split(','),
        allowHeaders: ['Authorization', 'Content-Type'],
        allowMethods: ['POST', 'GET', 'OPTIONS']
    })
);

app.get('/health', (c) => c.json({ ok: true, service: 'ai-agents' }));

// The a11y-fix agent: POST /a11y/fix, GET /a11y/active-run.
app.route('/a11y', createA11yRoutes());

const port = Number(process.env.PORT ?? 3001);

serve({ fetch: app.fetch, port }, (info) => {
    // eslint-disable-next-line no-console
    console.log(`ai-agents listening on http://localhost:${info.port}`);
});

export { app };
