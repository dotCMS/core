// @vitest-environment happy-dom
//
// jsdom defines `window.location` as a non-configurable getter, so the `withLocation`
// stub below dies on "Cannot redefine property: location" — jest-preset-angular's jsdom
// allowed the redefine, this project's Vitest jsdom does not. happy-dom keeps `location`
// configurable and records an `href` assignment instead of attempting a navigation, which
// is exactly what these two tests assert on.
import { createServiceFactory, mockProvider, SpectatorService } from '@openng/spectator/vitest';
import { Mock, vi } from 'vitest';

import { LoginService } from '@dotcms/dotcms-js';

import {
    DotAiCompletionsStreamService,
    DotAiStreamEvent
} from './dot-ai-completions-stream.service';

/**
 * Builds a fake `Response` whose body streams the given chunks verbatim, so a test can
 * control exactly where the byte boundaries fall. Mirrors the approach in
 * `dot-agent-run.service.spec.ts`.
 */
const streamResponse = (chunks: string[], ok = true, status = 200): Response => {
    const encoder = new TextEncoder();
    let i = 0;

    return {
        ok,
        status,
        body: {
            getReader: () => ({
                read: () =>
                    Promise.resolve(
                        i < chunks.length
                            ? { done: false, value: encoder.encode(chunks[i++]) }
                            : { done: true, value: undefined }
                    ),
                cancel: () => Promise.resolve(),
                releaseLock: () => undefined
            })
        }
    } as unknown as Response;
};

const neverEnding = (onCancel: () => void): Response =>
    ({
        ok: true,
        status: 200,
        body: {
            getReader: () => ({
                read: () => new Promise(() => undefined),
                cancel: () => {
                    onCancel();

                    return Promise.resolve();
                },
                releaseLock: () => undefined
            })
        }
    }) as unknown as Response;

const delta = (content: string) =>
    `data: ${JSON.stringify({ choices: [{ delta: { content } }] })}\n`;

