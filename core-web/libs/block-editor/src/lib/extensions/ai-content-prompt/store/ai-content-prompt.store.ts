import { ComponentStore, tapResponse } from '@ngrx/component-store';
import { Observable, of } from 'rxjs';

import { Injectable } from '@angular/core';

import { switchMap, tap, withLatestFrom } from 'rxjs/operators';

import { ComponentStatus } from '@dotcms/dotcms-models';

import { DotAiService } from '../../../shared';

export interface AiContentPromptState {
    prompt: string;
    content: string;
    acceptContent: boolean;
    deleteContent: boolean;
    status: ComponentStatus;
    error: string;
}

@Injectable({
    providedIn: 'root'
})
export class AiContentPromptStore extends ComponentStore<AiContentPromptState> {
    //Selectors
    readonly errorMsg$ = this.select(this.state$, ({ error }) => error);
    readonly content$ = this.select((state) => state.content);
    readonly deleteContent$ = this.select((state) => state.deleteContent);
    readonly status$ = this.select((state) => state.status);
    readonly vm$ = this.select((state) => state);
    //Updaters
    readonly setStatus = this.updater((state, status: ComponentStatus) => ({
        ...state,
        status
    }));
    readonly setAcceptContent = this.updater((state, acceptContent: boolean) => ({
        ...state,
        acceptContent
    }));
    readonly setDeleteContent = this.updater((state, deleteContent: boolean) => ({
        ...state,
        deleteContent
    }));
    // Effects
    readonly generateContent = this.effect((prompt$: Observable<string>) => {
        return prompt$.pipe(
            switchMap((prompt) => {
                this.patchState({ status: ComponentStatus.LOADING, prompt });

                return this.dotAiService.generateContent(prompt).pipe(
                    tapResponse(
                        (content) => {
                            this.patchState({ status: ComponentStatus.LOADED, content, error: '' });
                        },
                        (error: string) => {
                            this.patchState({
                                status: ComponentStatus.LOADED,
                                content: '',
                                error: error
                            });
                        }
                    )
                );
            })
        );
    });
    /**
     * When this effect is triggered, it uses the latest prompt value from the store's state
     * to generate content using the `generateContent` effect.
     *
     * @param trigger$ - An observable that triggers the effect when it emits a value (e.g., `this.reGenerateContent()`)
     * @returns An observable representing the triggering of the effect.
     * @memberof AiContentPromptStore
     */
    readonly reGenerateContent = this.effect((trigger$: Observable<void>) => {
        return trigger$.pipe(
            withLatestFrom(this.state$),
            tap(([_, { prompt }]) => this.generateContent(of(prompt)))
        );
    });
    readonly cleanError = this.updater((state) => ({
        ...state,
        error: ''
    }));

    constructor(private dotAiService: DotAiService) {
        super({
            prompt: '',
            content: '',
            acceptContent: false,
            deleteContent: false,
            status: ComponentStatus.INIT,
            error: ''
        });
    }
}
