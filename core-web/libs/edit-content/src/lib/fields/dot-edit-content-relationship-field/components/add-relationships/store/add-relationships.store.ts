import { tapResponse } from '@ngrx/operators';
import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { catchError, EMPTY, forkJoin, map, of, pipe, switchMap, tap } from 'rxjs';

import { computed, inject } from '@angular/core';

import { DotContentDriveService, DotLanguagesService } from '@dotcms/data-access';
import { SiteService } from '@dotcms/dotcms-js';
import {
    ComponentStatus,
    DotCMSContentlet,
    DotContentDriveItem,
    DotContentDriveSearchRequest,
    DotLanguage
} from '@dotcms/dotcms-models';

import {
    AddRelationshipsState,
    DotAddRelationshipsFilters,
    DotAddRelationshipsSort
} from './add-relationships.models';
import { ConstrainedIdentifiersService } from './constrained-identifiers.service';

import { needsCardinalityConstraintCheck } from '../../../utils';
import { AddRelationshipsInput } from '../models/add-relationships.models';

/**
 * Attaches the full {@link DotLanguage} to each row.
 *
 * `/drive/search` returns `languageId` only, while `LanguagePipe` — which the related-content table
 * renders the Locale column with — needs the object. Rows that reach the field without it show an
 * empty cell, which is what the editor saw.
 *
 * @param rows The page of results, as the endpoint returned it.
 * @param languages Every language in the instance.
 * @return The same rows, each contentlet carrying its language object when one matches. Rows with
 *   no language of their own — folders, which the union admits — are returned untouched.
 */
const withLanguages = (
    rows: DotContentDriveItem[],
    languages: DotLanguage[]
): DotContentDriveItem[] => {
    const byId = new Map(languages.map((language) => [language.id, language]));

    return rows.map((row) => {
        // `DotContentDriveItem` is a union with folders, which carry no language at all. The dialog
        // asks for `showFolders: false`, so none should arrive — but the type admits them, and
        // silently stamping `language: undefined` onto one would be a lie rather than a no-op.
        const languageId = (row as DotCMSContentlet).languageId;

        if (languageId == null) {
            return row;
        }

        return { ...row, language: byId.get(languageId) } as DotContentDriveItem;
    });
};

/**
 * Builds the `assetPath` the drive search resolves its host and folder from.
 *
 * Mandatory, and not merely a filter: `AssetPathResolver` parses it as a URI and throws
 * `can not resolve a valid hostName [null]` when the host is missing, before any content-type or
 * text filter is even considered.
 *
 * @param hostname The site to browse, or undefined when none could be resolved.
 * @param path Folder within that site, when the editor has narrowed to one.
 * @return A drive-search path such as `//demo.dotcms.com/` or `//demo.dotcms.com/blog/`.
 */
const buildAssetPath = (hostname?: string, path?: string): string => {
    // A hostname the backend cannot parse as a URI authority is not a browsable site. "System Host"
    // — the synthetic Shared Assets row — is the case in practice: it carries a space, and
    // `new URI("//System Host/")` throws `Illegal character in authority`, which surfaced as a 500
    // rather than as an empty result. Content Drive reaches the same conclusion its own way, by
    // skipping the fetch for SYSTEM_HOST outright.
    //
    // Checked by parsing rather than by matching the name: the rule is "the endpoint can resolve
    // this", not "this is the one site we know about".
    if (!hostname || !isBrowsableHost(hostname)) {
        return '';
    }

    const suffix = path && path !== '/' ? `/${path.replace(/^\/+|\/+$/g, '')}/` : '/';

    return `//${hostname}${suffix}`;
};

/**
 * Whether a hostname can appear in the authority of the URI the endpoint parses.
 *
 * @param hostname The candidate site name.
 * @return True when `//<hostname>/` is a parseable URI.
 */
const isBrowsableHost = (hostname: string): boolean => {
    try {
        return new URL(`https://${hostname}/`).hostname === hostname.toLowerCase();
    } catch {
        return false;
    }
};

