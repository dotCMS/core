const path = require('path');

// Load the compiled Angular server bundle once, at module init, rather than on
// every request (the dynamic import is cached, but this avoids the per-request
// path build and promise hop on the serverless hot path).
const appPromise = import(path.join(process.cwd(), 'dist/angular-ssr/server/server.mjs'));

/**
 * Vercel serverless entry for the Angular SSR app.
 *
 * Angular 20.3+ SSR validates the request host against an allowlist (SSRF
 * protection). Behind Vercel's proxy the real host arrives in `x-forwarded-host`
 * while `host` is an internal value, and Angular only reads the forwarded host
 * when proxy-header trust is enabled — a code path that proved order-dependent
 * across requests in the same warm function instance.
 *
 * To keep host validation deterministic we normalize the request here, before
 * Angular sees it: promote `x-forwarded-host` to `host`, then strip every
 * `x-forwarded-*` header so the engine validates a single, clean `host` value
 * with no proxy-trust logic involved. The allowlist itself (`NG_ALLOWED_HOSTS`)
 * is configured in `server.ts`.
 *
 * Angular declined to auto-resolve proxied hosts (angular/angular-cli#32616),
 * leaving this to the deployment layer — which is exactly what we do here.
 *
 * @see https://angular.dev/best-practices/security#preventing-server-side-request-forgery-ssrf
 * @see https://github.com/angular/angular-cli/issues/32616
 */
module.exports = async (req, res) => {
  const forwardedHost = req.headers['x-forwarded-host'];
  if (forwardedHost) {
    req.headers.host = Array.isArray(forwardedHost) ? forwardedHost[0] : forwardedHost;
  }

  // Remove proxy headers so Angular validates `host` directly and the
  // (order-sensitive) trustProxyHeaders path is never exercised. Node lowercases
  // incoming header names, so a plain prefix check is enough.
  for (const name of Object.keys(req.headers)) {
    if (name.startsWith('x-forwarded-')) {
      delete req.headers[name];
    }
  }

  const { default: app } = await appPromise;
  return app(req, res);
};
