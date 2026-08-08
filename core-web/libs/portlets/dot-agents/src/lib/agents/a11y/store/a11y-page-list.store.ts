import { patchState, signalStore, withHooks, withMethods, withState } from '@ngrx/signals';
import { EMPTY } from 'rxjs';

import { effect, inject, untracked } from '@angular/core';

import { catchError, take } from 'rxjs/operators';

import { DotContentSearchService, DotHttpErrorManagerService } from '@dotcms/data-access';
import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';

import { StudioPageRow } from '../models/accessibility-studio.models';

type PageListStatus = 'init' | 'loading' | 'loaded' | 'error';

interface A11yPageListState {
    /** The page rows for the current query + page. */
    pages: StudioPageRow[];
    /** Total matches for the current query (drives the paginator). */
    totalRecords: number;
    /** 1-based page number. */
    page: number;
    /** Rows per page. */
    rows: number;
    /** Free-text search term (title / path / urlmap prefix). */
    filter: string;
    pageListStatus: PageListStatus;
}

const initialState: A11yPageListState = {
    pages: [],
    totalRecords: 0,
    page: 1,
    rows: 25,
    filter: '',
    pageListStatus: 'init'
};

/**
 * Builds the Lucene query for the page list — pages (`basetype:5`) plus URL-mapped
 * content, working + not deleted, scoped to the current host. Search adds a
 * title / path / urlmap prefix clause.
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

/** Projects a search contentlet into the page-list row shape. */
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

/**
 * The Accessibility Studio **page list** store — owns only the page-list screen
 * (`agents/a11y`): the searchable, paginated list of pages to run against. It's
 * provided at {@link DotA11yPageListComponent}, so it lives and dies with that
 * route and never shares state with a run.
 *
 * Selecting a page navigates to the run route; the run screen reads the page from
 * the URL and drives its own {@link A11yRunStore}. This store never holds run
 * state (no selected page, no scan/fix/report) — that split is the whole point.
 */
export const A11yPageListStore = signalStore(
    withState<A11yPageListState>(initialState),
    withMethods((store) => {
        const contentSearchService = inject(DotContentSearchService);
        const httpErrorManager = inject(DotHttpErrorManagerService);
        const globalStore = inject(GlobalStore);

        function loadPages() {
            const siteId = globalStore.currentSiteId();
            // The current site loads asynchronously (GlobalStore → auth → HTTP). Until
            // it's known, a fetch would be unscoped (`+conhost` omitted) and return
            // pages from every site. Skip; the reload effect re-runs once the site
            // resolves (it tracks currentSiteId), so this fires exactly once, scoped.
            if (!siteId) {
                return;
            }
            patchState(store, { pageListStatus: 'loading' });

            const query = buildPagesQuery(store.filter(), siteId);
            const offset = (store.page() - 1) * store.rows();

            contentSearchService
                .get<{ jsonObjectView: { contentlets: DotCMSContentlet[] }; resultsSize: number }>({
                    query,
                    limit: store.rows(),
                    offset,
                    sort: 'modDate desc'
                })
                .pipe(
                    take(1),
                    catchError((error) => {
                        httpErrorManager.handle(error);
                        patchState(store, { pageListStatus: 'error' });

                        return EMPTY;
                    })
                )
                .subscribe((entity) => {
                    const contentlets = entity?.jsonObjectView?.contentlets ?? [];
                    patchState(store, {
                        pages: contentlets.map(toPageRow),
                        totalRecords: entity?.resultsSize ?? 0,
                        pageListStatus: 'loaded'
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
                    untracked(() => patchState(store, { page: 1 }));
                });

                // Reload the list on query / pagination / site changes. This store
                // only exists on the page-list route, so it always loads (no phase gate).
                effect(() => {
                    store.filter();
                    store.page();
                    store.rows();
                    globalStore.currentSiteId();

                    untracked(() => store.loadPages());
                });
            }
        };
    })
);
