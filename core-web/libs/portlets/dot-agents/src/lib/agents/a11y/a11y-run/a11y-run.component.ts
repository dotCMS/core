import {
    ChangeDetectionStrategy,
    Component,
    computed,
    DestroyRef,
    effect,
    ElementRef,
    inject,
    isDevMode,
    signal,
    untracked,
    viewChild
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import { AccordionModule } from 'primeng/accordion';
import { ButtonModule } from 'primeng/button';
import { ChartModule } from 'primeng/chart';
import { ToggleSwitchModule } from 'primeng/toggleswitch';
import { TooltipModule } from 'primeng/tooltip';

import { map } from 'rxjs/operators';

import { AgentMessage, DotAgentActivityLogComponent } from '@dotcms/ai-ui';
import { DotMessageService } from '@dotcms/data-access';
import { GlobalStore } from '@dotcms/store';
import { DotMessagePipe, SafeUrlPipe } from '@dotcms/ui';

import { DotA11yDiffViewerComponent } from '../a11y-diff/a11y-diff-viewer.component';
import { DotA11yDiffComponent } from '../a11y-diff/a11y-diff.component';
import { A11yAgentPresenter } from '../models/a11y-agent.presenter';
import {
    impactToSeverity,
    SEVERITY_COLOR,
    SEVERITY_LABEL,
    SEVERITY_ORDER,
    type Severity
} from '../models/a11y-severity';
import { PageDiffFile } from '../models/page-render-sources.models';
import { A11yMarkerService } from '../services/a11y-marker.service';
import { AccessibilityStudioStore } from '../store/accessibility-studio.store';

/** The side panel's two accordion panels. */
type StudioPanel = 'scanner' | 'files';

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
    selector: 'dot-a11y-run',
    standalone: true,
    imports: [
        FormsModule,
        AccordionModule,
        ButtonModule,
        ChartModule,
        ToggleSwitchModule,
        TooltipModule,
        DotMessagePipe,
        SafeUrlPipe,
        DotAgentActivityLogComponent,
        DotA11yDiffComponent,
        DotA11yDiffViewerComponent
    ],
    templateUrl: './a11y-run.component.html',
    styles: [
        `
            /* Scanning shows a skeleton with the SAME footprint as the results, so
               swapping to the real data is a pure crossfade — no layout shift. Both
               the skeleton and the results just fade in. */
            @keyframes studio-fade-in {
                from {
                    opacity: 0;
                }
                to {
                    opacity: 1;
                }
            }

            .studio-fade-in {
                animation: studio-fade-in 0.25s ease-out both;
            }

            @media (prefers-reduced-motion: reduce) {
                .studio-fade-in {
                    animation: none;
                }
            }
        `
    ],
    providers: [A11yMarkerService],
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'grid h-full min-h-0 grid-cols-[412px_1fr]' }
})
export class DotA11yRunComponent {
    readonly store = inject(AccessibilityStudioStore);

    private readonly markerService = inject(A11yMarkerService);
    private readonly router = inject(Router);
    private readonly route = inject(ActivatedRoute);
    private readonly globalStore = inject(GlobalStore);
    private readonly destroyRef = inject(DestroyRef);

    /**
     * The number shown in the ring center. Eased from its previous value up to
     * the store's `openCount()` whenever a scan resolves (or the count changes
     * while fixing), so the score "rolls" in sync with the donut sweep instead of
     * snapping. See {@link animateCountTo}.
     */
    readonly displayCount = signal(0);

    /**
     * The source file whose diff the right pane is showing, or null for the preview.
     * Set from the changed-files accordion in the left panel; the preview stays
     * mounted underneath so returning to it doesn't reload the iframes.
     */
    readonly diffFile = signal<PageDiffFile | null>(null);

    /**
     * Which of the side panel's accordion panels are open — the `scanner` (score,
     * issues, activity log + scan/fix actions) and `files` (changed files + publish).
     * `p-accordion` is in `multiple` mode, so this is the array it two-way binds:
     * the panels open and close independently and the user can watch a run while
     * reviewing the files it touched. The scanner starts open, files collapsed.
     */
    readonly openPanels = signal<StudioPanel[]>(['scanner']);

