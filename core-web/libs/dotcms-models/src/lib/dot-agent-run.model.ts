/**
 * Framework-agnostic contract for a streaming "AI agent run".
 *
 * A dotCMS agent studio (Accessibility, SEO, broken-links, …) drives an agent
 * that streams its progress over Server-Sent Events and finishes with a
 * domain-specific result. These types describe the *generic wire envelope* every
 * agent shares — the live steps, the terminal result, the run status. Each agent
 * parameterizes {@link AgentStreamEvent} over its own result payload.
 *
 * The *render* view-model (how a message looks in the UI) is a separate concern
 * and lives in `@dotcms/ai-ui` (`AgentMessage`), not here.
 */

/**
 * One live progress entry streamed by an agent (an SSE `step` event).
 * `message` is the human-readable line; `meta` carries any agent-specific
 * fields (e.g. a phase tag) that a presenter reads to pick an icon/tone.
 */
export interface AgentRunStep {
    message: string;
    meta?: Record<string, unknown>;
}

/**
 * The parsed stream of events an agent emits, generic over the terminal
 * result payload `TResult`. Discriminated by `type`:
 *   - `step`    — a live progress entry (many, non-terminal)
 *   - `done`    — the run completed; carries the full result
 *   - `aborted` — the user stopped the run early; carries the PARTIAL result
 *   - `error`   — the run failed; carries a message
 */
export type AgentStreamEvent<TResult> =
    | { type: 'step'; step: AgentRunStep }
    | { type: 'done'; result: TResult }
    | { type: 'aborted'; result: TResult }
    | { type: 'error'; message: string };

/**
 * Coarse lifecycle of an agent run, independent of any agent's own workflow
 * (which each agent models separately).
 */
export const AGENT_RUN_STATUS = {
    RUNNING: 'running',
    DONE: 'done',
    ERROR: 'error'
} as const;

export type AgentRunStatus = (typeof AGENT_RUN_STATUS)[keyof typeof AGENT_RUN_STATUS];
