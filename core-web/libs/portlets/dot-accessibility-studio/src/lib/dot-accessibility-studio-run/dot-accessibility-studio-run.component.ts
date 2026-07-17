import {
    ChangeDetectionStrategy,
    Component,
    computed,
    effect,
    ElementRef,
    inject,
    isDevMode,
    signal,
    viewChild
} from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { ChartModule } from 'primeng/chart';
import { SelectModule } from 'primeng/select';
import { ToggleSwitchModule } from 'primeng/toggleswitch';
import { TooltipModule } from 'primeng/tooltip';

import { AgentMessage, DotAgentActivityLogComponent } from '@dotcms/ai-ui';
import { DotMessageService } from '@dotcms/data-access';
import { AxeImpact } from '@dotcms/portlets/dot-ema/ui';
import { DotMessagePipe, SafeUrlPipe } from '@dotcms/ui';

import { A11yAgentPresenter } from '../models/a11y-agent.presenter';
import {
    impactToSeverity,
    SEVERITY_COLOR,
    SEVERITY_LABEL,
    SEVERITY_ORDER,
    type Severity
} from '../models/a11y-severity';
import { A11yMarkerService } from '../services/a11y-marker.service';
import { AccessibilityStudioStore } from '../store/accessibility-studio.store';

/** A severity legend / breakdown row beside the donut. */
interface SeverityRow {
    severity: Severity;
    label: string;
    color: string;
    count: number;
}

/**
 * The Studio run screen (§7): the agent column (score widget + recipe log +
 * state-driven action footer) beside a live preview pane.
 *
 * S3 is request/response with mock data — no SSE, no overlays, no before/after
 * split, no score animation. Those land in S4/S5. The score widget and recipe
 * log render the mock §6 report statically.
 */
@Component({
    selector: 'dot-accessibility-studio-run',
    standalone: true,
    imports: [
        FormsModule,
        ButtonModule,
        ChartModule,
        SelectModule,
        ToggleSwitchModule,
        TooltipModule,
        DotMessagePipe,
        SafeUrlPipe,
        DotAgentActivityLogComponent
    ],
    templateUrl: './dot-accessibility-studio-run.component.html',
    providers: [A11yMarkerService],
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'grid h-full min-h-0 grid-cols-[412px_1fr]' }
})
export class DotAccessibilityStudioRunComponent {
    readonly store = inject(AccessibilityStudioStore);

    private readonly markerService = inject(A11yMarkerService);

    /** Maps the agent stream + FixReport into shared activity-log bubbles. */
    private readonly presenter = new A11yAgentPresenter(inject(DotMessageService));

    /** The preview iframe — markers are injected into its (same-origin) document. */
    private readonly previewFrame =
        viewChild<ElementRef<HTMLIFrameElement>>('previewFrame');

    constructor() {
        // Redraw markers whenever the findings, preview mode, or phase change.
        effect(() => {
            const groups = this.store.a11yGroups();
            this.markerService.render(
                this.previewFrame()?.nativeElement,
                this.showMarkers() ? groups : []
            );
        });
    }

    /** Iframe finished (re)loading — (re)draw markers (see showMarkers()). */
    onPreviewLoad(): void {
        this.markerService.render(
            this.previewFrame()?.nativeElement,
            this.showMarkers() ? this.store.a11yGroups() : []
        );
    }

    /**
     * Whether the violation overlay should be drawn in the current preview render.
     * Markers come from the ORIGINAL scan, so they're only valid where those
     * violations still exist:
     *   - PRE-fix (scanned): both PREVIEW and LIVE still have them → show in either.
     *   - POST-fix (fixing/done/published): PREVIEW carries the agent's fixes so the
     *     old markers would be stale there → show on LIVE only.
     * Never before a scan has produced findings.
     */
    readonly showMarkers = computed<boolean>(() => {
        if (!this.store.scanned()) {
            return false;
        }
        if (this.store.isScanned()) {
            return true; // pre-fix: valid in both PREVIEW and LIVE
        }
        return this.previewMode() === 'LIVE'; // post-fix: only the unfixed LIVE render
    });


    /**
     * The section-header label above the scrollable body, by phase:
     *   scanning → "SCAN", scanned → "BY ISSUE TYPE", fixing/done → "AGENT ACTIVITY".
     */
    readonly logHeaderKey = computed<string>(() => {
        if (this.store.isScanning()) {
            return 'accessibility.studio.loghdr.scan';
        }
        if (this.store.isScanned()) {
            return 'accessibility.studio.loghdr.issues';
        }
        return 'accessibility.studio.loghdr.activity';
    });

