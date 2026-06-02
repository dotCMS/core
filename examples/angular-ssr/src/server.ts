import {
  AngularNodeAppEngine,
  createNodeRequestHandler,
  isMainModule,
  writeResponseToNodeResponse,
} from '@angular/ssr/node';
import express from 'express';
import { join } from 'node:path';
import { config } from 'dotenv';

import { createDotCMSClient } from '@dotcms/client';
import { DotCMSPageRequestParams, DotErrorPage } from '@dotcms/types';

// Load environment variables from .env file
config();

/**
 * Angular 20.3+ SSR validates the incoming `host` header against an allowlist
 * (SSRF protection). Allow `localhost` (for `serve:ssr`) and the `*.vercel.app`
 * wildcard, which covers Vercel's generated production, preview and branch
 * domains. On Vercel the proxy host is normalized into `host` by `api/index.js`
 * before this engine sees the request. Add a custom production domain via the
 * `NG_ALLOWED_HOSTS` env var. The engine reads this var when constructed below.
 * @see https://angular.dev/best-practices/security#preventing-server-side-request-forgery-ssrf
 */
const allowedHosts = new Set(
  (process.env['NG_ALLOWED_HOSTS'] ?? '').split(',').map((host) => host.trim())
);
allowedHosts.add('localhost');
allowedHosts.add('*.vercel.app');
process.env['NG_ALLOWED_HOSTS'] = [...allowedHosts].filter(Boolean).join(',');

const getClient = () => {
  const authToken = process.env['DOTCMS_AUTH_TOKEN'];
  if (!authToken) {
    throw new Error('DOTCMS_AUTH_TOKEN environment variable is required');
  }

  return createDotCMSClient({
    dotcmsUrl: process.env['DOTCMS_URL'] || 'https://demo.dotcms.com',
    authToken,
    siteId: process.env['DOTCMS_SITE_ID'] || 'YOUR_SITE_ID',
    logLevel: process.env['NODE_ENV'] === 'production' ? 'default' : 'verbose',
  });
};

const browserDistFolder = join(import.meta.dirname, '../browser');

const app = express();
// Parse JSON bodies for API routes
app.use(express.json());
const angularApp = new AngularNodeAppEngine();

// Changed from /api/page to /data/page to avoid Vercel routing conflicts
app.post('/data/page', async (req: express.Request, res: express.Response) => {
  const { url, params } = req.body as { url?: string; params?: DotCMSPageRequestParams };

  if (!url) {
    return res.status(400).json({ error: 'Missing "url" in request body' });
  }

  try {
    const client = getClient();
    const response = await client.page.get(url, params ?? {});
    return res.json(response);
  } catch (error) {
    if (error instanceof DotErrorPage) {
      return res.status(error.httpError?.status ?? 500).json(error.toJSON());
    }
    return res.status(500).json({ error: (error as Error).message });
  }
});

/**
 * Example Express Rest API endpoints can be defined here.
 * Uncomment and define endpoints as necessary.
 *
 * Example:
 * ```ts
 * app.get('/api/{*splat}', (req, res) => {
 *   // Handle API request
 * });
 * ```
 */

/**
 * Serve static files from /browser
 */
app.use(
  express.static(browserDistFolder, {
    maxAge: '1y',
    index: false,
    redirect: false,
  })
);

/**
 * Handle all other requests by rendering the Angular application.
 *
 * The Express `app` is passed through as the SSR request context so that, while
 * rendering on the server, the app's relative API calls (e.g. `/data/page`) can
 * be dispatched back through this same Express instance in-process instead of
 * over the network. This avoids the SSR self-fetch round-tripping through the
 * public (proxy) host — which is unreachable on Vercel, where the function is
 * invoked directly and never calls `app.listen()`. The dotCMS auth token stays
 * server-side: only the `/data/page` handler ever reads it.
 */
app.use((req, res, next) => {
  angularApp
    .handle(req, { app })
    .then((response) => (response ? writeResponseToNodeResponse(response, res) : next()))
    .catch(next);
});

/**
 * Start the server if this module is the main entry point.
 * The server listens on the port defined by the `PORT` environment variable, or defaults to 4000.
 */
if (isMainModule(import.meta.url)) {
  const port = process.env['PORT'] || 4000;
  app.listen(port, (error) => {
    if (error) {
      throw error;
    }

    console.log(`Node Express server listening on http://localhost:${port}`);
  });
}

/**
 * Request handler used by the Angular CLI (for dev-server and during build) or Firebase Cloud Functions.
 */
export const reqHandler = createNodeRequestHandler(app);

// Export the Express app for Vercel
export default app;
