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

import { AxeImpact } from '@dotcms/portlets/dot-ema/ui';
import { DotMessagePipe, SafeUrlPipe } from '@dotcms/ui';

import {
    impactToSeverity,
    SEVERITY_COLOR,
    SEVERITY_LABEL,
    SEVERITY_ORDER,
    type Severity
} from '../models/a11y-severity';
import { FixResult, StudioStepPhase } from '../models/accessibility-studio.models';
import { A11yMarkerService } from '../services/a11y-marker.service';
import { AccessibilityStudioStore } from '../store/accessibility-studio.store';

/** A severity legend / breakdown row beside the donut. */
interface SeverityRow {
    severity: Severity;
    label: string;
    color: string;
    count: number;
}

/** A human-readable line in the Agent Recipe log. */
interface RecipeStep {
    /** Stable id for @for tracking + entry animation. */
    id: string | number;
    icon: string;
    text: string;
    sub?: string;
    /** 'fixed' | 'reported' | 'info' — drives the bubble color. */
    tone: 'fixed' | 'reported' | 'info';
}

/** Icon for each live agent step phase (SSE `step` events). */
const STEP_PHASE_ICON: Record<StudioStepPhase, string> = {
    scan: 'pi pi-search',
    locate: 'pi pi-sitemap',
    read: 'pi pi-file',
    fix: 'pi pi-wrench',
    rescan: 'pi pi-verified'
};

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
        SafeUrlPipe
    ],
    templateUrl: './dot-accessibility-studio-run.component.html',
    styles: [
        `
            /* Each recipe step slides + fades in as it's appended to the log,
               giving the live agent activity a sense of motion. */
            @keyframes dot-recipe-step-in {
                from {
                    opacity: 0;
                    transform: translateY(6px);
                }
                to {
                    opacity: 1;
                    transform: translateY(0);
                }
            }

            .dot-recipe-step {
                animation: dot-recipe-step-in 0.28s ease-out both;
            }

            @media (prefers-reduced-motion: reduce) {
                .dot-recipe-step {
                    animation: none;
                }
            }
        `
    ],
    providers: [A11yMarkerService],
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'grid h-full min-h-0 grid-cols-[412px_1fr]' }
})
export class DotAccessibilityStudioRunComponent {
    readonly store = inject(AccessibilityStudioStore);

    private readonly markerService = inject(A11yMarkerService);

    /** The preview iframe — markers are injected into its (same-origin) document. */
    private readonly previewFrame =
        viewChild<ElementRef<HTMLIFrameElement>>('previewFrame');

    /** The scrollable recipe log — auto-scrolled to the latest live step. */
    private readonly recipeLog = viewChild<ElementRef<HTMLElement>>('recipeLog');

    constructor() {
        // Redraw markers whenever the findings or preview mode change. Markers
        // highlight the ORIGINAL violations, so they belong on the LIVE (published,
        // pre-fix) render — on the PREVIEW (working, post-fix) render they'd be
        // stale, so we clear them there to show the clean, fixed result.
        effect(() => {
            const groups = this.store.a11yGroups();
            const showMarkers = this.previewMode() === 'LIVE';
            this.markerService.render(
                this.previewFrame()?.nativeElement,
                showMarkers ? groups : []
            );
        });

        // Keep the latest live step in view as the agent streams its activity.
        effect(() => {
            // Read the step count so the effect re-runs on each new step.
            const stepCount = this.store.steps().length;
            const log = this.recipeLog()?.nativeElement;
            if (log && stepCount) {
                log.scrollTop = log.scrollHeight;
            }
        });
    }

    /** Iframe finished (re)loading — (re)draw markers (LIVE only; see constructor). */
    onPreviewLoad(): void {
        const showMarkers = this.previewMode() === 'LIVE';
        this.markerService.render(
            this.previewFrame()?.nativeElement,
            showMarkers ? this.store.a11yGroups() : []
        );
    }

    /** The big number in the donut center — the live open-issue count. */
    readonly centerCount = computed<number>(() => this.store.openCount());

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

    /** True while the agent is actively running (SSE in flight). */
    readonly isFixing = computed(() => this.store.isFixing());

