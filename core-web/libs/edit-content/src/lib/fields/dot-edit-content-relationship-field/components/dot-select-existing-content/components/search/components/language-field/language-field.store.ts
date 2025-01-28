import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe, of } from 'rxjs';

import { computed, inject } from '@angular/core';

import { tap, switchMap, catchError } from 'rxjs/operators';

import { DotLanguagesService } from '@dotcms/data-access';
import { ComponentStatus, DotLanguage } from '@dotcms/dotcms-models';

interface LanguageState {
    languages: DotLanguage[];
    selectedLanguageId: number | null;
    error: string | null;
    status: ComponentStatus;
}

const initialState: LanguageState = {
    languages: [],
    selectedLanguageId: null,
    error: null,
    status: ComponentStatus.INIT
};

export const LanguageFieldStore = signalStore(
    withState(initialState),
    withComputed((state) => ({
        isLoading: computed(() => state.status() === ComponentStatus.LOADING),
        selectedLanguage: computed(() =>
            state.languages().find((lang) => lang.id === state.selectedLanguageId())
        ),
        hasLanguages: computed(() => state.languages().length > 0)
    })),
    withMethods((store, languagesService = inject(DotLanguagesService)) => ({
        setLanguages(languages: DotLanguage[]) {
            patchState(store, { languages, status: ComponentStatus.LOADED });
        },

        setSelectedLanguage(languageId: number | null) {
            patchState(store, { selectedLanguageId: languageId });
        },

        setError(error: string | null) {
            patchState(store, { error, status: ComponentStatus.ERROR });
        },

        loadLanguages: rxMethod<void>(
            pipe(
                tap(() => {
                    patchState(store, { status: ComponentStatus.LOADING, error: null });
                }),
                switchMap(() =>
                    languagesService.get().pipe(
                        tap((languages) => {
                            patchState(store, {
                                languages,
                                status: ComponentStatus.LOADED,
                                error: null
                            });
                        }),
                        catchError((error) => {
                            patchState(store, {
                                error: error.message,
                                status: ComponentStatus.ERROR
                            });

                            return of([]); // Return empty array on error
                        })
                    )
                )
            )
        ),

        reset() {
            patchState(store, initialState);
        }
    }))
);