/**
 * Page size the dialog opens with.
 *
 * **20, matching `dot-folder-list-view`'s own `MIN_ROWS_PER_PAGE`** — that value is hard-coded in
 * the shared list and is the first of its 20/40/60 options, so a store fetching a different number
 * makes the paginator disagree with the data: at 10 the table believed one page of 20 held
 * everything and disabled "next" while the endpoint was still reporting more. The AssetPicker uses
 * 20 for the same reason.
 */
export const ADD_RELATIONSHIPS_PAGE_SIZE = 20;

const initialState: AddRelationshipsState = {
    contentTypeId: '',
    selectionMode: 'multiple',
    assetPath: '',
    scopeLabel: '',
    defaultAssetPath: '',
    defaultScopeLabel: '',
    items: [],
    selection: new Map<string, DotCMSContentlet>(),
    pendingInodes: new Set<string>(),
    filters: {},
    defaultFilters: {},
    page: { number: 1, limit: ADD_RELATIONSHIPS_PAGE_SIZE },
    pages: {},
    sort: { field: 'modDate', order: 'desc' },
    constrainedIdentifiers: new Set<string>(),
    viewMode: 'all',
    status: ComponentStatus.INIT,
    errorMessage: null
};

/**
 * State for the "Add Relationships" dialog.
 *
 * Two things about this store are load-bearing and easy to undo by accident:
 *
 * 1. **The selection is seeded from the caller, never from a search response.** See
 *    {@link AddRelationshipsState.selection}. The dialog it replaces rebuilt the pre-selection by
 *    filtering its first response, which silently dropped already-related content that response did
 *    not contain.
 * 2. **Searching never touches the selection.** Results page from the server, so the rows on screen
 *    are a window, not the set. Every browse path below patches `items`/`page` and leaves
 *    `selection` alone.
 *
 * Provided per dialog instance, never in `root`.
 */
