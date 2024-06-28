import { patchState } from '@ngrx/signals';

import type { TreeNodeSelectItem } from './../../../../models/dot-edit-content-host-folder-field.interface';

export const chooseNode = (store) => {
    return (event: TreeNodeSelectItem) => {
        const { node: nodeSelected } = event;
        const data = nodeSelected.data;
        if (!data) {
            return;
        }

        patchState(store, { nodeSelected });
    };
};
