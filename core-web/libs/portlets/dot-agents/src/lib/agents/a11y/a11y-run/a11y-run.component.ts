import { Location } from '@angular/common';
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
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { AccordionModule } from 'primeng/accordion';
import { ButtonModule } from 'primeng/button';
import { ChartModule } from 'primeng/chart';
import { ToggleSwitchModule } from 'primeng/toggleswitch';
import { TooltipModule } from 'primeng/tooltip';

import { AgentMessage, DotAgentActivityLogComponent } from '@dotcms/ai-ui';
import { DotAgentRunService, DotMessageService } from '@dotcms/data-access';
import { DotPageScannerService } from '@dotcms/portlets/dot-ema/ui';
import { DotMessagePipe, SafeUrlPipe } from '@dotcms/ui';

import { DotA11yDiffViewerComponent } from '../a11y-diff/a11y-diff-viewer.component';
import { DotA11yDiffComponent } from '../a11y-diff/a11y-diff.component';
import { A11Y_PAGE_LIST_ROUTE } from '../a11y.constants';
import { A11yAgentPresenter } from '../models/a11y-agent.presenter';
import {
    impactToSeverity,
    SEVERITY_COLOR,
    SEVERITY_LABEL,
    SEVERITY_ORDER,
    type Severity
} from '../models/a11y-severity';
import { StudioPageRow } from '../models/accessibility-studio.models';
import { PageDiffFile } from '../models/page-render-sources.models';
import { A11yMarkerService } from '../services/a11y-marker.service';
import { DotA11yAgentService } from '../services/dot-a11y-agent.service';
import { A11yRunStore } from '../store/a11y-run.store';

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

            /* Make the OPEN scanner panel's content scroll in place (headers stay put)
               instead of the whole accordion scrolling. PrimeNG's collapse animation
               wraps the content in a grid + <p-motion> + wrapper that its [pt] API
               can't reach; those need min-height:0 so the innermost content — which
               carries overflow-y:auto via [pt] — can be bounded and scroll.
               The wrapper also gets overflow:hidden: the inner content's own
               overflow-y:auto would otherwise escape the collapsed (0-height) panel
               and make the whole page scroll. Scoped to this component's accordion. */
            :host ::ng-deep [data-testid='studio-panels'] .p-accordioncontent {
                min-height: 0;
                /* Clip the content to its grid row. When collapsed PrimeNG animates
                   the row to 0; without this the content overflows that 0-height cell
                   and scrolls the whole page. The open panel scrolls via its inner
                   content's overflow-y (below), not this box. */
                overflow: hidden;
            }

            :host ::ng-deep [data-testid='studio-panels'] .p-accordioncontent p-motion {
                min-height: 0;
            }

            :host ::ng-deep [data-testid='studio-panels'] .p-accordioncontent-wrapper {
                min-height: 0;
            }

            /* Both the shrinkable grid row (lets content be bounded shorter than its
               natural height) and the scroll live ONLY on the ACTIVE panel. On a
               collapsed panel PrimeNG animates the grid row to 0; leaving the row
               shrinkable or the content scrollable there would let the content escape
               the 0-height cell and scroll the whole page. */
            :host
                ::ng-deep
                [data-testid='studio-panels']
                .p-accordionpanel-active
                .p-accordioncontent {
                grid-template-rows: minmax(0, 1fr);
            }

            :host
                ::ng-deep
                [data-testid='studio-panels']
                .p-accordionpanel-active
                .p-accordioncontent-content {
                min-height: 0;
                overflow-y: auto;
            }
        `
    ],
    // The run store + the services it drives are provided HERE (not at the root),
    // so each run route gets a fresh instance — navigating to a different page
    // recreates the store with clean scan/fix state.
    providers: [
        A11yRunStore,
        A11yMarkerService,
        DotPageScannerService,
        DotA11yAgentService,
        DotAgentRunService
    ],
    changeDetection: ChangeDetectionStrategy.OnPush,
    // Two rows: an `auto` row for the error banner (collapses to 0 when there is no
    // error, since the banner is the only thing in it) over the main content row, which
    // takes the rest. `minmax(0,1fr)` rather than `1fr` so the panes can be bounded
    // shorter than their content and scroll internally.
    host: { class: 'grid h-full min-h-0 grid-cols-[412px_1fr] grid-rows-[auto_minmax(0,1fr)]' }
})
export class DotA11yRunComponent {
    readonly store = inject(A11yRunStore);

    readonly #markerService = inject(A11yMarkerService);
    readonly #router = inject(Router);
    readonly #location = inject(Location);
    readonly #destroyRef = inject(DestroyRef);

    /**
     * The number shown in the ring center. Eased from its previous value up to
     * the store's `openCount()` whenever a scan resolves (or the count changes
     * while fixing), so the score "rolls" in sync with the donut sweep instead of
     * snapping. See {@link animateCountTo}.
     */
    readonly $displayCount = signal(0);

    /**
     * The source file whose diff the right pane is showing, or null for the preview.
     * Set from the changed-files accordion in the left panel; the preview stays
     * mounted underneath so returning to it doesn't reload the iframes.
     */
    readonly $diffFile = signal<PageDiffFile | null>(null);

    /**
     * Which of the side panel's accordion panels are open — the `scanner` (score,
     * issues, activity log + scan/fix actions) and `files` (changed files + publish).
     * `p-accordion` is in `multiple` mode, so this is the array it two-way binds:
     * the panels open and close independently and the user can watch a run while
     * reviewing the files it touched. The scanner starts open, files collapsed.
     */
    readonly $openPanels = signal<StudioPanel[]>(['scanner']);

    /** How many source files differ between working and live, for the count badge. */
    readonly $changedFileCount = signal(0);

    /** True when there's something to publish — drives the Publish bar. */
    readonly $hasChangedFiles = computed(() => this.$changedFileCount() > 0);

    /** rAF handle for the in-flight count-up, so a new scan can cancel it. */
    #countRaf: number | null = null;

    readonly #dm = inject(DotMessageService);

    /** Maps the agent stream + FixReport into shared activity-log bubbles. */
    readonly #presenter = new A11yAgentPresenter(this.#dm);

    /**
     * The two side-by-side preview iframes. Markers are injected only into the
     * LIVE frame's (same-origin) document — it always still carries the original
     * scan's violations (see {@link showMarkers}).
     */
    // NOTE: `private`, not `#`. Angular rejects an ES-private member for a signal query
    // outright — "Cannot use 'viewChild' on a class member that is declared as ES private"
    // — because the compiler has to write to the field from generated code.
    private readonly $liveFrame = viewChild<ElementRef<HTMLIFrameElement>>('liveFrame');
    private readonly $previewFrame = viewChild<ElementRef<HTMLIFrameElement>>('previewFrame');

    constructor() {
        // The page list hands the selected row over in the navigation's `state` (the
        // URL carries only its readable path, which can't supply identifier/host/
        // language). Adopt it, or bounce back to the list when there is none — which
        // is what a cold load, refresh, or pasted run URL looks like, since the run
        // route is reachable only THROUGH the list.
        const row = readHandoverRow(this.#location.getState());
        if (row) {
            this.store.openSelectedPage(row);
        } else {
            this.#toPageList();
        }

        // Redraw both frames' marker layers whenever their scans (or the phase)
        // change. Each frame gets its OWN scan's findings: the preview frame from
        // the primary/working scan (a11yGroups), the live frame from the
        // comparison scan (liveA11yGroups). We run separate scans, so a fix that
        // isn't published yet clears the preview markers while the live markers
        // (still-published violations) remain.
        effect(() => {
            const show = this.$showMarkers();
            const previewGroups = this.store.a11yGroups();
            const liveGroups = this.store.liveA11yGroups();
            this.#markerService.render(
                this.$previewFrame()?.nativeElement,
                show ? previewGroups : []
            );
            this.#markerService.render(this.$liveFrame()?.nativeElement, show ? liveGroups : []);
        });

        // Roll the ring count up to the live open-count whenever it changes and a
        // scan has produced results — the score animates in sync with the donut
        // sweep. Before any results (ready/scanning) it stays parked at 0, so each
        // scan / rescan rolls up fresh.
        effect(() => {
            const target = this.store.openCount();
            const scanned = this.store.hasResults();
            untracked(() => this.#animateCountTo(scanned ? target : 0));
        });

        // Cancel any in-flight count-up when the component is torn down.
        this.#destroyRef.onDestroy(() => this.#cancelCount());
    }

    /**
     * Ease {@link displayCount} from its current value to `target` over ~600ms
     * (easeOutCubic), synced with the donut's sweep. Snaps immediately when the
     * user prefers reduced motion or the delta is trivial.
     */
    #animateCountTo(target: number): void {
        this.#cancelCount();

        const from = this.$displayCount();
        if (from === target) {
            return;
        }
        const reduceMotion =
            typeof matchMedia === 'function' &&
            matchMedia('(prefers-reduced-motion: reduce)').matches;
        if (reduceMotion) {
            this.$displayCount.set(target);

            return;
        }

        const duration = 600;
        let start: number | null = null;
        const step = (now: number) => {
            start ??= now;
            const t = Math.min(1, (now - start) / duration);
            const eased = 1 - Math.pow(1 - t, 3); // easeOutCubic
            this.$displayCount.set(Math.round(from + (target - from) * eased));
            if (t < 1) {
                this.#countRaf = requestAnimationFrame(step);
            } else {
                this.#countRaf = null;
            }
        };
        this.#countRaf = requestAnimationFrame(step);
    }

    #cancelCount(): void {
        if (this.#countRaf !== null) {
            cancelAnimationFrame(this.#countRaf);
            this.#countRaf = null;
        }
    }

    /**
     * Navigate to the page-list route.
     *
     * Absolute, NOT relative: the run screen is the `**` route (the page path is
     * multi-segment, so it can't be a single param), and Angular's `..` drops one URL
     * SEGMENT rather than one route level. From `/agents/a11y/about-us/index` a `..`
     * yields `/agents/a11y/about-us`, which matches `**` again — the same component is
     * reused and the screen never changes.
     */
    #toPageList(): void {
        this.#router.navigate([A11Y_PAGE_LIST_ROUTE]).catch(() => {
            // Navigation cancelled (guard or a newer navigation superseded this
            // one). Nothing to recover — the router owns where we ended up.
        });
    }

    /**
     * Re-entrancy guard for scroll mirroring: setting frame B's scroll fires B's
     * own `scroll` event, which would mirror straight back to A — an infinite
     * bounce. While we're programmatically scrolling the target, ignore its echo.
     */
    #syncingScroll = false;

    /**
     * LIVE iframe finished (re)loading — (re)draw its markers from the LIVE
     * (comparison) scan + (re)wire scroll sync. A load replaces the document, so
     * the effect-drawn layer is gone and must be redrawn here.
     */
    onLiveLoad(): void {
        this.#markerService.render(
            this.$liveFrame()?.nativeElement,
            this.$showMarkers() ? this.store.liveA11yGroups() : []
        );
        this.#wireScrollSync(this.$liveFrame(), this.$previewFrame());
    }

    /**
     * PREVIEW iframe finished (re)loading — (re)draw its markers from the primary
     * (working) scan + (re)wire scroll sync.
     */
    onPreviewLoad(): void {
        this.#markerService.render(
            this.$previewFrame()?.nativeElement,
            this.$showMarkers() ? this.store.a11yGroups() : []
        );
        this.#wireScrollSync(this.$previewFrame(), this.$liveFrame());
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
    #wireScrollSync(
        source: ElementRef<HTMLIFrameElement> | undefined,
        target: ElementRef<HTMLIFrameElement> | undefined
    ): void {
        const srcWin = this.#frameWindow(source);
        if (!srcWin) {
            return;
        }
        srcWin.addEventListener(
            'scroll',
            () => {
                if (this.#syncingScroll) {
                    return;
                }
                const tgtWin = this.#frameWindow(target);
                if (!tgtWin) {
                    return;
                }
                this.#syncingScroll = true;
                tgtWin.scrollTo(srcWin.scrollX, srcWin.scrollY);
                // Release after the target's echoed scroll event has fired.
                requestAnimationFrame(() => (this.#syncingScroll = false));
            },
            { passive: true }
        );
    }

    /** The iframe's window; null when cross-origin or not yet loaded. */
    #frameWindow(frame: ElementRef<HTMLIFrameElement> | undefined): Window | null {
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
    readonly $showMarkers = computed<boolean>(() => this.store.hasResults());

    /**
     * The section-header label above the scrollable body, by phase:
     *   scanning → "SCAN", scanned → "BY ISSUE TYPE", fixing/done → "AGENT ACTIVITY".
     */
    readonly $logHeaderKey = computed<string>(() => {
        if (this.store.phase() === 'scanning') {
            return 'accessibility.studio.loghdr.scan';
        }
        if (this.store.phase() === 'scanned') {
            return 'accessibility.studio.loghdr.issues';
        }
        return 'accessibility.studio.loghdr.activity';
    });

    /** The working badge label beside the header ("SCANNING" / "WORKING"), or null. */
    readonly $logBadgeKey = computed<string | null>(() => {
        if (this.store.phase() === 'scanning') {
            return 'accessibility.studio.badge.scanning';
        }
        if (this.store.phase() === 'fixing') {
            return 'accessibility.studio.badge.working';
        }
        return null;
    });

    /** Headline above the severity legend, by phase. */
    readonly $scoreHeadlineKey = computed<string>(() => {
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
    readonly $severityRows = computed<SeverityRow[]>(() => {
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
    readonly $issueTypeRows = computed(() =>
        this.store.issueTypeRows().map((group) => ({
            ...group,
            color: SEVERITY_COLOR[impactToSeverity(group.impact)]
        }))
    );

    /** Needs-review rows with their "why a human is needed" i18n key resolved. */
    readonly $reviewRows = computed(() =>
        this.store.reviewGroups().map((group) => ({
            ...group,
            reasonKey:
                REVIEW_REASON_KEYS[group.code] ?? 'accessibility.studio.review.reason.default'
        }))
    );

    /** PrimeNG doughnut data — one arc per severity, colored by SEVERITY_COLOR. */
    readonly $donutData = computed(() => {
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
    readonly $activityMessages = computed<AgentMessage[]>(() => {
        if (this.store.phase() === 'fixing') {
            return this.store.steps().map((step, i) => this.#presenter.liveStep(step, i));
        }
        if (this.store.finished()) {
            const report = this.store.report();
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
    readonly $workingMessage = computed<AgentMessage | null>(() => {
        if (this.store.phase() !== 'fixing') {
            return null;
        }
        const sinceLastEventMs = this.store.heartbeat()?.sinceLastEventMs ?? 0;

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

    /** Footer title + sub keys derived from the current phase — single switch. */
    readonly $footerKeys = computed(() => {
        const p = this.store.phase();
        const base = `accessibility.studio.footer.${p}`;
        return { titleKey: `${base}.title`, subKey: `${base}.sub` };
    });

    /** Interpolation args for the footer title, by phase. */
    readonly $footerArgs = computed<string[]>(() => {
        switch (this.store.phase()) {
            case 'scanned':
                return [this.store.openCount().toString()];
            case 'fixing':
            case 'done':
            case 'published':
                return [this.store.fixedCount().toString(), this.store.reportedCount().toString()];
            default:
                return [];
        }
    });

    /** Small leading icon + bubble color for the footer copy, by phase. */
    readonly $footerIcon = computed<{ icon: string; cls: string } | null>(() => {
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
     *
     * NOTE: this pairs with the `/dot-page` rule in proxy-dev.conf.mjs — the two
     * must change together, or the preview frames 404 in local dev.
     *
     * Same-origin is a hard requirement, not a convenience: the marker overlay and
     * the scroll sync both reach into each frame's `contentWindow`
     * ({@link frameWindow}), which the browser forbids cross-origin. A cross-origin
     * frame would still render the page but silently draw no violation markers.
     *
     * NEEDS A BACKEND FIX: the proper solution is a first-class, same-origin dotCMS
     * endpoint that renders a page for inspection (a supported resource under
     * `/api`), so this component — and any future agent that has to inspect a
     * rendered page — can frame it directly with no origin games and no dev-server
     * rewrite. No such endpoint exists today; that gap is why the sentinel + proxy
     * pair exists. Delete both once it lands.
     */
    readonly #previewPathPrefix = isDevMode() ? '/dot-page' : '';

    /**
     * The page rendered in the given mode. `host_id` disambiguates which site's
     * copy renders. Shared by the two side-by-side frames (§8.2). An optional
     * cache-busting `rev` forces the iframe to reload when the working render
     * changes (see {@link previewUrl}).
     */
    #urlFor(mode: 'PREVIEW_MODE' | 'LIVE', rev = 0): string {
        const page = this.store.selected();
        if (!page) {
            return '';
        }
        const path = page.path.startsWith('/') ? page.path : `/${page.path}`;
        const bust = rev > 0 ? `&rev=${rev}` : '';
        return `${this.#previewPathPrefix}${path}?host_id=${page.hostId}&language_id=${page.languageId}&mode=${mode}${bust}`;
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
    readonly $liveUrl = computed(() => this.#urlFor('LIVE'));
    readonly $previewUrl = computed(() =>
        this.#urlFor('PREVIEW_MODE', this.store.previewRevision())
    );

    /**
     * Back button / "All pages" — return to the page list. No store reset needed: the
     * run store is provided at this component, so navigating away destroys it and
     * the next run starts fresh.
     */
    backToPageList(): void {
        this.#toPageList();
    }

    /**
     * Whether a given side panel is expanded. `p-accordion` renders the panel bodies
     * itself off the same two-way-bound `openPanels`, so nothing in the template needs
     * this — it's the readable way to assert open state from tests.
     */
    isPanelOpen(panel: StudioPanel): boolean {
        return this.$openPanels().includes(panel);
    }

    /**
     * Ensure a panel is open — used to jump the user to the files list. Not a toggle:
     * pressing "Review files" twice must not close the panel it just opened.
     */
    openPanel(panel: StudioPanel): void {
        this.$openPanels.update((current) =>
            current.includes(panel) ? current : [...current, panel]
        );
    }

    /** A file was picked in (or cleared from) the changed-files list. */
    onDiffFileSelected(file: PageDiffFile | null): void {
        this.$diffFile.set(file);
    }

    /** The changed-files list reports how many files differ, for the Publish gate. */
    onChangedFilesCount(count: number): void {
        this.$changedFileCount.set(count);
    }

    /**
     * Leave the diff view from the right pane's own control. The accordion's
     * highlighted row follows via its `activeFileId` input, so the two stay in sync.
     */
    closeDiff(): void {
        this.$diffFile.set(null);
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

/**
 * The page row the list hands over in the navigation's `state`, or null.
 *
 * Validated rather than cast: `history.state` is external input — it survives a reload,
 * and anything can put anything there via `history.pushState`. The three fields checked
 * are the ones the run screen cannot work without: `identifier` targets the fix,
 * `hostId` scopes the scan URL, and `languageId` selects the version to read. A partial
 * object reaching `openSelectedPage` would scan the wrong page, or a nonexistent one,
 * with no error to explain it — bouncing to the list is the recoverable outcome.
 */
function readHandoverRow(state: unknown): StudioPageRow | null {
    if (!state || typeof state !== 'object') {
        return null;
    }

    const row = (state as { row?: unknown }).row;
    if (!row || typeof row !== 'object') {
        return null;
    }

    const { identifier, hostId, languageId } = row as Partial<StudioPageRow>;

    return typeof identifier === 'string' &&
        identifier.length > 0 &&
        typeof hostId === 'string' &&
        hostId.length > 0 &&
        typeof languageId === 'number'
        ? (row as StudioPageRow)
        : null;
}