    /** The working badge label beside the header ("SCANNING" / "WORKING"), or null. */
    readonly logBadgeKey = computed<string | null>(() => {
        if (this.store.isScanning()) {
            return 'accessibility.studio.badge.scanning';
        }
        if (this.store.isFixing()) {
            return 'accessibility.studio.badge.working';
        }
        return null;
    });

    /** Headline above the severity legend, by phase. */
    readonly scoreHeadlineKey = computed<string>(() => {
        if (this.store.isFixing()) {
            return 'accessibility.studio.score.fixing';
        }
        if (this.store.isDone() || this.store.isPublished()) {
            return 'accessibility.studio.score.remaining';
        }
        return 'accessibility.studio.score.found';
    });

    /**
     * Severity legend rows beside the donut (Critical/Serious/Moderate/Minor with
     * their element counts). Drives both the legend and the donut segments. While
     * scanned we hide empty buckets (matches the mockup); once fixing/done we keep
     * them so the user sees a bucket reach 0.
     */
    readonly severityRows = computed<SeverityRow[]>(() => {
        const counts = this.store.severityCounts();
        const keepZeros = this.store.isFixing() || this.store.isDone() || this.store.isPublished();
        return SEVERITY_ORDER.map((severity) => ({
            severity,
            label: SEVERITY_LABEL[severity],
            color: SEVERITY_COLOR[severity],
            count: counts[severity]
        })).filter((row) => keepZeros || row.count > 0);
    });

    /** PrimeNG doughnut data — one arc per severity, colored by SEVERITY_COLOR. */
    readonly donutData = computed(() => {
        const counts = this.store.severityCounts();
        const open = this.store.openCount();
        const total = SEVERITY_ORDER.reduce((sum, s) => sum + counts[s], 0);
        // No open issues → render a single full "clear" ring (green) so the donut
        // still reads as a complete circle rather than collapsing.
        if (total === 0 || open === 0) {
            return {
                labels: ['Clear'],
                datasets: [{ data: [1], backgroundColor: ['#22c55e'], borderWidth: 0 }]
            };
        }
        return {
            labels: SEVERITY_ORDER.map((s) => SEVERITY_LABEL[s]),
            datasets: [
                {
                    data: SEVERITY_ORDER.map((s) => counts[s]),
                    backgroundColor: SEVERITY_ORDER.map((s) => SEVERITY_COLOR[s]),
                    borderWidth: 0
                }
            ]
        };
    });

