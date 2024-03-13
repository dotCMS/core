import { ComponentStore, tapResponse } from '@ngrx/component-store';
import { EMPTY, Observable, of } from 'rxjs';

import { Injectable } from '@angular/core';

import { switchMap, tap, withLatestFrom } from 'rxjs/operators';

import { ComponentStatus } from '@dotcms/dotcms-models';

import { PromptType } from './ai-image-prompt.models';

import { DotAiService } from '../../shared';
import {
    AIImagePrompt,
    DotAIImageOrientation,
    DotGeneratedAIImage
} from '../../shared/services/dot-ai/dot-ai.models';

const DEFAULT_INPUT_PROMPT = PromptType.INPUT;

export interface DotAiImagePromptComponentState {
    showDialog: boolean;
    prompt: string | null; // we always have the final prompt here
    editorContent: string | null;
    images: DotGeneratedAIImage[];
    status: ComponentStatus;
    error: string;
    selectedImage: DotGeneratedAIImage | null;
    galleryActiveIndex: number;
    orientation: DotAIImageOrientation;
}

export interface VmAiImagePrompt {
    showDialog: boolean;
    status: ComponentStatus;
    images: DotGeneratedAIImage[];
    galleryActiveIndex: number;
    orientation: DotAIImageOrientation;
    isLoading: boolean;
}

const initialState: DotAiImagePromptComponentState = {
    showDialog: false,
    status: ComponentStatus.INIT,
    images: [],
    prompt: null,
    editorContent: null,
    error: '',
    selectedImage: null,
    galleryActiveIndex: 0,
    orientation: DotAIImageOrientation.HORIZONTAL
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

    readonly errorMsg$ = this.select(this.state$, ({ error }) => error);
    readonly getImages$ = this.select(this.state$, ({ images }) => images);

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

    readonly setSelectedImage = this.updater(
        (state: DotAiImagePromptComponentState, selectedImage: DotGeneratedAIImage) => {
            return { ...state, selectedImage };
        }
    );

    readonly hideDialog = this.updater(() => ({
        ...initialState
    }));

    readonly vm$: Observable<VmAiImagePrompt> = this.select(
        this.state$,
        this.isLoading$,
        ({ showDialog, status, images, galleryActiveIndex, orientation }, isLoading) => ({
            showDialog,
            status,
            images,
            galleryActiveIndex,
            orientation,
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
    readonly generateImage = this.effect((prompt$: Observable<AIImagePrompt>) => {
        return prompt$.pipe(
            withLatestFrom(this.state$),
            switchMap(([prompt, { editorContent }]) => {
                const cleanPrompt = prompt.text?.trim() ?? '';

                const finalPrompt =
                    prompt.type === 'auto' && editorContent
                        ? `${cleanPrompt} to illustrate the following content: ${editorContent}`
                        : cleanPrompt;

                this.patchState({
                    status: ComponentStatus.LOADING,
                    prompt: finalPrompt,
                    error: ''
                });

                return this.dotAiService.generateAndPublishImage(finalPrompt, prompt.size).pipe(
                    tapResponse(
                        (response) => {
                            this.patchState((state) => ({
                                status: ComponentStatus.IDLE,
                                images: [...state.images, { request: prompt, response: response }],
                                galleryActiveIndex: state.images.length
                            }));
                        },
                        (error: string) => {
                            this.patchState({ status: ComponentStatus.IDLE, error });

                            return of(null);
                        }
                    )
                );
            })
        );
    });

    /**
     * Regenerate the image and call the generateImage effect with withLatestFrom prompt
     */
    readonly reGenerateContent = this.effect((trigger$: Observable<void>) => {
        return trigger$.pipe(
            withLatestFrom(this.state$),
            tap(([_, { prompt }]) => {
                if (!prompt) return EMPTY;

                return this.generateImage(of(prompt));
            })
        );
    });

    constructor(private dotAiService: DotAiService) {
        super(initialState);
    }
}
