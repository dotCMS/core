import { tapResponse } from '@ngrx/component-store';
import { patchState, signalStoreFeature, type, withMethods } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe, forkJoin, of, EMPTY } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';

import { switchMap, shareReplay, catchError, tap, take, map } from 'rxjs/operators';

import { DotLanguagesService, DotLicenseService, DotExperimentsService } from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';

import { DotPageApiService, DotPageApiParams } from '../../../services/dot-page-api.service';
import { UVE_STATUS } from '../../../shared/enums';
import { computeCanEditPage, computePageIsLocked, isForwardOrPage } from '../../../utils';
import { UVEState } from '../../models';

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
        withMethods((store) => {
            const dotPageApiService = inject(DotPageApiService);
            const dotLanguagesService = inject(DotLanguagesService);
            const dotLicenseService = inject(DotLicenseService);
            const loginService = inject(LoginService);
            const dotExperimentsService = inject(DotExperimentsService);
            const router = inject(Router);
            const activatedRoute = inject(ActivatedRoute);

            return {
                load: rxMethod<DotPageApiParams>(
                    pipe(
                        tap(() => {
                            patchState(store, { $status: UVE_STATUS.LOADING });
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
                                    }),
                                    tap({
                                        next: (pageAPIResponse) => {
                                            if (!pageAPIResponse) {
                                                return;
                                            }

                                            const { page, template } = pageAPIResponse;

                                            const isLayoutDisabled =
                                                !page.canEdit || !template.drawed;
                                            const pathIsLayout =
                                                activatedRoute?.firstChild?.snapshot?.url?.[0]
                                                    .path === 'layout';

                                            if (isLayoutDisabled && pathIsLayout) {
                                                // If the user can't edit the page or the template is not drawed we navigate to the content page
                                                router.navigate(['edit-page/content'], {
                                                    queryParamsHandling: 'merge'
                                                });
                                            }
                                        }
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
                                            $error: errorStatus,
                                            $status: UVE_STATUS.ERROR
                                        });
                                    }
                                }),
                                switchMap(({ pageAPIResponse, isEnterprise, currentUser }) =>
                                    forkJoin({
                                        experiment: dotExperimentsService
                                            .getById(params.experimentId)
                                            .pipe(
                                                // If there is an error, we return undefined
                                                // This is to avoid blocking the page if there is an error with the experiment
                                                catchError(() => of(undefined))
                                            ),
                                        languages: dotLanguagesService.getLanguagesUsedPage(
                                            pageAPIResponse.page.identifier
                                        )
                                    }).pipe(
                                        tap({
                                            next: ({ experiment, languages }) => {
                                                const canEditPage = computeCanEditPage(
                                                    pageAPIResponse,
                                                    currentUser,
                                                    experiment
                                                );

                                                const pageIsLocked = computePageIsLocked(
                                                    pageAPIResponse?.page,
                                                    currentUser
                                                );

                                                patchState(store, {
                                                    $pageAPIResponse: pageAPIResponse,
                                                    $isEnterprise: isEnterprise,
                                                    $currentUser: currentUser,
                                                    $experiment: experiment,
                                                    $languages: languages,
                                                    $params: params,
                                                    $canEditPage: canEditPage,
                                                    $pageIsLocked: pageIsLocked,
                                                    $status: UVE_STATUS.LOADED,
                                                    $isTraditionalPage: !params.clientHost // If we don't send the clientHost we are using as VTL page
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
                            patchState(store, { $status: UVE_STATUS.LOADING });
                        }),
                        switchMap(() => {
                            return dotPageApiService.get(store.$params()).pipe(
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
                                            store.$currentUser(),
                                            store.$experiment()
                                        );

                                        const pageIsLocked = computePageIsLocked(
                                            pageAPIResponse?.page,
                                            store.$currentUser()
                                        );

                                        patchState(store, {
                                            $pageAPIResponse: pageAPIResponse,
                                            $languages: languages,
                                            $canEditPage: canEditPage,
                                            $pageIsLocked: pageIsLocked,
                                            $status: UVE_STATUS.LOADED,
                                            $isTraditionalPage: !store.$params().clientHost // If we don't send the clientHost we are using as VTL page
                                        });
                                    },
                                    error: ({ status: errorStatus }: HttpErrorResponse) => {
                                        patchState(store, {
                                            $error: errorStatus,
                                            $status: UVE_STATUS.ERROR
                                        });
                                    }
                                }),
                                catchError(() => EMPTY)
                            );
                        })
                    )
                )
            };
        })
    );
}
