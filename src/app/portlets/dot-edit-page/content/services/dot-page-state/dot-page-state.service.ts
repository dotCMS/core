import { of, Observable, Subject, BehaviorSubject } from 'rxjs';

import { pluck, take, map, catchError } from 'rxjs/operators';
import { LoginService, User, ResponseView } from 'dotcms-js';
import { DotPageRenderState } from '../../../shared/models/dot-rendered-page-state.model';
import {
    DotPageRenderService,
    DotPageRenderOptions
} from '@services/dot-page-render/dot-page-render.service';
import { DotPageRender } from '../../../shared/models/dot-rendered-page.model';
import { Injectable } from '@angular/core';
import { DotContentletLockerService } from '@services/dot-contentlet-locker/dot-contentlet-locker.service';
import { DotPersona } from '@shared/models/dot-persona/dot-persona.model';
import { DotDevice } from '@shared/models/dot-device/dot-device.model';
import {
    DotHttpErrorManagerService,
    DotHttpErrorHandled
} from '@services/dot-http-error-manager/dot-http-error-manager.service';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { PageModelChangeEvent, PageModelChangeEventType } from '../dot-edit-content-html/models';

@Injectable()
export class DotPageStateService {
    state$: Subject<DotPageRenderState> = new Subject<DotPageRenderState>();
    haveContent$ = new BehaviorSubject<boolean>(false);
    private currentState: DotPageRenderState;

    private isInternalNavigation = false;

    constructor(
        private dotContentletLockerService: DotContentletLockerService,
        private dotHttpErrorManagerService: DotHttpErrorManagerService,
        private dotPageRenderService: DotPageRenderService,
        private dotRouterService: DotRouterService,
        private loginService: LoginService
    ) {}

    /**
     * Get the page state with the options passed
     *
     * @param {DotPageRenderOptions} [options={}]
     * @memberof DotPageStateService
     */
    get(options: DotPageRenderOptions = {}): void {
        if (!options.url) {
            options.url = this.currentState.page.pageURI;
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
     * Request an new page state and set the local internal state
     *
     * @param {DotPageRenderOptions} options
     * @returns {Observable<DotPageRenderState>}
     * @memberof DotPageStateService
     */
    requestPage(options: DotPageRenderOptions): Observable<DotPageRenderState> {
        return this.dotPageRenderService.get(options).pipe(
            catchError((err: ResponseView) => {
                this.handleSetPageStateFailed(err);
                return of(null);
            }),
            take(1),
            map((page: DotPageRender) => {
                if (page) {
                    const pageState = new DotPageRenderState(this.getCurrentUser(), page);
                    this.setCurrentState(pageState);
                    return pageState;
                }

                return this.currentState;
            })
        );
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
            .pipe(take(1))
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
        this.get({
            viewAs: {
                language
            }
        });
    }

    /**
     * Overwrite the local state and emit it
     *
     * @param {DotPageRenderState} state
     * @memberof DotPageStateService
     */
    setLocalState(state: DotPageRenderState): void {
        this.setCurrentState(state);
        this.state$.next(state);
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
     * Call to notify that a content was add into the page
     */
    contentAdded(): void {
        this.currentState.numberContents++;

        if (this.currentState.numberContents === 1 && !this.selectedIsDefaultPersona()) {
            this.haveContent$.next(true);
        }
    }

    /**
     * Call to notify that a content was remove from the page
     */
    contentRemoved(): void {
        this.currentState.numberContents--;

        if (this.currentState.numberContents === 0 && !this.selectedIsDefaultPersona()) {
            this.haveContent$.next(false);
        }
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

    private setCurrentState(newState: DotPageRenderState): void {
        this.currentState = newState;

        if (!this.selectedIsDefaultPersona()) {
            this.haveContent$.next(this.currentState.numberContents > 0);
        }
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

    private handleSetPageStateFailed(err: ResponseView): void {
        this.dotHttpErrorManagerService
            .handle(err)
            .pipe(take(1))
            .subscribe((res: DotHttpErrorHandled) => {
                if (res.forbidden) {
                    this.dotRouterService.goToSiteBrowser();
                } else {
                    this.reload();
                }
            });
    }

    private setState(state: DotPageRenderState): void {
        this.state$.next(state);
    }
}
