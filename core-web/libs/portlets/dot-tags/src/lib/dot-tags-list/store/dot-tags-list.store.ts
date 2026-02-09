import { patchState, signalStore, withHooks, withMethods, withState } from '@ngrx/signals';
import { EMPTY } from 'rxjs';

import { effect, inject } from '@angular/core';

import { ConfirmationService } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';

import { catchError, take } from 'rxjs/operators';

import { DotHttpErrorManagerService, DotTagsService } from '@dotcms/data-access';
import { DotTag } from '@dotcms/dotcms-models';

import { DotTagsCreateComponent } from '../../dot-tags-create/dot-tags-create.component';

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

        return {
            loadTags() {
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
            },

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

                ref.onClose.pipe(take(1)).subscribe((result) => {
                    if (result) {
                        patchState(store, { status: 'loading' });
                        tagsService
                            .createTag([{ name: result.name, siteId: result.siteId || undefined }])
                            .pipe(
                                take(1),
                                catchError((error) => {
                                    httpErrorManager.handle(error);
                                    patchState(store, { status: 'loaded' });

                                    return EMPTY;
                                })
                            )
                            .subscribe(() => {
                                this.loadTags();
                            });
                    }
                });
            },

            openEditDialog(tag: DotTag) {
                const ref = dialogService.open(DotTagsCreateComponent, {
                    header: 'Edit Tag',
                    width: '400px',
                    data: { tag }
                });

                ref.onClose.pipe(take(1)).subscribe((result) => {
                    if (result) {
                        patchState(store, { status: 'loading' });
                        tagsService
                            .updateTag(tag.id, {
                                tagName: result.name,
                                siteId: result.siteId || ''
                            })
                            .pipe(
                                take(1),
                                catchError((error) => {
                                    httpErrorManager.handle(error);
                                    patchState(store, { status: 'loaded' });

                                    return EMPTY;
                                })
                            )
                            .subscribe(() => {
                                this.loadTags();
                            });
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
                        const ids = store.selectedTags().map((tag) => tag.id);
                        patchState(store, { status: 'loading' });

                        tagsService
                            .deleteTags(ids)
                            .pipe(
                                take(1),
                                catchError((error) => {
                                    httpErrorManager.handle(error);
                                    patchState(store, { status: 'loaded' });

                                    return EMPTY;
                                })
                            )
                            .subscribe(() => {
                                patchState(store, { selectedTags: [] });
                                this.loadTags();
                            });
                    }
                });
            }
        };
    }),
    withHooks((store) => {
        return {
            onInit() {
                effect(() => {
                    // Track reactive dependencies
                    store.filter();
                    store.page();
                    store.rows();
                    store.sortField();
                    store.sortOrder();

                    // Trigger load whenever any of these change
                    store.loadTags();
                });
            }
        };
    })
);
