import { AgentRunStep } from '@dotcms/dotcms-models';

import { AgentMessage } from './agent-message';

/**
 * Per-agent strategy that turns an agent's stream into renderable
 * {@link AgentMessage} bubbles for the shared activity log. This is the seam
 * that keeps the shared UI agent-agnostic: the presenter is the only place that
 * knows the agent's domain shapes (its step phases, its result payload).
 *
 * @typeParam TResult The agent's terminal result payload
 *   (the `done`/`aborted` event data).
 */
export interface AgentMessagePresenter<TResult> {
    /**
     * Map one live SSE step to a bubble (icon + tone chosen from the step's
     * `meta`). `index` is the step's position in the run, useful for a stable id.
     */
    liveStep(step: AgentRunStep, index: number): AgentMessage;

    /**
     * Expand the terminal result into the bubbles that summarize it
     * ("result-as-more-messages"). Called once the run completes.
     */
    resultMessages(result: TResult): AgentMessage[];
}
