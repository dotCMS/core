import { patchState, signalStoreFeature, type, withMethods, withState } from '@ngrx/signals';

import { DotContentDriveItem } from '@dotcms/dotcms-models';

import { DotContentDriveState } from '../../../shared/models';

interface WithDraggingState {
    dragItems: DotContentDriveItem[];
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
            dragItems: []
        }),
        withMethods((store) => ({
            /**
             * Sets the items being dragged
             */
            setDragItems: (items: DotContentDriveItem[]) => {
                patchState(store, { dragItems: items });
            },

            /**
             * Clears the dragged items
             */
            cleanDragItems: () => {
                patchState(store, { dragItems: [] });
            }
        }))
    );
}
