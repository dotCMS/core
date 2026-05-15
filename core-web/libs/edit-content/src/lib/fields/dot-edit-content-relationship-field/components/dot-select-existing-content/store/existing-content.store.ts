import { tapResponse } from '@ngrx/operators';
import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { forkJoin, of, pipe } from 'rxjs';

import { computed, inject } from '@angular/core';

import { TablePageEvent } from 'primeng/table';
import { ToggleSwitchChangeEvent } from 'primeng/toggleswitch';

import { filter, switchMap, tap } from 'rxjs/operators';

import { ComponentStatus, DotCMSContentlet } from '@dotcms/dotcms-models';

import { ExistingContentService, RelationshipFieldQueryParams } from './existing-content.service';

import { Column } from '../../../models/column.model';
import {
    ContentletContext,
    ContentletFilterContext,
    InitLoadParams,
    SelectionMode
} from '../../../models/relationship.models';
import { SearchParams, SystemSearchableFields } from '../../../models/search.model';
import { needsCardinalityConstraintCheck } from '../../../utils';

const ViewMode = {
    all: 'all',
    selected: 'selected'
} as const;

type ViewMode = (typeof ViewMode)[keyof typeof ViewMode];

const SYSTEM_FOLDER = 'SYSTEM_FOLDER';

/**
 * Builds the initial filter context from the contentlet's data.
 */
function buildInitialFilters(ctx?: ContentletContext): ContentletFilterContext | null {
    if (!ctx) {
        return null;
    }

    const { host, hostName, languageId, folder, url } = ctx;

    if (!languageId && !host) {
        return null;
    }

    const isSystemFolder = !folder || folder === SYSTEM_FOLDER;
    const folderPath = url ? url.substring(0, url.lastIndexOf('/') + 1) : null;

    return {
        hostId: host || null,
        hostName: hostName || null,
        languageId: languageId || null,
        folderId: isSystemFolder ? null : folder,
        folderPath: isSystemFolder ? null : folderPath
    };
}

export interface ExistingContentState {
    contentTypeId: string;
    data: DotCMSContentlet[];
    status: ComponentStatus;
    searchData: DotCMSContentlet[];
    selectionMode: SelectionMode | null;
    errorMessage: string | null;
    columns: Column[];
    constrainedIdentifiers: Set<string>;
    pagination: {
        offset: number;
        currentPage: number;
        rowsPerPage: number;
        totalResults: number;
    };
    previousPagination: {
        offset: number;
        currentPage: number;
        rowsPerPage: number;
        totalResults: number;
    };
    selectionItems: DotCMSContentlet[] | DotCMSContentlet | null;
    viewMode: ViewMode;
    initialFilters: ContentletFilterContext | null;
}

const paginationInitialState: ExistingContentState['pagination'] = {
    offset: 0,
    currentPage: 1,
    rowsPerPage: 50,
    totalResults: 0
};

const initialState: ExistingContentState = {
    contentTypeId: null,
    data: [],
    searchData: [],
    columns: [],
    status: ComponentStatus.INIT,
    selectionMode: null,
    errorMessage: null,
    constrainedIdentifiers: new Set<string>(),
    pagination: { ...paginationInitialState },
    previousPagination: { ...paginationInitialState },
    selectionItems: null,
    viewMode: ViewMode.all,
    initialFilters: null
};

/**
 * Store for the ExistingContent component.
 * This store manages the state and actions related to the existing content.
 */
