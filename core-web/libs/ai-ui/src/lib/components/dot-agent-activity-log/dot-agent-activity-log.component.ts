import {
    afterRenderEffect,
    ChangeDetectionStrategy,
    Component,
    computed,
    ElementRef,
    inject,
    input
} from '@angular/core';

import { DotMessageService } from '@dotcms/data-access';

import { AgentMessage } from '../../models/agent-message';
import { DotAgentMessageComponent } from '../dot-agent-message/dot-agent-message.component';
import { DotAgentThinkingComponent } from '../dot-agent-thinking/dot-agent-thinking.component';

/**
 * The shared "watch the agent work" surface — a thin composer.
 *
 * It renders one settled bubble per message ({@link DotAgentMessageComponent}) and,
 * while the agent is running, appends ONE live thinking indicator at the end
 * ({@link DotAgentThinkingComponent}). The thinking item is a distinct component —
 * not a settled bubble with a spinner bolted on — driven by {@link workingMessage}
 * (which a consumer updates from the agent's current step + keep-alive heartbeat),
 * so a long, quiet step shows reassuring, ticking copy instead of looking hung.
 * The log auto-scrolls to the latest entry as it grows.
 *
 * Layout is the consumer's: the component imposes NO sizing on its own box (no
 * height, no flex, no overflow, no margins) — it just grows with its content.
 * Where it scrolls is the consumer's call:
 *   - give the host a bounded height + `overflow-y-auto` and it scrolls itself;
 *   - or place it inside a taller scroll container (among other content) and
 *     that container scrolls.
 * Either way, auto-scroll-to-latest follows the nearest scrollable ancestor
 * (including the host), so the newest entry stays in view without the consumer
 * wiring anything.
 */
@Component({
    selector: 'dot-agent-activity-log',
    imports: [DotAgentMessageComponent, DotAgentThinkingComponent],
    templateUrl: './dot-agent-activity-log.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'block' }
})
export class DotAgentActivityLogComponent {
    /** The settled bubbles — completed steps and/or the expanded terminal result. */
    readonly messages = input<AgentMessage[]>([]);

    /**
     * Drives the live thinking indicator shown while {@link working} is true. The
     * consumer updates it from the agent's current step + heartbeat, so it reads as
     * "still working…" and ticks even when no new step has landed. When null the
     * indicator falls back to {@link workingFallbackKey}. Only `text`/`sub` are
     * used — the thinking component owns its own spinner + styling.
     */
    readonly workingMessage = input<AgentMessage | null>(null);

    /** Whether the agent is actively running (shows the live thinking indicator). */
    readonly working = input<boolean>(false);

    /**
     * i18n key for the fallback thinking text when the agent is working but has
     * nothing specific to show yet. Consumers override it with their own key.
     */
    readonly workingFallbackKey = input<string>('agent.activity.working');

    private readonly dm = inject(DotMessageService);
    private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

    /** Primary line for the thinking indicator (working message, or the fallback). */
    readonly workingText = computed<string>(
        () => this.workingMessage()?.text ?? this.dm.get(this.workingFallbackKey())
    );

    /** Optional secondary line for the thinking indicator (e.g. elapsed seconds). */
    readonly workingSub = computed<string | undefined>(() => this.workingMessage()?.sub);

    constructor() {
        // Keep the latest entry — the live thinking indicator — in view as the
        // agent streams its activity. Uses afterRenderEffect (not a plain effect)
        // so it runs AFTER the newly-appended bubble is laid out; a plain effect
        // reads scrollHeight before the new DOM exists and stops one row short.
        // It pins whichever element actually scrolls — the host if the consumer
        // made it a scroll box, otherwise its nearest scrollable ancestor.
        afterRenderEffect(() => {
            const count = this.messages().length;
            const working = this.working();
            // Also track the working text so a heartbeat-driven reflow re-pins.
            this.workingText();
            if (!count && !working) {
                return;
            }
            const scroller = this.scrollParent(this.host.nativeElement);
            if (scroller) {
                scroller.scrollTop = scroller.scrollHeight;
            }
        });
    }

    /**
     * Walk up from the host to the nearest ancestor that scrolls vertically
     * (overflow auto/scroll and actually overflowing), or the host itself if it
     * scrolls. Returns null when nothing scrolls (the log grows freely).
     */
    private scrollParent(from: HTMLElement): HTMLElement | null {
        let el: HTMLElement | null = from;
        while (el) {
            const overflowY = getComputedStyle(el).overflowY;
            const scrolls =
                (overflowY === 'auto' || overflowY === 'scroll') &&
                el.scrollHeight > el.clientHeight;
            if (scrolls) {
                return el;
            }
            el = el.parentElement;
        }
        return null;
    }
}