    /**
     * Accordion design tokens, scoped to this accordion via `[dt]` — the component's
     * own styling contract, not a `::ng-deep` override.
     *
     * The app-wide CustomLaraPreset flattens accordions with `panel.borderWidth: '0'`
     * and leaves per-feature dividers to the consuming component (see theme.config.ts).
     * So the bottom rule that separates the scanner and files panels is added back
     * here, in gray-200.
     */
    readonly panelTokens = {
        panel: {
            borderWidth: '0 0 1px 0',
            borderColor: '{gray.200}'
        }
    };

    /** How many source files differ between working and live, for the count badge. */
    readonly changedFileCount = signal(0);

    /** True when there's something to publish — drives the Publish bar. */
    readonly hasChangedFiles = computed(() => this.changedFileCount() > 0);

    /** rAF handle for the in-flight count-up, so a new scan can cancel it. */
    private countRaf: number | null = null;

    private readonly dm = inject(DotMessageService);

    /** Maps the agent stream + FixReport into shared activity-log bubbles. */
    private readonly presenter = new A11yAgentPresenter(this.dm);

    /**
     * The two side-by-side preview iframes. Markers are injected only into the
     * LIVE frame's (same-origin) document — it always still carries the original
     * scan's violations (see {@link showMarkers}).
     */
    private readonly liveFrame = viewChild<ElementRef<HTMLIFrameElement>>('liveFrame');
    private readonly previewFrame =
        viewChild<ElementRef<HTMLIFrameElement>>('previewFrame');

    /**
     * The page path this run screen is opened against — reconstructed from the
     * wildcard route's segments (e.g. `['blog','post','hello']` → `/blog/post/hello`).
     */
    private readonly pageUri = toSignal(
        this.route.url.pipe(
            map((segments) =>
                segments.length ? `/${segments.map((s) => s.path).join('/')}` : null
            )
        )
    );

    constructor() {
        // The URL is the source of truth for which page is open. Drive the store
        // from the page path: rehydrate it (a no-op when that page is already the
        // selection, e.g. arriving from the picker), so a cold load / shared link
        // / refresh lands on the run screen for that page. Also track the current
        // site: the lookup is host-scoped, so on a cold load the effect must re-run
        // once the site resolves (it loads async after the component mounts).
        effect(() => {
            const uri = this.pageUri();
            const siteId = this.globalStore.currentSiteId();
            if (uri && siteId) {
                untracked(() => this.store.openPageByUri(uri));
            }
        });

        // A deep link to a page that no longer resolves → back to the picker.
        effect(() => {
            if (this.store.rehydrateStatus() === 'not-found') {
                untracked(() => this.toPicker());
            }
        });

        // Redraw both frames' marker layers whenever their scans (or the phase)
        // change. Each frame gets its OWN scan's findings: the preview frame from
        // the primary/working scan (a11yGroups), the live frame from the
        // comparison scan (liveA11yGroups). We run separate scans, so a fix that
        // isn't published yet clears the preview markers while the live markers
        // (still-published violations) remain.
        effect(() => {
            const show = this.showMarkers();
            const previewGroups = this.store.a11yGroups();
            const liveGroups = this.store.liveA11yGroups();
            this.markerService.render(
                this.previewFrame()?.nativeElement,
                show ? previewGroups : []
            );
            this.markerService.render(
                this.liveFrame()?.nativeElement,
                show ? liveGroups : []
            );
        });

        // Roll the ring count up to the live open-count whenever it changes and a
        // scan has produced results — the score animates in sync with the donut
        // sweep. Before any results (ready/scanning) it stays parked at 0, so each
        // scan / rescan rolls up fresh.
        effect(() => {
            const target = this.store.openCount();
            const scanned = this.store.hasResults();
            untracked(() => this.animateCountTo(scanned ? target : 0));
        });

        // Cancel any in-flight count-up when the component is torn down.
        this.destroyRef.onDestroy(() => this.cancelCount());
    }

