import { ComponentStore } from '@ngrx/component-store';
import { Observable, of } from 'rxjs';

import { Injectable } from '@angular/core';

import { catchError, switchMap, tap, withLatestFrom } from 'rxjs/operators';

import { DotAiService } from '../../../shared';

export interface AiContentPromptState {
    prompt: string;
    content: string;
    acceptContent: boolean;
    deleteContent: boolean;
    status: 'loading' | 'loaded' | 'open' | 'exit' | 'close';
}

@Injectable({
    providedIn: 'root'
})
export class AiContentPromptStore extends ComponentStore<AiContentPromptState> {
    constructor(private dotAiService: DotAiService) {
        super({
            prompt: '',
            content: '',
            acceptContent: false,
            deleteContent: false,
            status: 'close'
        });
    }

    //Selectors
    readonly prompt$ = this.select((state) => state.prompt);
    readonly content$ = this.select((state) => state.content);
    readonly deleteContent$ = this.select((state) => state.deleteContent);
    readonly status$ = this.select((state) => state.status);
    readonly vm$ = this.select((state) => state);

    //Updaters
    readonly setStatus = this.updater((state, status: AiContentPromptState['status']) => ({
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
                this.patchState({ status: 'loading', prompt });

                return this.dotAiService.generateContent(prompt).pipe(
                    tap((content) => this.patchState({ status: 'loaded', content })),
                    catchError(() => {
                        //TODO: Notify to handle error in the UI.
                        this.patchState({ status: 'loaded', content: '' });

                        return of(null);
                    })
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
}
