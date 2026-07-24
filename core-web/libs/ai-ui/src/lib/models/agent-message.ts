/**
 * The render view-model for the shared agent activity log. This is a UI concern
 * — not part of the agent wire contract (see `@dotcms/dotcms-models` for that).
 * An agent's presenter turns its live steps and terminal result into a list of
 * these; the shared components ({@link DotAgentMessageComponent} et al.) draw them.
 */

/**
 * Visual tone of a rendered agent message — drives the bubble/icon color.
 * Agent-neutral: a presenter maps its own outcomes (e.g. "fixed" → `success`,
 * "reported" → `warning`) onto these.
 */
export const AGENT_MESSAGE_TONE = {
    INFO: 'info',
    SUCCESS: 'success',
    WARNING: 'warning',
    DANGER: 'danger'
} as const;

export type AgentMessageTone = (typeof AGENT_MESSAGE_TONE)[keyof typeof AGENT_MESSAGE_TONE];

/**
 * A single renderable line in the shared agent activity log. Produced by a
 * presenter ("result-as-more-messages"), consumed by the message component.
 *
 * @property id   Stable id for `@for` tracking + entry animation.
 * @property icon PrimeNG icon class (e.g. `pi pi-check`) — a render detail the
 *   presenter chooses; the agent response carries no icon.
 * @property text Primary line.
 * @property sub  Optional secondary line (e.g. a rule id + file).
 * @property tone Bubble/icon color — see {@link AgentMessageTone}.
 */
export interface AgentMessage {
    id: string | number;
    icon: string;
    text: string;
    sub?: string;
    tone: AgentMessageTone;
}
