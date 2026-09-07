import { tapResponse } from '@ngrx/operators';
import { signalStoreFeature, type } from '@ngrx/signals';
import { Dispatcher, Events, on, withEventHandlers, withReducer } from '@ngrx/signals/events';

import { inject } from '@angular/core';

import { switchMap } from 'rxjs/operators';

import { ImageEditorState } from '../../models/image-editor.models';
import { DotImageEditorService } from '../../services/dot-image-editor.service';
import { imageEditorLifecycleEvents } from '../image-editor.events';
import { initialFocalPointState, initialImageEditorState } from '../image-editor.state';
import { contextFromParams, errorMessage } from '../image-editor.store-utils';

/**
 * Asset feature: resolves the asset being edited. A request resets the editor to
 * the new asset's context; the `loadAsset$` effect fetches its natural dimensions
 * and original size and folds them back in (or surfaces a load error).
 */
export function withAsset() {
    return signalStoreFeature(
        type<{ state: ImageEditorState }>(),
        withReducer(
            on(imageEditorLifecycleEvents.assetRequested, ({ payload }, _state) => ({
                ...initialImageEditorState,
                assetContext: contextFromParams(payload),
                // Seed the focal point from the asset so an existing focal point is
                // preserved on Save; the baseline lets a move-and-back read as pristine.
                focalPoint: payload.focalPoint ?? initialFocalPointState,
                seededFocalPoint: payload.focalPoint ?? initialFocalPointState,
                previewStatus: 'loading' as const
            })),
            on(imageEditorLifecycleEvents.assetLoaded, ({ payload }, state) => ({
                ...state,
                assetContext: {
                    ...state.assetContext,
                    naturalWidth: payload.naturalWidth,
                    naturalHeight: payload.naturalHeight
                },
                fileInfo: {
                    ...state.fileInfo,
                    originalBytes: payload.originalBytes,
                    currentBytes: payload.originalBytes
                }
            })),
            on(imageEditorLifecycleEvents.assetLoadFailed, ({ payload }, state) => ({
                ...state,
                previewStatus: 'error' as const,
                error: errorMessage(payload, 'Failed to load image')
            }))
        ),
        withEventHandlers((store) => {
            const events = inject(Events);
            const dispatcher = inject(Dispatcher);
            const service = inject(DotImageEditorService);

            return {
                // Load asset metadata whenever a new asset is requested.
                loadAsset$: events.on(imageEditorLifecycleEvents.assetRequested).pipe(
                    switchMap(() =>
                        service.loadAssetMeta(store.assetContext()).pipe(
                            tapResponse({
                                next: (meta) =>
                                    dispatcher.dispatch(
                                        imageEditorLifecycleEvents.assetLoaded(meta)
                                    ),
                                error: (error) =>
                                    dispatcher.dispatch(
                                        imageEditorLifecycleEvents.assetLoadFailed(error)
                                    )
                            })
                        )
                    )
                )
            };
        })
    );
}
