import { tapResponse } from '@ngrx/operators';
import { signalStoreFeature, type } from '@ngrx/signals';
import { Dispatcher, Events, on, withEventHandlers, withReducer } from '@ngrx/signals/events';

import { inject, Signal } from '@angular/core';

import { exhaustMap } from 'rxjs/operators';

import { AppliedFilter, ImageEditorState } from '../../models/image-editor.models';
import { DotImageEditorService } from '../../services/dot-image-editor.service';
import { buildSaveUrl } from '../../utils/image-filter-url.builder';
import { imageEditorLifecycleEvents } from '../image-editor.events';
import { errorMessage } from '../image-editor.store-utils';

/**
 * Save feature: stages the current edits (preview filter chain + focal point) into a
 * temp file. A `saveRequested` marks the editor saving; the `save$` effect GETs the
 * Save URL and folds the resulting temp file (`saveSucceeded`) or error (`saveFailed`)
 * back in. Consumes the `appliedFilters` selector from {@link withPreview}, so it
 * composes after it.
 */
export function withSave() {
    return signalStoreFeature(
        type<{ state: ImageEditorState; props: { appliedFilters: Signal<AppliedFilter[]> } }>(),
        withReducer(
            on(imageEditorLifecycleEvents.saveRequested, (_event, state) => ({
                ...state,
                saveStatus: 'saving' as const,
                saveError: null
            })),
            on(imageEditorLifecycleEvents.saveSucceeded, (_event, state) => ({
                ...state,
                saveStatus: 'idle' as const
            })),
            on(imageEditorLifecycleEvents.saveFailed, ({ payload }, state) => ({
                ...state,
                saveStatus: 'error' as const,
                saveError: errorMessage(payload, 'Failed to save image')
            }))
        ),
        withEventHandlers((store) => {
            const events = inject(Events);
            const dispatcher = inject(Dispatcher);
            const service = inject(DotImageEditorService);

            return {
                // Save the edited image whenever the user requests it. `exhaustMap` (not
                // switchMap) so a re-triggered save is ignored while one is in flight, rather
                // than cancelling the in-flight request. The Save button is already disabled
                // while saving, so this is defense-in-depth for non-UI triggers.
                save$: events.on(imageEditorLifecycleEvents.saveRequested).pipe(
                    exhaustMap(() => {
                        // Capture the focal point at save time and fold it into the returned
                        // temp's metadata. The servlet writes the focal to the temp's metadata
                        // STORAGE, but the serialized DotCMSTempFile.metadata is basic (no
                        // focalPoint), so an in-session reopen would read the temp metadata and
                        // seed the marker at centre. Injecting it here mirrors the contentlet
                        // read-back used after a page refresh, keeping both paths consistent.
                        const focalPoint = store.focalPoint();

                        return service
                            .saveEditedImage(
                                buildSaveUrl(
                                    store.assetContext(),
                                    store.appliedFilters(),
                                    focalPoint,
                                    store.seededFocalPoint(),
                                    store.assetContext().variable
                                )
                            )
                            .pipe(
                                tapResponse({
                                    next: (tempFile) =>
                                        dispatcher.dispatch(
                                            imageEditorLifecycleEvents.saveSucceeded({
                                                ...tempFile,
                                                metadata: tempFile.metadata
                                                    ? {
                                                          ...tempFile.metadata,
                                                          focalPoint: `${focalPoint.x},${focalPoint.y}`
                                                      }
                                                    : tempFile.metadata
                                            })
                                        ),
                                    error: (error) =>
                                        dispatcher.dispatch(
                                            imageEditorLifecycleEvents.saveFailed(error)
                                        )
                                })
                            );
                    })
                )
            };
        })
    );
}
