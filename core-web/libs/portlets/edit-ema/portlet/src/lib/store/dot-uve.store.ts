import { tapResponse } from '@ngrx/operators';
import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe, forkJoin, of, EMPTY } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { computed, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { switchMap, shareReplay, tap, catchError, take, map } from 'rxjs/operators';

import { DotExperimentsService, DotLanguagesService, DotLicenseService } from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';

import { ShellState, UVEState } from './models';

import { DotPageApiParams, DotPageApiService } from '../services/dot-page-api.service';
import { sanitizeURL } from '../utils';

const initialState: UVEState = {
    isEnterprise: false,
    languages: [],
    pageAPIResponse: undefined,
    currentUser: undefined,
    experiment: undefined,
    error: undefined,
    params: undefined
};

export const UVEStore = signalStore(
    withState<UVEState>(initialState),
    withComputed((store) => {
        return {
            shellState: computed<ShellState>(() => {
                const pageAPIResponse = store.pageAPIResponse();
                const currentUrl = '/' + sanitizeURL(pageAPIResponse.page.pageURI);

                const requestHostName = store.params().clientHost ?? window.location.origin;

                const page = pageAPIResponse.page;
                const templateDrawed = pageAPIResponse.template.drawed;

                const isLayoutDisabled = !page.canEdit || !templateDrawed;

                const languageId = pageAPIResponse.viewAs.language.id;
                const languages = store.languages();

                return {
                    canRead: page.canRead,
                    error: store.error(),
                    translateProps: {
                        page,
                        languageId,
                        languages
                    },
                    seoParams: {
                        siteId: pageAPIResponse.site.identifier,
                        languageId: pageAPIResponse.viewAs.language.id,
                        currentUrl,
                        requestHostName
                    },
                    uvePageInfo: {
                        NOT_FOUND: {
                            icon: 'compass',
                            title: 'editema.infopage.notfound.title',
                            description: 'editema.infopage.notfound.description',
                            buttonPath: '/pages',
                            buttonText: 'editema.infopage.button.gotopages'
                        },
                        ACCESS_DENIED: {
                            icon: 'ban',
                            title: 'editema.infopage.accessdenied.title',
                            description: 'editema.infopage.accessdenied.description',
                            buttonPath: '/pages',
                            buttonText: 'editema.infopage.button.gotopages'
                        }
                    },
                    items: [
                        {
                            icon: 'pi-file',
                            label: 'editema.editor.navbar.content',
                            href: 'content',
                            id: 'content'
                        },
                        {
                            icon: 'pi-table',
                            label: 'editema.editor.navbar.layout',
                            href: 'layout',
                            id: 'layout',
                            isDisabled: isLayoutDisabled,
                            tooltip: templateDrawed
                                ? null
                                : 'editema.editor.navbar.layout.tooltip.cannot.edit.advanced.template'
                        },
                        {
                            icon: 'pi-sliders-h',
                            label: 'editema.editor.navbar.rules',
                            id: 'rules',
                            href: `rules/${page.identifier}`,
                            isDisabled: !page.canEdit
                        },
                        {
                            iconURL: 'experiments',
                            label: 'editema.editor.navbar.experiments',
                            href: `experiments/${page.identifier}`,
                            id: 'experiments',
                            isDisabled: !page.canEdit
                        },
                        {
                            icon: 'pi-th-large',
                            label: 'editema.editor.navbar.page-tools',
                            id: 'page-tools'
                        },
                        {
                            icon: 'pi-ellipsis-v',
                            label: 'editema.editor.navbar.properties',
                            id: 'properties'
                        }
                    ]
                };
            })
        };
    }),

    withMethods((store) => {
        // Maybe I can move this to a feature called withLoad

        const dotPageApiService = inject(DotPageApiService);
        const dotLanguagesService = inject(DotLanguagesService);
        const dotLicenseService = inject(DotLicenseService);
        const loginService = inject(LoginService);
        const dotExperimentsService = inject(DotExperimentsService);
        const router = inject(Router);
        const activatedRoute = inject(ActivatedRoute);

        return {
            // This is the same method as the old store but I will manage the state differently
            load: rxMethod<DotPageApiParams>(
                pipe(
                    switchMap((params) => {
                        return forkJoin({
                            pageAPIResponse: dotPageApiService.get(params).pipe(
                                switchMap((pageAPIResponse) => {
                                    const { vanityUrl } = pageAPIResponse;

                                    // If there is no vanity and is not a redirect we just return the pageAPI response
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
                                }),
                                tap({
                                    next: (pageAPIResponse) => {
                                        if (!pageAPIResponse) {
                                            return;
                                        }

                                        const { page, template } = pageAPIResponse;

                                        const isLayoutDisabled = !page.canEdit || !template.drawed;
                                        const pathIsLayout =
                                            activatedRoute.firstChild.snapshot.url[0].path ===
                                            'layout';

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
                                error: ({ status }: HttpErrorResponse) => {
                                    patchState(store, { error: status });
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
                                            // This will be our global state. Here we have all the information we need to apply the logic in the components
                                            patchState(store, {
                                                pageAPIResponse,
                                                isEnterprise,
                                                currentUser,
                                                experiment,
                                                languages,
                                                params
                                            });
                                        }
                                    })
                                )
                            )
                        );
                    })
                )
            ),
            reload: rxMethod<DotPageApiParams>(
                pipe(
                    // I will implement this when I get to the editor, because I will probably need to do some logic there
                    // tap(() => this.updateEditorState(EDITOR_STATE.LOADING)),
                    switchMap((params) => {
                        return dotPageApiService.get(params).pipe(
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
                                    patchState(store, {
                                        pageAPIResponse,
                                        languages
                                    });
                                },
                                error: ({ status }: HttpErrorResponse) => {
                                    patchState(store, { error: status });
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
