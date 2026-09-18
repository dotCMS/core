import { patchState, signalStoreFeature, withMethods, type, withState } from '@ngrx/signals';

import {
    DotContentDriveDialog,
    DotContentDriveDialogDrillDown,
    DotContentDriveState
} from '../../../shared/models';

/**
 * Members are declared **required with an `| undefined` value**, not optional. In
 * `@ngrx/signals`, an optional state *key* makes the generated store *member* optional too, so
 * `store.x` itself becomes possibly-undefined and `store.x()` is not callable — that alone
 * accounted for 40 `TS2722` errors here. The values are still seeded and cleared with `undefined`,
 * so nothing changes at runtime.
 */
interface WithDialogState {
    dialog: DotContentDriveDialog | undefined;
    dialogDrillDown: DotContentDriveDialogDrillDown | undefined;
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
