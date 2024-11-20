import { tapResponse } from '@ngrx/operators';
import {
    patchState,
    signalStoreFeature,
    type,
    withHooks,
    withMethods,
    withState
} from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { forkJoin, pipe } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { effect, inject } from '@angular/core';

import { switchMap, take } from 'rxjs/operators';

import {
    DotContentletService,
    DotHttpErrorManagerService,
    DotLanguagesService
} from '@dotcms/data-access';
import { DotLanguage } from '@dotcms/dotcms-models';
import { ContentState } from '@dotcms/edit-content/feature/edit-content/store/features/content.feature';

interface LocalesState {
    locales: DotLanguage[] | null;
    defaultLocale: DotLanguage | null;
}

export const localesInitialState: LocalesState = {
    locales: null,
    defaultLocale: null
};

export function withLocales() {
    return signalStoreFeature(
        { state: type<ContentState>() },
        withState(localesInitialState),
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
                            return forkJoin({
                                locales: dotContentletService.getLanguages(identifier),
                                defaultLocale: dotLanguagesService.getDefault()
                            }).pipe(
                                take(1),
                                tapResponse({
                                    next: ({ locales, defaultLocale }) => {
                                        patchState(store, { locales, defaultLocale });
                                    },
                                    error: (error: HttpErrorResponse) => {
                                        dotHttpErrorManagerService.handle(error);
                                    }
                                })
                            );
                        })
                    )
                )
            })
        ),
        withHooks({
            onInit(store) {
                effect(() => {
                    const contentlet = store.contentlet();

                    if (contentlet) {
                        store.loadLocales(contentlet.identifier);
                    }
                });
            }
        })
    );
}
