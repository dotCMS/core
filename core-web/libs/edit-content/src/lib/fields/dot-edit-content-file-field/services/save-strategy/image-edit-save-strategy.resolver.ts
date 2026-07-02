import { inject, Injectable } from '@angular/core';

import { BinaryImageEditSaveStrategy } from './binary-image-edit-save.strategy';
import { DotAssetImageEditSaveStrategy } from './dotasset-image-edit-save.strategy';
import { ImageEditSaveStrategy } from './image-edit-save-strategy.model';

import { INPUT_TYPE, INPUT_TYPES } from '../../../../models/dot-edit-content-file.model';

/**
 * Resolves the {@link ImageEditSaveStrategy} for a field's input type.
 *
 * Binary fields apply the edit inline; everything else (Image/File) is a
 * reference-backed field and versions the referenced `dotAsset`.
 */
@Injectable()
export class ImageEditSaveStrategyResolver {
    readonly #binary = inject(BinaryImageEditSaveStrategy);
    readonly #dotAsset = inject(DotAssetImageEditSaveStrategy);

    resolve(inputType: INPUT_TYPE | null): ImageEditSaveStrategy {
        return inputType === INPUT_TYPES.Binary ? this.#binary : this.#dotAsset;
    }
}
