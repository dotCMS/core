import { ComponentStore } from '@ngrx/component-store';
import { Observable, of } from 'rxjs';

import { Injectable } from '@angular/core';

import { catchError, switchMap, tap } from 'rxjs/operators';

import { DotAiService } from '../../../shared';

export interface AiContentPromptState {
    prompt: string;
    loading: boolean;
    content: string;
    open: boolean;
    acceptContent: boolean;
}

@Injectable({
    providedIn: 'root'
})
export class AiContentPromptStore extends ComponentStore<AiContentPromptState> {
    constructor(private dotAiService: DotAiService) {
        super({ prompt: '', loading: false, content: '', open: false, acceptContent: false });
    }

    //Selectors
    readonly prompt$ = this.select((state) => state.prompt);
    readonly content$ = this.select((state) => state.content);
    readonly vm$ = this.select((state) => state);

    //Updaters
    readonly setOpen = this.updater((state, open: boolean) => ({ ...state, open }));
    readonly setAcceptContent = this.updater((state, acceptContent: boolean) => ({
        ...state,
        acceptContent
    }));

    // Effects
    readonly generateContent = this.effect((prompt$: Observable<string>) => {
        return prompt$.pipe(
            switchMap((prompt) => {
                this.patchState({ loading: true, prompt });

                return this.dotAiService.generateContent(prompt).pipe(
                    tap((content) => this.patchState({ loading: false, content })),
                    catchError(() => of(null))
                );
            })
        );
    });

    /**
     * Use the last prompt to generate content
     * @returns void
     * @memberof AiContentPromptStore
     */
    reGenerateContent() {
        this.generateContent(this.prompt$);
    }
}
