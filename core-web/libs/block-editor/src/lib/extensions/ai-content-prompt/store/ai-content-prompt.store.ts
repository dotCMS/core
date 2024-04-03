import { ComponentStore, tapResponse } from '@ngrx/component-store';
import { Observable } from 'rxjs';

import { Injectable } from '@angular/core';

import { switchMap } from 'rxjs/operators';

import { ComponentStatus } from '@dotcms/dotcms-models';

import { DotAiService } from '../../../shared';

export interface AiContentPromptState {
    prompt: string;
    content: string;
    selectedContent: string;
    status: ComponentStatus;
    error: string;
    showDialog: boolean;
}

const initialState: AiContentPromptState = {
    prompt: '',
    content: '',
    selectedContent: '',
    status: ComponentStatus.INIT,
    error: '',
    showDialog: false
};

@Injectable({
    providedIn: 'root'
})
export class AiContentPromptStore extends ComponentStore<AiContentPromptState> {
    //Selectors
    readonly errorMsg$ = this.select(this.state$, ({ error }) => error);
    readonly status$ = this.select((state) => state.status);
    readonly showDialog$ = this.select((state) => state.showDialog);
    readonly selectedContent$ = this.select((state) => state.selectedContent);
    readonly vm$ = this.select((state) => state);

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

    readonly showDialog = this.updater(() => ({
        ...initialState,
        showDialog: true
    }));

    readonly hideDialog = this.updater((state) => ({
        ...state,
        showDialog: false
    }));

    // Effects
    readonly generateContent = this.effect((prompt$: Observable<string>) => {
        return prompt$.pipe(
            switchMap((prompt) => {
                this.patchState({ status: ComponentStatus.LOADING, prompt });

                return this.dotAiService.generateContent(prompt).pipe(
                    tapResponse(
                        (content) => {
                            this.patchState({ status: ComponentStatus.IDLE, content, error: '' });
                        },
                        (error: string) => {
                            this.patchState({
                                status: ComponentStatus.IDLE,
                                content: '',
                                error: error
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
        super(initialState);
    }
}
