import { ComponentStore, tapResponse } from '@ngrx/component-store';
import { Observable, of } from 'rxjs';

import { Injectable } from '@angular/core';

import { map, switchMap, withLatestFrom } from 'rxjs/operators';

import { ComponentStatus, DotCMSContentlet } from '@dotcms/dotcms-models';

import { PromptType } from './ai-image-prompt.models';

import { DotAiService } from '../../shared';

const DEFAULT_INPUT_PROMPT: PromptType = 'input';

export interface DotAiImagePromptComponentState {
    showDialog: boolean;
    selectedPromptType: PromptType | null;
    prompt: string | null; // always will have the final prompt
    editorContent: string | null;
    contentlets: DotCMSContentlet[] | [];
    status: ComponentStatus;
    error: string | null;
}

const initialState: DotAiImagePromptComponentState = {
    selectedPromptType: null,
    showDialog: false,
    status: ComponentStatus.INIT,
    contentlets: [],
    prompt: null,
    editorContent: null,
    error: null
};

@Injectable({ providedIn: 'root' })
export class DotAiImagePromptStore extends ComponentStore<DotAiImagePromptComponentState> {
    readonly isOpenDialog$ = this.select(this.state$, ({ showDialog }) => showDialog);
    readonly editorContent$ = this.select(this.state$, ({ editorContent }) => editorContent);

    readonly isLoading$ = this.select(
        this.state$,
        ({ status }) => status === ComponentStatus.LOADING
    );

    readonly getContentlets$ = this.select(this.state$, ({ contentlets }) => contentlets);

    readonly setPromptType = this.updater((state, selectedPromptType: PromptType) => ({
        ...state,
        selectedPromptType
    }));

    readonly showDialog = this.updater((state, editorContent: string) => ({
        ...state,
        showDialog: true,
        selectedPromptType: DEFAULT_INPUT_PROMPT,
        editorContent
    }));

    readonly hideDialog = this.updater((state) => ({
        ...state,
        showDialog: false,
        selectedPromptType: null
    }));

    readonly vm$: Observable<DotAiImagePromptComponentState> = this.select(
        this.state$,
        ({
            selectedPromptType,
            showDialog,
            status,
            prompt,
            contentlets,
            error,
            editorContent
        }) => ({
            selectedPromptType,
            showDialog,
            status,
            prompt,
            contentlets,
            error,
            editorContent
        })
    );

    readonly generateImage = this.effect((prompt$: Observable<string>) => {
        return prompt$.pipe(
            withLatestFrom(this.state$),
            switchMap(([prompt, { selectedPromptType, editorContent }]) => {
                const finalPrompt =
                    selectedPromptType === 'auto' && editorContent
                        ? `${prompt} ${editorContent}`
                        : prompt;

                this.patchState({ status: ComponentStatus.LOADING, prompt: finalPrompt });

                return this.dotAiService.generateAndPublishImage(finalPrompt).pipe(
                    tapResponse(
                        (contentLets) => {
                            this.patchState({
                                status: ComponentStatus.IDLE,
                                contentlets: contentLets
                            });
                        },
                        () => {
                            this.patchState({ status: ComponentStatus.IDLE });
                        }
                    )
                );
            })
        );
    });

    readonly reGenerateContent = this.effect((trigger$: Observable<void>) => {
        return trigger$.pipe(
            withLatestFrom(this.state$),
            map(([_, { prompt }]) => {
                if (!prompt) return;
                this.generateImage(of(prompt));
            })
        );
    });

    constructor(private dotAiService: DotAiService) {
        super(initialState);
    }
}
