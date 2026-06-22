import { signalStoreFeature, type, withComputed } from '@ngrx/signals';
import { Dispatcher, on, withEventHandlers, withReducer } from '@ngrx/signals/events';

import { computed, inject } from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';

import { debounceTime, distinctUntilChanged, switchMap, tap } from 'rxjs/operators';

import { AUTO_PREVIEW_RETRY_LIMIT } from '../../image-editor.constants';
import { ImageEditorState } from '../../models/image-editor.models';
import { DotImageEditorService } from '../../services/dot-image-editor.service';
import { buildFilterChain, buildPreviewUrl } from '../../utils/image-filter-url.builder';
import { imageEditorLifecycleEvents } from '../image-editor.events';

/**
 * Preview feature — the heart of the "viewer". Derives the ordered server filter
 * chain and the cache-busted preview URL from the edit state, owns the preview
 * loading lifecycle (silent retry up to {@link AUTO_PREVIEW_RETRY_LIMIT}, then the
 * error UI) and resolves the rendered file size (debounced) as the URL changes.
 */
export function withPreview() {
    return signalStoreFeature(
        type<{ state: ImageEditorState }>(),
        withReducer(
            on(imageEditorLifecycleEvents.previewLoaded, (_event, state) => ({
                ...state,
                previewStatus: 'loaded' as const,
                previewRetries: 0,
                error: null
            })),
            // Image GETs for heavy filter chains can fail transiently even when the URL
            // is valid (a later HEAD/GET returns 200). Retry silently with a fresh
            // cache-bust up to AUTO_PREVIEW_RETRY_LIMIT before surfacing the error UI.
            on(imageEditorLifecycleEvents.previewErrored, (_event, state) => {
                if (state.previewRetries < AUTO_PREVIEW_RETRY_LIMIT) {
                    return {
                        ...state,
                        previewRetries: state.previewRetries + 1,
                        previewStatus: 'loading' as const,
                        cacheBust: state.cacheBust + 1
                    };
                }

                return {
                    ...state,
                    previewStatus: 'error' as const,
                    previewRetries: 0,
                    error: 'Failed to render preview'
                };
            }),
            on(imageEditorLifecycleEvents.retryRequested, (_event, state) => ({
                ...state,
                // A manual retry restores the silent-retry budget.
                previewRetries: 0,
                previewStatus: 'loading' as const,
                cacheBust: state.cacheBust + 1
            })),
            on(imageEditorLifecycleEvents.previewSizeResolved, ({ payload }, state) => ({
                ...state,
                fileInfo: { ...state.fileInfo, currentBytes: payload }
            }))
        ),
        withComputed((store) => {
            const appliedFilters = computed(() =>
                buildFilterChain({
                    adjust: store.adjust(),
                    transform: store.transform(),
                    crop: store.crop(),
                    fileInfo: store.fileInfo(),
                    naturalWidth: store.assetContext().naturalWidth,
                    naturalHeight: store.assetContext().naturalHeight
                })
            );

            return {
                /** The ordered server filter chain derived from the current edits. */
                appliedFilters,
                /** The fully-qualified, cache-busted preview URL for the current edits. */
                previewUrl: computed(() =>
                    buildPreviewUrl(store.assetContext(), appliedFilters(), store.cacheBust())
                ),
                /** Whether any edit produces a non-empty filter chain. */
                isDirty: computed(() => appliedFilters().length > 0),
                /** Whether the editor is mid-flight loading a preview or saving. */
                isBusy: computed(
                    () => store.previewStatus() === 'loading' || store.saveStatus() === 'saving'
                )
            };
        }),
        withEventHandlers((store) => {
            const dispatcher = inject(Dispatcher);
            const service = inject(DotImageEditorService);

            return {
                // Resolve the edited preview size, debounced against rapid edits.
                resolveSize$: toObservable(store.previewUrl).pipe(
                    debounceTime(250),
                    distinctUntilChanged(),
                    switchMap((url) =>
                        service
                            .getFileSize(url)
                            .pipe(
                                tap((bytes) =>
                                    dispatcher.dispatch(
                                        imageEditorLifecycleEvents.previewSizeResolved(bytes)
                                    )
                                )
                            )
                    )
                )
            };
        })
    );
}