    /**
     * Ease {@link displayCount} from its current value to `target` over ~600ms
     * (easeOutCubic), synced with the donut's sweep. Snaps immediately when the
     * user prefers reduced motion or the delta is trivial.
     */
    private animateCountTo(target: number): void {
        this.cancelCount();

        const from = this.displayCount();
        if (from === target) {
            return;
        }
        const reduceMotion =
            typeof matchMedia === 'function' &&
            matchMedia('(prefers-reduced-motion: reduce)').matches;
        if (reduceMotion) {
            this.displayCount.set(target);

            return;
        }

        const duration = 600;
        let start: number | null = null;
        const step = (now: number) => {
            start ??= now;
            const t = Math.min(1, (now - start) / duration);
            const eased = 1 - Math.pow(1 - t, 3); // easeOutCubic
            this.displayCount.set(Math.round(from + (target - from) * eased));
            if (t < 1) {
                this.countRaf = requestAnimationFrame(step);
            } else {
                this.countRaf = null;
            }
        };
        this.countRaf = requestAnimationFrame(step);
    }

    private cancelCount(): void {
        if (this.countRaf !== null) {
            cancelAnimationFrame(this.countRaf);
            this.countRaf = null;
        }
    }

    /** Navigate up to the picker route (`/agents/a11y`). */
    private toPicker(): void {
        this.router.navigate(['..'], { relativeTo: this.route });
    }

    /**
     * Re-entrancy guard for scroll mirroring: setting frame B's scroll fires B's
     * own `scroll` event, which would mirror straight back to A — an infinite
     * bounce. While we're programmatically scrolling the target, ignore its echo.
     */
    private syncingScroll = false;

    /**
     * LIVE iframe finished (re)loading — (re)draw its markers from the LIVE
     * (comparison) scan + (re)wire scroll sync. A load replaces the document, so
     * the effect-drawn layer is gone and must be redrawn here.
     */
    onLiveLoad(): void {
        this.markerService.render(
            this.liveFrame()?.nativeElement,
            this.showMarkers() ? this.store.liveA11yGroups() : []
        );
        this.wireScrollSync(this.liveFrame(), this.previewFrame());
    }

    /**
     * PREVIEW iframe finished (re)loading — (re)draw its markers from the primary
     * (working) scan + (re)wire scroll sync.
     */
    onPreviewLoad(): void {
        this.markerService.render(
            this.previewFrame()?.nativeElement,
            this.showMarkers() ? this.store.a11yGroups() : []
        );
        this.wireScrollSync(this.previewFrame(), this.liveFrame());
    }

    /**
     * Mirror `source`'s scroll onto `target` so the two side-by-side renders stay
     * aligned — makes the before/after diff scannable without scrolling each pane
     * separately. Both frames are same-origin (the `/dot-page` proxy / BE origin),
     * so we can read/write `contentWindow.scroll*` directly; cross-origin access
     * throws and we no-op.
     *
     * Wired on every `load`: a reload/navigation replaces the frame's window, which
     * drops the old listener for free, so we just attach a fresh one each time.
     */
    private wireScrollSync(
        source: ElementRef<HTMLIFrameElement> | undefined,
        target: ElementRef<HTMLIFrameElement> | undefined
    ): void {
        const srcWin = this.frameWindow(source);
        if (!srcWin) {
            return;
        }
        srcWin.addEventListener(
            'scroll',
            () => {
                if (this.syncingScroll) {
                    return;
                }
                const tgtWin = this.frameWindow(target);
                if (!tgtWin) {
                    return;
                }
                this.syncingScroll = true;
                tgtWin.scrollTo(srcWin.scrollX, srcWin.scrollY);
                // Release after the target's echoed scroll event has fired.
                requestAnimationFrame(() => (this.syncingScroll = false));
            },
            { passive: true }
        );
    }

    /** The iframe's window; null when cross-origin or not yet loaded. */
    private frameWindow(frame: ElementRef<HTMLIFrameElement> | undefined): Window | null {
        try {
            return frame?.nativeElement.contentWindow ?? null;
        } catch {
            return null;
        }
    }

    /**
     * Whether the violation overlays should be drawn. Each frame draws its OWN
     * scan's findings (preview ← primary scan, live ← comparison scan), so the
     * only shared gate is: a scan pass has run. Empty groups (e.g. the live scan
     * hasn't landed yet, or a frame came back clean) simply draw no markers.
     */
    readonly showMarkers = computed<boolean>(() => this.store.hasResults());


