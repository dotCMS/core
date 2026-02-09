import { patchState, signalStore, withHooks, withMethods, withState } from '@ngrx/signals';
import { EMPTY, Observable } from 'rxjs';

import { effect, inject, untracked } from '@angular/core';

import { ConfirmationService } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';

import { catchError, take } from 'rxjs/operators';

import { DotHttpErrorManagerService, DotTagsService } from '@dotcms/data-access';
import { DotTag } from '@dotcms/dotcms-models';
import { getDownloadLink } from '@dotcms/utils';

import { DotTagsCreateComponent } from '../../dot-tags-create/dot-tags-create.component';
import { DotTagsImportComponent } from '../../dot-tags-import/dot-tags-import.component';

type DotTagsListStatus = 'init' | 'loading' | 'loaded' | 'error';

interface DotTagsListState {
    tags: DotTag[];
    selectedTags: DotTag[];
    totalRecords: number;
    page: number;
    rows: number;
    filter: string;
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
    sortField: 'tagname',
    sortOrder: 'ASC',
    status: 'init'
};

export const DotTagsListStore = signalStore(
    withState<DotTagsListState>(initialState),
    withMethods((store) => {
        const tagsService = inject(DotTagsService);
        const dialogService = inject(DialogService);
        const confirmationService = inject(ConfirmationService);
        const httpErrorManager = inject(DotHttpErrorManagerService);

        function loadTags() {
            patchState(store, { status: 'loading' });
            tagsService
                .getTagsPaginated({
                    filter: store.filter() || undefined,
                    page: store.page(),
                    per_page: store.rows(),
                    orderBy: store.sortField(),
                    direction: store.sortOrder()
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

            setPagination(page: number, rows: number) {
                patchState(store, { page, rows });
            },

            setSort(field: string, order: string) {
                patchState(store, { sortField: field, sortOrder: order });
            },

            setSelectedTags(tags: DotTag[]) {
                patchState(store, { selectedTags: tags });
            },

            openCreateDialog() {
                const ref = dialogService.open(DotTagsCreateComponent, {
                    header: 'Add Tag',
                    width: '400px'
                });

                ref?.onClose.pipe(take(1)).subscribe((result) => {
                    if (result) {
                        handleTagAction(
                            tagsService.createTag([
                                { name: result.name, siteId: result.siteId || undefined }
                            ]),
                            () => loadTags()
                        );
                    }
                });
            },

            openEditDialog(tag: DotTag) {
                const ref = dialogService.open(DotTagsCreateComponent, {
                    header: 'Edit Tag',
                    width: '400px',
                    data: { tag }
                });

                ref?.onClose.pipe(take(1)).subscribe((result) => {
                    if (result) {
                        handleTagAction(
                            tagsService.updateTag(tag.id, {
                                tagName: result.name,
                                siteId: result.siteId || tag.siteId
                            }),
                            () => loadTags()
                        );
                    }
                });
            },

            confirmDelete() {
                const count = store.selectedTags().length;

                confirmationService.confirm({
                    message: `Are you sure you want to delete ${count} tag(s)?`,
                    header: 'Delete Tags',
                    acceptButtonStyleClass: 'p-button-outlined',
                    rejectButtonStyleClass: 'p-button-primary',
                    accept: () => {
                        const ids = store.selectedTags().map((t) => t.id);
                        handleTagAction(tagsService.deleteTags(ids), () => {
                            patchState(store, { selectedTags: [] });
                            loadTags();
                        });
                    }
                });
            },

            exportSelectedTags() {
                const tags = store.selectedTags();
                if (tags.length === 0) {
                    return;
                }

                const header = '"Tag Name","Host ID"';
                const rows = tags.map((tag) => {
                    return `${sanitizeCsvValue(tag.label)},${sanitizeCsvValue(tag.siteId)}`;
                });

                const csv = [header, ...rows].join('\n');

                try {
                    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
                    const date = new Date().toISOString().slice(0, 10);
                    getDownloadLink(blob, `tags-export-${date}.csv`).click();
                } catch (error) {
                    httpErrorManager.handle(error);
                }
            },

            openImportDialog() {
                const ref = dialogService.open(DotTagsImportComponent, {
                    header: 'Import Tags',
                    width: '500px'
                });

                ref?.onClose.pipe(take(1)).subscribe((result) => {
                    if (result) {
                        loadTags();
                    }
                });
            }
        };
    }),
    withHooks((store) => {
        return {
            onInit() {
                effect(() => {
                    store.filter();
                    store.page();
                    store.rows();
                    store.sortField();
                    store.sortOrder();

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
