import {
    patchState,
    signalStore,
    withComputed,
    withHooks,
    withMethods,
    withState
} from '@ngrx/signals';
import { EMPTY, Subscription } from 'rxjs';

import { computed, effect, inject, untracked } from '@angular/core';

import { catchError, take } from 'rxjs/operators';

import { DotContentSearchService, DotHttpErrorManagerService } from '@dotcms/data-access';
import {
    AgentChangedFile,
    AgentHeartbeat,
    AgentProgress,
    AgentRunStep,
    DotCMSContentlet
} from '@dotcms/dotcms-models';
import { DotPageScannerService, PageScannerA11yResponse } from '@dotcms/portlets/dot-ema/ui';
import { GlobalStore } from '@dotcms/store';

import { A11yGroup, buildA11yGroups } from '../models/a11y-groups';
import {
    impactToSeverity,
    SEVERITY_ORDER,
    severityBreakdown,
    type SeverityCounts
} from '../models/a11y-severity';
import {
    AgentFixRequest,
    FixReport,
    FixResult,
    StudioPageRow,
    StudioPhase
} from '../models/accessibility-studio.models';
import { DotA11yAgentService } from '../services/dot-a11y-agent.service';

type PickerStatus = 'init' | 'loading' | 'loaded' | 'error';

/**
 * Status of rehydrating the selected page from a deep link (`/agents/a11y/:id`
 * opened cold): `loading` while the single-page fetch is in flight, `not-found`
 * when the id resolves to no page (so the run screen can bounce back to the
 * picker), `idle` otherwise.
 */
type RehydrateStatus = 'idle' | 'loading' | 'not-found';

interface AccessibilityStudioState {
    /** Studio state machine (§7). */
    phase: StudioPhase;
    /** Picker data + query state. */
    pages: StudioPageRow[];
    totalRecords: number;
    page: number;
    rows: number;
    filter: string;
    pickerStatus: PickerStatus;
    /** The page selected to run against. */
    selected: StudioPageRow | null;
    /** Deep-link rehydration status for {@link openPageById}. */
    rehydrateStatus: RehydrateStatus;
    /** Per-run opt-out: when true, the agent reports CSS contrast instead of fixing it (§3). */
    skipCss: boolean;
    /**
     * The real axe scan result of the PREVIEW (working) render — populated by
     * runScan() via DotPageScannerService. This is the scan that owns the whole
     * UI: score donut, issue list, fix flow, and the preview-frame markers.
     */
    scanResult: PageScannerA11yResponse | null;
    /**
     * The axe scan result of the LIVE (published) render. Used ONLY for the
     * side-by-side comparison — it draws the live-frame marker layer so the user
     * sees the violations that still exist on the published page (which may lag
     * the preview when fixes aren't published yet). Feeds no other widget.
     */
    liveScanResult: PageScannerA11yResponse | null;
    /** Live agent activity log — appended from SSE `phase` events during a fix run. */
    steps: AgentRunStep[];
    /**
     * Live violation count from SSE `progress` events — the authoritative running
     * score while fixing (baseline → current, cleared so far). Null until the
     * first progress frame arrives.
     */
    progress: AgentProgress | null;
    /**
     * Files the agent has changed in the working version so far — accumulated from
     * SSE `workingChanged` events during the run (each frame carries the full set,
     * so we replace rather than append). Confirmed by the terminal report.
     */
    changedFiles: AgentChangedFile[];
    /**
     * The current run's id — captured from the stream's first `run` event, used to
     * target the /stop request at this specific run. Null when no run is active.
     */
    runId: string | null;
    /**
     * Latest keep-alive tick from SSE `heartbeat` events — how long the run and the
     * current action have been going. Drives the "still working…" indicator so a
     * long, quiet step (a model call) doesn't look hung. Null between runs.
     */
    heartbeat: AgentHeartbeat | null;
    /** Set when a fix run fails — surfaced inline so the user can retry. */
    fixError: string | null;
    /** The §6 run report — populated when the fix pass completes (SSE `done`). */
    report: FixReport | null;
}

const initialState: AccessibilityStudioState = {
    phase: 'picker',
    pages: [],
    totalRecords: 0,
    page: 1,
    rows: 25,
    filter: '',
    pickerStatus: 'init',
    selected: null,
    rehydrateStatus: 'idle',
    skipCss: false,
    scanResult: null,
    liveScanResult: null,
    steps: [],
    progress: null,
    changedFiles: [],
    runId: null,
    heartbeat: null,
    fixError: null,
    report: null
};

