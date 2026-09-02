import { patchState, signalStoreFeature, type, withMethods, withState } from '@ngrx/signals';

import {
    DotCMSContentlet,
    DotContentDriveFolder,
    DotContentDriveItem
} from '@dotcms/dotcms-models';

import { DotContentDriveState } from '../../../shared/models';
import { isFolder } from '../../../utils/functions';

interface WithDraggingState {
    dragItems: {
        folders: DotContentDriveFolder[];
        contentlets: DotCMSContentlet[];
    };
}

/**
 * @description withDragging feature
 * @returns {SignalStoreFeature}
 */
export function withDragging() {
    return signalStoreFeature(
        {
            state: type<DotContentDriveState>()
        },
        withState<WithDraggingState>({
            dragItems: {
                folders: [],
                contentlets: []
            }
        }),
        withMethods((store) => ({
            /**
             * Sets the items being dragged
             */
            setDragItems: (items: DotContentDriveItem[]) => {
                const dragItems = items.reduce(
                    (acc, item) => {
                        if (isFolder(item)) {
                            acc.folders.push(item);
                        } else {
                            acc.contentlets.push(item);
                        }
                        return acc;
                    },
                    {
                        folders: [] as DotContentDriveFolder[],
                        contentlets: [] as DotCMSContentlet[]
                    }
                );

                patchState(store, {
                    dragItems
                });
            },

            /**
             * Clears the dragged items
             */
            cleanDragItems: () => {
                patchState(store, { dragItems: { folders: [], contentlets: [] } });
            }
        }))
    );
}
