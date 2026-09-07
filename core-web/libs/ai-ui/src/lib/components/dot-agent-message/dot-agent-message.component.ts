import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { DotColorIconComponent } from '@dotcms/ui';

import { AgentMessage, AgentMessageTone } from '../../models/agent-message';

/**
 * `dot-color-icon` accent per message tone. The component derives the chip's
 * background + foreground from this single color, so tones stay one token each.
 */
const TONE_COLOR: Record<AgentMessageTone, string> = {
    info: 'primary',
    success: 'green',
    warning: 'orange',
    danger: 'red'
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
    imports: [DotColorIconComponent],
    templateUrl: './dot-agent-message.component.html',
    // The entrance animation lives here, not in the app's Tailwind theme: it is
    // this library's own presentation detail, and a consuming app shouldn't have to
    // register a keyframe for the component to look right. Same treatment as the
    // shimmer in DotAgentThinkingComponent.
    styles: [
        `
            @keyframes agent-enter {
                from {
                    opacity: 0;
                    transform: translateY(6px);
                }
                to {
                    opacity: 1;
                    transform: translateY(0);
                }
            }

            @media (prefers-reduced-motion: no-preference) {
                :host {
                    animation: agent-enter 0.28s ease-out both;
                }
            }
        `
    ],
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        class: 'relative flex gap-3 py-1.5',
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

    /** `dot-color-icon` accent for the message's tone. */
    readonly toneColor = computed<string>(() => TONE_COLOR[this.message().tone]);
}
