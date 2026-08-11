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
 * How close to the bottom still counts as "following along", in px. Absorbs fractional
 * layout heights and a partially-rendered in-flight row, either of which would otherwise
 * read as the user having scrolled away.
 */
const PINNED_TO_BOTTOM_TOLERANCE_PX = 32;

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
    host: { class: 'flex flex-col', 'data-testid': 'agent-activity-steps' }
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
        //
        // Auto-scroll ONLY while the user is already at the bottom. The scroller is the
        // nearest scrollable ancestor, which in a consumer like the Accessibility Studio
        // is the whole side pane — score ring, legend and issue list included — so an
        // unconditional pin dragged the user back down every time it ran. Scrolling up to
        // read something mid-run is an explicit "leave me here", and this now honours it
        // until they scroll back to the bottom themselves.
        //
        // NOTE: `workingText` is deliberately NOT tracked. It changes on every heartbeat
        // (a few seconds apart) with no new content, so it was the reason a run yanked the
        // pane back roughly every 5 seconds for its entire duration.
        afterRenderEffect(() => {
            const count = this.messages().length;
            const working = this.working();
            if (!count && !working) {
                return;
            }
            const scroller = this.scrollParent(this.host.nativeElement);
            // Measured BEFORE the write, or the comparison is against the value we are
            // about to set and every check trivially passes.
            if (scroller && this.isPinnedToBottom(scroller)) {
                scroller.scrollTop = scroller.scrollHeight;
            }
        });
    }

    /**
     * Whether the scroller is at (or within a hair of) the bottom.
     *
     * The tolerance absorbs fractional layout heights and the partially-rendered row that
     * is normally in flight while content streams in — without it, sub-pixel rounding
     * alone would read as "the user scrolled away" and auto-scroll would stop for good.
     */
    private isPinnedToBottom(scroller: HTMLElement): boolean {
        const distanceFromBottom =
            scroller.scrollHeight - scroller.scrollTop - scroller.clientHeight;

        return distanceFromBottom <= PINNED_TO_BOTTOM_TOLERANCE_PX;
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
