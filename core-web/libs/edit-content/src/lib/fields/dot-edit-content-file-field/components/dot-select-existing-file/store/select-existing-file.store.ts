import { faker } from '@faker-js/faker';
import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';

import { computed } from '@angular/core';

import { TreeNode } from 'primeng/api';

import { ComponentStatus, DotCMSContentlet } from '@dotcms/dotcms-models';

export interface Content {
    id: string;
    image: string;
    title: string;
    modifiedBy: string;
    lastModified: Date;
}

export interface SelectExisingFileState {
    folders: {
        data: TreeNode[];
        status: ComponentStatus;
    };
    content: {
        data: Content[];
        status: ComponentStatus;
    };
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
            loadFolders: () => {
                const mockFolders = [
                    {
                        label: 'demo.dotcms.com',
                        expandedIcon: 'pi pi-folder-open',
                        collapsedIcon: 'pi pi-folder',
                        children: [
                            {
                                label: 'demo.dotcms.com',
                                expandedIcon: 'pi pi-folder-open',
                                collapsedIcon: 'pi pi-folder',
                                children: [
                                    {
                                        label: 'documents'
                                    }
                                ]
                            },
                            {
                                label: 'demo.dotcms.com',
                                expandedIcon: 'pi pi-folder-open',
                                collapsedIcon: 'pi pi-folder'
                            }
                        ]
                    },
                    {
                        label: 'nico.dotcms.com',
                        expandedIcon: 'pi pi-folder-open',
                        collapsedIcon: 'pi pi-folder'
                    }
                ];

                patchState(store, {
                    folders: {
                        data: mockFolders,
                        status: ComponentStatus.LOADED
                    }
                });
            }
        };
    })
);
