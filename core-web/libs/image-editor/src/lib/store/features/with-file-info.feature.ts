import { signalStoreFeature, type } from '@ngrx/signals';
import { on, withReducer } from '@ngrx/signals/events';

import { COMPRESSION_LABELS, RANGES } from '../../image-editor.constants';
import { FileInfoState, ImageEditorState } from '../../models/image-editor.models';
import { clamp } from '../../utils/dimensions.util';
import { imageEditorFileInfoEvents } from '../image-editor.events';
import { fileInfoPatch } from '../image-editor.store-utils';

/**
 * File info feature: compression strategy and quality. Folds both into the
 * `fileInfo` slice (quality clamped to {@link RANGES}) as coalesced history
 * entries via {@link fileInfoPatch}.
 */
export function withFileInfo() {
    return signalStoreFeature(
        type<{ state: ImageEditorState }>(),
        withReducer(
            on(imageEditorFileInfoEvents.compressionChanged, ({ payload }, state) => {
                const fileInfo: FileInfoState = { ...state.fileInfo, compression: payload };

                return fileInfoPatch(
                    state,
                    fileInfo,
                    'compression',
                    `Compression ${COMPRESSION_LABELS[payload]}`
                );
            }),
            on(imageEditorFileInfoEvents.qualityChanged, ({ payload }, state) => {
                const value = clamp(payload, RANGES.quality.min, RANGES.quality.max);
                const fileInfo: FileInfoState = { ...state.fileInfo, quality: value };

                return fileInfoPatch(state, fileInfo, 'compression', `Quality ${value}`);
            })
        )
    );
}
