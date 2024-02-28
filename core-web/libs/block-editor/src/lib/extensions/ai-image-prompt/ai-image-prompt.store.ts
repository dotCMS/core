import { ComponentStore, tapResponse } from '@ngrx/component-store';
import { EMPTY, Observable, of } from 'rxjs';

import { Injectable } from '@angular/core';

import { SelectItem } from 'primeng/api';

import { switchMap, tap, withLatestFrom } from 'rxjs/operators';

import { ComponentStatus } from '@dotcms/dotcms-models';

import { PromptType } from './ai-image-prompt.models';

import { AIImageSize, DotAiService } from '../../shared';
import { DotAIImageGenerationResponse } from '../../shared/services/dot-ai/dot-ai.models';

const DEFAULT_INPUT_PROMPT: PromptType = 'input';

export interface AIImagePrompt {
    text: string;
    type: PromptType;
    size: AIImageSize;
}

export interface DotAiImagePromptComponentState {
    showDialog: boolean;
    prompt: string | null; // we always have the final prompt here
    editorContent: string | null;
    images: DotAIImageGenerationResponse[];
    status: ComponentStatus;
    error: string;
    orientationOptions: SelectItem<string>[];
    selectedImage: DotAIImageGenerationResponse | null;
    galleryActiveIndex: number;
}

export interface VmAiImagePrompt {
    showDialog: boolean;
    status: ComponentStatus;
    orientationOptions: SelectItem[];
    images: DotAIImageGenerationResponse[];
    galleryActiveIndex: number;
}

const initialState: DotAiImagePromptComponentState = {
    showDialog: false,
    status: ComponentStatus.INIT,
    images: [],
    prompt: null,
    editorContent: null,
    error: '',
    orientationOptions: [
        { value: '1792x1024', label: 'Horizontal (1792 x 1024)' },
        { value: '1024x1024', label: 'Square (1024 x 1024)' },
        { value: '1024x1792', label: 'Vertical (1024 x 1792)' }
    ],
    selectedImage: null,
    galleryActiveIndex: 0
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

    readonly hideDialog = this.updater((state) => ({
        ...state,
        showDialog: false,
        selectedPromptType: null
    }));

    readonly vm$: Observable<VmAiImagePrompt> = this.select(
        this.state$,
        ({ showDialog, status, orientationOptions, images,galleryActiveIndex }) => ({
            showDialog,
            status,
            orientationOptions,
            images,
            galleryActiveIndex
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
                                images: [...state.images, response],
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