    /** Doughnut options — thin ring, no legend/tooltip (the center text is overlaid). */
    readonly donutOptions = {
        cutout: '74%',
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: false }, tooltip: { enabled: false } },
        animation: { duration: 500 }
    };

    /**
     * The scanning mini-log (screenshot 2): the two fixed rows shown while the
     * axe scan runs — "Loading page" then "Running axe-core scan".
     */
    readonly scanningRows = computed(() => [
        {
            icon: 'pi pi-search',
            text: 'accessibility.studio.scanning.loading',
            // The page path is shown verbatim (not an i18n string).
            sub: this.store.selected()?.path ?? '',
            translateSub: false
        },
        {
            icon: 'pi pi-spin pi-spinner',
            text: 'accessibility.studio.scanning.running',
            sub: 'accessibility.studio.scanning.running.sub',
            translateSub: true
        }
    ]);

    /**
     * The bubbles for the shared activity log, via the a11y presenter:
     *   - while fixing → one bubble per live SSE `step` (watch it work)
     *   - after done   → the final report expanded into bubbles (scan/fixed/reported/rescan)
     */
    readonly activityMessages = computed<AgentMessage[]>(() => {
        if (this.store.isFixing()) {
            return this.store.steps().map((step, i) => this.presenter.liveStep(step, i));
        }
        if (this.store.isDone() || this.store.isPublished()) {
            const report = this.store.report();
            return report ? this.presenter.resultMessages(report) : [];
        }
        return [];
    });

    /** The "now doing" banner content — the latest live step while fixing. */
    readonly activeMessage = computed<AgentMessage | null>(() => {
        const step = this.store.latestStep();
        return step ? this.presenter.liveStep(step, this.store.steps().length - 1) : null;
    });

    /** Footer title + sub keys derived from the current phase — single switch. */
    readonly footerKeys = computed(() => {
        const p = this.store.phase();
        const base = `accessibility.studio.footer.${p}`;
        return { titleKey: `${base}.title`, subKey: `${base}.sub` };
    });

    /** Interpolation args for the footer title, by phase. */
    readonly footerArgs = computed<string[]>(() => {
        switch (this.store.phase()) {
            case 'scanned':
                return [this.store.openCount().toString()];
            case 'fixing':
            case 'done':
            case 'published':
                return [
                    this.store.fixedCount().toString(),
                    this.store.reportedCount().toString()
                ];
            default:
                return [];
        }
    });

    /** Small leading icon + bubble color for the footer copy, by phase. */
    readonly footerIcon = computed<{ icon: string; cls: string } | null>(() => {
        switch (this.store.phase()) {
            case 'scanned':
                return { icon: 'pi pi-sparkles', cls: 'bg-primary-50 text-primary' };
            case 'fixing':
                return { icon: 'pi pi-bolt', cls: 'bg-orange-50 text-orange-600' };
            default:
                return null;
        }
    });

    /**
     * Which version of the page the iframe shows, so the user can compare the
     * agent's working fixes against the currently-published page:
     *   PREVIEW_MODE — the working/draft render (carries the agent's fixes, no chrome)
     *   LIVE         — the published render (what visitors see today, pre-fix)
     * Defaults to PREVIEW so the post-fix result is shown first.
     */
    readonly previewMode = signal<'PREVIEW_MODE' | 'LIVE'>('PREVIEW_MODE');

    /** Options for the preview/live p-select. */
    readonly previewModeOptions = [
        { label: 'accessibility.studio.preview.mode.preview', value: 'PREVIEW_MODE' },
        { label: 'accessibility.studio.preview.mode.live', value: 'LIVE' }
    ] as const;

    /**
     * Same-origin prefix for the preview iframe URL.
     *
     * In DEV the Angular dev server can't render dotCMS pages, so the iframe must
     * hit the backend. The dev proxy maps the `/dot-page` sentinel → the BE page
     * renderer (see apps/dotcms-ui/proxy-dev.conf.mjs). In PROD the portlet is
     * served from the dotCMS origin, so the page lives at its own path with NO
     * prefix — `/dot-page` would 404 there. `isDevMode()` is build-time accurate
     * (true under `ng serve`, false in a production build) and needs no app-env
     * import, so the dev-only prefix never leaks to production.
     */
    private readonly previewPathPrefix = isDevMode() ? '/dot-page' : '';

    /**
     * Preview URL for the iframe — the page rendered in the selected mode (§8.2).
     * `host_id` disambiguates which site's copy renders. PREVIEW_MODE shows the
     * working version (agent fixes); LIVE shows the published version — letting
     * the user compare before/after.
     */
    readonly previewUrl = computed(() => {
        const page = this.store.selected();
        if (!page) {
            return '';
        }
        const path = page.path.startsWith('/') ? page.path : `/${page.path}`;
        return `${this.previewPathPrefix}${path}?host_id=${page.hostId}&language_id=${page.languageId}&mode=${this.previewMode()}`;
    });

    backToPicker(): void {
        this.store.backToPicker();
    }

    runScan(): void {
        this.store.runScan();
    }

    stopScan(): void {
        this.store.stopScan();
    }

    startFix(): void {
        this.store.startFix();
    }

    stopAgent(): void {
        this.store.stopAgent();
    }

    publish(): void {
        this.store.publish();
    }

    discard(): void {
        this.store.discard();
    }

    onSkipCssChange(value: boolean): void {
        this.store.setSkipCss(value);
    }

    /** Dot color for an issue-type row, by axe impact (used by the BY ISSUE TYPE list). */
    severityColorFor(impact: AxeImpact | null): string {
        return SEVERITY_COLOR[impactToSeverity(impact)];
    }

    /**
     * i18n key for WHY an axe `incomplete` rule needs a human — keyed by rule code,
     * with a generic fallback. These are the common needs-review rules; axe couldn't
     * confirm them automatically, so a person has to judge.
     */
    reviewReasonKey(code: string): string {
        const known = REVIEW_REASON_KEYS[code];
        return known ?? 'accessibility.studio.review.reason.default';
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
