import { patchState, signalStoreFeature, type, withComputed, withMethods } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { EMPTY, pipe } from 'rxjs';

import { computed, inject } from '@angular/core';

import { catchError, exhaustMap, tap } from 'rxjs/operators';

import { DotAiContentService } from '@dotcms/data-access';
import { DotAIImageOrientation, DotAIImageResponse } from '@dotcms/dotcms-models';

import { DotAiPortletState } from '../../models/dot-ai-portlet.models';

/**
 * Image generation.
 *
 * Generate and Save are **separate** actions (FR-037). The existing
 * `generateAndPublishImage` chains straight into `createAndPublishContentlet`, so every
 * generation — including ones the user goes on to discard — publishes a live dotAsset. That
 * is why `generateImage` was extracted: this slice composes the two halves itself, and
 * nothing reaches the workflow endpoint until the user asks for it.
 */
export function withAiImage() {
    return signalStoreFeature(
        type<{ state: DotAiPortletState }>(),
        withComputed((store) => ({
            /**
             * Same-origin asset URL, used for both the preview and the download.
             *
             * A plain anchor to this is the whole download feature — no new backend, and the
             * legacy portlet already linked exactly this path.
             */
            imageUrl: computed(() => {
                const image = store.image();

                return image ? `/dA/${image.response}/asset.png` : null;
            })
        })),
        withMethods((store) => {
            const contentService = inject(DotAiContentService);

            /**
             * `DotAiContentService` rejects with a **string**, not an `HttpErrorResponse`: an
             * i18n key for the cases it recognises, otherwise the provider's own words. That
             * shape is the block editor's, and it is not ours to change, so the message is
             * rendered inline through `| dm` — which translates a key and passes prose
             * through untouched (FR-014). Routing it to `DotHttpErrorManagerService` matched
             * no handler at all, and the failure was silent.
             */
            const toMessage = (error: unknown): string =>
                typeof error === 'string' ? error : ((error as Error)?.message ?? String(error));

            return {
                setOrientation(imageOrientation: DotAIImageOrientation): void {
                    patchState(store, { imageOrientation });
                },

                dismissImageError(): void {
                    patchState(store, { imageError: null });
                },

                generateImage: rxMethod<string>(
                    pipe(
                        exhaustMap((prompt) => {
                            const trimmed = prompt.trim();

                            if (!trimmed) {
                                return EMPTY;
                            }

                            patchState(store, {
                                imageGenerating: true,
                                image: null,
                                imageError: null
                            });

                            return contentService
                                .generateImage(trimmed, store.imageOrientation())
                                .pipe(
                                    tap((response: DotAIImageResponse) =>
                                        patchState(store, {
                                            imageGenerating: false,
                                            image: {
                                                response: response.response,
                                                tempFileName: response.tempFileName,
                                                originalPrompt: trimmed,
                                                // The provider rewrites the prompt; always show
                                                // it, so the difference is never hidden.
                                                revisedPrompt: response.revised_prompt ?? trimmed,
                                                published: false,
                                                size: store.imageOrientation()
                                            }
                                        })
                                    ),
                                    catchError((error: unknown) => {
                                        patchState(store, {
                                            imageGenerating: false,
                                            imageError: toMessage(error)
                                        });

                                        return EMPTY;
                                    })
                                );
                        })
                    )
                ),

                saveImage: rxMethod<void>(
                    pipe(
                        // exhaustMap: a double click must not publish twice (FR-035).
                        exhaustMap(() => {
                            const image = store.image();

                            if (!image || image.published) {
                                return EMPTY;
                            }

                            patchState(store, { imageSaving: true, imageError: null });

                            return contentService
                                .createAndPublishContentlet({
                                    response: image.response,
                                    tempFileName: image.tempFileName
                                })
                                .pipe(
                                    tap(() =>
                                        patchState(store, {
                                            imageSaving: false,
                                            image: { ...image, published: true }
                                        })
                                    ),
                                    catchError((error: unknown) => {
                                        // The image stays on screen so the user can retry or
                                        // just download it instead (FR-040).
                                        patchState(store, {
                                            imageSaving: false,
                                            imageError: toMessage(error)
                                        });

                                        return EMPTY;
                                    })
                                );
                        })
                    )
                )
            };
        })
    );
}
