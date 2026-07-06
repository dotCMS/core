import {
    patchState,
    signalStore,
    withComputed,
    withHooks,
    withMethods,
    withState
} from '@ngrx/signals';
import { EMPTY, Observable } from 'rxjs';

import { computed, effect, inject, untracked } from '@angular/core';

import { catchError, take } from 'rxjs/operators';

import { DotHttpErrorManagerService, DotTagsService } from '@dotcms/data-access';
import { DotTag } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';
import { getDownloadLink } from '@dotcms/utils';

type DotTagsListStatus = 'init' | 'loading' | 'loaded' | 'error';

interface DotTagsListState {
    tags: DotTag[];
    selectedTags: DotTag[];
    totalRecords: number;
    page: number;
    rows: number;
    filter: string;
    showGlobal: boolean;
    sortField: string;
    sortOrder: string;
    status: DotTagsListStatus;
}

const initialState: DotTagsListState = {
    tags: [],
    selectedTags: [],
    totalRecords: 0,
    page: 1,
    rows: 25,
    filter: '',
    showGlobal: false,
    sortField: 'tagname',
    sortOrder: 'ASC',
    status: 'init'
};

export const DotTagsListStore = signalStore(
    withState<DotTagsListState>(initialState),
    withComputed((store) => ({
        /**
         * Whether to show the "Export All" button alongside "Export Selected".
         * Only meaningful when every visible row is selected (header checkbox is on) AND
         * the filter result spans more than one page — otherwise the selection already
         * contains everything that "Export All" would fetch.
         */
        showExportAll: computed(() => {
            const visible = store.tags().length;
            const selected = store.selectedTags().length;
            return visible > 0 && selected === visible && store.totalRecords() > visible;
        })
    })),
    withMethods((store) => {
        const tagsService = inject(DotTagsService);
        const httpErrorManager = inject(DotHttpErrorManagerService);
        const globalStore = inject(GlobalStore);

        function buildBaseParams() {
            return {
                filter: store.filter() || undefined,
                site: globalStore.currentSiteId() || undefined,
                global: store.showGlobal() || undefined,
                orderBy: store.sortField(),
                direction: store.sortOrder()
            };
        }

        function loadTags() {
            patchState(store, { status: 'loading' });
            tagsService
                .getTagsPaginated({
                    ...buildBaseParams(),
                    page: store.page(),
                    per_page: store.rows()
                })
                .pipe(
                    take(1),
                    catchError((error) => {
                        httpErrorManager.handle(error);
                        patchState(store, { status: 'error' });

                        return EMPTY;
                    })
                )
                .subscribe((response) => {
                    patchState(store, {
                        tags: response.entity,
                        totalRecords: response.pagination?.totalEntries ?? 0,
                        status: 'loaded'
                    });
                });
        }

        function downloadCsv(tags: DotTag[]) {
            if (tags.length === 0) {
                return;
            }

            const header = '"Tag Name","Host ID"';
            const rows = tags.map(
                (tag) => `${sanitizeCsvValue(tag.label)},${sanitizeCsvValue(tag.siteId)}`
            );
            const csv = [header, ...rows].join('\n');

            try {
                const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
                const date = new Date().toISOString().slice(0, 10);
                getDownloadLink(blob, `tags-export-${date}.csv`).click();
            } catch (error) {
                httpErrorManager.handle(error);
            }
        }

        // Observable<unknown> because we don't use the response — we just reload the list on success.
        function handleTagAction(source$: Observable<unknown>, onSuccess: () => void) {
            patchState(store, { status: 'loading' });
            source$
                .pipe(
                    take(1),
                    catchError((error) => {
                        httpErrorManager.handle(error);
                        patchState(store, { status: 'loaded' });

                        return EMPTY;
                    })
                )
                .subscribe(() => onSuccess());
        }

        return {
            loadTags,

            setFilter(filter: string) {
                patchState(store, { filter, page: 1 });
            },

            setShowGlobal(showGlobal: boolean) {
                patchState(store, { showGlobal, page: 1 });
            },

            setPagination(page: number, rows: number) {
                patchState(store, { page, rows });
            },

            setSort(field: string, order: string) {
                patchState(store, { sortField: field, sortOrder: order });
            },

            setSelectedTags(tags: DotTag[]) {
                patchState(store, { selectedTags: tags });
            },

            createTag(form: { name: string; siteId?: string }) {
                handleTagAction(
                    tagsService.createTag([{ name: form.name, siteId: form.siteId || undefined }]),
                    () => loadTags()
                );
            },

            updateTag(tag: DotTag, form: { name: string; siteId?: string }) {
                handleTagAction(
                    tagsService.updateTag(tag.id, {
                        tagName: form.name,
                        siteId: form.siteId || tag.siteId
                    }),
                    () => loadTags()
                );
            },

            deleteTags() {
                const ids = store.selectedTags().map((t) => t.id);
                handleTagAction(tagsService.deleteTags(ids), () => {
                    patchState(store, { selectedTags: [] });
                    loadTags();
                });
            },

            exportSelected() {
                const selected = store.selectedTags();
                if (selected.length === 0) {
                    return;
                }
                downloadCsv(selected);
            },

            exportAll() {
                const total = store.totalRecords();
                if (total === 0) {
                    return;
                }
                patchState(store, { status: 'loading' });
                tagsService
                    .getTagsPaginated({
                        ...buildBaseParams(),
                        page: 1,
                        per_page: total
                    })
                    .pipe(
                        take(1),
                        catchError((error) => {
                            httpErrorManager.handle(error);
                            patchState(store, { status: 'loaded' });

                            return EMPTY;
                        })
                    )
                    .subscribe((response) => {
                        downloadCsv(response.entity);
                        patchState(store, { status: 'loaded' });
                    });
            }
        };
    }),
    withHooks((store) => {
        return {
            onInit() {
                const globalStore = inject(GlobalStore);

                // When site changes, reset pagination and show loading so the table shows skeleton rows until new site's tags load
                effect(() => {
                    globalStore.currentSiteId();
                    untracked(() =>
                        patchState(store, { page: 1, status: 'loading', selectedTags: [] })
                    );
                });

                effect(() => {
                    store.filter();
                    store.showGlobal();
                    store.page();
                    store.rows();
                    store.sortField();
                    store.sortOrder();
                    // React to site change: tags are per-site, so reload when current site changes
                    globalStore.currentSiteId();

                    untracked(() => store.loadTags());
                });
            }
        };
    })
);

/**
 * Sanitizes a value for CSV export to prevent spreadsheet formula injection.
 * Prefixes values starting with formula characters (=, +, -, @, tab, carriage return)
 * with a single quote, which is the standard mitigation for CSV injection.
 */
function sanitizeCsvValue(value: string): string {
    const escaped = value.replace(/"/g, '""');

    if (/^[=+\-@\t\r]/.test(escaped)) {
        return `"'${escaped}"`;
    }

    return `"${escaped}"`;
}