describe('DotAiCompletionsStreamService', () => {
    let spectator: SpectatorService<DotAiCompletionsStreamService>;
    let fetchMock: Mock;
    const originalFetch = global.fetch;

    const createService = createServiceFactory({
        service: DotAiCompletionsStreamService,
        providers: [mockProvider(LoginService, { auth: { user: { userId: 'dotcms.org.1' } } })]
    });

    const form = { prompt: 'hi', indexName: 'default', stream: true };

    beforeEach(() => {
        spectator = createService();
        fetchMock = vi.fn();
        global.fetch = fetchMock as unknown as typeof fetch;
    });

    afterEach(() => {
        global.fetch = originalFetch;
    });

    it('should emit deltas in order and complete on [DONE]', async () => {
        fetchMock.mockResolvedValue(
            streamResponse([delta('Hello'), delta(' world'), 'data: [DONE]\n'])
        );

        const events: unknown[] = [];
        const completed = await new Promise<boolean>((resolve) => {
            spectator.service.stream(form).subscribe({
                next: (e) => events.push(e),
                complete: () => resolve(true)
            });
        });

        expect(completed).toBe(true);
        expect(events).toEqual([
            { type: 'delta', content: 'Hello' },
            { type: 'delta', content: ' world' }
        ]);
    });

    it('should reassemble a JSON object split across two chunks', async () => {
        // The legacy portlet got this right and it is the easiest thing to lose in a rewrite:
        // a chunk boundary can fall in the middle of a JSON object.
        const whole = delta('split me');
        const cut = Math.floor(whole.length / 2);

        fetchMock.mockResolvedValue(
            streamResponse([whole.slice(0, cut), whole.slice(cut), 'data: [DONE]\n'])
        );

        const events: unknown[] = [];
        await new Promise<void>((resolve) => {
            spectator.service.stream(form).subscribe({
                next: (e) => events.push(e),
                complete: () => resolve()
            });
        });

        expect(events).toEqual([{ type: 'delta', content: 'split me' }]);
    });

    it('should treat a CRLF pair split across two reads as one frame separator', async () => {
        fetchMock.mockResolvedValue(
            streamResponse([delta('a').replace('\n', '\r'), '\n', 'data: [DONE]\n'])
        );

        const events: unknown[] = [];
        await new Promise<void>((resolve) => {
            spectator.service.stream(form).subscribe({
                next: (e) => events.push(e),
                complete: () => resolve()
            });
        });

        expect(events).toEqual([{ type: 'delta', content: 'a' }]);
    });

    it('should skip blank lines and :comment keep-alives', async () => {
        fetchMock.mockResolvedValue(
            streamResponse(['\n', ': keep-alive\n', delta('x'), '\n', 'data: [DONE]\n'])
        );

        const events: unknown[] = [];
        await new Promise<void>((resolve) => {
            spectator.service.stream(form).subscribe({
                next: (e) => events.push(e),
                complete: () => resolve()
            });
        });

        expect(events).toEqual([{ type: 'delta', content: 'x' }]);
    });

    it('should emit an error event for an in-band error frame', async () => {
        fetchMock.mockResolvedValue(
            streamResponse([`data: ${JSON.stringify({ error: 'rate limited' })}\n`])
        );

        const events: DotAiStreamEvent[] = [];
        await new Promise<void>((resolve) => {
            spectator.service.stream(form).subscribe({
                next: (e) => events.push(e),
                complete: () => resolve()
            });
        });

        expect(events[0]).toEqual({ type: 'error', message: 'rate limited' });
    });

    it('should error the observable when the response is not ok', async () => {
        fetchMock.mockResolvedValue(streamResponse([], false, 500));

        const caught = await new Promise<unknown>((resolve) => {
            spectator.service.stream(form).subscribe({ error: (e) => resolve(e) });
        });

        expect(caught).toBeTruthy();
    });

    it('should abort the request when the subscription is torn down', async () => {
        const cancelled = vi.fn();
        fetchMock.mockResolvedValue(neverEnding(cancelled));

        const sub = spectator.service.stream(form).subscribe();
        await Promise.resolve();
        sub.unsubscribe();
        await Promise.resolve();

        const signal = fetchMock.mock.calls[0][1].signal as AbortSignal;
        expect(signal.aborted).toBe(true);
    });

    it('should post the form as the request body', async () => {
        fetchMock.mockResolvedValue(streamResponse(['data: [DONE]\n']));

        await new Promise<void>((resolve) => {
            spectator.service.stream(form).subscribe({ complete: () => resolve() });
        });

        const [url, init] = fetchMock.mock.calls[0];
        expect(url).toContain('/api/v1/ai/completions');
        expect(init.method).toBe('POST');
        expect(JSON.parse(init.body)).toMatchObject({ prompt: 'hi', stream: true });
    });
    it('should NOT ask for text/event-stream', async () => {
        // The endpoint is a StreamingOutput, not SSE, and answers 406 to that Accept header.
        // A unit test cannot see the 406 — this guards the regression that e2e caught.
        fetchMock.mockResolvedValue(streamResponse(['data: [DONE]\n']));

        await new Promise<void>((resolve) => {
            spectator.service.stream(form).subscribe({ complete: () => resolve() });
        });

        const headers = fetchMock.mock.calls[0][1].headers;
        expect(headers.Accept).toBeUndefined();
        expect(JSON.stringify(headers)).not.toContain('event-stream');
    });
    it('should surface a bare JSON error line that carries no data: prefix', async () => {
        // Observed against a live instance: when retrieval matches nothing, the endpoint
        // answers with a plain JSON object and no SSE framing at all. Parsing only `data:`
        // lines drops it, and the user sees an empty answer with no explanation (FR-014).
        fetchMock.mockResolvedValue(
            streamResponse(['{"error":"no matching content found in the index for your query"}\n'])
        );

        const events: DotAiStreamEvent[] = [];
        await new Promise<void>((resolve) => {
            spectator.service.stream(form).subscribe({
                next: (e) => events.push(e),
                complete: () => resolve()
            });
        });

        expect(events[0]).toEqual({
            type: 'error',
            message: 'no matching content found in the index for your query'
        });
    });

    it('should ignore a bare JSON line that is not an error', async () => {
        fetchMock.mockResolvedValue(
            streamResponse(['{"ok":true}\n', delta('answered'), 'data: [DONE]\n'])
        );

        const events: DotAiStreamEvent[] = [];
        await new Promise<void>((resolve) => {
            spectator.service.stream(form).subscribe({
                next: (e) => events.push(e),
                complete: () => resolve()
            });
        });

        expect(events).toEqual([{ type: 'delta', content: 'answered' }]);
    });

    describe('a stream that produces nothing', () => {
        it('should report it rather than completing on an empty answer', async () => {
            // The alternative is an answer card that renders blank, which is
            // indistinguishable from a bug in the client.
            fetchMock.mockResolvedValue(streamResponse(['data: [DONE]\n']));

            const events: DotAiStreamEvent[] = [];
            await new Promise<void>((resolve) => {
                spectator.service.stream(form).subscribe({
                    next: (e) => events.push(e),
                    complete: () => resolve()
                });
            });

            expect(events).toEqual([{ type: 'error', message: 'dotai.chat.error.empty' }]);
        });

        it('should also report it when the body ends without a [DONE]', async () => {
            fetchMock.mockResolvedValue(streamResponse([': keep-alive\n']));

            const events: DotAiStreamEvent[] = [];
            await new Promise<void>((resolve) => {
                spectator.service.stream(form).subscribe({
                    next: (e) => events.push(e),
                    complete: () => resolve()
                });
            });

            expect(events).toEqual([{ type: 'error', message: 'dotai.chat.error.empty' }]);
        });

        it('should stay quiet when an unframed error already explained it', async () => {
            fetchMock.mockResolvedValue(
                streamResponse(['{"error":"no matching content found"}\n'])
            );

            const events: DotAiStreamEvent[] = [];
            await new Promise<void>((resolve) => {
                spectator.service.stream(form).subscribe({
                    next: (e) => events.push(e),
                    complete: () => resolve()
                });
            });

            expect(events).toEqual([{ type: 'error', message: 'no matching content found' }]);
        });
    });

    describe('401 mid-screen', () => {
        // `fetch` is what makes incremental reads possible, and the price is that this request
        // never reaches `serverErrorInterceptor` — so the logout it performs has to be done
        // here, or the dotAI tab is the one screen that silently fails on a dead session.
        const withLocation = (): { href: string } => {
            const stub = { href: '' };
            Object.defineProperty(window, 'location', {
                value: stub,
                writable: true,
                configurable: true
            });

            return stub;
        };

        it('should redirect to logout when there is a session', async () => {
            const location = withLocation();
            fetchMock.mockResolvedValue(streamResponse([], false, 401));

            await new Promise<void>((resolve) => {
                spectator.service.stream(form).subscribe({
                    error: () => resolve(),
                    complete: () => resolve()
                });
                setTimeout(resolve, 0);
            });

            expect(location.href).toContain('/dotAdmin/logout');
        });

        it('should surface the failure instead when nobody is logged in', async () => {
            const location = withLocation();
            spectator.inject(LoginService).auth = { user: null } as never;
            fetchMock.mockResolvedValue(streamResponse([], false, 401));

            const error = await new Promise<Error>((resolve) => {
                spectator.service.stream(form).subscribe({
                    error: (e: Error) => resolve(e)
                });
            });

            expect(location.href).toBe('');
            expect(error.message).toContain('401');
        });
    });
});
