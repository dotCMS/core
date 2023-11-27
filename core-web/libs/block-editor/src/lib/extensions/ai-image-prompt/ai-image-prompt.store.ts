import { ComponentStore, tapResponse } from '@ngrx/component-store';
import { Observable } from 'rxjs';

import { Injectable } from '@angular/core';

import { switchMap, tap, withLatestFrom } from 'rxjs/operators';

import { ComponentStatus, DotCMSContentlet } from '@dotcms/dotcms-models';

import { PromptType } from './ai-image-prompt.models';

import { AiContentService } from '../../shared';

export interface DotAiImagePromptComponentState {
    showDialog: boolean;
    selectedPromptType: PromptType | null;
    prompt: string | null;
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
        selectedPromptType: 'input',
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
            tap((prompt) => {
                this.patchState({ status: ComponentStatus.LOADING, prompt });
            }),
            switchMap((prompt) => {
                return this.aiContentService.generateAndPublishImage(prompt).pipe(
                    tapResponse(
                        (contentLets) => {
                            this.patchState({
                                status: ComponentStatus.IDLE,
                                contentlets: contentLets
                            });
                        },
                        () => {
                            // TODO: handle errors
                            this.patchState({
                                status: ComponentStatus.IDLE
                            });
                        }
                    )
                );
            })
        );
    });

    readonly generateImageUsingBlockEditorContent = this.effect((prompt$: Observable<string>) => {
        return prompt$.pipe(
            tap((prompt) => {
                this.patchState({ status: ComponentStatus.LOADING, prompt });
            }),
            withLatestFrom(this.editorContent$),
            switchMap(([prompt, editorContent]) => {
                const promptWithEditorContent = `${prompt} ${editorContent}`;

                return this.aiContentService.generateAndPublishImage(promptWithEditorContent).pipe(
                    tapResponse(
                        (contentLets) => {
                            this.patchState({
                                status: ComponentStatus.IDLE,
                                contentlets: contentLets
                            });
                        },
                        () => {
                            // TODO: handle errors
                            this.patchState({
                                status: ComponentStatus.IDLE
                            });
                        }
                    )
                );
            })
        );
    });

    constructor(private aiContentService: AiContentService) {
        super(initialState);
    }
}
