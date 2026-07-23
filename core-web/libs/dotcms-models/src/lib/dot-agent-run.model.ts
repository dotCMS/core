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
 * One live progress entry streamed by an agent (an SSE `phase` event, or the
 * legacy `step`). `message` is the human-readable line; `meta` carries any
 * agent-specific fields (e.g. a `phase` tag) that a presenter reads to pick an
 * icon/tone.
 */
export interface AgentRunStep {
    message: string;
    meta?: Record<string, unknown>;
}

/**
 * A running violation/issue count streamed while an agent works (SSE `progress`).
 * `baseline` is the count at the start, `current` the live count, `cleared` how
 * many have been resolved so far — the authoritative source for a live score
 * (an agent no longer has to be inferred from step text).
 */
export interface AgentProgress {
    baseline: number;
    current: number;
    cleared: number;
}

/**
 * A file the agent has changed in the working version but not yet published
 * (SSE `workingChanged` and the terminal report's `changedFiles`).
 */
export interface AgentChangedFile {
    /** Host-qualified asset path, e.g. `//site/application/themes/x/style.css`. */
    path: string;
    /** dotCMS content identifier of the changed asset. */
    identifier: string;
}

/**
 * A keep-alive tick streamed while the agent is thinking between actions (SSE
 * `heartbeat`). Some steps (a model call, a long read) take many seconds with no
 * `phase` change; the heartbeat lets the UI show elapsed time and reassure the
 * user the run hasn't hung.
 *
 * `elapsedMs` is the total run time so far; `sinceLastEventMs` is how long since
 * the last non-heartbeat event (i.e. how long the current action has been running).
 */
export interface AgentHeartbeat {
    elapsedMs: number;
    sinceLastEventMs: number;
}

/**
 * The parsed stream of events an agent emits, generic over the terminal
 * result payload `TResult`. Discriminated by `type`:
 *   - `run`            — the run's id, emitted on the first frame; needed to
 *                        target a subsequent stop request at this specific run
 *   - `phase`          — a live progress entry (many, non-terminal); the primary
 *                        activity signal. Carries the phase tag in `step.meta.phase`.
 *   - `progress`       — a live violation/issue count (many, non-terminal)
 *   - `workingChanged` — the set of files changed so far (many, non-terminal)
 *   - `heartbeat`      — a keep-alive tick while the agent is thinking (many,
 *                        non-terminal); carries elapsed timings, no new activity
 *   - `step`           — legacy alias of `phase`, kept for back-compat
 *   - `done`           — the run completed; carries the full result
 *   - `aborted`        — the user stopped the run early; carries the PARTIAL result
 *   - `error`          — the run failed; carries a message
 */
export type AgentStreamEvent<TResult> =
    | { type: 'run'; runId: string }
    | { type: 'phase'; step: AgentRunStep }
    | { type: 'progress'; progress: AgentProgress }
    | { type: 'workingChanged'; changedFiles: AgentChangedFile[] }
    | { type: 'heartbeat'; heartbeat: AgentHeartbeat }
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