    /**
     * The section-header label above the scrollable body, by phase:
     *   scanning → "SCAN", scanned → "BY ISSUE TYPE", fixing/done → "AGENT ACTIVITY".
     */
    readonly logHeaderKey = computed<string>(() => {
        if (this.store.phase() === 'scanning') {
            return 'accessibility.studio.loghdr.scan';
        }
        if (this.store.phase() === 'scanned') {
            return 'accessibility.studio.loghdr.issues';
        }
        return 'accessibility.studio.loghdr.activity';
    });

    /** The working badge label beside the header ("SCANNING" / "WORKING"), or null. */
    readonly logBadgeKey = computed<string | null>(() => {
        if (this.store.phase() === 'scanning') {
            return 'accessibility.studio.badge.scanning';
        }
        if (this.store.phase() === 'fixing') {
            return 'accessibility.studio.badge.working';
        }
        return null;
    });

    /** Headline above the severity legend, by phase. */
    readonly scoreHeadlineKey = computed<string>(() => {
        if (this.store.phase() === 'fixing') {
            return 'accessibility.studio.score.fixing';
        }
        if (this.store.finished()) {
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
        const keepZeros = this.store.runStarted();
        return SEVERITY_ORDER.map((severity) => ({
            severity,
            label: SEVERITY_LABEL[severity],
            color: SEVERITY_COLOR[severity],
            count: counts[severity]
        })).filter((row) => keepZeros || row.count > 0);
    });

    /**
     * BY ISSUE TYPE rows with their dot color resolved. Projected here rather than
     * calling a method from the template: this component's change detection is driven
     * by a live SSE stream plus a rAF count-up, so a template method would re-run for
     * every row many times a second.
     */
    readonly issueTypeRows = computed(() =>
        this.store.issueTypeRows().map((group) => ({
            ...group,
            color: SEVERITY_COLOR[impactToSeverity(group.impact)]
        }))
    );

    /** Needs-review rows with their "why a human is needed" i18n key resolved. */
    readonly reviewRows = computed(() =>
        this.store.reviewGroups().map((group) => ({
            ...group,
            reasonKey:
                REVIEW_REASON_KEYS[group.code] ?? 'accessibility.studio.review.reason.default'
        }))
    );

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

    /**
     * Doughnut options — thin ring, no legend/tooltip (the center text is overlaid).
     * p-chart is sized via its `width`/`height` inputs (124px square); PrimeNG then
     * sets `maintainAspectRatio: false` itself so the ring fills that square. We
     * don't set responsive/aspect here — letting PrimeNG own the sizing keeps the
     * ring centered in the box, aligned with the absolutely-centered count.
     */
    readonly donutOptions = {
        cutout: '74%',
        plugins: { legend: { display: false }, tooltip: { enabled: false } },
        animation: { duration: 500 }
    };


    /**
     * The SETTLED bubbles for the shared activity log, via the a11y presenter:
     *   - while fixing → one bubble per streamed SSE `phase` step (the completed
     *     actions); the live "now working" item is {@link workingMessage}, appended
     *     by the log itself
     *   - after done   → the final report expanded into bubbles (scan/fixed/reported/rescan)
     */
    readonly activityMessages = computed<AgentMessage[]>(() => {
        if (this.store.phase() === 'fixing') {
            return this.store.steps().map((step, i) => this.presenter.liveStep(step, i));
        }
        if (this.store.finished()) {
            const report = this.store.report();
            return report ? this.presenter.resultMessages(report) : [];
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
    readonly workingMessage = computed<AgentMessage | null>(() => {
        if (this.store.phase() !== 'fixing') {
            return null;
        }
        const sinceLastEventMs = this.store.heartbeat()?.sinceLastEventMs ?? 0;

        // Elapsed on the current action, once it's been running a beat.
        const sinceSec = Math.floor(sinceLastEventMs / 1000);
        const sub =
            sinceSec >= 3
                ? this.dm.get('accessibility.studio.working.elapsed', String(sinceSec))
                : undefined;

        return {
            id: 'agent-working',
            icon: 'pi pi-spin pi-spinner',
            text: this.dm.get(this.workingReassuranceKey(sinceLastEventMs)),
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
    private workingReassuranceKey(sinceLastEventMs: number): string {
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
     * Same-origin prefix for the preview iframe URLs.
     *
     * In DEV the Angular dev server can't render dotCMS pages, so the iframes must
     * hit the backend. The dev proxy maps the `/dot-page` sentinel → the BE page
     * renderer (see apps/dotcms-ui/proxy-dev.conf.mjs). In PROD the portlet is
     * served from the dotCMS origin, so the page lives at its own path with NO
     * prefix — `/dot-page` would 404 there. `isDevMode()` is build-time accurate
     * (true under `ng serve`, false in a production build) and needs no app-env
     * import, so the dev-only prefix never leaks to production.
     */
    private readonly previewPathPrefix = isDevMode() ? '/dot-page' : '';

    /**
     * The page rendered in the given mode. `host_id` disambiguates which site's
     * copy renders. Shared by the two side-by-side frames (§8.2). An optional
     * cache-busting `rev` forces the iframe to reload when the working render
     * changes (see {@link previewUrl}).
     */
    private urlFor(mode: 'PREVIEW_MODE' | 'LIVE', rev = 0): string {
        const page = this.store.selected();
        if (!page) {
            return '';
        }
        const path = page.path.startsWith('/') ? page.path : `/${page.path}`;
        const bust = rev > 0 ? `&rev=${rev}` : '';
        return `${this.previewPathPrefix}${path}?host_id=${page.hostId}&language_id=${page.languageId}&mode=${mode}${bust}`;
    }

    /**
     * The two frames shown side by side so the diff reads at a glance:
     *   LIVE     — the published render (what visitors see today, pre-fix) + markers
     *   PREVIEW  — the working render (carries the agent's fixes, the "after")
     *
     * The PREVIEW url carries the store's `previewRevision` as a cache-buster, so
     * the iframe reloads whenever the agent applies fixes (each mid-fix re-scan +
     * the final report) and the page updates visually. LIVE never changes mid-run
     * (it's the published render), so it takes no revision.
     */
    readonly liveUrl = computed(() => this.urlFor('LIVE'));
    readonly previewUrl = computed(() => this.urlFor('PREVIEW_MODE', this.store.previewRevision()));

    backToPicker(): void {
        this.store.backToPicker();
        this.toPicker();
    }

    /**
     * Whether a given side panel is expanded. `p-accordion` renders the panel bodies
     * itself off the same two-way-bound `openPanels`, so nothing in the template needs
     * this — it's the readable way to assert open state from tests.
     */
    isPanelOpen(panel: StudioPanel): boolean {
        return this.openPanels().includes(panel);
    }

    /**
     * Ensure a panel is open — used to jump the user to the files list. Not a toggle:
     * pressing "Review files" twice must not close the panel it just opened.
     */
    openPanel(panel: StudioPanel): void {
        this.openPanels.update((current) =>
            current.includes(panel) ? current : [...current, panel]
        );
    }

    /** A file was picked in (or cleared from) the changed-files list. */
    onDiffFileSelected(file: PageDiffFile | null): void {
        this.diffFile.set(file);
    }

    /** The changed-files list reports how many files differ, for the Publish gate. */
    onChangedFilesCount(count: number): void {
        this.changedFileCount.set(count);
    }

    /**
     * Leave the diff view from the right pane's own control. The accordion's
     * highlighted row follows via its `activeFileId` input, so the two stay in sync.
     */
    closeDiff(): void {
        this.diffFile.set(null);
    }

    /**
     * "Apply these changes" (done phase): publish the page. That promotes the whole
     * working version — the page and its changed source files together — since there
     * is no per-file publishing. The changed-files accordion above lists exactly
     * what goes live.
     */
    applyChanges(): void {
        this.store.publish();
    }

    /** Drop this run's working fixes → back to the scanned state. */
    discardChanges(): void {
        this.store.discard();
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

    onSkipCssChange(value: boolean): void {
        this.store.setSkipCss(value);
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
