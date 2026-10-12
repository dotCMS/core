import { patchState, signalStoreFeature, type, withMethods, withState } from '@ngrx/signals';

import { DotSourceEditorRequest } from '@dotcms/edit-content';

import { DotContentDriveState } from '../../../shared/models';

interface WithSourceEditorState {
    /** The file open in the Edit Source panel, or `null` when the panel is closed. */
    sourceEditor: DotSourceEditorRequest | null;
}

/**
 * Which file the Edit Source panel shows. Data only: the panel loads and saves the source itself,
 * because a failed save has to leave the author's edits on screen.
 */
export function withSourceEditor() {
    return signalStoreFeature(
        {
            state: type<DotContentDriveState>()
        },
        withState<WithSourceEditorState>({
            sourceEditor: null
        }),
        withMethods((store) => ({
            openSourceEditor: (sourceEditor: DotSourceEditorRequest) => {
                patchState(store, { sourceEditor });
            },
            closeSourceEditor: () => {
                patchState(store, { sourceEditor: null });
            }
        }))
    );
}
