/**
 * Amplify Hosting compute entry point.
 *
 * Amplify requires the compute entry to start an HTTP server on port 3000
 * (deployment spec). Angular's `server.mjs` only calls `listen()` when it is the
 * main module, so this wrapper imports the Express app and serves it itself.
 *
 * It also:
 *  1. Loads a `.env` file bundled next to this file. The variables set in the
 *     Amplify console are available to the build, not to the compute runtime, so
 *     `amplify.yml` writes them to `.env` and the post-build script bundles it
 *     here (see "Environment variables" in the README).
 *  2. Promotes `x-forwarded-host` to `host` and strips `x-forwarded-*`, so
 *     Angular's SSRF host allowlist sees a single clean host behind CloudFront.
 */
const http = require('node:http');
const path = require('node:path');

try {
  process.loadEnvFile(path.join(__dirname, '.env'));
} catch {
  // No .env bundled: rely on the process environment.
}

// Amplify does not set NODE_ENV for the compute; default to production so the
// dotCMS client does not log verbosely (see getClient in src/server.ts).
process.env.NODE_ENV = process.env.NODE_ENV || 'production';

// Amplify routes traffic to port 3000, always. `PORT` is deliberately not read:
// a bundled .env may carry the dev server's `PORT=4000`.
const PORT = 3000;
const appPromise = import(path.join(__dirname, 'server', 'server.mjs'));

const server = http.createServer(async (req, res) => {
  const forwardedHost = req.headers['x-forwarded-host'];
  if (forwardedHost) {
    req.headers.host = Array.isArray(forwardedHost) ? forwardedHost[0] : forwardedHost;
  }
  for (const name of Object.keys(req.headers)) {
    if (name.startsWith('x-forwarded-')) {
      delete req.headers[name];
    }
  }

  try {
    const { default: app } = await appPromise;
    app(req, res);
  } catch (error) {
    console.error('SSR bootstrap failed', error);
    res.statusCode = 500;
    res.end('Internal Server Error');
  }
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`Angular SSR listening on port ${PORT}`);
});
