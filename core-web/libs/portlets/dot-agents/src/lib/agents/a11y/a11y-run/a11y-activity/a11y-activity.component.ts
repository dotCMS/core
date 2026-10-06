import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';

import { AgentMessage, DotAgentActivityLogComponent } from '@dotcms/ai-ui';
import { DotMessageService } from '@dotcms/data-access';
import { AgentHeartbeat, AgentRunStep } from '@dotcms/dotcms-models';
import { A11yGroup } from '@dotcms/portlets/dot-ema/ui';
import { DotMessagePipe } from '@dotcms/ui';

import { A11yAgentPresenter } from '../../models/a11y-agent.presenter';
import { impactToSeverity, SEVERITY_COLOR } from '../../models/a11y-severity';
import { FixReport, StudioPhase } from '../../models/accessibility-studio.models';

/**
 * The scanner panel's scrollable body: a phase-aware section header over whichever
 * of the four bodies fits the moment —
 *   ready    → what to expect
 *   scanning → the issue-list skeleton
 *   scanned  → BY ISSUE TYPE, one row per axe rule
 *   fixing / done → the live agent activity log, then the final report
 * with the needs-review rows (axe `incomplete`) below whenever there are any.
 */
@Component({
    selector: 'dot-a11y-activity',
    imports: [DotAgentActivityLogComponent, DotMessagePipe],
    templateUrl: './a11y-activity.component.html',
    styleUrl: '../studio-fade-in.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
    // `contents`: the header and the log are separate rows of the panel's flex
    // column, so this component must not introduce a box between them.
    host: { class: 'contents' }
})
export class DotA11yActivityComponent {
    /** Drives which body renders, plus the header label and working badge. */
    readonly phase = input.required<StudioPhase>();

    /** True in the done/published phases — the report replaces the live stream. */
    readonly finished = input(false);

    /** The streamed SSE steps, rendered as settled bubbles while fixing. */
    readonly steps = input<AgentRunStep[]>([]);

    /** The final FixReport, expanded into bubbles once the run finished. */
    readonly report = input<FixReport | null>(null);

    /** Last agent heartbeat — drives the "still working" copy and its elapsed sub-line. */
    readonly heartbeat = input<AgentHeartbeat | null>(null);

    /** One group per failing axe rule, for the BY ISSUE TYPE list. */
    readonly issueTypeGroups = input<A11yGroup[]>([]);

    /** axe `incomplete` groups — flagged but unconfirmed, so a human decides. */
    readonly reviewGroups = input<A11yGroup[]>([]);

    readonly #dm = inject(DotMessageService);

    /** Maps the agent stream + FixReport into shared activity-log bubbles. */
    readonly #presenter = new A11yAgentPresenter(this.#dm);

    /**
     * The section-header label above the scrollable body, by phase:
     *   scanning → "SCAN", scanned → "BY ISSUE TYPE", fixing/done → "AGENT ACTIVITY".
     */
    protected readonly $logHeaderKey = computed<string>(() => {
        if (this.phase() === 'scanning') {
            return 'accessibility.studio.loghdr.scan';
        }
        if (this.phase() === 'scanned') {
            return 'accessibility.studio.loghdr.issues';
        }
        return 'accessibility.studio.loghdr.activity';
    });

    /** The working badge label beside the header ("SCANNING" / "WORKING"), or null. */
    protected readonly $logBadgeKey = computed<string | null>(() => {
        if (this.phase() === 'scanning') {
            return 'accessibility.studio.badge.scanning';
        }
        if (this.phase() === 'fixing') {
            return 'accessibility.studio.badge.working';
        }
        return null;
    });

    /**
     * BY ISSUE TYPE rows with their dot color resolved. Projected here rather than
     * calling a method from the template: this component's change detection is driven
     * by a live SSE stream, so a template method would re-run for every row many
     * times a second.
     */
    protected readonly $issueTypeRows = computed(() =>
        this.issueTypeGroups().map((group) => ({
            ...group,
            color: SEVERITY_COLOR[impactToSeverity(group.impact)]
        }))
    );

    /** Needs-review rows with their "why a human is needed" i18n key resolved. */
    protected readonly $reviewRows = computed(() =>
        this.reviewGroups().map((group) => ({
            ...group,
            reasonKey:
                REVIEW_REASON_KEYS[group.code] ?? 'accessibility.studio.review.reason.default'
        }))
    );

    /**
     * The SETTLED bubbles for the shared activity log, via the a11y presenter:
     *   - while fixing → one bubble per streamed SSE `phase` step (the completed
     *     actions); the live "now working" item is {@link workingMessage}, appended
     *     by the log itself
     *   - after done   → the final report expanded into bubbles (scan/fixed/reported/rescan)
     */
    protected readonly $activityMessages = computed<AgentMessage[]>(() => {
        if (this.phase() === 'fixing') {
            return this.steps().map((step, i) => this.#presenter.liveStep(step, i));
        }
        if (this.finished()) {
            const report = this.report();
            return report ? this.#presenter.resultMessages(report) : [];
        }
        return [];
    });

    /**
     * The live "thinking" copy shown while fixing. It is ALWAYS generic
     * loading/working/thinking text — never the last step's message — so the
     * indicator reads clearly as "the agent is busy" and doesn't get mistaken for a
     * finished step. The phrases cycle (and loop) as the run progresses so the line
     * keeps visibly changing; elapsed seconds on the current action ride along as
     * the sub-line.
     */
    protected readonly $workingMessage = computed<AgentMessage | null>(() => {
        if (this.phase() !== 'fixing') {
            return null;
        }
        const sinceLastEventMs = this.heartbeat()?.sinceLastEventMs ?? 0;

        // Elapsed on the current action, once it's been running a beat.
        const sinceSec = Math.floor(sinceLastEventMs / 1000);
        const sub =
            sinceSec >= 3
                ? this.#dm.get('accessibility.studio.working.elapsed', String(sinceSec))
                : undefined;

        return {
            id: 'agent-working',
            // Unused: dot-agent-thinking renders its own spinner and reads only
            // text/sub. Kept non-empty only to satisfy AgentMessage.
            icon: '',
            text: this.#dm.get(this.#workingReassuranceKey(sinceLastEventMs)),
            sub,
            tone: 'info'
        };
    });

    /**
     * Pick a generic reassurance line. Cycles through the phrases as the current
     * action runs so the copy keeps visibly changing — and LOOPS, since a step can
     * run for minutes and no phrase should imply it's nearly done or freeze on one
     * message.
     */
    #workingReassuranceKey(sinceLastEventMs: number): string {
        const KEYS = [
            'accessibility.studio.working.thinking',
            'accessibility.studio.working.analyzing',
            'accessibility.studio.working.reasoning',
            'accessibility.studio.working.stillworking'
        ];
        // Advance one phrase roughly every 5s, wrapping around forever.
        const index = Math.floor(sinceLastEventMs / 5000) % KEYS.length;

        return KEYS[index];
    }
}

/** Per-rule "why it needs review" i18n keys for the common axe incomplete rules. */
const REVIEW_REASON_KEYS: Record<string, string> = {
    'color-contrast': 'accessibility.studio.review.reason.colorcontrast',
    'color-contrast-enhanced': 'accessibility.studio.review.reason.colorcontrast',
    'link-in-text-block': 'accessibility.studio.review.reason.linkintext',
    'scrollable-region-focusable': 'accessibility.studio.review.reason.scrollable',
    'aria-allowed-attr': 'accessibility.studio.review.reason.aria',
    'nested-interactive': 'accessibility.studio.review.reason.nested'
};
