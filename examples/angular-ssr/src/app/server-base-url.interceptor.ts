import { HttpErrorResponse, HttpInterceptorFn, HttpResponse } from '@angular/common/http';
import { inject, REQUEST_CONTEXT } from '@angular/core';
import { Observable } from 'rxjs';
import { EventEmitter } from 'node:events';
import { createRequest, createResponse, RequestMethod } from 'node-mocks-http';
import type { Express } from 'express';

interface ServerRequestContext {
  app: Express;
}

/**
 * During SSR, Angular's `HttpClient` resolves relative URLs (e.g. `/data/page`)
 * against the incoming request's origin. Behind a proxy such as Vercel that
 * origin is the public deployment host, so the server ends up fetching its own
 * API over the network — which fails (the public URL is unreachable from inside
 * the function, and Vercel never runs `app.listen()` so there is no localhost
 * server either). The failed render then falls back to CSR, which also fails
 * because `index.csr.html` is not emitted in `outputMode: server`.
 *
 * This interceptor (registered only on the server) dispatches relative requests
 * straight through the Express `app` in-process — no network, no port — using
 * the app handed in via `REQUEST_CONTEXT` from `server.ts`. The dotCMS auth
 * token never leaves the server: the `/data/page` handler still runs in-process
 * and is the only place that reads it.
 */
export const serverBaseUrlInterceptor: HttpInterceptorFn = (req, next) => {
  const context = inject(REQUEST_CONTEXT) as ServerRequestContext | null;
  const app = context?.app;

  // Absolute URLs (e.g. direct calls to dotCMS) and requests made when no
  // Express app is available fall through to the default fetch backend.
  if (!app || /^https?:\/\//i.test(req.url)) {
    return next(req);
  }

  const path = req.url.startsWith('/') ? req.url : `/${req.url}`;

  return new Observable((observer) => {
    const mockReq = createRequest({
      method: req.method as RequestMethod,
      url: path,
      headers: { 'content-type': 'application/json' },
      body: req.body ?? undefined,
    });
    const mockRes = createResponse({ eventEmitter: EventEmitter });

    mockRes.on('end', () => {
      const status = mockRes.statusCode;
      const body = mockRes._getData();
      const parsed = typeof body === 'string' && body.length ? safeJsonParse(body) : body;

      if (status >= 200 && status < 300) {
        observer.next(
          new HttpResponse({ status, body: parsed, url: path })
        );
        observer.complete();
      } else {
        observer.error(
          new HttpErrorResponse({ status, error: parsed, url: path })
        );
      }
    });

    // Run the request through the Express app in-memory.
    app(mockReq as never, mockRes as never);
  });
};

function safeJsonParse(value: string): unknown {
  try {
    return JSON.parse(value);
  } catch {
    return value;
  }
}
