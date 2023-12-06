import { ComponentStore } from '@ngrx/component-store';
import { Observable, of } from 'rxjs';

import { Injectable } from '@angular/core';

import { catchError, switchMap, tap, withLatestFrom } from 'rxjs/operators';

import { DotAiService } from '../../../shared';

export interface AiContentPromptState {
    prompt: string;
    loading: boolean;
    content: string;
    open: boolean;
    acceptContent: boolean;
    deleteContent: boolean;
    exit: boolean;
}

@Injectable({
    providedIn: 'root'
})
export class AiContentPromptStore extends ComponentStore<AiContentPromptState> {
    constructor(private dotAiService: DotAiService) {
        super({
            prompt: '',
            loading: false,
            content: '',
            open: false,
            acceptContent: false,
            deleteContent: false,
            exit: false
        });
    }

    //Selectors
    readonly prompt$ = this.select((state) => state.prompt);
    readonly content$ = this.select((state) => state.content);
    readonly open$ = this.select((state) => state.open);
    readonly deleteContent$ = this.select((state) => state.deleteContent);
    readonly exit$ = this.select((state) => state.exit);
    readonly vm$ = this.select((state) => state);

    //Updaters
    readonly setOpen = this.updater((state, open: boolean) => ({ ...state, open }));
    readonly setExit = this.updater((state, exit: boolean) => ({
        ...state,
        exit,
        open: false // Set open to false when exit is updated
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
                this.patchState({ loading: true, prompt });

                return this.dotAiService.generateContent(prompt).pipe(
                    tap((content) => this.patchState({ loading: false, content })),
                    catchError(() => {
                        //TODO: Notify to handle error in the UI.
                        this.patchState({ loading: false, content: '' });

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
