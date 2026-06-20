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
import { SelectModule } from 'primeng/select';
import { ToggleSwitchModule } from 'primeng/toggleswitch';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessagePipe, SafeUrlPipe } from '@dotcms/ui';

import { FixResult, StudioStepPhase } from '../models/accessibility-studio.models';
import { A11yMarkerService } from '../services/a11y-marker.service';
import { AccessibilityStudioStore } from '../store/accessibility-studio.store';

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

    /** Score ring geometry — circumference of r=54 circle. */
    private readonly RING_CIRCUMFERENCE = 339.292;

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

    /** The number shown in the ring center: open violations, or a dash pre-scan. */
    readonly centerCount = computed<string | number>(() => {
        if (this.store.isScanning() || !this.store.scanned()) {
            return '–';
        }
        // While "scanned" (pre-fix) show the real before count; after the
        // (mocked) fix show the after count.
        return this.store.isDone() || this.store.isPublished()
            ? this.store.afterCount()
            : this.store.beforeCount();
    });

    /** Ring fill 0→1 based on issues cleared (before − current) / before. */
    readonly ringProgress = computed(() => {
        if (!this.store.isDone() && !this.store.isPublished()) {
            return 0;
        }
        const before = this.store.beforeCount();
        const cleared = before - this.store.afterCount();
        return before > 0 ? Math.min(1, Math.max(0, cleared / before)) : 0;
    });

    readonly ringOffset = computed(
        () => this.RING_CIRCUMFERENCE * (1 - this.ringProgress())
    );

    readonly ringClass = computed(() => {
        if (!this.store.scanned()) {
            return 'stroke-surface-300';
        }
        return this.ringProgress() >= 1 ? 'stroke-green-500' : 'stroke-primary';
    });

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
            steps.push({
                id: `reported-${i}`,
                icon: 'pi pi-flag',
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

    startFix(): void {
        this.store.startFix();
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
}
