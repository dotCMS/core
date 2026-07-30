import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/**
 * The live "thinking / working" indicator for the agent activity log.
 *
 * A distinct primitive from {@link DotAgentMessageComponent}: a settled message is
 * a finished step, whereas this is the agent's *current, in-progress* state — a
 * spinner beside generic "working" copy with an AI-chatbot-style gradient-text
 * shimmer sweeping through it (plus an optional elapsed sub-line). It deliberately
 * does NOT look like a settled step bubble, and its text is always generic loading
 * copy — never a step message.
 *
 * Render it once, at the bottom of the log, while a run is in flight (see
 * {@link DotAgentActivityLogComponent}). Styling is Tailwind-only.
 */
@Component({
    selector: 'dot-agent-thinking',
    templateUrl: './dot-agent-thinking.component.html',
    // Plain CSS for the gradient-text shimmer: background-clip:text is fiddly and
    // Tailwind utilities can't express the moving, REPEATING gradient cleanly. A
    // repeating gradient is what keeps the sweep from clipping the leading glyphs —
    // there's always paint under every character regardless of the sweep offset.
    styles: [
        `
            .agent-shimmer {
                background: repeating-linear-gradient(
                    100deg,
                    var(--p-gray-500, #6b7280) 0%,
                    var(--p-gray-500, #6b7280) 40%,
                    var(--p-gray-900, #111827) 50%,
                    var(--p-gray-500, #6b7280) 60%,
                    var(--p-gray-500, #6b7280) 100%
                );
                background-size: 200% 100%;
                background-clip: text;
                -webkit-background-clip: text;
                color: transparent;
                -webkit-text-fill-color: transparent;
                animation: agent-shimmer-sweep 2s linear infinite;
            }

            @keyframes agent-shimmer-sweep {
                from {
                    background-position: 200% 0;
                }
                to {
                    background-position: 0 0;
                }
            }

            @media (prefers-reduced-motion: reduce) {
                .agent-shimmer {
                    background: none;
                    color: var(--p-gray-500, #6b7280);
                    -webkit-text-fill-color: currentColor;
                    animation: none;
                }
            }
        `
    ],
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        class: 'flex items-center gap-2.5 py-2 motion-safe:animate-agent-enter',
        'data-testid': 'agent-thinking',
        // Announce the busy state to assistive tech (the spinner is decorative).
        role: 'status',
        'aria-live': 'polite'
    }
})
export class DotAgentThinkingComponent {
    /** The primary line — generic loading/working/thinking copy (never a step). */
    readonly text = input.required<string>();

    /** Optional secondary line, e.g. elapsed seconds on the current action. */
    readonly sub = input<string | undefined>(undefined);
}
