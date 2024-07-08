import { BehaviorSubject, forkJoin, Observable, of, Subject } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { catchError, map, pluck, switchMap, take, tap } from 'rxjs/operators';

import {
    DotContentletLockerService,
    DotExperimentsService,
    DotFavoritePageService,
    DotHttpErrorHandled,
    DotHttpErrorManagerService,
    DotLicenseService,
    DotMessageService,
    DotPageRenderService,
    DotRouterService
} from '@dotcms/data-access';
import { CurrentUser, HttpCode, LoginService, User } from '@dotcms/dotcms-js';
import {
    DotCMSContentlet,
    DotDevice,
    DotExperiment,
    DotExperimentStatus,
    DotPageRenderOptions,
    DotPageRenderParameters,
    DotPageRenderState,
    DotPersona,
    ESContent,
    PageModelChangeEvent,
    PageModelChangeEventType
} from '@dotcms/dotcms-models';
import { generateDotFavoritePageUrl } from '@dotcms/utils';

@Injectable()
export class DotPageStateService {
    state$: Subject<DotPageRenderState> = new Subject<DotPageRenderState>();
    haveContent$ = new BehaviorSubject<boolean>(false);
    private currentState: DotPageRenderState;

    private isInternalNavigation = false;

    constructor(
        private dotContentletLockerService: DotContentletLockerService,
        private dotHttpErrorManagerService: DotHttpErrorManagerService,
        private dotMessageService: DotMessageService,
        private dotPageRenderService: DotPageRenderService,
        private dotRouterService: DotRouterService,
        private loginService: LoginService,
        private dotFavoritePageService: DotFavoritePageService,
        private dotExperimentsService: DotExperimentsService,
        private dotLicenseService: DotLicenseService
    ) {}

    get pagePersonalization() {
        const persona = this.currentState?.viewAs?.persona;

        if (!persona) {
            return `dot:default`;
        }

        return `dot:${persona.contentType}:${persona.keyTag}`;
    }

    /**
     * Get the page state with the options passed
     *
     * @param {DotPageRenderOptions} [options={}]
     * @memberof DotPageStateService
     */
    get(options: DotPageRenderOptions = {}): void {
        if (!options.url) {
            options.url = this.dotRouterService.queryParams.url;
        }

        this.requestPage(options).subscribe((pageState: DotPageRenderState) => {
            this.setState(pageState);
        });
    }

    /**
     * Get the current state if it was set from internal navigation
     *
     * @returns {DotPageRenderState}
     * @memberof DotPageStateService
     */
    getInternalNavigationState(): DotPageRenderState {
        if (this.isInternalNavigation) {
            this.isInternalNavigation = false;

            return this.currentState;
        }

        return null;
    }

    /**
     * Reload the current page state
     *
     * @memberof DotPageStateService
     */
    reload(): void {
        this.get({
            mode: this.currentState.state.mode
        });
    }

    /**
     * Set the page state of view as to received device
     *
     * @param {DotDevice} device
     * @memberof DotPageStateService
     */
    setDevice(device: DotDevice): void {
        this.get({
            viewAs: {
                device
            }
        });
    }

    /**
     * Set the page state from internal navigation
     *
     * @param {DotPageRenderState} state
     * @memberof DotPageStateService
     */
    setInternalNavigationState(state: DotPageRenderState): void {
        const urlParam = generateDotFavoritePageUrl({
            deviceInode: state.viewAs.device?.inode,
            languageId: state.viewAs.language.id,
            pageURI: state.page.pageURI,
            siteId: state.site?.identifier
        });

        this.dotFavoritePageService
            .get({ limit: 10, userId: state.user.userId, url: urlParam })
            .pipe(take(1))
            .subscribe(
                (response: ESContent) => {
                    const favoritePage = response.jsonObjectView?.contentlets[0];

                    if (favoritePage) {
                        state.favoritePage = favoritePage;
                        this.setCurrentState(state);
                    }
                },
                (error: HttpErrorResponse) => {
                    this.dotHttpErrorManagerService.handle(error, true);
                }
            );

        this.getRunningExperiment(state.page.identifier)
            .pipe(take(1))
            .subscribe((experiment: DotExperiment) => {
                if (experiment) {
                    state.runningExperiment = experiment;
                    this.setCurrentState(state);
                }
            });

        this.setCurrentState(state);
        this.isInternalNavigation = true;
    }