export const ExistingContentStore = signalStore(
    { providedIn: 'root' },
    withState(initialState),
    withComputed((state) => ({
        /**
         * Computes whether the content is currently loading.
         * @returns {boolean} True if the content is loading, false otherwise.
         */
        isLoading: computed(() => state.status() === ComponentStatus.LOADING),
        /**
         * Computes the total number of pages based on the data and rows per page.
         * @returns {number} The total number of pages.
         */
        totalPages: computed(() => Math.ceil(state.data().length / state.pagination().rowsPerPage)),
        /**
         * Computes the items based on the selected items.
         * @returns {DotCMSContentlet[]} The items.
         */
        currentItems: computed(() => {
            const selectionItems = state.selectionItems();

            if (!selectionItems) {
                return [];
            }

            const isArray = Array.isArray(selectionItems);

            return isArray ? selectionItems : [selectionItems];
        }),
        /**
         * Computes the filtered data based on the showOnlySelected state.
         * @returns {DotCMSContentlet[]} The filtered data.
         */
        filteredData: computed(() => {
            const viewMode = state.viewMode();
            const isSelectedView = viewMode === ViewMode.selected;
            const data = isSelectedView ? state.data() : state.searchData();

            if (isSelectedView) {
                const selectionItems = state.selectionItems();
                const isArray = Array.isArray(selectionItems);
                const currentItemsIds = isArray
                    ? selectionItems.map((item) => item.inode)
                    : [selectionItems.inode];

                return data.filter((item) => currentItemsIds.includes(item.inode));
            }

            return data;
        }),
        /**
         * Computes whether the show only selected state is true.
         * @returns {boolean} True if the show only selected state is true, false otherwise.
         */
        isSelectedView: computed(() => state.viewMode() === ViewMode.selected),
        /**
         * Returns a function that checks if a contentlet identifier is constrained
         * (already related to another parent in a cardinality-restricted relationship).
         */
        isItemConstrained: computed(() => {
            const constrained = state.constrainedIdentifiers();

            return (identifier: string) => constrained.has(identifier);
        })
    })),
    withMethods((store) => {
        const existingContentService = inject(ExistingContentService);

        return {
            /**
             * Initiates the loading of content by setting the status to LOADING and fetching content from the service.
             * Optionally loads constrained identifiers in parallel when cardinality constraints apply.
             * @returns {Observable<void>} An observable that completes when the content has been loaded.
             */
            initLoad: rxMethod<InitLoadParams>(
                pipe(
                    tap(({ selectionMode, contentTypeId, contentletContext }) => {
                        if (!contentTypeId) {
                            patchState(store, {
                                status: ComponentStatus.ERROR,
                                errorMessage: 'dot.file.relationship.dialog.content.id.required'
                            });

                            return;
                        }

                        patchState(store, {
                            status: ComponentStatus.LOADING,
                            selectionMode,
                            constrainedIdentifiers: new Set<string>(),
                            viewMode: ViewMode.all,
                            pagination: { ...paginationInitialState },
                            initialFilters: buildInitialFilters(contentletContext)
                        });
                    }),
                    filter(({ contentTypeId }) => !!contentTypeId),
                    switchMap((params) => {
                        const {
                            contentTypeId,
                            selectedItemsIds,
                            cardinality,
                            parentContentTypeId,
                            fieldVariable,
                            isParentField,
                            currentContentIdentifier
                        } = params;

                        const shouldCheckConstraints =
                            cardinality != null &&
                            isParentField != null &&
                            parentContentTypeId &&
                            fieldVariable &&
                            needsCardinalityConstraintCheck(cardinality, isParentField);

                        const constrainedIds$ = shouldCheckConstraints
                            ? existingContentService.getConstrainedIdentifiers({
                                  parentContentTypeId,
                                  fieldVariable,
                                  currentContentIdentifier: currentContentIdentifier ?? null
                              })
                            : of(new Set<string>());

                        // Build systemSearchableFields from initialFilters for the initial load
                        const initialFilters = store.initialFilters();
                        const systemSearchableFields: SystemSearchableFields = {};

                        if (initialFilters?.languageId) {
                            systemSearchableFields.languageId = initialFilters.languageId;
                        }

                        if (initialFilters?.folderId) {
                            systemSearchableFields.folderId = initialFilters.folderId;
                        } else if (initialFilters?.hostId) {
                            systemSearchableFields.siteId = initialFilters.hostId;
                        }

                        const hasInitialFilters = Object.keys(systemSearchableFields).length > 0;

                        return forkJoin([
                            existingContentService.getColumnsAndContent(
                                contentTypeId,
                                hasInitialFilters ? systemSearchableFields : undefined
                            ),
                            constrainedIds$
                        ]).pipe(
                            tapResponse({
                                next: ([columnsAndContent, constrainedIdentifiers]) => {
                                    if (!columnsAndContent) {
                                        patchState(store, {
                                            status: ComponentStatus.ERROR,
                                            errorMessage:
                                                'dot.file.relationship.dialog.content.request.failed'
                                        });

                                        return;
                                    }

                                    const [columns, searchResponse] = columnsAndContent;
                                    const data = searchResponse.contentlets;
                                    const selectionItems =
                                        selectedItemsIds.length > 0
                                            ? data.filter((item) =>
                                                  selectedItemsIds.includes(item.inode)
                                              )
                                            : [];

                                    patchState(store, {
                                        contentTypeId,
                                        columns,
                                        data,
                                        searchData: data,
                                        status: ComponentStatus.LOADED,
                                        selectionItems,
                                        constrainedIdentifiers,
                                        pagination: {
                                            ...store.pagination(),
                                            totalResults: searchResponse.totalResults
                                        }
                                    });
                                },
                                error: () =>
                                    patchState(store, {
                                        status: ComponentStatus.ERROR,
                                        errorMessage:
                                            'dot.file.relationship.dialog.content.request.failed'
                                    })
                            })
                        );
                    })
                )
            ),
            /**
             * Advances the pagination to the next page and updates the state accordingly.
             */
            nextPage: () => {
                patchState(store, {
                    pagination: {
                        ...store.pagination(),
                        offset: store.pagination().offset + store.pagination().rowsPerPage,
                        currentPage: store.pagination().currentPage + 1
                    }
                });
            },
            /**
             * Moves the pagination to the previous page and updates the state accordingly.
             */
            previousPage: () => {
                const { currentPage } = store.pagination();

                if (currentPage === 1) {
                    return;
                }

                patchState(store, {
                    pagination: {
                        ...store.pagination(),
                        offset: store.pagination().offset - store.pagination().rowsPerPage,
                        currentPage: currentPage - 1
                    }
                });
            },
            /**
             * Sets the selected items in the state.
             * @param items The items to set as selected.
             */
            setSelectionItems: (items: DotCMSContentlet[] | DotCMSContentlet) => {
                patchState(store, {
                    selectionItems: items
                });
            },
            /**
             * Changes the view mode between all and selected items.
             * @param event The event containing the checked property.
             */
            changeViewMode: (event: ToggleSwitchChangeEvent) => {
                const viewMode = event.checked ? ViewMode.selected : ViewMode.all;
                const isSelectedView = viewMode === ViewMode.selected;

                patchState(store, {
                    viewMode,
                    previousPagination: { ...store.pagination() },
                    pagination: isSelectedView
                        ? { ...paginationInitialState }
                        : { ...store.previousPagination() }
                });
            },
            /**
             * Sets the offset and current page in the state.
             * @param event The event containing the first and rows properties.
             */
            setOffset: ({ first }: TablePageEvent) => {
                patchState(store, {
                    pagination: {
                        ...store.pagination(),
                        offset: first,
                        currentPage: Math.floor(first / store.pagination().rowsPerPage) + 1
                    }
                });
            },
            /**
             * Searches for content based on the provided search parameters.
             * @param searchParams The search parameters to use for filtering content.
             * @returns An observable that completes when the search has been performed.
             */
            search: rxMethod<SearchParams>(
                pipe(
                    tap(() =>
                        patchState(store, {
                            status: ComponentStatus.LOADING,
                            // Reset pagination when performing a new search
                            pagination: { ...paginationInitialState }
                        })
                    ),
                    switchMap((searchParams) => {
                        // Map SearchParams to RelationshipFieldQueryParams
                        const queryParams: RelationshipFieldQueryParams = {
                            contentTypeId: store.contentTypeId()
                        };

                        if (searchParams.query) {
                            queryParams.globalSearch = searchParams.query;
                        }

                        if (
                            searchParams.systemSearchableFields &&
                            Object.keys(searchParams.systemSearchableFields).length > 0
                        ) {
                            queryParams.systemSearchableFields = {
                                ...searchParams.systemSearchableFields
                            };
                        }

                        return existingContentService.search(queryParams).pipe(
                            tapResponse({
                                next: (data) => {
                                    patchState(store, {
                                        searchData: data.contentlets,
                                        status: ComponentStatus.LOADED,
                                        pagination: {
                                            ...store.pagination(),
                                            totalResults: data.totalResults
                                        }
                                    });
                                },
                                error: () => {
                                    patchState(store, {
                                        status: ComponentStatus.ERROR,
                                        errorMessage: 'dot.file.relationship.dialog.search.failed'
                                    });
                                }
                            })
                        );
                    })
                )
            )
        };
    })
);
