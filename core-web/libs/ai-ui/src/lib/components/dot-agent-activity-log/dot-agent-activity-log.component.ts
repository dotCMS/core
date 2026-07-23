import {
    ChangeDetectionStrategy,
    Component,
    computed,
    ElementRef,
    effect,
    inject,
    input
} from '@angular/core';

import { DotMessageService } from '@dotcms/data-access';

import { AgentMessage } from '../../models/agent-message';
import { DotAgentMessageComponent } from '../dot-agent-message/dot-agent-message.component';

/**
 * The shared "watch the agent work" surface — a thin composer.
 *
 * It renders one bubble per message ({@link DotAgentMessageComponent}) and, while
 * the agent is running, marks the LAST bubble as the live/in-progress step
 * (spinner + primary tint). There is no separate "now doing" banner: the live cue
 * rides on the real list item, so the current step never appears twice. The log
 * auto-scrolls to the latest entry as the stream grows.
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
    imports: [DotAgentMessageComponent],
    templateUrl: './dot-agent-activity-log.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'block' }
})
export class DotAgentActivityLogComponent {
    /** The bubbles to render — live steps and/or the expanded terminal result. */
    readonly messages = input<AgentMessage[]>([]);

    /**
     * The step the agent is currently working on. When the run has produced steps
     * this is normally the last of {@link messages}; it's used only as the live
     * fallback bubble when the agent is working but hasn't reported a step yet.
     * Null → no explicit active step.
     */
    readonly activeMessage = input<AgentMessage | null>(null);

    /** Whether the agent is actively running (marks the last bubble as live). */
    readonly working = input<boolean>(false);

    /**
     * i18n key for the fallback bubble text when the agent is working but hasn't
     * reported a step yet. Consumers override it with their own key.
     */
    readonly workingFallbackKey = input<string>('agent.activity.working');

    private readonly dm = inject(DotMessageService);
    private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

    /**
     * The bubbles actually rendered. Normally just {@link messages}, but when the
     * agent is working with nothing streamed yet we synthesize a single live
     * fallback bubble so the user sees the agent has started.
     */
    readonly displayMessages = computed<AgentMessage[]>(() => {
        const messages = this.messages();
        if (this.working() && messages.length === 0) {
            return [
                this.activeMessage() ?? {
                    id: 'agent-working',
                    icon: 'pi pi-spin pi-spinner',
                    text: this.dm.get(this.workingFallbackKey()),
                    tone: 'info'
                }
            ];
        }
        return messages;
    });

    /**
     * Index of the bubble to render as the live/in-progress step — the last one
     * while the agent is working, otherwise none (-1).
     */
    readonly activeIndex = computed<number>(() =>
        this.working() ? this.displayMessages().length - 1 : -1
    );

    constructor() {
        // Keep the latest entry in view as the agent streams its activity, by
        // pinning whichever element actually scrolls — the host if the consumer
        // made it a scroll box, otherwise its nearest scrollable ancestor.
        effect(() => {
            const count = this.displayMessages().length;
            if (!count) {
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
