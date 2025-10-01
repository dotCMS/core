import { type, signalStoreFeature, withState, withHooks, patchState } from '@ngrx/signals';

import { effect, EffectRef, inject } from '@angular/core';

import { DotFolderService } from '@dotcms/data-access';

import { DotContentDriveState } from '../../../shared/models';
import { ALL_FOLDER } from '../../../utils/tree-folder.utils';

interface WithDotAssetState {
    allowedFileTypes: string;
}

export function withDotAsset() {
    return signalStoreFeature(
        {
            state: type<DotContentDriveState>()
        },
        withState<WithDotAssetState>({
            allowedFileTypes: ''
        }),
        withHooks((store) => {
            let pathEffect: EffectRef;
            const folderService = inject(DotFolderService);

            return {
                /**
                 * Initializes an effect that watches for changes to the selected folder path.
                 * When the path changes:
                 * 1. Gets the currently selected folder
                 * 2. If a valid folder is selected (not null and not ALL_FOLDER), fetches its allowed file masks
                 * 3. Transforms the file masks into a comma-separated list of allowed file extensions
                 * 4. Updates the store's allowedFileTypes state with the computed extensions
                 */
                onInit() {
                    pathEffect = effect(() => {
                        const currentFolder = store.selectedNode();

                        if (!currentFolder || currentFolder.key === ALL_FOLDER.key) {
                            return;
                        }

                        folderService
                            .getFileMasksForFolder(currentFolder.key)
                            .subscribe((filesMasks) => {
                                const allowedFileTypes = filesMasks
                                    .split(',')
                                    .map((fileMask) => fileMask.replace('*', ''))
                                    .join(',');

                                patchState(store, { allowedFileTypes });
                            });
                    });
                },
                onDestroy() {
                    pathEffect?.destroy();
                }
            };
        })
    );
}
