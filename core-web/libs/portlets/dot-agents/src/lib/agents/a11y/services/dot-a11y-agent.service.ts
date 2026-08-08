import { Observable } from 'rxjs';

import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

import { DotAgentRunService } from '@dotcms/data-access';

import {
    A11yAgentStreamEvent,
    AgentFixRequest,
    FixReport
} from '../models/accessibility-studio.models';

/**
 * Talks to the ai-agents a11y-fix agent.
 *
 * A thin, a11y-specific wrapper over the generic {@link DotAgentRunService}: it
 * owns only the a11y proxy endpoints and the `FixReport` result type. The agent
 * runs the fix loop and streams its progress over Server-Sent Events (parsed by
 * the generic run service):
 *   event: step  → { phase, message }   (live, many)
 *   event: done  → { ...FixReport }      (terminal, the §6 report)
 *   event: aborted → { ...FixReport }    (terminal, partial report after stop)
 *   event: error → { message }           (terminal)
 *
 * Calls go same-origin to the dotCMS proxy resource at `/api/v1/agents/a11y/*`
 * (plan §8.1); the Java proxy authenticates the session, mints a short-lived JWT,
 * resolves the page, and streams the agent response back. The browser never holds
 * a token — the proxy is the auth boundary (plan §8.2).
 */

/** dotCMS proxy resource (plan §8.1) — the browser's same-origin entry point. */
const AGENT_BASE = '/api/v1/agents/a11y';

@Injectable()
export class DotA11yAgentService {
    readonly #runService = inject(DotAgentRunService);

    /**
     * Ask the agent to stop a specific in-flight run (cooperative). The agent
     * identifies the run by `runId` — the id it announced on the stream's first
     * frame (the `run` event). It stops at the next safe checkpoint and the open
     * SSE stream emits a terminal `aborted` event with the partial report. 202 if
     * a run was signalled, 404 if none — both are fine here, so errors are
     * swallowed by the caller.
     */
    stop(runId: string): Observable<unknown> {
        return this.#runService.stop(`${AGENT_BASE}/stop`, { runId });
    }

    /**
     * Run the fix loop, streaming each agent step. The observable emits one
     * {@link A11yAgentStreamEvent} per SSE event and completes after
     * `done`/`aborted`/`error` (or when the caller unsubscribes, which aborts the
     * in-flight request).
     *
     * The agent's terminal `done`/`aborted` payload wraps the report as
     * `{ report: FixReport }`; we unwrap it here so downstream consumers receive
     * the bare {@link FixReport} as `result` (the generic transport is agent-
     * agnostic and passes the whole payload through untouched).
     */
    fixStream(request: AgentFixRequest): Observable<A11yAgentStreamEvent> {
        return this.#runService
            .run<FixReport | { report: FixReport }>(`${AGENT_BASE}/fix/stream`, request)
            .pipe(
                map((event): A11yAgentStreamEvent => {
                    if (event.type === 'done' || event.type === 'aborted') {
                        return { ...event, result: unwrapReport(event.result) };
                    }

                    return event;
                })
            );
    }
}

/**
 * Unwrap the agent's terminal payload to a bare {@link FixReport}. The agent
 * sends `{ report: FixReport }`; older/other shapes may send the report directly,
 * so accept both.
 */
function unwrapReport(payload: FixReport | { report: FixReport }): FixReport {
    const wrapped = payload as { report?: FixReport };

    return wrapped?.report ?? (payload as FixReport);
}
