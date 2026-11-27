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
 * (SSRF protection). Allow `localhost` (for `serve:ssr`) and the `*.amplifyapp.com`
 * wildcard, which covers Amplify's generated branch domains. Behind CloudFront the
 * proxy host is normalized into `host` by `amplify/compute-entry.cjs` before this
 * engine sees the request. Add a custom production domain via the
 * `NG_ALLOWED_HOSTS` env var. The engine reads this var when constructed below.
 *
 * `NG_ALLOWED_HOSTS` is an official Angular alternative to the `angular.json`
 * `security.allowedHosts` option; we use it here so deployers can add domains
 * without rebuilding. Allowlisting the deployment host is the app's
 * responsibility by design — Angular declined to auto-handle proxied hosts
 * (angular/angular-cli#32616), so this is the sanctioned pattern, not a hack.
 * @see https://angular.dev/best-practices/security#preventing-server-side-request-forgery-ssrf
 */
process.env['NG_ALLOWED_HOSTS'] = ['localhost', '*.amplifyapp.com', process.env['NG_ALLOWED_HOSTS']]
  .filter(Boolean)
  .join(',');

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

/**
 * `/data/page` runs with the server-side dotCMS token and is reachable from the
 * internet. The app only ever sends `url` and, optionally, `params.graphql`;
 * everything else (`mode`, `publishDate`, ...) is dropped so callers cannot ask
 * for unpublished content through those params.
 *
 * `params.graphql` is forwarded as sent, so any client can run read queries with
 * the server token: the `sec-fetch-site` check below only stops browsers on other
 * sites, not `curl`. Use a read-only token limited to content the site already
 * publishes, and in a production app replace the pass-through with fixed
 * server-side queries.
 */
const MAX_URL_LENGTH = 2048;
const MAX_GRAPHQL_LENGTH = 16 * 1024;

const isSafePagePath = (url: unknown): url is string =>
  typeof url === 'string' &&
  url.length <= MAX_URL_LENGTH &&
  url.startsWith('/') &&
  !url.startsWith('//') &&
  !url.includes('\\') &&
  !/[\u0000-\u001f]/.test(url);

const sanitizePageParams = (params: unknown): DotCMSPageRequestParams | null => {
  if (params === undefined || params === null) {
    return {};
  }
  if (typeof params !== 'object' || Array.isArray(params)) {
    return null;
  }
  const { graphql } = params as { graphql?: unknown };
  if (graphql === undefined) {
    return {};
  }
  if (
    typeof graphql !== 'object' ||
    graphql === null ||
    JSON.stringify(graphql).length > MAX_GRAPHQL_LENGTH
  ) {
    return null;
  }
  return { graphql } as DotCMSPageRequestParams;
};

// Proxies page requests to dotCMS so the auth token stays on the server.
app.post('/data/page', async (req: express.Request, res: express.Response) => {
  // Browsers mark cross-site requests; the app's own calls are same-origin or in-process.
  if (req.headers['sec-fetch-site'] === 'cross-site') {
    return res.status(403).json({ error: 'Forbidden' });
  }

  const { url, params } = (req.body ?? {}) as { url?: unknown; params?: unknown };

  if (!isSafePagePath(url)) {
    return res.status(400).json({ error: 'Invalid or missing "url" in request body' });
  }

  const pageParams = sanitizePageParams(params);
  if (!pageParams) {
    return res.status(400).json({ error: 'Invalid "params" in request body' });
  }

  try {
    const client = getClient();
    const response = await client.page.get(url, pageParams);
    return res.json(response);
  } catch (error) {
    // Log the real cause server-side; never return stack traces or upstream
    // details (dotCMS URL, raw response bodies) to the caller.
    console.error('/data/page failed:', (error as Error).message);
    if (error instanceof DotErrorPage) {
      const status = error.httpError?.status ?? 500;
      return res.status(status).json({ error: 'Page request failed', status });
    }
    return res.status(500).json({ error: 'Internal Server Error', status: 500 });
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
 * public (CloudFront) host. The dotCMS auth token stays server-side: only the
 * `/data/page` handler ever reads it.
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

// Export the Express app: amplify/compute-entry.cjs imports it and serves it on port 3000.
export default app;
