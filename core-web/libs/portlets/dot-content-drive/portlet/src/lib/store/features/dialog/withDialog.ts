import { patchState, signalStoreFeature, withMethods, type, withState } from '@ngrx/signals';

import {
    DotContentDriveDialog,
    DotContentDriveDialogDrillDown,
    DotContentDriveState
} from '../../../shared/models';

interface WithDialogState {
    dialog?: DotContentDriveDialog;
    dialogDrillDown?: DotContentDriveDialogDrillDown;
}

export function withDialog() {
    return signalStoreFeature(
        {
            state: type<DotContentDriveState>()
        },
        withState<WithDialogState>({
            dialog: undefined,
            dialogDrillDown: undefined
        }),
        withMethods((store) => ({
            setDialog: (dialog: DotContentDriveDialog) => {
                // A fresh dialog always starts at its top level, never inside a previous drill-down.
                patchState(store, { dialog, dialogDrillDown: undefined });
            },
            closeDialog: () => {
                patchState(store, { dialog: undefined, dialogDrillDown: undefined });
            },
            /** Replaces the dialog header while its body is inside a sub-screen. */
            setDialogDrillDown: (dialogDrillDown: DotContentDriveDialogDrillDown) => {
                patchState(store, { dialogDrillDown });
            },
            /** Restores the dialog's own header. */
            clearDialogDrillDown: () => {
                patchState(store, { dialogDrillDown: undefined });
            }
        }))
    );
}