/**
 * Builds the Lucene query for the picker — pages (`basetype:5`) plus URL-mapped
 * content, working + not deleted, scoped to the current host. Search adds a
 * title / path / urlmap prefix clause. Mirrors the §7 picker query.
 */
function buildPagesQuery(filter: string, siteId: string | null): string {
    const clauses = ['+working:true', '+(urlmap:* OR basetype:5)', '+deleted:false'];

    if (siteId) {
        clauses.push(`+conhost:${siteId}`);
    }

    const q = filter.trim();
    if (q) {
        // Escape Lucene special characters that would break the query.
        const safe = q.replace(/[+\-&|!(){}[\]^"~*?:\\/]/g, '\\$&');
        clauses.push(`+(title:${safe}* OR path:*${safe}* OR urlmap:*${safe}*)`);
    }

    return clauses.join(' ');
}

/** Projects a search contentlet into the picker row shape. */
function toPageRow(content: DotCMSContentlet): StudioPageRow {
    return {
        identifier: content.identifier,
        title: content.title || content.url || content.identifier,
        // Prefer the urlMap for URL-mapped content (e.g. Blog): it's the real
        // navigable path visitors use, whereas `url` may point at the detail
        // template. Fall back to `url` for plain pages (no urlMap).
        path: content['urlMap'] || content.url || '',
        type: content.contentType,
        languageId: content.languageId,
        hostId: content.host,
        hostName: content.hostName,
        modDate: content.modDate,
        modUserName: content.modUserName,
        live: !!content.live
    };
}

export const AccessibilityStudioStore = signalStore(
    withState<AccessibilityStudioState>(initialState),
    withComputed((store) => ({
        inPicker: computed(() => store.phase() === 'picker'),
        inStudio: computed(() => store.phase() !== 'picker'),
        isReady: computed(() => store.phase() === 'ready'),
        isScanning: computed(() => store.phase() === 'scanning'),
        isScanned: computed(() => store.phase() === 'scanned'),
        isFixing: computed(() => store.phase() === 'fixing'),
        isDone: computed(() => store.phase() === 'done'),
        isPublished: computed(() => store.phase() === 'published'),
        isWorking: computed(() => store.phase() === 'scanning' || store.phase() === 'fixing'),
        /** True once a scan has produced (or is producing) results. */
        scanned: computed(() =>
            ['scanned', 'fixing', 'done', 'published'].includes(store.phase())
        ),
        /** Real axe findings grouped per rule (violations → error, incomplete → warning). */
        a11yGroups: computed<A11yGroup[]>(() => buildA11yGroups(store.scanResult())),
        /**
         * The LIVE (published) render's findings, grouped per rule — drives ONLY the
         * live-frame marker layer for the side-by-side comparison. Empty until the
         * live scan lands (it runs alongside the preview scan on Scan / Re-scan).
         */
        liveA11yGroups: computed<A11yGroup[]>(() => buildA11yGroups(store.liveScanResult())),
        /** Real axe error-element count (confirmed violations). */
        errorCount: computed(() =>
            buildA11yGroups(store.scanResult())
                .filter((g) => g.type === 'error')
                .reduce((total, g) => total + g.count, 0)
        ),
        /** Real axe warning-element count (incomplete / needs review). */
        warningCount: computed(() =>
            buildA11yGroups(store.scanResult())
                .filter((g) => g.type === 'warning')
                .reduce((total, g) => total + g.count, 0)
        ),
        /**
         * Axe `incomplete` groups (needs manual review) — one per rule, sorted by
         * occurrence count. The agent doesn't fix these (axe couldn't confirm them),
         * so the panel lists them separately with an explanation.
         */
        reviewGroups: computed<A11yGroup[]>(() =>
            buildA11yGroups(store.scanResult())
                .filter((g) => g.type === 'warning')
                .sort((a, b) => b.count - a.count)
        ),
        /** Total violations found by the real initial scan (error elements). */
        beforeCount: computed(() =>
            buildA11yGroups(store.scanResult())
                .filter((g) => g.type === 'error')
                .reduce((total, g) => total + g.count, 0)
        ),
        /** Violations remaining after the fix pass. */
        afterCount: computed(() => store.report()?.scan.after.violations ?? 0),
        fixedResults: computed<FixResult[]>(
            () => store.report()?.results.filter((r) => r.status === 'fixed-to-working') ?? []
        ),
        reportedResults: computed<FixResult[]>(
            () =>
                store
                    .report()
                    ?.results.filter((r) =>
                        ['reported', 'skipped', 'regressed', 'failed'].includes(r.status)
                    ) ?? []
        ),
        latestStep: computed<AgentRunStep | null>(() => {
            const steps = store.steps();
            return steps.length ? steps[steps.length - 1] : null;
        }),
        /**
         * Confirmed-violation groups (axe `error`s), one per rule, sorted for the
         * "BY ISSUE TYPE" list: highest severity first, then most occurrences.
         */
        issueTypeRows: computed<A11yGroup[]>(() => {
            const rank = (g: A11yGroup) => SEVERITY_ORDER.indexOf(impactToSeverity(g.impact));
            return buildA11yGroups(store.scanResult())
                .filter((g) => g.type === 'error')
                .sort((a, b) => rank(a) - rank(b) || b.count - a.count);
        }),
        /** Open issues broken down by severity (element counts) — drives the donut + legend. */
        severityCounts: computed<SeverityCounts>(() =>
            severityBreakdown(
                buildA11yGroups(store.scanResult()).filter((g) => g.type === 'error')
            )
        ),
        /**
         * Live "open" count for the score widget. After the run finishes it's the
         * report's authoritative after-count; while fixing it's the live count from
         * the agent's `progress` stream (`current`) so the donut animates down as
         * fixes land; before any run it's the scan's before-count.
         */
        openCount: computed<number>(() => {
            const report = store.report();
            if (report) {
                return report.scan.after.violations;
            }
            if (store.phase() === 'fixing') {
                // Authoritative live count straight from the agent's `progress`
                // events. Before the first progress frame lands, fall back to the
                // baseline (the scan's before-count).
                const progress = store.progress();
                if (progress) {
                    return Math.max(0, progress.current);
                }
            }
            const errorGroups = buildA11yGroups(store.scanResult()).filter(
                (g) => g.type === 'error'
            );
            return errorGroups.reduce((total, g) => total + g.count, 0);
        }),
        fixedCount: computed<number>(() =>
            store.report()?.results.filter((r) => r.status === 'fixed-to-working').length ?? 0
        ),
        reportedCount: computed<number>(
            () =>
                store
                    .report()
                    ?.results.filter((r) =>
                        ['reported', 'skipped', 'regressed', 'failed'].includes(r.status)
                    ).length ?? 0
        )
    })),
    withMethods((store) => {
        const contentSearchService = inject(DotContentSearchService);
        const scannerService = inject(DotPageScannerService);
        const agentService = inject(DotA11yAgentService);
        const httpErrorManager = inject(DotHttpErrorManagerService);
        const globalStore = inject(GlobalStore);

        // The in-flight scan / fix-stream subscription, held so Stop can cancel it
        // (unsubscribing aborts the underlying fetch). Not reactive UI state.
        let activeSub: Subscription | null = null;
        // The comparison-only LIVE scan, tracked separately so Stop cancels it too
        // without coupling it to the UI-driving preview scan's lifecycle.
        let liveScanSub: Subscription | null = null;

        /**
         * The dotCMS backend origin the agent must render + call against. In prod the
         * portlet is served FROM the dotCMS origin, so `window.location.origin` is
         * already correct and the agent trusts it verbatim. The dev split below is the
         * ONLY adjustment — see backendOrigin().
         */
        function backendOrigin(): string {
            // ====================================================================
            // ⚠️ DEV-ONLY HACK — REMOVE BEFORE PRODUCTION ⚠️
            // --------------------------------------------------------------------
            // The Angular dev server runs on :4200, but the agent (and the dotCMS
            // scanner it drives) render/call the page server-side and can only reach
            // the BE on :8080. Rewrite the dev-server origin → the BE origin so the
            // agent receives a backend-reachable dotcmsBaseUrl. Mirrors the same
            // :4200→:8080 hack in DotPageScannerService.checkA11y. In prod there is
            // no split, so this is a no-op. MUST be replaced by an env-aware origin.
            // ====================================================================
            return window.location.origin.replace('4200', '8080');
        }

        /**
         * Build the absolute URL the scanner renders + checks. It must be on the
         * backend origin (never the content-site hostname, which may not be publicly
         * reachable) with `host_id` to disambiguate the site.
         *
         * `mode` selects which render to scan:
         *   - `EDIT_MODE` (default) — the working version. NOTE: DotPageScannerService
         *     rewrites EDIT_MODE → PREVIEW_MODE at its chokepoint (editor chrome would
         *     produce phantom violations), so this scans the PREVIEW render — the same
         *     one the left "with fixes" frame shows. This is the scan that owns the UI.
         *   - `LIVE` — the published render, for the comparison-only live scan. `LIVE`
         *     passes through the scanner untouched.
         * Mirrors DotEmaShellComponent.handleScannerToolClick.
         */
        function buildScanUrl(page: StudioPageRow, mode: 'EDIT_MODE' | 'LIVE' = 'EDIT_MODE'): string {
            const path = page.path.startsWith('/') ? page.path : `/${page.path}`;
            const url = new URL(path, backendOrigin());
            url.searchParams.set('host_id', page.hostId);
            url.searchParams.set('language_id', String(page.languageId));
            url.searchParams.set('mode', mode);
            return url.toString();
        }

        function loadPages() {
            const siteId = globalStore.currentSiteId();
            // The current site loads asynchronously (GlobalStore → auth → HTTP). Until
            // it's known, a fetch would be unscoped (`+conhost` omitted) and return
            // pages from every site. Skip; the picker effect re-runs once the site
            // resolves (it tracks currentSiteId), so this fires exactly once, scoped.
            if (!siteId) {
                return;
            }
            patchState(store, { pickerStatus: 'loading' });

            const query = buildPagesQuery(store.filter(), siteId);
            const offset = (store.page() - 1) * store.rows();

            contentSearchService
                .get<{ jsonObjectView: { contentlets: DotCMSContentlet[] }; resultsSize: number }>(
                    {
                        query,
                        limit: store.rows(),
                        offset,
                        sort: 'modDate desc'
                    }
                )
                .pipe(
                    take(1),
                    catchError((error) => {
                        httpErrorManager.handle(error);
                        patchState(store, { pickerStatus: 'error' });

                        return EMPTY;
                    })
                )
                .subscribe((entity) => {
                    const contentlets = entity?.jsonObjectView?.contentlets ?? [];
                    patchState(store, {
                        pages: contentlets.map(toPageRow),
                        totalRecords: entity?.resultsSize ?? 0,
                        pickerStatus: 'loaded'
                    });
                });
        }

        /** Open a page → studio "ready" (waits for the user to scan). */
        function openPage(selected: StudioPageRow) {
            patchState(store, {
                selected,
                phase: 'ready',
                rehydrateStatus: 'idle',
                scanResult: null,
                liveScanResult: null,
                steps: [],
                progress: null,
                changedFiles: [],
                runId: null,
                heartbeat: null,
                fixError: null,
                report: null
            });
        }

        return {
            loadPages,
            openPage,

            setFilter(filter: string) {
                patchState(store, { filter, page: 1 });
            },

            setPagination(page: number, rows: number) {
                patchState(store, { page, rows });
            },

            setSkipCss(skipCss: boolean) {
                patchState(store, { skipCss });
            },

            /**
             * Rehydrate the selected page from a deep link (`/agents/a11y/<path>`
             * opened cold or refreshed): fetch the single page by its URI, then
             * `openPage` it → run screen "ready". If the path resolves to no page,
             * flag `not-found` so the run screen can bounce back to the picker.
             *
             * A no-op when the requested page is already selected (in-session
             * navigation into the run route), so re-entering the URL doesn't refetch
             * or reset an in-progress run.
             *
             * Paths are unique per-site, so the lookup is host-scoped — matching the
             * picker query. Until the current site is known the lookup would be
             * ambiguous, so we wait (the run effect re-runs when the site resolves).
             */
            openPageByUri(uri: string) {
                if (store.selected()?.path === uri) {
                    return;
                }
                const siteId = globalStore.currentSiteId();
                if (!siteId) {
                    // Site not resolved yet — stay in loading; the caller re-runs.
                    patchState(store, { rehydrateStatus: 'loading' });

                    return;
                }
                patchState(store, { rehydrateStatus: 'loading' });

                // Escape Lucene special characters in the path (it comes from the URL).
                const safeUri = uri.replace(/[+\-&|!(){}[\]^"~*?:\\]/g, '\\$&');
                // Match the picker query shape (pages + urlmapped, working, not
                // deleted, host-scoped) but pinned to this exact path / urlmap.
                const query =
                    `+working:true +(urlmap:* OR basetype:5) +deleted:false ` +
                    `+conhost:${siteId} +(path:"${safeUri}" OR urlmap:"${safeUri}")`;

                contentSearchService
                    .get<{ jsonObjectView: { contentlets: DotCMSContentlet[] } }>({
                        query,
                        limit: 1,
                        offset: 0
                    })
                    .pipe(
                        take(1),
                        catchError((error) => {
                            httpErrorManager.handle(error);
                            patchState(store, { rehydrateStatus: 'not-found' });

                            return EMPTY;
                        })
                    )
                    .subscribe((entity) => {
                        const content = entity?.jsonObjectView?.contentlets?.[0];
                        if (!content) {
                            patchState(store, { rehydrateStatus: 'not-found' });

                            return;
                        }
                        // openPage resets rehydrateStatus → 'idle'.
                        openPage(toPageRow(content));
                    });
            },

            backToPicker() {
                patchState(store, {
                    phase: 'picker',
                    selected: null,
                    rehydrateStatus: 'idle',
                    scanResult: null,
                    liveScanResult: null,
                    steps: [],
                    progress: null,
                    changedFiles: [],
                    runId: null,
                    heartbeat: null,
                    fixError: null,
                    report: null
                });
            },

            /**
             * Run the REAL axe scan via DotPageScannerService against the page's
             * EDIT_MODE render, then store the result and move to "scanned". The
             * fix pass (startFix) is still mocked until the agent proxy lands (S4).
             */
            runScan() {
                const page = store.selected();
                // Allow scanning from `ready` (first scan) and `scanned` (the re-scan
                // button) — both transition into `scanning`.
                if ((store.phase() !== 'ready' && store.phase() !== 'scanned') || !page) {
                    return;
                }
                // Drop any prior scan/report so the widgets reflect the fresh scan.
                patchState(store, {
                    phase: 'scanning',
                    scanResult: null,
                    liveScanResult: null,
                    report: null,
                    fixError: null
                });

                // Primary scan — the PREVIEW (working) render. Owns the phase + all
                // widgets. Failure returns to `ready` so the user can retry.
                activeSub = scannerService
                    .checkA11y(buildScanUrl(page, 'EDIT_MODE'))
                    .pipe(
                        take(1),
                        catchError((error) => {
                            httpErrorManager.handle(error);
                            // Return to ready so the user can retry the scan.
                            patchState(store, { phase: 'ready' });

                            return EMPTY;
                        })
                    )
                    .subscribe((scanResult) => {
                        patchState(store, { scanResult, phase: 'scanned' });
                    });

                // Comparison-only scan — the LIVE (published) render. Draws the
                // live-frame markers and nothing else, so its failure must NOT touch
                // the phase or surface an error dialog: swallow it and leave the live
                // markers empty. Runs in parallel with the primary scan.
                liveScanSub = scannerService
                    .checkA11y(buildScanUrl(page, 'LIVE'))
                    .pipe(
                        take(1),
                        catchError(() => EMPTY)
                    )
                    .subscribe((liveScanResult) => {
                        patchState(store, { liveScanResult });
                    });
            },

            /** Cancel the in-flight scan (unsubscribe aborts the request) → back to ready. */
            stopScan() {
                if (store.phase() !== 'scanning') {
                    return;
                }
                activeSub?.unsubscribe();
                activeSub = null;
                // Cancel the parallel comparison scan too.
                liveScanSub?.unsubscribe();
                liveScanSub = null;
                patchState(store, { phase: 'ready' });
            },

            /**
             * Run the real fix pass: POST the page to the agent and stream its
             * progress over SSE. Each `phase` event appends to the live activity
             * log; `progress` updates the live violation count; `workingChanged`
             * tracks the files touched so far; `done` sets the §6 report and moves
             * to "done"; `error` returns to "scanned" so the user can retry. The
             * browser holds no token — the dev/prod proxy injects the bearer (see
             * DotA11yAgentService).
             */
            startFix() {
                const page = store.selected();
                if (store.phase() !== 'scanned' || !page) {
                    return;
                }
                patchState(store, {
                    phase: 'fixing',
                    steps: [],
                    progress: null,
                    changedFiles: [],
                    runId: null,
                    heartbeat: null,
                    fixError: null,
                    report: null
                });

                // The Java proxy (plan §8.1) resolves page details and builds the full
                // FixRequest; the Studio sends only the identifier, languageId, and skipCss.
                const request: AgentFixRequest = {
                    identifier: page.identifier,
                    languageId: page.languageId,
                    skipCss: store.skipCss()
                };

                activeSub = agentService
                    .fixStream(request)
                    .pipe(
                        catchError((error: unknown) => {
                            const message =
                                error instanceof Error ? error.message : 'The agent run failed.';
                            patchState(store, { phase: 'scanned', fixError: message });

                            return EMPTY;
                        })
                    )
                    .subscribe((event) => {
                        switch (event.type) {
                            case 'run':
                                // First frame: capture the run id so Stop can target it.
                                patchState(store, { runId: event.runId });
                                break;
                            // `step` is the legacy alias of `phase` — treat identically.
                            case 'phase':
                            case 'step':
                                patchState(store, { steps: [...store.steps(), event.step] });
                                break;
                            case 'progress':
                                // Live violation count → drives the score donut down.
                                patchState(store, { progress: event.progress });
                                break;
                            case 'workingChanged':
                                // Each frame carries the full set of changed files so
                                // far — replace, don't append.
                                patchState(store, { changedFiles: event.changedFiles });
                                break;
                            case 'heartbeat':
                                // Keep-alive while the agent is thinking between
                                // actions — drives the "still working…" indicator so a
                                // long, quiet step doesn't look hung.
                                patchState(store, { heartbeat: event.heartbeat });
                                break;
                            case 'done':
                            case 'aborted':
                                // done = full run; aborted = stopped early with a partial
                                // report (fixes already applied are kept). Both land on
                                // the done screen with the report the agent returned; sync
                                // the changed-file list to the report's authoritative set.
                                patchState(store, {
                                    phase: 'done',
                                    report: event.result,
                                    changedFiles: event.result.changedFiles ?? store.changedFiles()
                                });
                                break;
                            case 'error':
                                // Terminal error event from the agent.
                                patchState(store, {
                                    phase: 'scanned',
                                    fixError: event.message
                                });
                                break;
                            default:
                                // Exhaustive: any unhandled event type is ignored.
                                break;
                        }
                    });
            },

            /**
             * Stop the in-flight agent run. Tells the agent (by run id) to stop; it
             * returns a partial report via the stream's `aborted` event, keeping
             * fixes already applied. We keep the stream subscribed so that terminal
             * event still lands and moves us to the done screen. No-op if the run id
             * hasn't arrived yet (the agent hasn't announced the run).
             */
            stopAgent() {
                const runId = store.runId();
                if (store.phase() !== 'fixing' || !runId) {
                    return;
                }
                agentService.stop(runId).pipe(take(1), catchError(() => EMPTY)).subscribe();
            },

            /** Promote the working fixes to live (the only publish; human-triggered). */
            publish() {
                if (store.phase() !== 'done') {
                    return;
                }
                patchState(store, { phase: 'published' });
            },

            /** Discard the working fixes → back to the scanned (all-detected) state. */
            discard() {
                patchState(store, { phase: 'scanned' });
            }
        };
    }),
    withHooks((store) => {
        return {
            onInit() {
                const globalStore = inject(GlobalStore);

                // Reset pagination when the site changes; pages are per-site.
                effect(() => {
                    globalStore.currentSiteId();
                    untracked(() => patchState(store, { page: 1, selected: null }));
                });

                // Reload the picker list on query/pagination/site changes — only while
                // the picker is the active screen (don't refetch during a studio run).
                effect(() => {
                    store.filter();
                    store.page();
                    store.rows();
                    globalStore.currentSiteId();

                    untracked(() => {
                        if (store.phase() === 'picker') {
                            store.loadPages();
                        }
                    });
                });
            }
        };
    })
);
