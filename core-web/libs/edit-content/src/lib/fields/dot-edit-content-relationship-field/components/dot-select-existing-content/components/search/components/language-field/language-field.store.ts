import { tapResponse } from '@ngrx/operators';
import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe } from 'rxjs';

import { computed, inject } from '@angular/core';

import { tap, switchMap } from 'rxjs/operators';

import { DotLanguagesService } from '@dotcms/data-access';
import { ComponentStatus, DotLanguage } from '@dotcms/dotcms-models';

/**
 * Interface representing the state of the language field.
 * @interface LanguageState
 * @property {DotLanguage[]} languages - Array of available languages
 * @property {number | null} selectedLanguageId - ID of the currently selected language
 * @property {string | null} error - Error message if any
 * @property {ComponentStatus} status - Current status of the component
 */
interface LanguageState {
    languages: DotLanguage[];
    selectedLanguageId: number | null;
    error: string | null;
    status: ComponentStatus;
}

/**
 * Initial state for the language field store
 */
const initialState: LanguageState = {
    languages: [],
    selectedLanguageId: null,
    error: null,
    status: ComponentStatus.INIT
};

/**
 * Signal store for managing language field state and operations.
 * Provides computed properties and methods for language selection and data loading.
 */
export const LanguageFieldStore = signalStore(
    withState(initialState),
    withComputed((state) => ({
        /**
         * Indicates if languages are currently being loaded
         */
        isLoading: computed(() => state.status() === ComponentStatus.LOADING),
        /**
         * Currently selected language object
         */
        selectedLanguage: computed(() =>
            state.languages().find((lang) => lang.id === state.selectedLanguageId())
        ),
        /**
         * Indicates if there are any languages available
         */
        hasLanguages: computed(() => state.languages().length > 0)
    })),
    withMethods((store, languagesService = inject(DotLanguagesService)) => ({
        /**
         * Updates the available languages in the store
         * @param {DotLanguage[]} languages - Array of languages to set
         */
        setLanguages(languages: DotLanguage[]) {
            patchState(store, { languages, status: ComponentStatus.LOADED });
        },

        /**
         * Sets the selected language by ID
         * @param {number | null} languageId - ID of the language to select
         */
        setSelectedLanguage(languageId: number | null) {
            patchState(store, { selectedLanguageId: languageId });
        },

        /**
         * Sets an error message in the store
         * @param {string | null} error - Error message to set
         */
        setError(error: string | null) {
            patchState(store, { error, status: ComponentStatus.ERROR });
        },

        /**
         * Loads available languages from the server.
         * Updates store state with loading status and handles success/error cases.
         */
        loadLanguages: rxMethod<void>(
            pipe(
                tap(() => {
                    patchState(store, { status: ComponentStatus.LOADING, error: null });
                }),
                switchMap(() =>
                    languagesService.get().pipe(
                        tapResponse({
                            next: (languages) => {
                                patchState(store, {
                                    languages,
                                    status: ComponentStatus.LOADED,
                                    error: null
                                });
                            },
                            error: () => {
                                patchState(store, {
                                    error: 'dot.file.relationship.dialog.search.language.failed',
                                    status: ComponentStatus.ERROR
                                });
                            }
                        })
                    )
                )
            )
        ),

        /**
         * Resets the store to its initial state
         */
        reset() {
            patchState(store, initialState);
        }
    }))
);
