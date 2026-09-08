import { Observable } from 'rxjs';

import { inject, Injectable, Injector } from '@angular/core';

import { HttpCode, LoginService, LOGOUT_URL } from '@dotcms/dotcms-js';
import { DotAiCompletionsForm } from '@dotcms/dotcms-models';

import { AI_API_ENDPOINT } from './dot-ai.constants';

export type DotAiStreamEvent =
    | { type: 'delta'; content: string }
    | { type: 'error'; message: string };

/** One `data:` frame from the completions stream. */
interface DotAiCompletionFrame {
    error?: unknown;
    message?: unknown;
    choices?: { delta?: { content?: string } }[];
}

const DONE = '[DONE]';
const DATA_PREFIX = 'data:';

/**
 * Shown when the stream closes having produced neither a delta nor an error.
 *
 * Without it the answer settles into COMPLETE with empty content, which renders as a blank
 * card — indistinguishable from a bug in the client (FR-014).
 */
const EMPTY_STREAM_KEY = 'dotai.chat.error.empty';

/**
 * Streams a chat completion token by token.
 *
 * **Not `providedIn: 'root'`** — it is provided by the dotAI route so it lives and dies with
 * the screen. Teardown is what aborts the in-flight `fetch`, which is what makes the Stop
 * button and "leaving the tab cancels generation" real rather than cosmetic.
 *
 * It uses `fetch` rather than `HttpClient` because reading a response incrementally needs a
 * `ReadableStream`. The consequence, stated plainly: this bypasses Angular's interceptor
 * chain. It is same-origin with credentials, which is what the legacy portlet relied on.
 *
 * The frames here are **bare** `data:` lines with no event name, terminated by `data: [DONE]`
 * — a different protocol from `DotAgentRunService`, which parses named `event:` frames onto a
 * closed union and would drop every one of these. That service is prior art for the
 * technique, not a dependency.
 */
@Injectable()
export class DotAiCompletionsStreamService {
    // Resolved lazily, exactly as `serverErrorInterceptor` does it: this service is provided
    // by the dotAI route, and `LoginService` pulls in the whole auth graph.
    readonly #injector = inject(Injector);

    stream(form: DotAiCompletionsForm): Observable<DotAiStreamEvent> {
        return new Observable<DotAiStreamEvent>((subscriber) => {
            const controller = new AbortController();

            void this.#run(form, controller, subscriber);

            return () => controller.abort();
        });
    }

