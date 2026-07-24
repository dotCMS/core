import {
    ChangeDetectionStrategy,
    Component,
    ElementRef,
    effect,
    inject,
    input
} from '@angular/core';

import { AgentMessage } from '../../models/agent-message';
import { DotAgentMessageComponent } from '../dot-agent-message/dot-agent-message.component';
import { DotAgentNowDoingComponent } from '../dot-agent-now-doing/dot-agent-now-doing.component';

/**
 * The shared "watch the agent work" surface — a thin composer.
 *
 * It renders the pulsing "now doing" banner ({@link DotAgentNowDoingComponent})
 * and one bubble per message ({@link DotAgentMessageComponent}), and auto-scrolls
 * to the latest entry as the stream grows.
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
    imports: [DotAgentMessageComponent, DotAgentNowDoingComponent],
    templateUrl: './dot-agent-activity-log.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'block' }
})
export class DotAgentActivityLogComponent {
    /** The bubbles to render — live steps and/or the expanded terminal result. */
    readonly messages = input<AgentMessage[]>([]);

    /**
     * The message the agent is currently working on — drives the pulsing "now
     * doing" banner. Null hides the banner. Shown only while `working` is true.
     */
    readonly activeMessage = input<AgentMessage | null>(null);

    /** Whether the agent is actively running (shows the "now doing" banner + pulse). */
    readonly working = input<boolean>(false);

    /**
     * i18n key for the fallback banner text when the agent is working but hasn't
     * reported a step yet. Consumers override it with their own key.
     */
    readonly workingFallbackKey = input<string>('agent.activity.working');

    private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

    constructor() {
        // Keep the latest entry in view as the agent streams its activity, by
        // pinning whichever element actually scrolls — the host if the consumer
        // made it a scroll box, otherwise its nearest scrollable ancestor.
        effect(() => {
            const count = this.messages().length;
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
