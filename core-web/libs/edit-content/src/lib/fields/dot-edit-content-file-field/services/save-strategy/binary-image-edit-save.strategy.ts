import { inject, Injectable } from '@angular/core';

import { DotCMSTempFile } from '@dotcms/dotcms-models';

import { ImageEditSaveStrategy } from './image-edit-save-strategy.model';

import { FileFieldStore } from '../../store/file-field.store';

/**
 * Binary field save strategy — the existing behavior.
 *
 * The binary lives inline on the contentlet, so applying the edit just swaps the
 * preview to the edited temp file; its temp id becomes the field value and is
 * resolved to a binary on the standard contentlet check-in.
 */
@Injectable()
export class BinaryImageEditSaveStrategy implements ImageEditSaveStrategy {
    readonly #store = inject(FileFieldStore);

    apply(tempFile: DotCMSTempFile): void {
        this.#store.applyTempFile(tempFile);
    }
}
