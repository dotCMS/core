import { tapResponse } from '@ngrx/operators';
import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe } from 'rxjs';

import { computed, inject } from '@angular/core';

import { switchMap, tap } from 'rxjs/operators';

import { DotAiService } from '@dotcms/data-access';
import {
    AIImagePrompt,
    ComponentStatus,
    DotAIImageOrientation,
    DotGeneratedAIImage,
    PromptType
} from '@dotcms/dotcms-models';

export interface AiImagePromptdState {
    context: string | null;
    images: DotGeneratedAIImage[];
    status: ComponentStatus;
    galleryActiveIndex: number;
    formValue: AIImagePrompt;
    error: string | null;
}

const initialState: AiImagePromptdState = {
    status: ComponentStatus.INIT,
    images: [],
    context: null,
    galleryActiveIndex: 0,
    error: null,
    formValue: {
        text: '',
        type: PromptType.INPUT,
        size: DotAIImageOrientation.HORIZONTAL
    }
};

export const DotAiImagePromptStore = signalStore(
    { protectedState: false }, // TODO: remove when the unit tests are fixed
    withState(initialState),
    withComputed(({ status, context, images, galleryActiveIndex }) => ({
        isLoading: computed(() => {
            const currentStatus = status();

            return currentStatus === ComponentStatus.LOADING;
        }),
        hasContext: computed(() => {
            const currentContext = context();

            return !!currentContext;
        }),
        currentImage: computed(() => {
            const currentImages = images();
            const currentGalleryActiveIndex = galleryActiveIndex();

            return currentImages[currentGalleryActiveIndex];
        }),
        hasImages: computed(() => {
            const currentImages = images();

            return currentImages.length > 0;
        }),
        currentImageHasError: computed(() => {
            const currentImages = images();
            const currentGalleryActiveIndex = galleryActiveIndex();

            const currentImage = currentImages[currentGalleryActiveIndex];

            return currentImage?.error;
        })
    })),
    withMethods((store) => {
        const dotAiService = inject(DotAiService);

        return {
            setGalleryActiveIndex: (galleryActiveIndex: number) => {
                patchState(store, { galleryActiveIndex });
            },
            setContext: (context: string) => {
                patchState(store, { context });
            },
            setFormValue: (formValue: AIImagePrompt) => {
                patchState(store, { formValue });
            },
            generateImage: rxMethod<void>(
                pipe(
                    tap(() => {
                        patchState(store, { status: ComponentStatus.LOADING });
                    }),
                    switchMap(() => {
                        const images = store.images();
                        const galleryActiveIndex = store.galleryActiveIndex();
                        const formValue = store.formValue();
                        const context = store.context();

                        const isImageWithError = !!images[galleryActiveIndex]?.error;
                        const imagesArray = [...images];
                        const cleanPrompt = formValue.text?.trim() ?? '';
                        const finalPrompt =
                            formValue.type === PromptType.AUTO && context
                                ? `illustrate the following content: ${context}`
                                : cleanPrompt;

                        return dotAiService
                            .generateAndPublishImage(finalPrompt, formValue.size)
                            .pipe(
                                tapResponse({
                                    next: (response) => {
                                        const newImage: DotGeneratedAIImage = {
                                            request: formValue,
                                            response: response,
                                            error: null
                                        };
                                        if (isImageWithError) {
                                            imagesArray[galleryActiveIndex] = newImage;
                                        } else {
                                            imagesArray.push(newImage);
                                        }
                                        patchState(store, {
                                            status: ComponentStatus.IDLE,
                                            images: imagesArray,
                                            galleryActiveIndex: isImageWithError
                                                ? galleryActiveIndex
                                                : imagesArray.length - 1
                                        });
                                    },
                                    error: (error: string) => {
                                        patchState(store, {
                                            status: ComponentStatus.ERROR,
                                            error: error
                                        });
                                    }
                                })
                            );
                    })
                )
            )
        };
    })
);
