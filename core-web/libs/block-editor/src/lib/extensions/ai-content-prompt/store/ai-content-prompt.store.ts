import { ComponentStore } from '@ngrx/component-store';
import { tapResponse } from '@ngrx/operators';
import { Observable } from 'rxjs';

import { inject, Injectable } from '@angular/core';

import { switchMap, withLatestFrom } from 'rxjs/operators';

import { DotAiService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';

export interface AiGenerateContent {
    prompt: string;
    content: string;
    error?: string;
}

export interface AiContentPromptState {
    prompt: string;
    generatedContent: AiGenerateContent[];
    selectedContent: string;
    activeIndex: number;
    status: ComponentStatus;
    showDialog: boolean;
    submitLabel: string;
}

const initialState: AiContentPromptState = {
    prompt: '',
    generatedContent: [],
    selectedContent: '',
    activeIndex: null,
    status: ComponentStatus.INIT,
    showDialog: false,
    submitLabel: 'block-editor.extension.ai-image.generate'
};

@Injectable({
    providedIn: 'root'
})
export class AiContentPromptStore extends ComponentStore<AiContentPromptState> {
    //Selectors
    readonly activeIndex$ = this.select((state) => state.activeIndex);
    readonly generatedContent$ = this.select((state) => state.generatedContent);
    readonly status$ = this.select((state) => state.status);
    readonly showDialog$ = this.select((state) => state.showDialog);
    readonly selectedContent$ = this.select((state) => state.selectedContent);
    readonly vm$ = this.select((state) => state);

    readonly activeContent$ = this.select(
        this.activeIndex$,
        this.generatedContent$,
        (activeIndex, generatedContent) => {
            return generatedContent[activeIndex];
        }
    );

    readonly submitLabel$ = this.select(
        this.status$,
        this.generatedContent$,
        (status, generatedContent) => {
            if (status === ComponentStatus.LOADING) {
                return 'block-editor.extension.ai-image.generating';
            }

            return generatedContent.length
                ? 'block-editor.extension.ai-image.regenerate'
                : 'block-editor.extension.ai-image.generate';
        }
    );

    //Updaters
    readonly setStatus = this.updater((state, status: ComponentStatus) => ({
        ...state,
        status
    }));

    readonly setSelectedContent = this.updater((state, selectedContent: string) => ({
        ...state,
        showDialog: false,
        selectedContent
    }));

    readonly showDialog = this.updater(() => {
        return {
            ...initialState,
            generatedContent: [],
            showDialog: true
        };
    });

    readonly hideDialog = this.updater((state) => ({
        ...state,
        showDialog: false
    }));

    readonly updateActiveIndex = this.updater((state, activeIndex: number) => ({
        ...state,
        activeIndex
    }));

    // Effects
    readonly generateContent = this.effect((prompt$: Observable<string>) => {
        return prompt$.pipe(
            withLatestFrom(this.state$),
            switchMap(([prompt, { generatedContent, activeIndex }]) => {
                this.patchState({ status: ComponentStatus.LOADING, prompt });

                return this.dotAiService.generateContent(prompt).pipe(
                    tapResponse(
                        (response) => {
                            const newContent = { prompt, content: response };
                            generatedContent[activeIndex]?.error
                                ? (generatedContent[activeIndex] = newContent)
                                : generatedContent.push(newContent);

                            this.patchState({
                                status: ComponentStatus.IDLE,
                                generatedContent: [...generatedContent], // like this to cover the scenario when replacing an error.
                                activeIndex: generatedContent.length - 1
                            });
                        },
                        (error: string) => {
                            const errorContent = { prompt, content: null, error };

                            generatedContent[activeIndex]?.error
                                ? (generatedContent[activeIndex] = errorContent)
                                : generatedContent.push(errorContent);

                            this.patchState({
                                status: ComponentStatus.IDLE,
                                generatedContent,
                                activeIndex: generatedContent.length - 1
                            });
                        }
                    )
                );
            })
        );
    });

    readonly cleanError = this.updater((state) => ({
        ...state,
        error: ''
    }));

    private readonly dotAiService = inject(DotAiService);

    constructor() {
        super({ ...initialState });
    }
}
