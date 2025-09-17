import { patchState, signalStoreFeature, withMethods, type, withState } from '@ngrx/signals';

import { DotContentDriveDialog, DotContentDriveState } from '../../../shared/models';

interface WithDialogState {
    dialog?: DotContentDriveDialog;
}

export function withDialog() {
    return signalStoreFeature(
        {
            state: type<DotContentDriveState>()
        },
        withState<WithDialogState>({
            dialog: undefined
        }),
        withMethods((store) => ({
            setDialog: (dialog: DotContentDriveDialog) => {
                patchState(store, { dialog });
            },
            resetDialog: () => {
                patchState(store, { dialog: undefined });
            }
        }))
    );
}
