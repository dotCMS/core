import { ComponentStore, tapResponse } from '@ngrx/component-store';
import { Observable } from 'rxjs';

import { Injectable } from '@angular/core';

import { switchMap, withLatestFrom } from 'rxjs/operators';

import { ComponentStatus } from '@dotcms/dotcms-models';

import { DotAiService } from '../../../shared';

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
    error: string;
    showDialog: boolean;
    submitLabel: string;
}

const initialState: AiContentPromptState = {
    prompt: '',
    generatedContent: [],
    selectedContent: '',
    activeIndex: null,
    status: ComponentStatus.INIT,
    error: '',
    showDialog: false,
    submitLabel: 'block-editor.extension.ai-image.generate'
};

@Injectable({
    providedIn: 'root'
})
export class AiContentPromptStore extends ComponentStore<AiContentPromptState> {
    //Selectors
    readonly errorMsg$ = this.select(this.state$, ({ error }) => error);
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
            switchMap(([prompt, { generatedContent }]) => {
                this.patchState({ status: ComponentStatus.LOADING, prompt });

                return this.dotAiService.generateContent(prompt).pipe(
                    tapResponse(
                        (response) => {
                            generatedContent.push({ prompt, content: response });
                            this.patchState({
                                status: ComponentStatus.IDLE,
                                generatedContent,
                                error: '',
                                activeIndex: generatedContent.length - 1
                            });
                        },
                        (error: string) => {
                            generatedContent.push({ prompt, content: null, error: error });
                            this.patchState({
                                status: ComponentStatus.IDLE,
                                generatedContent,
                                error: error,
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

    constructor(private dotAiService: DotAiService) {
        super({ ...initialState });
    }
}