export const AddRelationshipsStore = signalStore(
    withState(initialState),
    withComputed((state) => ({
        /** Whether a given identifier is selected — independent of what page it is on. */
        $isSelected: computed(() => {
            const selection = state.selection();

            return (identifier: string): boolean => selection.has(identifier);
        }),
        /** The whole selection, in insertion order. What confirmation emits. */
        $selectedItems: computed(() => [...state.selection().values()]),
        $selectedCount: computed(() => state.selection().size),
        /** Whether a given identifier is claimed by another parent and so not selectable. */
        $isConstrained: computed(() => {
            const constrained = state.constrainedIdentifiers();

            return (identifier: string): boolean => constrained.has(identifier);
        }),
        /**
         * The same set as a list, for the table's `unselectable` input.
         *
         * A separate computed rather than spreading the set in the template: that would allocate a
         * new array on every change-detection pass and defeat the table's own `computed` set. It
         * exists because refusing the pick in `toggleSelection` is not enough on its own — the
         * control still toggles under the cursor, so the row has to *look* refused too.
         */
        $constrainedList: computed(() => [...state.constrainedIdentifiers()]),
        /**
         * Row count the paginator divides into pages.
         *
         * **Not `contentCount`.** The drive API is cursor-based and never returns a grand total —
         * `contentCount` is the size of the page it just returned. Handing that to the paginator
         * makes every full page look like the last one, so it computes a single page and disables
         * "next". The AssetPicker documents this trap and solves it the same way; this store had
         * walked straight into it.
         *
         * So: while the bookmark for the page on screen reports more, claim one page beyond to keep
         * the arrow live; once it does not, the page is the last and the exact total is knowable.
         */
        totalItems: computed(() => {
            const { number, limit } = state.page();

            return state.pages()[number]?.hasMoreContent
                ? limit * (number + 1)
                : limit * (number - 1) + state.items().length;
        }),
        /** Zero-based index of the first row on the current page, for the paging footer. */
        $offset: computed(() => (state.page().number - 1) * state.page().limit),
        $isLoading: computed(() => state.status() === ComponentStatus.LOADING),
        /**
         * Whether anything differs from the defaults — which is what decides if "Clear all" is
         * worth offering. Not the same question as "are there filters at all": the defaults are
         * always present, so counting keys would answer yes on a dialog nobody has filtered.
         */
        $hasNonDefaultFilters: computed(() => {
            const current = state.filters();
            const defaults = state.defaultFilters();
            const keys = new Set([...Object.keys(current), ...Object.keys(defaults)]);

            return [...keys].some((key) => {
                const a = current[key];
                const b = defaults[key];

                return Array.isArray(a) && Array.isArray(b)
                    ? a.length !== b.length || a.some((v, i) => v !== b[i])
                    : a !== b;
            });
        }),
        /**
         * What the list shows: the current page, or the editor's whole selection.
         *
         * The selected view reads from `selection` and not from `items`, so it lists picks the
         * current page does not contain — including ones seeded by the caller that no search has
         * ever returned.
         */
        $visibleItems: computed(() =>
            state.viewMode() === 'selected' ? [...state.selection().values()] : state.items()
        ),
        /** Whether every selectable row on the current page is selected. Drives the header box. */
        $allVisibleSelected: computed(() => {
            const constrained = state.constrainedIdentifiers();
            const selection = state.selection();
            const selectable = state.items().filter((item) => !constrained.has(item.identifier));

            return selectable.length > 0 && selectable.every((i) => selection.has(i.identifier));
        }),
        /** Empty state vs. results. Distinct from the error state, which renders instead. */
        $isEmpty: computed(
            () => state.status() === ComponentStatus.LOADED && state.items().length === 0
        )
    })),
    withMethods(
        (
            store,
            contentDriveService = inject(DotContentDriveService),
            languagesService = inject(DotLanguagesService),
            siteService = inject(SiteService),
            constrainedIdentifiersService = inject(ConstrainedIdentifiersService)
        ) => {
            /**
             * Builds the request for the current state.
             *
             * `contentTypes` is pinned to the relationship's target type and is deliberately not read
             * from `filters`: it is a caller restriction, not something the editor can change. The
             * content-type chip is not offered for the same reason.
             */
            const buildRequest = (): DotContentDriveSearchRequest => {
                // Destructured by name rather than spread: whatever else the filter bag holds — a key a
                // future chip mints, or `contentTypes` written straight in — cannot reach the request.
                // That is what keeps the target-type pin unwidenable (O8).
                const { title, languageId } = store.filters();

                return {
                    assetPath: store.assetPath(),
                    contentTypes: [store.contentTypeId()],
                    // A relationship relates content, never a folder. The endpoint returns folders
                    // unless told not to, which is why the list showed them.
                    showFolders: false,
                    ...(languageId?.length ? { language: languageId } : {}),
                    ...(title ? { filters: { text: title } } : {}),
                    sortBy: `${store.sort().field}:${store.sort().order}`,
                    // The bookmark left by the previous page, not "the last cursor we saw": page 3 is
                    // reached from page 2's bookmark, and page 1 always starts at 0.
                    contentCursor: store.pages()[store.page().number - 1]?.contentCursor ?? 0,
                    maxResults: store.page().limit
                };
            };

            /**
             * Loads the current page.
             *
             * Patches `items` and `page` only. `selection` is untouched on purpose — that is what lets
             * a pick on page 1 survive a trip to page 2.
             *
             * Declared here rather than inline in the returned object so `nextPage` can invoke it.
             */
            const load = rxMethod<void>(
                pipe(
                    tap(() =>
                        patchState(store, { status: ComponentStatus.LOADING, errorMessage: null })
                    ),
                    switchMap(() => {
                        // Nothing to ask for: the scope is not a browsable site (Shared Assets). An
                        // empty result is the honest answer — the previous behaviour was a 500 shown
                        // to the editor as "The search could not be completed".
                        if (!store.assetPath()) {
                            patchState(store, {
                                items: [],
                                status: ComponentStatus.LOADED,
                                errorMessage: null
                            });

                            return EMPTY;
                        }

                        return (
                            // Languages travel with the rows, not separately: `LanguagePipe` needs a whole
                            // `DotLanguage` (`.language`, `.languageCode`) and `/drive/search` rows carry
                            // only `languageId`, so a row handed back unenriched renders a blank Locale
                            // cell in the related-content table. The dialog this replaced resolved them the
                            // same way, for the same reason.
                            forkJoin([
                                contentDriveService.search(buildRequest()),
                                // A languages failure must not take the results down with it: the
                                // Locale column degrades to blank, which is exactly what it did
                                // before this enrichment existed. `forkJoin` fails on its first
                                // error, so the recovery has to sit on this arm rather than around
                                // the pair.
                                languagesService
                                    .get()
                                    .pipe(catchError(() => of([] as DotLanguage[])))
                            ]).pipe(
                                map(([response, languages]) => ({
                                    ...response,
                                    list: withLanguages(response.list, languages)
                                })),
                                tapResponse({
                                    next: (response) => {
                                        // Resolve any inode-only pre-selection this page happens to
                                        // contain. Nothing else in this method touches `selection`.
                                        const pending = store.pendingInodes();

                                        if (pending.size) {
                                            const selection = new Map(store.selection());
                                            const stillPending = new Set(pending);

                                            for (const row of response.list) {
                                                if (pending.has(row.inode)) {
                                                    selection.set(
                                                        row.identifier,
                                                        row as DotCMSContentlet
                                                    );
                                                    stillPending.delete(row.inode);
                                                }
                                            }

                                            patchState(store, {
                                                selection,
                                                pendingInodes: stillPending
                                            });
                                        }

                                        patchState(store, {
                                            items: response.list,
                                            // The bookmark for the page just loaded: where the
                                            // next one starts, and whether there is one.
                                            pages: {
                                                ...store.pages(),
                                                [store.page().number]: {
                                                    contentCursor: response.nextContentCursor,
                                                    hasMoreContent: response.hasMoreContent
                                                }
                                            },
                                            status: ComponentStatus.LOADED
                                        });
                                    },
                                    error: () =>
                                        patchState(store, {
                                            status: ComponentStatus.ERROR,
                                            errorMessage:
                                                'dot.relationship.add.dialog.search.failed'
                                        })
                                })
                            )
                        );
                    })
                )
            );

            /**
             * Fetches the identifiers already claimed by another parent.
             *
             * **Guarded, not unconditional.** The lookup only means something when the field is the
             * parent side of a ONE_TO_ONE or ONE_TO_MANY relationship — the two cardinalities where
             * a child belongs to exactly one parent. The filter-time consumer (Content Drive's
             * field-filter chip) supplies none of this context, so the same guard is what keeps
             * contract C3: a lookup computed from a partial set would disable rows for the wrong
             * reason.
             */
            const loadConstrained = rxMethod<AddRelationshipsInput>(
                pipe(
                    switchMap((input) => {
                        const shouldCheck =
                            input.cardinality != null &&
                            input.isParentField != null &&
                            Boolean(input.parentContentTypeId) &&
                            Boolean(input.fieldVariable) &&
                            needsCardinalityConstraintCheck(input.cardinality, input.isParentField);

                        if (!shouldCheck) {
                            return EMPTY;
                        }

                        return constrainedIdentifiersService
                            .get({
                                parentContentTypeId: input.parentContentTypeId as string,
                                fieldVariable: input.fieldVariable as string,
                                currentContentIdentifier: input.currentContentIdentifier ?? null
                            })
                            .pipe(
                                tap((constrainedIdentifiers) =>
                                    patchState(store, { constrainedIdentifiers })
                                )
                            );
                    })
                )
            );

            return {
                load,

                /**
                 * Seeds the dialog from its caller.
                 *
                 * The selection is built here, from the contentlets the caller already holds — the one
                 * place it is ever populated from outside a user action.
                 */
                initialize(input: AddRelationshipsInput): void {
                    const hostname =
                        input.contentletContext?.hostName ?? siteService.currentSite?.hostname;
                    // Seeded from the contentlet being edited: a starting point, never a restriction.
                    const seeded: DotAddRelationshipsFilters = input.contentletContext?.languageId
                        ? { languageId: [String(input.contentletContext.languageId)] }
                        : {};

                    patchState(store, {
                        ...initialState,
                        contentTypeId: input.contentTypeId,
                        selectionMode: input.selectionMode,
                        // `/drive/search` resolves a host and folder from this and rejects anything it
                        // cannot parse — an empty string throws
                        // "can not resolve a valid hostName [null]" before any filter is applied.
                        assetPath: buildAssetPath(hostname),
                        scopeLabel: hostname ?? '',
                        defaultAssetPath: buildAssetPath(hostname),
                        defaultScopeLabel: hostname ?? '',
                        selection: new Map(input.selected.map((item) => [item.identifier, item])),
                        pendingInodes: new Set(input.selectedInodes ?? []),
                        filters: seeded,
                        defaultFilters: seeded
                    });

                    // Which rows are unselectable is part of opening the dialog, not of browsing:
                    // it depends on the parent context, never on the current page.
                    loadConstrained(input);
                },

                /** Reads one filter. `undefined` (not set) and `[]` (set to nothing) differ. */
                getFilterValue(key: string) {
                    return store.filters()[key];
                },

                /**
                 * Merges filter values and returns to the first page.
                 *
                 * Paging is reset unconditionally here — a cursor taken against a wider result set must
                 * never be applied to a narrower one. The no-op guard that keeps a chip re-emitting its
                 * current selection from bouncing the editor back to page 1 lives in the facade, so
                 * this method's other callers keep their unconditional semantics.
                 *
                 * `selection` is deliberately absent from the patch.
                 */
                patchFilters(patch: Record<string, string | string[]>): void {
                    patchState(store, {
                        filters: { ...store.filters(), ...patch },
                        page: { ...store.page(), number: 1 },
                        pages: {}
                    });
                },

                /**
                 * Removes a filter entirely, rather than setting it to an empty value.
                 *
                 * The key is deleted rather than set to `undefined`: the two mean different things to a
                 * chip — "no filter" versus "filtered to nothing selected" — and only one of them
                 * should ever come back from `getFilterValue`.
                 */
                removeFilter(key: string): void {
                    // Removing a key that was never set changes nothing, so it must not reset paging
                    // either — otherwise a chip tidying up on close silently sends the editor back to
                    // page 1. Same reasoning as the facade's no-op guard, one level down.
                    if (!(key in store.filters())) {
                        return;
                    }

                    const filters = { ...store.filters() };
                    delete filters[key];

                    patchState(store, {
                        filters,
                        page: { ...store.page(), number: 1 },
                        pages: {}
                    });
                },

                /**
                 * Returns the dialog to the state it opened in — filters **and** browsed scope.
                 *
                 * Wider than the filter bag on purpose. The browsed site is not a filter — it lives
                 * outside the bag, the way Content Drive keeps its browsed folder out — but to an
                 * editor it is one more thing they changed, and an editor who browsed into a site
                 * with nothing in it has no filters to clear. Clearing only filters would leave
                 * them exactly where they were stuck.
                 *
                 * This is what the shared bar's "Clear all" calls, through the facade.
                 */
                reset(): void {
                    patchState(store, {
                        filters: { ...store.defaultFilters() },
                        assetPath: store.defaultAssetPath(),
                        scopeLabel: store.defaultScopeLabel(),
                        page: { ...store.page(), number: 1 },
                        pages: {}
                    });
                    load();
                },

                /** Returns filters to the values the dialog opened with — not to an empty set. */
                clearFilters(): void {
                    patchState(store, {
                        filters: { ...store.defaultFilters() },
                        page: { ...store.page(), number: 1 },
                        pages: {}
                    });
                },

                /**
                 * Adds or removes one item.
                 *
                 * A constrained item is refused outright rather than silently ignored downstream: it is
                 * claimed by another parent, and letting it into the selection would produce broken
                 * content on save.
                 *
                 * In `single` mode the selection is a slot, so a second pick replaces the first.
                 */
                toggleSelection(item: DotCMSContentlet): void {
                    if (store.constrainedIdentifiers().has(item.identifier)) {
                        return;
                    }

                    const selection = new Map(store.selection());

                    if (selection.has(item.identifier)) {
                        selection.delete(item.identifier);
                    } else if (store.selectionMode() === 'single') {
                        selection.clear();
                        selection.set(item.identifier, item);
                    } else {
                        selection.set(item.identifier, item);
                    }

                    patchState(store, { selection });
                },

                /**
                 * Select-all for the **current page**.
                 *
                 * Scoped to what is on screen in both directions, and unselecting therefore clears only
                 * these rows. Anything picked on another page — or seeded by the caller and never
                 * listed at all — is left alone, because discarding picks the editor cannot see is the
                 * exact failure this dialog was rebuilt to remove.
                 */
                toggleSelectAllVisible(checked: boolean): void {
                    const constrained = store.constrainedIdentifiers();
                    const visible = store
                        .items()
                        .filter((item) => !constrained.has(item.identifier)) as DotCMSContentlet[];
                    const selection = new Map(store.selection());

                    for (const item of visible) {
                        if (checked) {
                            selection.set(item.identifier, item);
                        } else {
                            selection.delete(item.identifier);
                        }
                    }

                    patchState(store, { selection });
                },

                /**
                 * Records which identifiers are claimed by another parent.
                 *
                 * Left empty when the caller supplies no parent context — a filter-time caller has
                 * none, and a lookup computed from a partial set would disable rows for the wrong
                 * reason (contract C3).
                 */
                setConstrainedIdentifiers(constrainedIdentifiers: Set<string>): void {
                    patchState(store, { constrainedIdentifiers });
                },

                /** Switches between the page of results and the editor's own selection. */
                setViewMode(viewMode: 'all' | 'selected'): void {
                    patchState(store, { viewMode });
                },

                /** Re-sorts, returning to the first page — a cursor does not survive a re-sort. */
                setSort(sort: DotAddRelationshipsSort): void {
                    patchState(store, {
                        sort,
                        page: { ...store.page(), number: 1 },
                        pages: {}
                    });
                    load();
                },

                /**
                 * Advances one page, carrying the cursor the last response handed back.
                 *
                 * Leaves `selection` alone, like every browse path — that is the whole point.
                 */
                nextPage(): void {
                    patchState(store, {
                        page: { ...store.page(), number: store.page().number + 1 }
                    });
                    load();
                },

                /**
                 * Moves the browsed scope to a site or one of its folders.
                 *
                 * Reloads from the first page: a cursor taken against one site cannot be replayed
                 * against another. The selection is untouched — changing where you are looking is not
                 * un-picking what you already picked.
                 */
                setScope(scope: { hostname: string; path?: string }): void {
                    patchState(store, {
                        assetPath: buildAssetPath(scope.hostname, scope.path),
                        scopeLabel: scope.path ? `${scope.hostname}${scope.path}` : scope.hostname,
                        page: { ...store.page(), number: 1 },
                        pages: {}
                    });
                    load();
                },

                /** Jumps to a page number. Used by the footer's paging control. */
                /**
                 * Moves to a page, and to a page size when the editor changes it.
                 *
                 * A size change invalidates every bookmark — they were taken against pages of the
                 * old size — so they are discarded and the list returns to the first page.
                 */
                setPage(number: number, limit?: number): void {
                    const changedSize = limit !== undefined && limit !== store.page().limit;

                    patchState(store, {
                        page: {
                            number: changedSize ? 1 : number,
                            limit: limit ?? store.page().limit
                        },
                        ...(changedSize ? { pages: {} } : {})
                    });
                }
            };
        }
    )
);
