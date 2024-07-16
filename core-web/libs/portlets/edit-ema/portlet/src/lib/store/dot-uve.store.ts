import { patchState, signalStore, withMethods, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe, forkJoin, of } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';

import { switchMap, shareReplay, tap, catchError, take } from 'rxjs/operators';

import { DotExperimentsService, DotLanguagesService, DotLicenseService } from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';

import { UVEState } from './models';

import { DotPageApiParams, DotPageApiService } from '../services/dot-page-api.service';

export const UVEStore = signalStore(
    withState<UVEState>({
        isEnterprise: false,
        languages: [],
        page: undefined,
        currentUser: undefined,
        experiment: undefined
    }),
    withMethods((store) => {
        const dotPageApiService = inject(DotPageApiService);
        const dotLanguagesService = inject(DotLanguagesService);
        const dotLicenseService = inject(DotLicenseService);
        const loginService = inject(LoginService);
        const dotExperimentsService = inject(DotExperimentsService);
        const router = inject(Router);

        return {
            // This is the same method as the old store but I will manage the state differently
            load: rxMethod<DotPageApiParams>(
                pipe(
                    switchMap((params) => {
                        return forkJoin({
                            page: dotPageApiService.get(params).pipe(
                                switchMap((pageAPIResponse) => {
                                    const { vanityUrl } = pageAPIResponse;

                                    // If there is no vanity and is not a redirect we just return the response which is the pageAPI response
                                    if (
                                        !vanityUrl ||
                                        (!vanityUrl.permanentRedirect &&
                                            !vanityUrl.temporaryRedirect)
                                    ) {
                                        return of(pageAPIResponse);
                                    }

                                    const queryParams = {
                                        ...params,
                                        url: vanityUrl.forwardTo.replace('/', '')
                                    };

                                    // We navigate to the new url and return undefined
                                    router.navigate([], {
                                        queryParams,
                                        queryParamsHandling: 'merge'
                                    });

                                    return of(undefined);
                                })
                            ),
                            isEnterprise: dotLicenseService
                                .isEnterprise()
                                .pipe(take(1), shareReplay()),
                            currentUser: loginService.getCurrentUser()
                        }).pipe(
                            tap({
                                error: ({ status: _status }: HttpErrorResponse) => {
                                    // PATCH TO EMPTY STATE OR ERROR
                                }
                            }),
                            switchMap(({ page, isEnterprise, currentUser }) =>
                                forkJoin({
                                    experiment: dotExperimentsService
                                        .getById(params.experimentId)
                                        .pipe(
                                            // If there is an error, we return undefined
                                            // This is to avoid blocking the page if there is an error with the experiment
                                            catchError(() => of(undefined))
                                        ),
                                    languages: dotLanguagesService.getLanguagesUsedPage(
                                        page.page.identifier
                                    )
                                }).pipe(
                                    tap({
                                        next: ({ experiment, languages }) => {
                                            // This will be our global state. Here we have all the information we need to apply the logic in the components
                                            patchState(store, {
                                                page,
                                                isEnterprise,
                                                currentUser,
                                                experiment,
                                                languages
                                            });
                                        }
                                    })
                                )
                            )
                        );
                    })
                )
            )
        };
    })
);
