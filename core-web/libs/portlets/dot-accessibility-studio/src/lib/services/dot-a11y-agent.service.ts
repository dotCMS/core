import { Observable } from 'rxjs';

import { Injectable } from '@angular/core';

import {
    AgentFixRequest,
    AgentStreamEvent,
    FixReport,
    StudioStepPhase
} from '../models/accessibility-studio.models';

/**
 * Talks to the ai-agents a11y-fix agent.
 *
 * The agent runs the fix loop and streams its progress over Server-Sent Events:
 *   event: step  → { phase, message }   (live, many)
 *   event: done  → { report }           (terminal, the §6 FixReport)
 *   event: error → { message }          (terminal)
 *
 * Angular's HttpClient can't read a streaming response incrementally, so this
 * uses the fetch() ReadableStream and hand-parses SSE frames, surfacing each
 * event through an Observable. In dev the request is same-origin via the dev
 * proxy (`/ai-agents/*` → :3001), which also injects the bearer token; in
 * production the dotCMS proxy does the same. The browser never holds a token.
 */

/** Same-origin base; the dev/prod proxy strips this and forwards to the agent. */
const AGENT_BASE = '/ai-agents/a11y';

const STEP_PHASES: ReadonlyArray<StudioStepPhase> = ['scan', 'locate', 'read', 'fix', 'rescan'];

function isStepPhase(value: unknown): value is StudioStepPhase {
    return typeof value === 'string' && STEP_PHASES.includes(value as StudioStepPhase);
}

@Injectable()
export class DotA11yAgentService {
    /**
     * Run the fix loop, streaming each agent step. The observable emits one
     * AgentStreamEvent per SSE event and completes after `done`/`error` (or when
     * the caller unsubscribes, which aborts the in-flight request).
     */
    fixStream(request: AgentFixRequest): Observable<AgentStreamEvent> {
        return new Observable<AgentStreamEvent>((subscriber) => {
            const controller = new AbortController();

            (async () => {
                let response: Response;
                try {
                    response = await fetch(`${AGENT_BASE}/fix/stream`, {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json', Accept: 'text/event-stream' },
                        body: JSON.stringify(request),
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
                        new Error(`Agent request failed (${response.status} ${response.statusText})`)
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
                    for (;;) {
                        const { value, done } = await reader.read();
                        if (done) {
                            break;
                        }
                        buffer += decoder.decode(value, { stream: true });

                        let delimiter = buffer.indexOf('\n\n');
                        while (delimiter !== -1) {
                            const frame = buffer.slice(0, delimiter);
                            buffer = buffer.slice(delimiter + 2);
                            const parsed = this.parseFrame(frame);
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
    private parseFrame(frame: string): AgentStreamEvent | null {
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

        return this.toEvent(event, payload);
    }

    /** Map a raw SSE (event, payload) to the discriminated AgentStreamEvent. */
    private toEvent(event: string, payload: unknown): AgentStreamEvent | null {
        const data = (payload ?? {}) as Record<string, unknown>;
        switch (event) {
            case 'step': {
                const phase = isStepPhase(data['phase']) ? data['phase'] : 'fix';
                const message = typeof data['message'] === 'string' ? data['message'] : '';

                return { type: 'step', phase, message };
            }
            case 'done': {
                const report = data['report'] as FixReport | undefined;
                if (!report) {
                    return { type: 'error', message: 'Agent finished without a report.' };
                }

                return { type: 'done', report };
            }
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