    async #run(
        form: DotAiCompletionsForm,
        controller: AbortController,
        subscriber: {
            next: (event: DotAiStreamEvent) => void;
            error: (error: unknown) => void;
            complete: () => void;
        }
    ): Promise<void> {
        let sawOutput = false;
        // Wraps `subscriber` so every emit path — framed, bare, or error — is counted in one
        // place, rather than each call site having to remember to set a flag.
        const tracked = {
            next: (event: DotAiStreamEvent) => {
                sawOutput = true;
                subscriber.next(event);
            }
        };

        /** Nothing came back at all: say so rather than completing on an empty answer. */
        const complete = () => {
            if (!sawOutput) {
                subscriber.next({ type: 'error', message: EMPTY_STREAM_KEY });
            }

            subscriber.complete();
        };

        try {
            const response = await fetch(`${AI_API_ENDPOINT}/completions`, {
                method: 'POST',
                credentials: 'same-origin',
                // No `Accept: text/event-stream` here, deliberately. This endpoint returns a
                // JAX-RS StreamingOutput, not SSE, and does not declare that media type — asking
                // for it gets HTTP 406 Not Acceptable. (Verified against a running instance;
                // DotAgentRunService does send it, because /api/v1/agents/* really is SSE.)
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ ...form, stream: true }),
                signal: controller.signal
            });

            if (!response.ok || !response.body) {
                // `fetch` is what makes incremental reads possible, and the cost is that this
                // request never sees `serverErrorInterceptor`. A 401 here means the session
                // died mid-screen; without this the user is left staring at a failed answer on
                // a portlet whose every other call is redirecting them to the login page.
                if (response.status === HttpCode.UNAUTHORIZED && this.#hasSession()) {
                    window.location.href = `${LOGOUT_URL}?r=${new Date().getTime()}`;

                    return;
                }

                subscriber.error(new Error(`Completions stream failed: ${response.status}`));

                return;
            }

            const reader = response.body.getReader();
            const decoder = new TextDecoder();
            let buffer = '';

            for (;;) {
                const { done, value } = await reader.read();

                if (done) {
                    break;
                }

                buffer += decoder.decode(value, { stream: true });
                // Normalise across the whole buffer, so a CRLF pair split between two reads
                // still resolves to a single separator.
                buffer = buffer.replace(/\r\n/g, '\n').replace(/\r/g, '\n');

                const lines = buffer.split('\n');
                // The last element is whatever came after the final newline — possibly half a
                // JSON object. Keep it and prepend it to the next chunk.
                buffer = lines.pop() ?? '';

                for (const line of lines) {
                    if (this.#emit(line, tracked)) {
                        complete();

                        return;
                    }
                }
            }

            // Flush a trailing frame that arrived without a closing newline.
            if (buffer.trim()) {
                this.#emit(buffer, tracked);
            }

            complete();
        } catch (error) {
            if (!controller.signal.aborted) {
                subscriber.error(error);
            }
        }
    }

    /**
     * Whether there is a session to log out of.
     *
     * Same guard as the interceptor: a 401 on a screen nobody is logged into is just a 401,
     * and redirecting to logout from there is a loop.
     */
    #hasSession(): boolean {
        return !!this.#injector.get(LoginService).auth?.user;
    }

    /** Returns true when the stream is finished. */
    #emit(line: string, subscriber: { next: (event: DotAiStreamEvent) => void }): boolean {
        const trimmed = line.trim();

        // Blank lines separate frames; ':' lines are keep-alive comments.
        if (!trimmed || trimmed.startsWith(':')) {
            return false;
        }

        // Not every line is SSE. When retrieval matches nothing the endpoint answers with a
        // bare JSON object and no framing at all — `{"error":"no matching content found..."}`.
        // Dropping it as "not a data: line" leaves the user staring at an empty answer with
        // no explanation, so it is parsed for an error here (FR-014).
        if (!trimmed.startsWith(DATA_PREFIX)) {
            const bare = this.#parseErrorMessage(trimmed);

            if (bare) {
                subscriber.next({ type: 'error', message: bare });
            }

            return false;
        }

        const payload = trimmed.slice(DATA_PREFIX.length).trim();

        if (payload === DONE) {
            return true;
        }

        let parsed: DotAiCompletionFrame;

        try {
            parsed = JSON.parse(payload);
        } catch {
            // Not a whole object yet. The caller keeps the fragment for the next chunk, so
            // dropping it here is correct rather than lossy.
            return false;
        }

        const message = parsed?.error ?? parsed?.message;

        if (message) {
            subscriber.next({
                type: 'error',
                message: typeof message === 'string' ? message : JSON.stringify(message)
            });

            return false;
        }

        const content = parsed?.choices?.[0]?.delta?.content;

        if (content) {
            subscriber.next({ type: 'delta', content });
        }

        return false;
    }

    /** Reads an error message out of a bare (unframed) JSON line, if it is one. */
    #parseErrorMessage(line: string): string | null {
        if (!line.startsWith('{')) {
            return null;
        }

        try {
            const parsed = JSON.parse(line) as DotAiCompletionFrame;
            const message = parsed?.error ?? parsed?.message;

            if (!message) {
                return null;
            }

            return typeof message === 'string' ? message : JSON.stringify(message);
        } catch {
            return null;
        }
    }
}
