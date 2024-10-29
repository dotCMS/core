import { signalStore, withComputed, withState } from '@ngrx/signals';

import { computed } from '@angular/core';

import { TreeNode } from 'primeng/api';

import { ComponentStatus, DotCMSContentlet } from '@dotcms/dotcms-models';

export interface SelectExisingFileState {
    folders: {
        data: TreeNode[];
        status: ComponentStatus;
    };
    content: {
        data: DotCMSContentlet[];
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
    }))
);
