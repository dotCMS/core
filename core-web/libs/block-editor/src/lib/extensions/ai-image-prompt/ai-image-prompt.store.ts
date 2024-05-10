import { ComponentStore, tapResponse } from '@ngrx/component-store';
import { Observable, of } from 'rxjs';

import { Injectable } from '@angular/core';

import { switchMap, withLatestFrom } from 'rxjs/operators';

import { ComponentStatus } from '@dotcms/dotcms-models';

import { PromptType } from './ai-image-prompt.models';

import { DotAiService } from '../../shared';
import {
    AIImagePrompt,
    DotAIImageContent,
    DotAIImageOrientation,
    DotGeneratedAIImage
} from '../../shared/services/dot-ai/dot-ai.models';

const DEFAULT_INPUT_PROMPT = PromptType.INPUT;

export interface DotAiImagePromptComponentState {
    showDialog: boolean;
    editorContent: string | null;
    images: DotGeneratedAIImage[];
    status: ComponentStatus;
    selectedImage: DotGeneratedAIImage | null;
    galleryActiveIndex: number;
    formValue: AIImagePrompt;
}

export interface VmAiImagePrompt {
    showDialog: boolean;
    status: ComponentStatus;
    images: DotGeneratedAIImage[];
    galleryActiveIndex: number;
    isLoading: boolean;
    formValue: AIImagePrompt;
}

const initialState: DotAiImagePromptComponentState = {
    showDialog: false,
    status: ComponentStatus.INIT,
    images: [],
    editorContent: null,
    selectedImage: null,
    galleryActiveIndex: 0,
    formValue: {
        text: '',
        type: DEFAULT_INPUT_PROMPT,
        size: DotAIImageOrientation.HORIZONTAL
    }
};

@Injectable({ providedIn: 'root' })
export class DotAiImagePromptStore extends ComponentStore<DotAiImagePromptComponentState> {
    //Selectors
    readonly isOpenDialog$ = this.select(this.state$, ({ showDialog }) => showDialog);
    readonly isLoading$ = this.select(
        this.state$,
        ({ status }) => status === ComponentStatus.LOADING
    );

    readonly selectedImage$ = this.select(this.state$, ({ selectedImage }) => selectedImage);

    //Updaters

    readonly showDialog = this.updater((state, editorContent: string) => ({
        ...state,
        showDialog: true,
        selectedPromptType: DEFAULT_INPUT_PROMPT,
        editorContent
    }));

    readonly cleanError = this.updater((state) => ({
        ...state,
        error: ''
    }));

    readonly setFormValue = this.updater(
        (state: DotAiImagePromptComponentState, formValue: AIImagePrompt) => ({
            ...state,
            formValue
        })
    );

    readonly setSelectedImage = this.updater(
        (state: DotAiImagePromptComponentState, selectedImage: DotGeneratedAIImage) => ({
            ...state,
            selectedImage
        })
    );

    readonly setGalleryActiveIndex = this.updater(
        (state: DotAiImagePromptComponentState, galleryActiveIndex: number) => ({
            ...state,
            galleryActiveIndex
        })
    );

    readonly hideDialog = this.updater(() => ({
        ...initialState
    }));

    readonly vm$: Observable<VmAiImagePrompt> = this.select(
        this.state$,
        this.isLoading$,
        ({ showDialog, status, images, galleryActiveIndex, formValue }, isLoading) => ({
            showDialog,
            status,
            images,
            galleryActiveIndex,
            formValue,
            isLoading
        })
    );

    // Effects

    /**
     * The `generateImage` variable is a function that generates an image based on a given prompt.
     *
     * @param {Observable<string>} prompt$ - An observable representing the prompt.
     * @returns {Observable} - An observable that emits the generated image.
     */
    readonly generateImage = this.effect((trigger$: Observable<void>) => {
        return trigger$.pipe(
            withLatestFrom(this.state$),
            switchMap(([_, { editorContent, formValue, images, galleryActiveIndex }]) => {
                const isImageWithError = !!images[galleryActiveIndex]?.error;
                const imagesArray = [...images];
                const cleanPrompt = formValue.text?.trim() ?? '';
                const finalPrompt =
                    formValue.type === PromptType.AUTO && editorContent
                        ? `illustrate the following content: ${editorContent}`
                        : cleanPrompt;

                this.patchState({
                    status: ComponentStatus.LOADING
                });

                return this.dotAiService.generateAndPublishImage(finalPrompt, formValue.size).pipe(
                    tapResponse(
                        (response) => {
                            this.updateImageState(
                                response,
                                formValue,
                                isImageWithError,
                                imagesArray,
                                galleryActiveIndex
                            );
                        },
                        (error: string) => {
                            this.updateImageState(
                                null,
                                formValue,
                                isImageWithError,
                                imagesArray,
                                galleryActiveIndex,
                                error
                            );

                            return of(null);
                        }
                    )
                );
            })
        );
    });

    private updateImageState(
        response: DotAIImageContent,
        formValue: AIImagePrompt,
        isImageWithError: boolean,
        imagesArray: DotGeneratedAIImage[],
        galleryActiveIndex: number,
        error?: string
    ) {
        const newImage: DotGeneratedAIImage = {
            request: formValue,
            response: response,
            error: error
        };

        if (isImageWithError) {
            imagesArray[galleryActiveIndex] = newImage;
        } else {
            imagesArray.push(newImage);
        }

        this.patchState({
            status: ComponentStatus.IDLE,
            images: imagesArray,
            galleryActiveIndex: isImageWithError ? galleryActiveIndex : imagesArray.length - 1
        });
    }

    constructor(private dotAiService: DotAiService) {
        super(initialState);
    }
}
