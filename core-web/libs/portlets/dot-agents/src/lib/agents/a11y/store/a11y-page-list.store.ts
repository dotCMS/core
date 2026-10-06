import { patchState, signalStore, withHooks, withMethods, withState } from '@ngrx/signals';
import { EMPTY } from 'rxjs';

import { effect, inject, untracked } from '@angular/core';

import { catchError, take } from 'rxjs/operators';

import {
    DotContentSearchService,
    DotHttpErrorManagerService,
    DotLanguagesService
} from '@dotcms/data-access';
import { DotCMSContentlet, DotLanguage, ESContent } from '@dotcms/dotcms-models';
import { GlobalStore, SubscriptionSlot } from '@dotcms/store';

import { StudioPageRow } from '../models/accessibility-studio.models';

type PageListStatus = 'init' | 'loading' | 'loaded' | 'error';

/** dotCMS's own default language id — the fallback when the lookup fails. */
const DEFAULT_LANGUAGE_ID = 1;

/**
 * The language the page list scopes to: the instance default.
 *
 * Explicitly the one flagged `defaultLanguage`, NOT simply the first returned — the
 * endpoint's order is not a contract, and silently listing a non-default language would
 * be invisible on a screen that renders no language column. Falls back to the first
 * entry, then to dotCMS's own default id, so a missing flag still yields a usable list.
 */
export function pickDefaultLanguageId(languages: DotLanguage[] | null | undefined): number {
    if (!languages?.length) {
        return DEFAULT_LANGUAGE_ID;
    }

    return languages.find((language) => language.defaultLanguage)?.id ?? languages[0].id;
}

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
    /**
     * The language the list is scoped to — the instance default, resolved once on init.
     * Null until it lands, which gates the first load the same way `currentSiteId` does.
     *
     * Scoping to ONE language is what stops a multilingual site returning the same page
     * once per language. Default rather than user-selectable because the table renders no
     * language column; surfacing a language picker (and a column) is the follow-up that
     * would let a user reach a non-default translation from this screen.
     */
    languageId: number | null;
    pageListStatus: PageListStatus;
}

const initialState: A11yPageListState = {
    pages: [],
    totalRecords: 0,
    page: 1,
    rows: 25,
    filter: '',
    languageId: null,
    pageListStatus: 'init'
};

/**
 * Escape a user-typed term for use inside a Lucene clause.
 *
 * Two separate jobs. The character class escapes the metacharacters that would
 * otherwise change the query's structure. The whitespace collapse then matters just as
 * much: a field-qualified term ends at the first space, so an unquoted `about us` in
 * `path:*about us*` parses as `path:*about` followed by a bare `us*` against the DEFAULT
 * field — which quietly returns any content starting with "us" (users, usage) and never
 * applies the intended path wildcard to the full phrase. Spaces become `?`, Lucene's
 * single-character wildcard, keeping the whole phrase inside one term.
 */
function escapeLuceneTerm(term: string): string {
    return term.replace(/[+\-&|!(){}[\]^"~*?:\\/]/g, '\\$&').replace(/\s+/g, '?');
}

/**
 * Builds the Lucene query for the page list — pages (`basetype:5`) plus URL-mapped
 * content, working + not deleted, scoped to the current host and language. Search adds
 * a title / path / urlmap prefix clause.
 *
 * `languageId` is not optional in practice: without it a multilingual site returns one
 * row PER LANGUAGE for the same page, identical in the table (which renders no language
 * column), so the "N of M" count doubles and which row the user clicks is arbitrary —
 * they can scan and fix the Spanish page believing it's the English one.
 */
function buildPagesQuery(filter: string, siteId: string | null, languageId: number): string {
    const clauses = [
        '+working:true',
        '+(urlmap:* OR basetype:5)',
        '+deleted:false',
        `+languageId:${languageId}`
    ];

    if (siteId) {
        clauses.push(`+conhost:${siteId}`);
    }

    const q = filter.trim();
    if (q) {
        const safe = escapeLuceneTerm(q);
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
        const languagesService = inject(DotLanguagesService);
        const httpErrorManager = inject(DotHttpErrorManagerService);
        const globalStore = inject(GlobalStore);

        // The in-flight search. Held so a newer query supersedes an older one: typing
        // "blog" then paging is two overlapping POSTs, and if the FIRST resolves last it
        // overwrites `pages` and `totalRecords` while the paginator shows the newer page,
        // so the table and paginator disagree and a row click opens a page the user did
        // not select. Cancelling on destroy also stops `patchState` running against a
        // destroyed store when the user navigates away mid-search.
        const activeSearch = new SubscriptionSlot();
        /** The one-shot default-language lookup; cancelled on destroy like the search. */
        const languageLoad = new SubscriptionSlot();

        function loadPages() {
            const siteId = globalStore.currentSiteId();
            const languageId = store.languageId();
            // Both load asynchronously (GlobalStore → auth → HTTP; languages → HTTP).
            // Until they're known a fetch would be unscoped — every site, every language —
            // so skip. The reload effect tracks both and re-runs once they resolve, so
            // this fires exactly once, fully scoped.
            if (!siteId || !languageId) {
                return;
            }
            patchState(store, { pageListStatus: 'loading' });

            const query = buildPagesQuery(store.filter(), siteId, languageId);
            const offset = (store.page() - 1) * store.rows();

            activeSearch.set(
                contentSearchService
                    // `ESContent` rather than an inline restatement of the envelope: the
                    // inline copy omitted `contentTook`/`queryTook`, so wanting query timing
                    // later would mean a second partial copy instead of one shared type.
                    .get<ESContent>({
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
                    })
            );
        }

        return {
            loadPages,

            /**
             * Resolve the instance's default language, which scopes every query. On
             * failure fall back to id 1 (dotCMS's default) rather than blocking the
             * screen: a wrong-but-plausible language still lists pages, where a null
             * would leave the list permanently empty with no explanation.
             */
            loadDefaultLanguage() {
                languageLoad.set(
                    languagesService
                        .get()
                        .pipe(
                            take(1),
                            catchError(() => {
                                patchState(store, { languageId: DEFAULT_LANGUAGE_ID });

                                return EMPTY;
                            })
                        )
                        .subscribe((languages) => {
                            patchState(store, { languageId: pickDefaultLanguageId(languages) });
                        })
                );
            },

            setFilter(filter: string) {
                patchState(store, { filter, page: 1 });
            },

            setPagination(page: number, rows: number) {
                patchState(store, { page, rows });
            },

            /** Cancel the in-flight search + language lookup (see the `onDestroy` hook). */
            teardown() {
                activeSearch.cancel();
                languageLoad.cancel();
            }
        };
    }),
    withHooks((store) => {
        return {
            onInit() {
                const globalStore = inject(GlobalStore);

                // Every query is language-scoped, so resolve the default language once
                // up front; the reload effect below tracks it and fires the first load.
                store.loadDefaultLanguage();

                // Reset pagination when the site changes; pages are per-site.
                effect(() => {
                    globalStore.currentSiteId();
                    untracked(() => patchState(store, { page: 1 }));
                });

                // Reload the list on query / pagination / site / language changes. This
                // store only exists on the page-list route, so it always loads (no phase
                // gate).
                effect(() => {
                    store.filter();
                    store.page();
                    store.rows();
                    store.languageId();
                    globalStore.currentSiteId();

                    untracked(() => store.loadPages());
                });
            },

            /**
             * Cancel in-flight requests when the store is destroyed (route navigation),
             * so a late response can't `patchState` a store that no longer exists.
             */
            onDestroy() {
                store.teardown();
            }
        };
    })
);