    /**
     * Lock or unlock the page and set a new state
     *
     * @param {DotPageRenderOptions} options
     * @param {boolean} [lock=null]
     * @memberof DotPageStateService
     */
    setLock(options: DotPageRenderOptions, lock: boolean = null): void {
        this.getLockMode(this.currentState.page.inode, lock)
            .pipe(
                take(1),
                catchError(() => of(null))
            )
            .subscribe(() => {
                this.get(options);
            });
    }

    /**
     * Set the page state of view as to received language
     *
     * @param {number} language
     * @memberof DotPageStateService
     */
    setLanguage(language: number): void {
        this.dotRouterService.replaceQueryParams({ language_id: language });
    }

    /**
     * Overwrite the local state and emit it
     *
     * @param {DotPageRenderState} state
     * @memberof DotPageStateService
     */
    setLocalState(state: DotPageRenderState): void {
        this.getRunningExperiment(state.page.identifier)
            .pipe(take(1))
            .subscribe((experiment: DotExperiment) => {
                if (experiment) {
                    state.runningExperiment = experiment;
                }

                this.setCurrentState(state);
                this.state$.next(state);
            });
    }

    /**
     * Set the page state of view as to received persona and update the mode and lock state if
     * persona is not personalized.
     *
     * @param {DotPersona} persona
     * @memberof DotPageStateService
     */
    setPersona(persona: DotPersona): void {
        this.get({
            viewAs: {
                persona
            }
        });
    }

    /**
     * Set the FavoritePageHighlight flag status
     *
     * @param {DotCMSContentlet} favoritePage
     * @memberof DotPageStateService
     */
    setFavoritePageHighlight(favoritePage: DotCMSContentlet): void {
        this.currentState.favoritePage = favoritePage;
        this.state$.next(this.currentState);
    }

    /**
     * Set the SEO media to show in the page
     * @param seoTitle
     */
    setSeoMedia(seoMedia: string): void {
        this.currentState.seoMedia = seoMedia;
        this.state$.next(this.currentState);
    }

    /**
     * Update page content status
     *
     * @param {PageModelChangeEvent} event
     * @memberof DotPageStateService
     */
    updatePageStateHaveContent(event: PageModelChangeEvent) {
        if (event.type === PageModelChangeEventType.ADD_CONTENT) {
            this.contentAdded();
        } else if (event.type === PageModelChangeEventType.REMOVE_CONTENT) {
            this.contentRemoved();
        }
    }

    /**
     * Get the page from API, set local state and return DotPageRenderState
     *
     * @param {DotPageRenderOptions} options
     * @returns {Observable<DotPageRenderState>}
     * @memberof DotPageStateService
     */
    requestPage(options: DotPageRenderOptions): Observable<DotPageRenderState> {
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        const { url, ...extraParams } = this.dotRouterService.queryParams;

        return forkJoin([
            this.dotPageRenderService.get(options, extraParams),
            this.loginService.getCurrentUser()
        ]).pipe(
            catchError((err: HttpErrorResponse) => {
                return this.handleSetPageStateFailed(err);
            }),
            take(1),
            switchMap(
                (
                    [page, user]: [page: DotPageRenderParameters, user: CurrentUser] = [null, null]
                ) => {
                    if (page) {
                        const urlParam = generateDotFavoritePageUrl({
                            deviceInode: page.viewAs?.device?.inode,
                            languageId: page.viewAs?.language?.id || 1,
                            pageURI: page.page?.pageURI,
                            siteId: page.site?.identifier
                        });

                        return forkJoin([
                            this.getFavoritePage(user, urlParam),
                            this.getRunningExperiment(page.page.identifier)
                        ]).pipe(
                            take(1),
                            switchMap(
                                ([favoritePage, experiment]: [
                                    favoritePage: DotCMSContentlet,
                                    experiment: DotExperiment
                                ]) => {
                                    return this.setLocalPageState(
                                        page,
                                        favoritePage,
                                        experiment,
                                        options.viewAs?.device
                                    );
                                }
                            )
                        );
                    }

                    return of(this.currentState);
                }
            )
        );
    }

