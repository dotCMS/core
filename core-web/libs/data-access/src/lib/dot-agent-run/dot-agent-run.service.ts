import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { AgentRunStep, AgentStreamEvent } from '@dotcms/dotcms-models';

/**
 * Generic transport for a streaming "AI agent run".
 *
 * dotCMS agents (Accessibility, SEO, broken-links, …) run a loop server-side and
 * stream their progress over Server-Sent Events:
 *   event: run            → { runId }                       (first frame)
 *   event: phase          → { phase, message }              (live, many)
 *   event: progress       → { baseline, current, cleared }  (live, many)
 *   event: workingChanged → { changedFiles:[{path,identifier}] } (live, many)
 *   event: heartbeat      → { elapsedMs, sinceLastEventMs }  (live keep-alive, many)
 *   event: done           → { ...result }                   (terminal — the agent's result)
 *   event: aborted        → { ...result }                   (terminal — partial result after stop)
 *   event: error          → { message }                     (terminal)
 *   event: step           → legacy alias of `phase` (still parsed)
 *
 * Angular's HttpClient can't read a streaming response incrementally, so this
 * uses the fetch() ReadableStream and hand-parses SSE frames, surfacing each
 * event through an Observable of the generic {@link AgentStreamEvent} union.
 *
 * This service is agent-agnostic: the caller supplies the endpoint URL and the
 * result type parameter, and interprets the terminal `result` payload. Calls go
 * same-origin to a dotCMS proxy resource that authenticates the session and
 * streams the agent response back — the browser never holds a token.
 *
 * NOT provided at the root: add it to the `providers` of the agent route/component
 * that runs a stream, so it lives and dies with that screen instead of being
 * retained app-wide by every consumer of `@dotcms/data-access`.
 */
@Injectable()
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

                        // SSE allows CR LF line endings, and a `\r\n\r\n` delimiter would
                        // never match the search below — the whole body would buffer to
                        // the end and reach #parseFrame as one blob that fails to parse,
                        // leaving a finished run looking hung. Normalising the WHOLE
                        // buffer (not just this chunk) also catches a CR LF pair split
                        // across two reads. Raw newlines are invalid inside JSON strings,
                        // so this can never touch payload content.
                        buffer = buffer.replace(/\r\n/g, '\n');

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

                    // Flush a trailing frame that never got its blank line. A server that
                    // ends the body right after writing the terminal event leaves it here,
                    // and dropping it turns a finished run into one that looks like it
                    // hung — the consumer waits forever for a `done` that was sent.
                    const tail = buffer.trim();
                    if (tail) {
                        const parsed = this.#parseFrame<TResult>(tail);
                        if (parsed) {
                            subscriber.next(parsed);
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

        const data = dataLines.join('\n');
        let payload: unknown;
        try {
            payload = JSON.parse(data);
        } catch {
            // Dropping the frame is right — one bad frame from a flaky backend must not
            // take down the whole stream — but do it loudly. Silently discarding a frame
            // that happened to be the terminal `done` leaves the run looking hung with
            // nothing anywhere to explain why.
            console.warn(
                `[DotAgentRunService] dropped an unparseable "${event}" frame:`,
                data.length > 500 ? `${data.slice(0, 500)}…` : data
            );

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

        // The agent's first frame announces the run id (`{ "runId": "..." }`),
        // often with no `event:` name and no `message`. Surface it as a `run`
        // event so the caller can target a later stop at this specific run. Checked
        // ahead of the switch so it works regardless of the (possibly absent)
        // event name — but only for non-terminal, non-step frames, so a step (whose
        // meta may carry a runId) or a bare-report done/aborted isn't misread.
        const KNOWN =
            event === 'phase' ||
            event === 'progress' ||
            event === 'workingChanged' ||
            event === 'heartbeat' ||
            event === 'step' ||
            event === 'done' ||
            event === 'aborted' ||
            event === 'error';
        if (!KNOWN && typeof data['runId'] === 'string' && !('message' in data)) {
            return { type: 'run', runId: data['runId'] as string };
        }

        switch (event) {
            // `phase` (and its legacy alias `step`) → a live progress entry. Split
            // off `message`; keep the rest (e.g. the `phase` tag) as `meta` so a
            // presenter can read domain fields.
            case 'phase':
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

                // Emit under the frame's own name so callers can distinguish the
                // modern `phase` stream from the legacy `step` stream if needed.
                return event === 'phase' ? { type: 'phase', step } : { type: 'step', step };
            }
            case 'progress': {
                const num = (key: string): number =>
                    typeof data[key] === 'number' ? (data[key] as number) : 0;

                return {
                    type: 'progress',
                    progress: {
                        baseline: num('baseline'),
                        current: num('current'),
                        cleared: num('cleared')
                    }
                };
            }
            case 'workingChanged': {
                const raw = Array.isArray(data['changedFiles']) ? data['changedFiles'] : [];
                const changedFiles = raw
                    .map((f) => f as Record<string, unknown>)
                    .filter(
                        (f) => typeof f['path'] === 'string' && typeof f['identifier'] === 'string'
                    )
                    .map((f) => ({
                        path: f['path'] as string,
                        identifier: f['identifier'] as string
                    }));

                return { type: 'workingChanged', changedFiles };
            }
            case 'heartbeat': {
                const num = (key: string): number =>
                    typeof data[key] === 'number' ? (data[key] as number) : 0;

                return {
                    type: 'heartbeat',
                    heartbeat: {
                        elapsedMs: num('elapsedMs'),
                        sinceLastEventMs: num('sinceLastEventMs')
                    }
                };
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