    /**
     * Live agent activity — one entry per streamed SSE `step` event. Rendered
     * while the agent runs so the user watches the work happen in real time.
     */
    readonly liveSteps = computed<RecipeStep[]>(() =>
        this.store.steps().map((s) => ({
            id: s.id,
            icon: STEP_PHASE_ICON[s.phase],
            text: s.message,
            tone: 'info'
        }))
    );

    /**
     * The Agent Recipe step log:
     *   - while fixing → the live SSE activity (liveSteps)
     *   - after done   → the final report (fixed + reported, bookended by scan/rescan)
     */
    readonly recipeSteps = computed<RecipeStep[]>(() => {
        if (this.store.isFixing()) {
            return this.liveSteps();
        }
        if (!this.store.isDone() && !this.store.isPublished()) {
            return [];
        }
        const report = this.store.report();
        if (!report) {
            return [];
        }

        const steps: RecipeStep[] = [
            {
                id: 'scan',
                icon: 'pi pi-search',
                text: 'Scanned page against WCAG 2.2 AA',
                sub: `${report.scan.before.violations} issues found`,
                tone: 'info'
            },
            {
                id: 'locate',
                icon: 'pi pi-sitemap',
                text: 'Located source templates & containers',
                tone: 'info'
            }
        ];

        this.store.fixedResults().forEach((r, i) => {
            steps.push({
                id: `fixed-${i}`,
                icon: 'pi pi-check',
                text: r.review ?? `Fixed ${r.ruleId}`,
                sub: this.ruleAndFile(r),
                tone: 'fixed'
            });
        });

        this.store.reportedResults().forEach((r, i) => {
            // Distinct icon per non-fixed status: reverted → undo, regressed → undo,
            // everything else (reported/skipped/failed) → flag.
            const reverted = r.reverted || r.status === 'regressed';
            steps.push({
                id: `reported-${i}`,
                icon: reverted ? 'pi pi-replay' : 'pi pi-flag',
                text: r.review ?? r.reason ?? `Flagged ${r.ruleId}`,
                sub: this.ruleAndFile(r),
                tone: 'reported'
            });
        });

        steps.push({
            id: 'rescan',
            icon: 'pi pi-verified',
            text: 'Re-scanned working copy — fixes confirmed',
            sub: `${report.scan.before.violations} → ${report.scan.after.violations} violations`,
            tone: 'info'
        });

        return steps;
    });

    /** Footer headline copy by phase. */
    readonly footerTitleKey = computed(() => {
        switch (this.store.phase()) {
            case 'ready':
                return 'accessibility.studio.footer.ready.title';
            case 'scanning':
                return 'accessibility.studio.footer.scanning.title';
            case 'scanned':
                return 'accessibility.studio.footer.scanned.title';
            case 'fixing':
                return 'accessibility.studio.footer.fixing.title';
            case 'done':
                return 'accessibility.studio.footer.done.title';
            case 'published':
                return 'accessibility.studio.footer.published.title';
            default:
                return '';
        }
    });

    /** Interpolation args for the footer title, by phase. */
    readonly footerArgs = computed<string[]>(() => {
        switch (this.store.phase()) {
            case 'scanned':
                // "N issues ready to fix"
                return [this.store.openCount().toString()];
            case 'fixing':
            case 'done':
            case 'published':
                // "N fixed to working …" / "N fixed · M flagged"
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

    readonly footerSubKey = computed(() => {
        switch (this.store.phase()) {
            case 'ready':
                return 'accessibility.studio.footer.ready.sub';
            case 'scanning':
                return 'accessibility.studio.footer.scanning.sub';
            case 'scanned':
                return 'accessibility.studio.footer.scanned.sub';
            case 'fixing':
                return 'accessibility.studio.footer.fixing.sub';
            case 'done':
                return 'accessibility.studio.footer.done.sub';
            case 'published':
                return 'accessibility.studio.footer.published.sub';
            default:
                return '';
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

    private ruleAndFile(r: FixResult): string {
        const file = r.file ? r.file.split('/').pop() : undefined;
        return file ? `${r.ruleId} · ${file}` : r.ruleId;
    }

    /** Dot color for an issue-type row, by axe impact (used by the BY ISSUE TYPE list). */
    severityColorFor(impact: AxeImpact): string {
        return SEVERITY_COLOR[impactToSeverity(impact)];
    }
}
