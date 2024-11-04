import { faker } from '@faker-js/faker';
import { tapResponse } from '@ngrx/component-store';
import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe } from 'rxjs';

import { computed, inject } from '@angular/core';

import { TreeNode } from 'primeng/api';

import { exhaustMap, switchMap, tap } from 'rxjs/operators';

import { ComponentStatus, DotCMSContentlet } from '@dotcms/dotcms-models';

import {
    TreeNodeItem,
    TreeNodeSelectItem
} from '../../../../../models/dot-edit-content-host-folder-field.interface';
import { DotEditContentService } from '../../../../../services/dot-edit-content.service';

export const PEER_PAGE_LIMIT = 7000;

export interface Content {
    id: string;
    image: string;
    title: string;
    modifiedBy: string;
    lastModified: Date;
}

export interface SelectExisingFileState {
    folders: {
        data: TreeNodeItem[];
        status: ComponentStatus;
    };
    content: {
        data: Content[];
        status: ComponentStatus;
    };
    nodeExpaned: TreeNodeSelectItem['node'] | null;
    selectedFolder: TreeNode | null;
    selectedFile: DotCMSContentlet | null;
    searchQuery: string;
    viewMode: 'list' | 'grid';
}

const initialState: SelectExisingFileState = {
    folders: {
        data: [],
        status: ComponentStatus.INIT
    },
    content: {
        data: [],
        status: ComponentStatus.INIT
    },
    nodeExpaned: null,
    selectedFolder: null,
    selectedFile: null,
    searchQuery: '',
    viewMode: 'list'
};

export const SelectExisingFileStore = signalStore(
    withState(initialState),
    withComputed((state) => ({
        foldersIsLoading: computed(() => state.folders().status === ComponentStatus.LOADING),
        contentIsLoading: computed(() => state.content().status === ComponentStatus.LOADING)
    })),
    withMethods((store) => {
        const dotEditContentService = inject(DotEditContentService);

        return {
            loadContent: () => {
                const mockContent = faker.helpers.multiple(
                    () => ({
                        id: faker.string.uuid(),
                        image: faker.image.url(),
                        title: faker.commerce.productName(),
                        modifiedBy: faker.internet.displayName(),
                        lastModified: faker.date.recent()
                    }),
                    { count: 100 }
                );

                patchState(store, {
                    content: {
                        data: mockContent,
                        status: ComponentStatus.LOADED
                    }
                });
            },
            loadFolders: rxMethod<void>(
                pipe(
                    tap(() =>
                        patchState(store, {
                            folders: { ...store.folders(), status: ComponentStatus.LOADING }
                        })
                    ),
                    switchMap(() => {
                        return dotEditContentService
                            .getSitesTreePath({ perPage: PEER_PAGE_LIMIT, filter: '*' })
                            .pipe(
                                tapResponse({
                                    next: (data) =>
                                        patchState(store, {
                                            folders: { data, status: ComponentStatus.LOADED }
                                        }),
                                    error: () =>
                                        patchState(store, {
                                            folders: { data: [], status: ComponentStatus.ERROR }
                                        })
                                })
                            );
                    })
                )
            ),
            loadChildren: rxMethod<TreeNodeSelectItem>(
                pipe(
                    exhaustMap((event: TreeNodeSelectItem) => {
                        const { node } = event;
                        const { hostname, path } = node.data;

                        node.loading = true;

                        return dotEditContentService.getFoldersTreeNode(hostname, path).pipe(
                            tap((children) => {
                                node.loading = false;
                                node.leaf = true;
                                node.icon = 'pi pi-folder-open';
                                node.children = [...children];
                                patchState(store, { nodeExpaned: node });
                            })
                        );
                    })
                )
            )
        };
    })
);
