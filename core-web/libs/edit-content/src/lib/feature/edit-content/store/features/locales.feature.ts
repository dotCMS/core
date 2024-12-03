import { tapResponse } from '@ngrx/operators';
import {
    patchState,
    signalStoreFeature,
    type,
    withComputed,
    withHooks,
    withMethods,
    withState
} from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { forkJoin, pipe } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { computed, effect, inject, untracked } from '@angular/core';

import { switchMap, take } from 'rxjs/operators';

import {
    DotContentletService,
    DotHttpErrorManagerService,
    DotLanguagesService
} from '@dotcms/data-access';
import { ComponentStatus, DotLanguage } from '@dotcms/dotcms-models';
import { ContentState } from '@dotcms/edit-content/feature/edit-content/store/features/content.feature';

export interface LocalesState {
    locales: DotLanguage[] | null;
    defaultLocale: DotLanguage | null;
    localesStatus: {
        status: ComponentStatus;
        error: string;
    };
}

export const localesInitialState: LocalesState = {
    locales: null,
    defaultLocale: null,
    localesStatus: {
        status: ComponentStatus.INIT,
        error: ''
    }
};

export function withLocales() {
    return signalStoreFeature(
        { state: type<ContentState>() },
        withState(localesInitialState),
        withComputed((store) => ({
            /**
             * Computed property that indicates whether the locales are currently being loaded.
             *
             * @param state The current state of the locales feature.
             * @returns `true` if the locales are being loaded, `false` otherwise.
             */
            isLoadingLocales: computed(
                () => store.localesStatus().status === ComponentStatus.LOADING
            )
        })),
        withMethods(
            (
                store,
                dotContentletService = inject(DotContentletService),
                dotLanguagesService = inject(DotLanguagesService),
                dotHttpErrorManagerService = inject(DotHttpErrorManagerService)
            ) => ({
                loadLocales: rxMethod<string>(
                    pipe(
                        switchMap((identifier) => {
                            patchState(store, {
                                localesStatus: { status: ComponentStatus.LOADING, error: '' }
                            });

                            return forkJoin({
                                locales: dotContentletService.getLanguages(identifier),
                                defaultLocale: dotLanguagesService.getDefault()
                            }).pipe(
                                take(1),
                                tapResponse({
                                    next: ({ locales, defaultLocale }) => {
                                        patchState(store, {
                                            locales,
                                            defaultLocale,
                                            localesStatus: {
                                                status: ComponentStatus.LOADED,
                                                error: ''
                                            }
                                        });
                                    },
                                    error: (error: HttpErrorResponse) => {
                                        dotHttpErrorManagerService.handle(error);
                                        patchState(store, {
                                            localesStatus: {
                                                status: ComponentStatus.ERROR,
                                                error: error.message
                                            }
                                        });
                                    }
                                })
                            );
                        })
                    )
                ),
                switchLocale: (_locale: DotLanguage) => {
                    // TODO: Implement locale switching.
                    // patchState(store, {
                    //     contentlet: { ...store.contentlet(), languageId: locale.id }
                    // });
                    // Check if there are modifications to the contentlet and prompt the user to save them.
                    // check if is a new language or just a switch
                    // if is a new language, ask to load from default language or start from scratch
                    //console.log('Switching locale to', locale);
                    //console.log('Switching locale fields', store.contentType().fields);
                }
            })
        ),
        withHooks({
            onInit(store) {
                effect(() => {
                    const contentlet = store.contentlet();

                    //console.log('Contentlet changed', contentlet);

                    untracked(() => {
                        if (contentlet) {
                            store.loadLocales(contentlet.identifier);
                        }
                    });
                });
            }
        })
    );
}
