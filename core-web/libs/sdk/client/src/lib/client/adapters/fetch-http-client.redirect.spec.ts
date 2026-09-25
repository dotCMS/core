// @vitest-environment node
/**
 * Runs FetchHttpClient against real local HTTP servers instead of a mocked fetch, so the
 * `redirected` / `url` / `content-type` signals come from an actual fetch redirect.
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

    it('names both URLs and dotcmsUrl when a failed request was 301d to another origin', async () => {
        const requested = `${redirector.origin}/api/v1/graphql`;
        const error = await captureError(
            new FetchHttpClient().request(requested, {
                method: 'POST',
                headers: { Authorization: 'Bearer token', 'Content-Type': 'application/json' },
                body: '{}'
            })
        );

        expect(error).toBeInstanceOf(DotHttpError);
        expect(error.status).toBe(500);
        expect(error.message).toMatch(/^HTTP 500: Internal Server Error\. The request to /);
        expect(error.message).toContain(`'${requested}'`);
        expect(error.message).toContain(`'${target.origin}/api/v1/graphql'`);
        expect(error.message).toContain('dotcmsUrl');
        expect(error.message).toContain("'text/html'");
    });

    it('does not blame dotcmsUrl when the redirect stayed on the same origin', async () => {
        const error = await captureError(
            new FetchHttpClient().request(`${redirector.origin}/same-origin/start`)
        );

        expect(error).toBeInstanceOf(DotHttpError);
        expect(error.message).toBe('HTTP 500: Internal Server Error');
    });

    it('rejects a 200 HTML page reached through a cross-origin redirect', async () => {
        const requested = `${redirector.origin}/api/v1/nav/`;
        const error = await captureError(new FetchHttpClient().request(requested));

        expect(error).toBeInstanceOf(DotHttpError);
        expect(error.status).toBe(502);
        expect(error.statusText).toBe('Bad Gateway');
        expect(error.message).toContain('(HTTP 200)');
        expect(error.data).toBe('<html>Sign in</html>');
        expect(error.message).toContain(`'${requested}'`);
        expect(error.message).toContain(`'${target.origin}/api/v1/nav/'`);
        expect(error.message).toContain('not JSON');
        expect(error.message).toContain('dotcmsUrl');
    });

    it('carries the diagnosis through client.page.get() to DotErrorPage.message', async () => {
        const client = createDotCMSClient({ dotcmsUrl: redirector.origin, authToken: 'token' });

        const error = await captureError<DotErrorPage>(client.page.get('/'));

        expect(error).toBeInstanceOf(DotErrorPage);
        expect(error.status).toBe(500);
        expect(error.message).toContain(`'${redirector.origin}/api/v1/graphql'`);
        expect(error.message).toContain(`'${target.origin}/api/v1/graphql'`);
        expect(error.message).toContain('dotcmsUrl');
    });
});
