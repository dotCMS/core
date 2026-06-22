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
import { DotCMSContentlet } from '@dotcms/dotcms-models';
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
    StudioPhase,
    StudioStep
} from '../models/accessibility-studio.models';
import { DotA11yAgentService } from '../services/dot-a11y-agent.service';

type PickerStatus = 'init' | 'loading' | 'loaded' | 'error';

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
    /** Per-run opt-out: when true, the agent reports CSS contrast instead of fixing it (§3). */
    skipCss: boolean;
    /** The real axe scan result — populated by runScan() via DotPageScannerService. */
    scanResult: PageScannerA11yResponse | null;
    /** Live agent activity log — appended from SSE `step` events during a fix run. */
    steps: StudioStep[];
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
    skipCss: false,
    scanResult: null,
    steps: [],
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
        path: content.url ?? content['urlMap'] ?? '',
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
        /** Total violations found by the real initial scan (error elements). */
        beforeCount: computed(() =>
            buildA11yGroups(store.scanResult())
                .filter((g) => g.type === 'error')
                .reduce((total, g) => total + g.count, 0)
        ),
        /** Violations remaining after the (mocked) fix pass. */
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
        fixedCount: computed(
            () =>
                store.report()?.results.filter((r) => r.status === 'fixed-to-working').length ?? 0
        ),
        reportedCount: computed(
            () =>
                store
                    .report()
                    ?.results.filter((r) =>
                        ['reported', 'skipped', 'regressed', 'failed'].includes(r.status)
                    ).length ?? 0
        ),
        /** The most recent agent step — drives the headline status line while fixing. */
        latestStep: computed<StudioStep | null>(() => {
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
         * report's authoritative after-count; while fixing it's an optimistic
         * estimate (before − violations cleared so far) so the donut animates down
         * as fixes land; before any run it's the scan's before-count.
         */
        openCount: computed<number>(() => {
            const before = buildA11yGroups(store.scanResult())
                .filter((g) => g.type === 'error')
                .reduce((total, g) => total + g.count, 0);
            const report = store.report();
            if (report) {
                return report.scan.after.violations;
            }
            if (store.phase() === 'fixing') {
                const cleared = store
                    .steps()
                    .filter((s) => s.phase === 'fix' && /^Fixed |Added |Set |Wrapped |Named /.test(s.message))
                    .length;
                return Math.max(0, before - cleared);
            }
            return before;
        })
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
         * reachable) with `host_id` to disambiguate the site and `mode=EDIT_MODE` for
         * the working version (§8.2). Mirrors DotEmaShellComponent.handleScannerToolClick.
         */
        function buildScanUrl(page: StudioPageRow): string {
            const path = page.path.startsWith('/') ? page.path : `/${page.path}`;
            const url = new URL(path, backendOrigin());
            url.searchParams.set('host_id', page.hostId);
            url.searchParams.set('language_id', String(page.languageId));
            url.searchParams.set('mode', 'EDIT_MODE');
            return url.toString();
        }

        function loadPages() {
            patchState(store, { pickerStatus: 'loading' });

            const query = buildPagesQuery(store.filter(), globalStore.currentSiteId());
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

        return {
            loadPages,

            setFilter(filter: string) {
                patchState(store, { filter, page: 1 });
            },

            setPagination(page: number, rows: number) {
                patchState(store, { page, rows });
            },

            setSkipCss(skipCss: boolean) {
                patchState(store, { skipCss });
            },

            /** Open a page from the picker → studio "ready" (waits for the user to scan). */
            openPage(selected: StudioPageRow) {
                patchState(store, {
                    selected,
                    phase: 'ready',
                    scanResult: null,
                    steps: [],
                    fixError: null,
                    report: null
                });
            },

            backToPicker() {
                patchState(store, {
                    phase: 'picker',
                    selected: null,
                    scanResult: null,
                    steps: [],
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
                patchState(store, { phase: 'scanning', scanResult: null, report: null, fixError: null });

                activeSub = scannerService
                    .checkA11y(buildScanUrl(page))
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
            },

            /** Cancel the in-flight scan (unsubscribe aborts the request) → back to ready. */
            stopScan() {
                if (store.phase() !== 'scanning') {
                    return;
                }
                activeSub?.unsubscribe();
                activeSub = null;
                patchState(store, { phase: 'ready' });
            },

            /**
             * Run the real fix pass: POST the page to the agent and stream its
             * progress over SSE. Each `step` event appends to the live activity
             * log; `done` sets the §6 report and moves to "done"; `error` returns
             * to "scanned" so the user can retry. The browser holds no token — the
             * dev/prod proxy injects the bearer (see DotA11yAgentService).
             */
            startFix() {
                const page = store.selected();
                if (store.phase() !== 'scanned' || !page) {
                    return;
                }
                patchState(store, { phase: 'fixing', steps: [], fixError: null, report: null });

                const uri = page.path.startsWith('/') ? page.path : `/${page.path}`;
                const origin = backendOrigin();
                const request: AgentFixRequest = {
                    runId: `r_${page.identifier}_${page.languageId}`,
                    // The dotCMS backend origin — the agent renders the page (server-side
                    // scan) and calls the dotCMS API against this. In prod it equals the
                    // page origin; in dev backendOrigin() maps :4200 → :8080 (see above).
                    dotcmsBaseUrl: origin,
                    page: {
                        identifier: page.identifier,
                        uri,
                        liveUrl: new URL(uri, origin).toString(),
                        host: page.hostName,
                        hostId: page.hostId,
                        languageId: page.languageId
                    },
                    options: { skipCss: store.skipCss() }
                };

                let nextStepId = 0;
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
                        if (event.type === 'step') {
                            patchState(store, {
                                steps: [
                                    ...store.steps(),
                                    { id: nextStepId++, phase: event.phase, message: event.message }
                                ]
                            });
                        } else if (event.type === 'done' || event.type === 'aborted') {
                            // done = full run; aborted = stopped early with a partial
                            // report (fixes already applied are kept). Both land on the
                            // done screen with the report the agent returned.
                            patchState(store, { phase: 'done', report: event.report });
                        } else {
                            // Terminal error event from the agent.
                            patchState(store, { phase: 'scanned', fixError: event.message });
                        }
                    });
            },

            /**
             * Stop the in-flight agent run. Tells the agent to stop (it returns a
             * partial report via the stream's `aborted` event, keeping fixes already
             * applied). We keep the stream subscribed so that terminal event still
             * lands and moves us to the done screen.
             */
            stopAgent() {
                if (store.phase() !== 'fixing') {
                    return;
                }
                agentService.stop().pipe(take(1), catchError(() => EMPTY)).subscribe();
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