    private setLocalPageState(
        page: DotPageRenderParameters,
        favoritePage?: DotCMSContentlet,
        runningExperiment?: DotExperiment,
        device?: DotDevice
    ): Observable<DotPageRenderState> {
        const pageState = new DotPageRenderState(
            this.getCurrentUser(),
            page,
            favoritePage,
            runningExperiment
        );

        if (!pageState.viewAs?.device && device) {
            pageState.viewAs.device = device;
        }

        this.setCurrentState(pageState);

        return of(pageState);
    }

    private contentAdded(): void {
        this.currentState.numberContents++;

        if (this.currentState.numberContents === 1 && !this.selectedIsDefaultPersona()) {
            this.haveContent$.next(true);
        }
    }

    private contentRemoved(): void {
        this.currentState.numberContents--;

        if (this.currentState.numberContents === 0 && !this.selectedIsDefaultPersona()) {
            this.haveContent$.next(false);
        }
    }

    private setCurrentState(newState: DotPageRenderState): void {
        this.currentState = newState;
        this.haveContent$.next(this.currentState?.numberContents > 0);
    }

    private selectedIsDefaultPersona(): boolean {
        return !!this.currentState.viewAs.persona;
    }

    private getCurrentUser(): User {
        return this.loginService.auth.loginAsUser || this.loginService.auth.user;
    }

    private getLockMode(workingInode: string, lock: boolean): Observable<string> {
        if (lock === true) {
            return this.dotContentletLockerService.lock(workingInode).pipe(pluck('message'));
        } else if (lock === false) {
            return this.dotContentletLockerService.unlock(workingInode).pipe(pluck('message'));
        }

        return of(null);
    }

    private handleSetPageStateFailed(err: HttpErrorResponse): Observable<DotHttpErrorHandled> {
        return this.dotHttpErrorManagerService.handle(err).pipe(
            take(1),
            tap(({ status }: DotHttpErrorHandled) => {
                if (status === HttpCode.FORBIDDEN || status === HttpCode.NOT_FOUND) {
                    this.dotRouterService.goToSiteBrowser();
                } else {
                    this.reload();
                }
            }),
            map(() => undefined)
        );
    }

    private setState(state: DotPageRenderState): void {
        this.state$.next(state);
    }

    private getFavoritePage(user: CurrentUser, urlParam: string): Observable<DotCMSContentlet> {
        return this.dotFavoritePageService
            .get({
                limit: 10,
                userId: user.userId,
                url: urlParam
            })
            .pipe(
                take(1),
                catchError((error: HttpErrorResponse) => {
                    // Set message to throw a custom Favorite Page error message
                    error.error.message = this.dotMessageService.get(
                        'favoritePage.error.fetching.data'
                    );

                    this.dotHttpErrorManagerService.handle(error, true);

                    return of(null);
                }),
                switchMap((content: ESContent) => {
                    return of(content?.jsonObjectView?.contentlets[0]);
                })
            );
    }

    private getRunningExperiment(pageId: string): Observable<DotExperiment> {
        return this.dotLicenseService.isEnterprise().pipe(
            switchMap((isEnterprise: boolean) => {
                if (!isEnterprise) {
                    return of(null);
                }

                return this.dotExperimentsService
                    .getByStatus(pageId, DotExperimentStatus.RUNNING)
                    .pipe(
                        take(1),
                        catchError((error: HttpErrorResponse) => {
                            error.error.message = this.dotMessageService.get(
                                'experiments.error.fetching.data'
                            );

                            this.dotHttpErrorManagerService.handle(error, true);

                            return of(null);
                        }),
                        switchMap((experiments: DotExperiment[]) => {
                            return of(
                                experiments && experiments.length > 0 ? experiments[0] : null
                            );
                        })
                    );
            })
        );
    }
}
