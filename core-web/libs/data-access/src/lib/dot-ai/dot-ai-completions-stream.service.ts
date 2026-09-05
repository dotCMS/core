import { Observable } from 'rxjs';

import { Injectable } from '@angular/core';

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
        try {
            const response = await fetch(`${AI_API_ENDPOINT}/completions`, {
                method: 'POST',
                credentials: 'same-origin',
                headers: { 'Content-Type': 'application/json', Accept: 'text/event-stream' },
                body: JSON.stringify({ ...form, stream: true }),
                signal: controller.signal
            });

            if (!response.ok || !response.body) {
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
                    if (this.#emit(line, subscriber)) {
                        subscriber.complete();

                        return;
                    }
                }
            }

            // Flush a trailing frame that arrived without a closing newline.
            if (buffer.trim()) {
                this.#emit(buffer, subscriber);
            }

            subscriber.complete();
        } catch (error) {
            if (!controller.signal.aborted) {
                subscriber.error(error);
            }
        }
    }

    /** Returns true when the stream is finished. */
    #emit(line: string, subscriber: { next: (event: DotAiStreamEvent) => void }): boolean {
        const trimmed = line.trim();

        // Blank lines separate frames; ':' lines are keep-alive comments.
        if (!trimmed || trimmed.startsWith(':') || !trimmed.startsWith(DATA_PREFIX)) {
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
}
