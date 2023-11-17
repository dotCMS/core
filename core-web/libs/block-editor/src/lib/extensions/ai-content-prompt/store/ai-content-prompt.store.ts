import { ComponentStore } from '@ngrx/component-store';
import { Observable, of } from 'rxjs';

import { Injectable } from '@angular/core';

import { catchError, switchMap, tap } from 'rxjs/operators';

import { AiContentService } from '../../../shared';

export interface AiContentPromptState {
    prompt: string;
    loading: boolean;
    content: string;
}

@Injectable({
    providedIn: 'root'
})
export class AiContentPromptStore extends ComponentStore<AiContentPromptState> {
    constructor(private aiContentService: AiContentService) {
        super({ prompt: '', loading: false, content: '' });
    }

    //Selectors
    readonly prompt$ = this.select((state) => state.prompt);
    readonly loading$ = this.select((state) => state.loading);
    readonly content$ = this.select((state) => state.content);

    readonly vm$ = this.select((state) => state);

    //Effects
    readonly generateContent = this.effect((prompt: Observable<string>) => {
        return prompt.pipe(
            switchMap((prompt) => {
                this.patchState({ loading: true, prompt });

                return this.aiContentService.generateContent(prompt).pipe(
                    tap((content) => this.patchState({ loading: false, content })),
                    catchError(() => of(null))
                );
            })
        );
    });
}
