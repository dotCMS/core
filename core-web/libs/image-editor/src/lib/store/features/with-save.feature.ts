import { tapResponse } from '@ngrx/operators';
import { signalStoreFeature, type, withComputed } from '@ngrx/signals';
import { Dispatcher, Events, on, withEventHandlers, withReducer } from '@ngrx/signals/events';
import { EMPTY } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { computed, inject, Signal } from '@angular/core';

import { catchError, exhaustMap, switchMap, tap } from 'rxjs/operators';

import { DotHttpErrorManagerService } from '@dotcms/data-access';
import { DotCMSTempFile } from '@dotcms/dotcms-models';

import { AppliedFilter, ImageEditorState } from '../../models/image-editor.models';
import { DotImageEditorService } from '../../services/dot-image-editor.service';
import { imageEditorLifecycleEvents } from '../image-editor.events';
import { errorMessage } from '../image-editor.store-utils';

/**
 * Save feature: persisting the edited image and triggering downloads. Consumes
 * the `previewUrl` and `appliedFilters` selectors from {@link withPreview} (so it
 * composes after it), exposes `canSave`, persists the focal point before saving
 * and keeps the editor open on failure (the store is the single error surface).
 */
export function withSave() {
    return signalStoreFeature(
        type<{
            state: ImageEditorState;
            props: { previewUrl: Signal<string>; appliedFilters: Signal<AppliedFilter[]> };
        }>(),
        withReducer(
            on(imageEditorLifecycleEvents.saveRequested, (_event, state) => ({
                ...state,
                saveStatus: 'saving' as const
            })),
            on(imageEditorLifecycleEvents.saveAsRequested, (_event, state) => ({
                ...state,
                saveStatus: 'saving' as const
            })),
            on(imageEditorLifecycleEvents.saveSucceeded, ({ payload }, state) => ({
                ...state,
                savedTempFile: payload,
                saveStatus: 'saved' as const
            })),
            on(imageEditorLifecycleEvents.saveFailed, ({ payload }, state) => ({
                ...state,
                saveStatus: 'error' as const,
                error: errorMessage(payload, 'Failed to save image')
            }))
        ),
        withComputed((store) => ({
            /** Whether a save can be initiated right now. */
            canSave: computed(
                () =>
                    store.previewStatus() === 'loaded' &&
                    store.saveStatus() !== 'saving' &&
                    store.appliedFilters().length > 0
            )
        })),
        withEventHandlers((store) => {
            const events = inject(Events);
            const dispatcher = inject(Dispatcher);
            const service = inject(DotImageEditorService);
            const httpErrorManager = inject(DotHttpErrorManagerService);

            // Save the edited image and dispatch the outcome.
            const saveEditedImage$ = () =>
                service.saveEditedImage(store.previewUrl(), store.assetContext().variable).pipe(
                    tapResponse({
                        next: (tempFile: DotCMSTempFile) =>
                            dispatcher.dispatch(imageEditorLifecycleEvents.saveSucceeded(tempFile)),
                        error: (error: HttpErrorResponse) => {
                            // Surface the error but keep the editor open for retry.
                            httpErrorManager.handle(error);
                            dispatcher.dispatch(imageEditorLifecycleEvents.saveFailed(error));
                        }
                    }),
                    // Swallow the rethrown error so the effect stream stays alive.
                    catchError(() => EMPTY)
                );

            return {
                // Persist the focal point first (when active), then save the image.
                // `exhaustMap` ignores new save triggers while one is in flight: a
                // destructive write must not be cancelled mid-flight, which would
                // strand `saveStatus: 'saving'` with no terminal event.
                save$: events
                    .on(
                        imageEditorLifecycleEvents.saveRequested,
                        imageEditorLifecycleEvents.saveAsRequested
                    )
                    .pipe(
                        exhaustMap(() => {
                            const focalPoint = store.focalPoint();

                            if (!focalPoint.active) {
                                return saveEditedImage$();
                            }

                            return service
                                .persistFocalPoint(store.assetContext().originalUrl, {
                                    x: focalPoint.x,
                                    y: focalPoint.y
                                })
                                .pipe(switchMap(() => saveEditedImage$()));
                        })
                    ),

                // Trigger a client-side download of the current preview.
                download$: events
                    .on(imageEditorLifecycleEvents.downloadRequested)
                    .pipe(
                        tap(() =>
                            service.triggerDownload(
                                store.previewUrl(),
                                store.assetContext().fileName
                            )
                        )
                    )
            };
        })
    );
}
