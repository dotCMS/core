import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { AgentMessage, AgentMessageTone } from '../../models/agent-message';

/** Tailwind chip classes (icon background + foreground) per message tone. */
const TONE_CLASS: Record<AgentMessageTone, string> = {
    info: 'bg-primary-50 text-primary',
    success: 'bg-green-50 text-green-600',
    warning: 'bg-orange-50 text-orange-700',
    danger: 'bg-red-50 text-red-600'
};

/**
 * One SETTLED agent activity bubble: a tone-tinted icon chip, an optional
 * connector line down to the next bubble, and the message text + optional
 * sub-line. Pure presentation — the {@link AgentMessage} view-model carries
 * everything it needs. This renders finished steps only; the live "in-progress"
 * state is a separate primitive ({@link DotAgentThinkingComponent}). Render it
 * standalone or inside a list (see {@link DotAgentActivityLogComponent}).
 */
@Component({
    selector: 'dot-agent-message',
    templateUrl: './dot-agent-message.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    // The bubble fades + slides in as it's appended to the stream (motion-safe
    // only). The `agent-enter` animation is registered in the app's Tailwind theme.
    host: {
        class: 'relative flex gap-3 py-1.5 motion-safe:animate-agent-enter',
        'data-testid': 'agent-message'
    }
})
export class DotAgentMessageComponent {
    /** The bubble to render. */
    readonly message = input.required<AgentMessage>();

    /**
     * Whether this is the last bubble in a sequence — hides the trailing
     * connector line. Defaults to true (standalone bubbles have no connector).
     */
    readonly last = input<boolean>(true);

    /** Tailwind chip classes for the message's tone. */
    readonly toneClass = computed<string>(() => TONE_CLASS[this.message().tone]);
}
