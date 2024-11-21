import { tapResponse } from '@ngrx/operators';
import { patchState, signalStoreFeature, type, withMethods } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { EMPTY, forkJoin, of, pipe } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';

import { map, shareReplay, switchMap, take, tap } from 'rxjs/operators';

import { DotExperimentsService, DotLanguagesService, DotLicenseService } from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';

import { DotPageApiParams, DotPageApiService } from '../../../services/dot-page-api.service';
import { UVE_STATUS } from '../../../shared/enums';
import { computeCanEditPage, computePageIsLocked, isForwardOrPage } from '../../../utils';
import { UVEState } from '../../models';
import { withClient } from '../client/withClient';

/**
 * Add load and reload method to the store
 *
 * @export
 * @return {*}
 */
export function withLoad() {
    return signalStoreFeature(
        {
            state: type<UVEState>()
        },
        withClient(),
        withMethods((store) => {
            const dotPageApiService = inject(DotPageApiService);
            const dotLanguagesService = inject(DotLanguagesService);
            const dotLicenseService = inject(DotLicenseService);
            const loginService = inject(LoginService);
            const dotExperimentsService = inject(DotExperimentsService);

            return {
                init: rxMethod<DotPageApiParams>(
                    pipe(
                        tap(() => store.resetClientConfiguration()),
                        tap(() => {
                            patchState(store, { status: UVE_STATUS.LOADING, isClientReady: false });
                        }),
                        switchMap((params) => {
                            return forkJoin({
                                pageAPIResponse: dotPageApiService.get(params).pipe(
                                    switchMap((pageAPIResponse) => {
                                        const { vanityUrl } = pageAPIResponse;

                                        // If there is no vanity and is not a redirect we just return the pageAPI response
                                        if (isForwardOrPage(vanityUrl)) {
                                            return of(pageAPIResponse);
                                        }

                                        const pageParams = {
                                            ...params,
                                            url: vanityUrl.forwardTo.replace('/', '')
                                        };

                                        patchState(store, { pageParams });

                                        // EMPTY is a simple Observable that only emits the complete notification.
                                        return EMPTY;
                                    })
                                ),
                                isEnterprise: dotLicenseService
                                    .isEnterprise()
                                    .pipe(take(1), shareReplay()),
                                currentUser: loginService.getCurrentUser()
                            }).pipe(
                                tap({
                                    error: ({ status: errorStatus }: HttpErrorResponse) => {
                                        patchState(store, {
                                            errorCode: errorStatus,
                                            status: UVE_STATUS.ERROR
                                        });
                                    }
                                }),
                                switchMap(({ pageAPIResponse, isEnterprise, currentUser }) =>
                                    forkJoin({
                                        experiment: dotExperimentsService.getById(
                                            params.experimentId
                                        ),
                                        languages: dotLanguagesService.getLanguagesUsedPage(
                                            pageAPIResponse.page.identifier
                                        )
                                    }).pipe(
                                        tap({
                                            next: ({ experiment, languages }) => {
                                                const canEditPage = computeCanEditPage(
                                                    pageAPIResponse?.page,
                                                    currentUser,
                                                    experiment
                                                );

                                                const pageIsLocked = computePageIsLocked(
                                                    pageAPIResponse?.page,
                                                    currentUser
                                                );

                                                const isTraditionalPage = !params.clientHost; // If we don't send the clientHost we are using as VTL page

                                                patchState(store, {
                                                    pageAPIResponse,
                                                    isEnterprise,
                                                    currentUser,
                                                    experiment,
                                                    languages,
                                                    canEditPage,
                                                    pageIsLocked,
                                                    isTraditionalPage,
                                                    isClientReady: isTraditionalPage, // If is a traditional page we are ready
                                                    status: UVE_STATUS.LOADED
                                                });
                                            },
                                            error: ({ status: errorStatus }: HttpErrorResponse) => {
                                                patchState(store, {
                                                    errorCode: errorStatus,
                                                    status: UVE_STATUS.ERROR
                                                });
                                            }
                                        })
                                    )
                                )
                            );
                        })
                    )
                ),
                reload: rxMethod<void>(
                    pipe(
                        tap(() => {
                            patchState(store, {
                                status: UVE_STATUS.LOADING
                            });
                        }),
                        switchMap(() => {
                            return dotPageApiService
                                .getClientPage(store.pageParams(), store.clientRequestProps())
                                .pipe(
                                    switchMap((pageAPIResponse) =>
                                        dotLanguagesService
                                            .getLanguagesUsedPage(pageAPIResponse.page.identifier)
                                            .pipe(
                                                map((languages) => ({
                                                    pageAPIResponse,
                                                    languages
                                                }))
                                            )
                                    ),
                                    tapResponse({
                                        next: ({ pageAPIResponse, languages }) => {
                                            const canEditPage = computeCanEditPage(
                                                pageAPIResponse?.page,
                                                store.currentUser(),
                                                store.experiment()
                                            );

                                            const pageIsLocked = computePageIsLocked(
                                                pageAPIResponse?.page,
                                                store.currentUser()
                                            );

                                            patchState(store, {
                                                pageAPIResponse,
                                                languages,
                                                canEditPage,
                                                pageIsLocked,
                                                status: UVE_STATUS.LOADED,
                                                isClientReady: true,
                                                isTraditionalPage: !store.pageParams().clientHost // If we don't send the clientHost we are using as VTL page
                                            });
                                        },
                                        error: ({ status: errorStatus }: HttpErrorResponse) => {
                                            patchState(store, {
                                                errorCode: errorStatus,
                                                status: UVE_STATUS.ERROR
                                            });
                                        }
                                    })
                                );
                        })
                    )
                )
            };
        })
    );
}
