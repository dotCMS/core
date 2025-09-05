import { patchState, signalStoreFeature, withMethods, type, withState } from '@ngrx/signals';

import { DotContentDriveContextMenu, DotContentDriveState } from '../../shared/models';

interface WithContextMenuState {
    contextMenu: DotContentDriveContextMenu | null;
}

export function withContextMenu() {
    return signalStoreFeature(
        {
            state: type<DotContentDriveState>()
        },
        withState<WithContextMenuState>({
            contextMenu: {
                triggeredEvent: null,
                contentlet: null,
                showAddToBundle: false
            }
        }),
        withMethods((store) => ({
            setContextMenu: (contextMenu: DotContentDriveContextMenu) => {
                patchState(store, { contextMenu });
            },
            patchContextMenu(contextMenu: Partial<DotContentDriveContextMenu>) {
                patchState(store, { contextMenu: { ...store.contextMenu(), ...contextMenu } });
            },
            resetContextMenu: () => {
                patchState(store, {
                    contextMenu: { triggeredEvent: null, contentlet: null, showAddToBundle: false }
                });
            },
            setShowAddToBundle: (showAddToBundle: boolean) => {
                patchState(store, { contextMenu: { ...store.contextMenu(), showAddToBundle } });
            }
        }))
    );
}
