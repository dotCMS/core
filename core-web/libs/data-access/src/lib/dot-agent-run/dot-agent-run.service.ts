import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { AgentRunStep, AgentStreamEvent } from '@dotcms/dotcms-models';

/**
 * Generic transport for a streaming "AI agent run".
 *
 * dotCMS agents (Accessibility, SEO, broken-links, …) run a loop server-side and
 * stream their progress over Server-Sent Events:
 *   event: step  → { message, ...meta }        (live, many)
 *   event: done  → { ...result }                (terminal — the agent's result)
 *   event: aborted → { ...result }              (terminal — partial result after stop)
 *   event: error → { message }                  (terminal)
 *
 * Angular's HttpClient can't read a streaming response incrementally, so this
 * uses the fetch() ReadableStream and hand-parses SSE frames, surfacing each
 * event through an Observable of the generic {@link AgentStreamEvent} union.
 *
 * This service is agent-agnostic: the caller supplies the endpoint URL and the
 * result type parameter, and interprets the terminal `result` payload. Calls go
 * same-origin to a dotCMS proxy resource that authenticates the session and
 * streams the agent response back — the browser never holds a token.
 */
@Injectable({ providedIn: 'root' })
export class DotAgentRunService {
    readonly #http = inject(HttpClient);

    /**
     * POST `body` to `url` and stop the caller's in-flight run (cooperative). The
     * agent stops at the next safe checkpoint and the open stream emits a terminal
     * `aborted` event with the partial result. Errors are the caller's to handle.
     */
    stop(url: string, body: unknown = {}): Observable<unknown> {
        return this.#http.post(url, body);
    }

    /**
     * Run the agent loop, streaming each event. The observable emits one
     * {@link AgentStreamEvent} per SSE event and completes after
     * `done`/`aborted`/`error` (or when the caller unsubscribes, which aborts the
     * in-flight request). `TResult` is the shape of the terminal `done`/`aborted`
     * payload — opaque to this service.
     */
    run<TResult>(url: string, body: unknown): Observable<AgentStreamEvent<TResult>> {
        return new Observable<AgentStreamEvent<TResult>>((subscriber) => {
            const controller = new AbortController();

            (async () => {
                let response: Response;
                try {
                    response = await fetch(url, {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json',
                            Accept: 'text/event-stream'
                        },
                        body: JSON.stringify(body),
                        signal: controller.signal
                    });
                } catch (e) {
                    if (!controller.signal.aborted) {
                        subscriber.error(e);
                    }

                    return;
                }

                if (!response.ok || !response.body) {
                    subscriber.error(
                        new Error(
                            `Agent request failed (${response.status} ${response.statusText})`
                        )
                    );

                    return;
                }

                const reader = response.body.getReader();
                const decoder = new TextDecoder();
                let buffer = '';

                try {
                    // Read the byte stream, splitting on the SSE frame delimiter
                    // (a blank line). A frame may straddle chunk boundaries, so we
                    // accumulate in `buffer` and only consume complete frames.
                    let done = false;
                    while (!done) {
                        const chunk = await reader.read();
                        done = chunk.done;
                        if (chunk.value) {
                            buffer += decoder.decode(chunk.value, { stream: true });
                        }

                        let delimiter = buffer.indexOf('\n\n');
                        while (delimiter !== -1) {
                            const frame = buffer.slice(0, delimiter);
                            buffer = buffer.slice(delimiter + 2);
                            const parsed = this.#parseFrame<TResult>(frame);
                            if (parsed) {
                                subscriber.next(parsed);
                            }
                            delimiter = buffer.indexOf('\n\n');
                        }
                    }
                    subscriber.complete();
                } catch (e) {
                    if (!controller.signal.aborted) {
                        subscriber.error(e);
                    }
                }
            })();

            return () => controller.abort();
        });
    }

    /** Parse one SSE frame (`event:` + `data:` lines) into a typed event, or null. */
    #parseFrame<TResult>(frame: string): AgentStreamEvent<TResult> | null {
        let event = 'message';
        const dataLines: string[] = [];
        for (const line of frame.split('\n')) {
            if (line.startsWith('event:')) {
                event = line.slice('event:'.length).trim();
            } else if (line.startsWith('data:')) {
                dataLines.push(line.slice('data:'.length).trim());
            }
        }
        if (!dataLines.length) {
            return null;
        }

        let payload: unknown;
        try {
            payload = JSON.parse(dataLines.join('\n'));
        } catch {
            return null;
        }

        return this.#toEvent<TResult>(event, payload);
    }

    /**
     * Map a raw SSE (event, payload) to the generic {@link AgentStreamEvent}.
     * `step` splits off `message` and keeps the rest as `meta` (so an agent's
     * presenter can read domain fields like a phase tag). `done`/`aborted` pass
     * the whole payload through as the opaque `TResult`.
     */
    #toEvent<TResult>(event: string, payload: unknown): AgentStreamEvent<TResult> | null {
        const data = (payload ?? {}) as Record<string, unknown>;
        switch (event) {
            case 'step': {
                const message = typeof data['message'] === 'string' ? data['message'] : '';
                const meta: Record<string, unknown> = {};
                for (const key of Object.keys(data)) {
                    if (key !== 'message') {
                        meta[key] = data[key];
                    }
                }
                const step: AgentRunStep = { message };
                if (Object.keys(meta).length) {
                    step.meta = meta;
                }

                return { type: 'step', step };
            }
            case 'done':
                return { type: 'done', result: payload as TResult };
            case 'aborted':
                return { type: 'aborted', result: payload as TResult };
            case 'error': {
                const message =
                    typeof data['message'] === 'string' ? data['message'] : 'Agent run failed.';

                return { type: 'error', message };
            }
            default:
                return null;
        }
    }
}
