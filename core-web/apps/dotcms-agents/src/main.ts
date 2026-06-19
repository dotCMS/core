import { serve } from '@hono/node-server';
import { Hono } from 'hono';

/**
 * dotcms-agents — host service for dotCMS agent capabilities.
 *
 * First capability: the accessibility-fix agent (S1). Routes are grouped by
 * capability (e.g. `/a11y/...`) so additional agents can be added later without
 * a new service. Plain JSON for now; SSE streaming arrives in a later phase
 * (plan §8) without changing the request shape.
 */
const app = new Hono();

app.get('/health', (c) => c.json({ ok: true, service: 'dotcms-agents' }));

const port = Number(process.env.PORT ?? 3001);

serve({ fetch: app.fetch, port }, (info) => {
    // eslint-disable-next-line no-console
    console.log(`dotcms-agents listening on http://localhost:${info.port}`);
});

export { app };
