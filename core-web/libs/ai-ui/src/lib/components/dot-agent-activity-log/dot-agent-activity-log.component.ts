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
 * It renders one settled bubble per message ({@link DotAgentMessageComponent}) and,
 * while the agent is running, appends ONE live "working" bubble at the end
 * (spinner + primary tint). That live bubble is a separate item from the settled
 * steps — its text is driven by {@link workingMessage} (which a consumer updates
 * from the agent's current step + keep-alive heartbeat), so a long, quiet step
 * shows reassuring, ticking copy instead of looking hung. There is no separate
 * "now doing" banner. The log auto-scrolls to the latest entry as it grows.
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
    /** The settled bubbles — completed steps and/or the expanded terminal result. */
    readonly messages = input<AgentMessage[]>([]);

    /**
     * The live "working" bubble appended at the end while {@link working} is true.
     * The consumer updates its text from the agent's current step + heartbeat, so
     * it reads as "still working…" and ticks even when no new step has landed. When
     * null (working but nothing to say yet) a fallback bubble is synthesized from
     * {@link workingFallbackKey}.
     */
    readonly workingMessage = input<AgentMessage | null>(null);

    /** Whether the agent is actively running (appends the live working bubble). */
    readonly working = input<boolean>(false);

    /**
     * i18n key for the fallback working-bubble text when the agent is working but
     * has nothing specific to show yet. Consumers override it with their own key.
     */
    readonly workingFallbackKey = input<string>('agent.activity.working');

    private readonly dm = inject(DotMessageService);
    private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

    /**
     * The bubbles actually rendered: the settled {@link messages}, plus — while the
     * agent is working — one live working bubble appended at the end. The live
     * bubble is always the last item, so it reads as "happening now" beneath the
     * finished steps.
     */
    readonly displayMessages = computed<AgentMessage[]>(() => {
        const messages = this.messages();
        if (!this.working()) {
            return messages;
        }
        const live: AgentMessage = this.workingMessage() ?? {
            id: 'agent-working',
            icon: 'pi pi-spin pi-spinner',
            text: this.dm.get(this.workingFallbackKey()),
            tone: 'info'
        };

        return [...messages, { ...live, id: 'agent-working' }];
    });

    /**
     * Index of the live/working bubble — the last one while working (it's the
     * appended item), otherwise none (-1).
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
