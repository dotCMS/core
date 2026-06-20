import {
    patchState,
    signalStore,
    withComputed,
    withHooks,
    withMethods,
    withState
} from '@ngrx/signals';
import { EMPTY } from 'rxjs';

import { computed, effect, inject, untracked } from '@angular/core';

import { catchError, take } from 'rxjs/operators';

import { DotContentSearchService, DotHttpErrorManagerService } from '@dotcms/data-access';
import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { DotPageScannerService, PageScannerA11yResponse } from '@dotcms/portlets/dot-ema/ui';
import { GlobalStore } from '@dotcms/store';

import { A11yGroup, buildA11yGroups } from '../models/a11y-groups';
import {
    FixReport,
    FixResult,
    StudioPageRow,
    StudioPhase
} from '../models/accessibility-studio.models';
import { MOCK_FIX_REPORT } from '../models/mock-fix-report';

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
    /** The §6 run report — populated when the (mocked) fix pass completes. */
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
        )
    })),
    withMethods((store) => {
        const contentSearchService = inject(DotContentSearchService);
        const scannerService = inject(DotPageScannerService);
        const httpErrorManager = inject(DotHttpErrorManagerService);
        const globalStore = inject(GlobalStore);

        /**
         * Build the absolute URL the scanner renders + checks. It must be on the
         * runtime origin (`window.location.origin`) — never the content-site
         * hostname, which may not be publicly reachable — with `host_id` to
         * disambiguate the site and `mode=EDIT_MODE` for the working version (§8.2).
         * Mirrors DotEmaShellComponent.handleScannerToolClick.
         */
        function buildScanUrl(page: StudioPageRow): string {
            const path = page.path.startsWith('/') ? page.path : `/${page.path}`;
            const url = new URL(path, window.location.origin);
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
                patchState(store, { selected, phase: 'ready', scanResult: null, report: null });
            },

            backToPicker() {
                patchState(store, {
                    phase: 'picker',
                    selected: null,
                    scanResult: null,
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
                if (store.phase() !== 'ready' || !page) {
                    return;
                }
                patchState(store, { phase: 'scanning' });

                scannerService
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

            /**
             * Run the (mocked) fix pass. S3 has no SSE — this resolves to the done
             * state with the full mock §6 report. S4 streams real step events.
             */
            startFix() {
                if (store.phase() !== 'scanned') {
                    return;
                }
                patchState(store, { phase: 'fixing' });
                patchState(store, { phase: 'done', report: MOCK_FIX_REPORT });
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
