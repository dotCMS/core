// @vitest-environment node
/**
 * Runs FetchHttpClient against real local HTTP servers instead of a mocked fetch, so the
 * `redirected` / `url` / `content-type` signals come from an actual fetch redirect, and the
 * connection error (and its `cause.code`) from an actual refused connection.
 *
 * Two servers on two ports stand in for `http://demo.dotcms.com` and `https://demo.dotcms.com`:
 * a request to the first is 301'd to the second, which answers 500 with an empty HTML body —
 * what demo.dotcms.com does once an authenticated POST has lost its body and credentials on
 * the scheme change. A different port is a different origin, which is what the client checks.
 */
import { afterAll, beforeAll, describe, expect, it } from 'vitest';

import { createServer, IncomingMessage, Server, ServerResponse } from 'node:http';
import { AddressInfo } from 'node:net';

import { DotErrorPage, DotHttpError } from '@dotcms/types';

import { FetchHttpClient } from './fetch-http-client';

import { createDotCMSClient } from '../client';

type Handler = (req: IncomingMessage, res: ServerResponse) => void;

function listen(handler: Handler): Promise<{ server: Server; origin: string }> {
    return new Promise((resolve) => {
        const server = createServer(handler);
        server.listen(0, '127.0.0.1', () => {
            const { port } = server.address() as AddressInfo;
            resolve({ server, origin: `http://127.0.0.1:${port}` });
        });
    });
}

function close(server: Server): Promise<void> {
    return new Promise((resolve) => server.close(() => resolve()));
}

async function captureError<T = DotHttpError>(promise: Promise<unknown>): Promise<T> {
    try {
        await promise;
    } catch (error) {
        return error as T;
    }

    throw new Error('Expected the request to reject, but it resolved');
}

describe('FetchHttpClient against a redirecting fixture server', () => {
    let target: { server: Server; origin: string };
    let redirector: { server: Server; origin: string };

    beforeAll(async () => {
        // The "https" side: fails every request the way dotCMS does after the lost POST.
        target = await listen((req, res) => {
            if (req.url === '/api/v1/nav/') {
                res.writeHead(200, { 'content-type': 'text/html' });
                res.end('<html>Sign in</html>');

                return;
            }

            res.writeHead(500, { 'content-type': 'text/html' });
            res.end();
        });

        // The "http" side: moves everything to the target origin, except /same-origin/*,
        // which it moves to a path on itself.
        redirector = await listen((req, res) => {
            if (req.url?.startsWith('/same-origin/')) {
                if (req.url === '/same-origin/moved') {
                    res.writeHead(500, { 'content-type': 'application/json' });
                    res.end(JSON.stringify({ message: 'boom' }));

                    return;
                }

                res.writeHead(301, { location: '/same-origin/moved' });
                res.end();

                return;
            }

            res.writeHead(301, { location: `${target.origin}${req.url}` });
            res.end();
        });
    });

    afterAll(async () => {
        await Promise.all([close(target.server), close(redirector.server)]);
    });

    it('leads with the dotcmsUrl fix when a failed request was 301d to another origin', async () => {
        const error = await captureError(
            new FetchHttpClient().request(`${redirector.origin}/api/v1/graphql`, {
                method: 'POST',
                headers: { Authorization: 'Bearer token', 'Content-Type': 'application/json' },
                body: '{}'
            })
        );

        expect(error).toBeInstanceOf(DotHttpError);
        expect(error.status).toBe(500);
        // The redirect explains the HTML answer, so no competing content-type cause.
        expect(error.message).toBe(
            `dotcmsUrl is '${redirector.origin}' but the server redirected to '${target.origin}'. Set dotcmsUrl to '${target.origin}'. A redirect to another origin drops the Authorization header and can turn a POST into a GET (HTTP 500 after the redirect).`
        );
    });

    it('does not blame dotcmsUrl when the redirect stayed on the same origin', async () => {
        const error = await captureError(
            new FetchHttpClient().request(`${redirector.origin}/same-origin/start`)
        );

        expect(error).toBeInstanceOf(DotHttpError);
        expect(error.message).toBe('HTTP 500: Internal Server Error');
    });

    it('rejects a 200 HTML page reached through a cross-origin redirect', async () => {
        const error = await captureError(
            new FetchHttpClient().request(`${redirector.origin}/api/v1/nav/`)
        );

        expect(error).toBeInstanceOf(DotHttpError);
        expect(error.status).toBe(502);
        expect(error.statusText).toBe('Bad Gateway');
        expect(error.data).toBe('<html>Sign in</html>');
        expect(error.message).toBe(
            `dotcmsUrl is '${redirector.origin}' but the server redirected to '${target.origin}'. Set dotcmsUrl to '${target.origin}'. A redirect to another origin drops the Authorization header and can turn a POST into a GET (HTTP 200 after the redirect).`
        );
    });

    it('carries the fix through client.page.get() to DotErrorPage.message', async () => {
        const client = createDotCMSClient({ dotcmsUrl: redirector.origin, authToken: 'token' });

        const error = await captureError<DotErrorPage>(client.page.get('/'));

        expect(error).toBeInstanceOf(DotErrorPage);
        expect(error.status).toBe(500);
        expect(error.message).toContain(`Set dotcmsUrl to '${target.origin}'.`);
    });
});

describe('FetchHttpClient when nothing answers', () => {
    let closedOrigin: string;

    beforeAll(async () => {
        // Take a free port, then close it, so connecting to it is refused.
        const { server, origin } = await listen((_req, res) => res.end());
        await close(server);
        closedOrigin = origin;
    });

    it('names the URL, the cause code and the host, without browser-only causes', async () => {
        const requested = `${closedOrigin}/api/v1/graphql`;
        const error = await captureError(new FetchHttpClient().request(requested));

        expect(error).toBeInstanceOf(DotHttpError);
        expect(error.status).toBe(0);
        expect(error.statusText).toBe('Network Error');
        expect(error.message).toBe(
            `Couldn't reach '${requested}' (ECONNREFUSED ${new URL(closedOrigin).host}). Check the scheme, host and port in dotcmsUrl, and that dotCMS is reachable at that address.`
        );
    });

    it('carries it through client.page.get() to DotErrorPage.message', async () => {
        const client = createDotCMSClient({ dotcmsUrl: closedOrigin, authToken: 'token' });

        const error = await captureError<DotErrorPage>(client.page.get('/'));

        expect(error).toBeInstanceOf(DotErrorPage);
        expect(error.status).toBe(0);
        expect(error.message).toContain(`Couldn't reach '${closedOrigin}/api/v1/graphql'`);
    });
});
