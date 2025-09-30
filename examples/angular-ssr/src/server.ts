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

const getClient = () => {
  const authToken = process.env['DOTCMS_AUTH_TOKEN'];
  if (!authToken) {
    throw new Error('DOTCMS_AUTH_TOKEN environment variable is required');
  }

  return createDotCMSClient({
    dotcmsUrl: process.env['DOTCMS_URL'] || 'https://demo.dotcms.com',
    authToken,
    siteId: process.env['DOTCMS_SITE_ID'] || 'YOUR_SITE_ID',
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
 */
app.use((req, res, next) => {
  angularApp
    .handle(req)
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
