import { of as observableOf, Observable, Subject } from 'rxjs';

import { mergeMap, pluck, take, map } from 'rxjs/operators';
import { DotPage } from './../../../shared/models/dot-page.model';
import { LoginService } from 'dotcms-js/dotcms-js';
import { DotPageState, DotRenderedPageState } from '../../../shared/models/dot-rendered-page-state.model';
import { DotRenderHTMLService, DotRenderPageOptions } from '@services/dot-render-html/dot-render-html.service';
import { DotRenderedPage } from '../../../shared/models/dot-rendered-page.model';
import { Injectable } from '@angular/core';
import { DotContentletLockerService } from '@services/dot-contentlet-locker/dot-contentlet-locker.service';
import { DotEditPageViewAs } from '@models/dot-edit-page-view-as/dot-edit-page-view-as.model';

@Injectable()
export class DotPageStateService {
    reload$: Subject<DotRenderedPage> = new Subject<DotRenderedPage>();

    constructor(
        private dotRenderHTMLService: DotRenderHTMLService,
        private dotContentletLockerService: DotContentletLockerService,
        private loginService: LoginService
    ) {}

    /**
     * Set the page state
     *
     * @param {DotRenderedPage} page
     * @param {DotEditPageState} state
     * @returns {Observable<any>}
     * @memberof DotRenderHTMLService
     */
    set(page: DotPage, state: DotPageState, viewAs?: DotEditPageViewAs): Observable<DotRenderedPageState> {
        const lockUnlock$: Observable<string> = this.getLockMode(page.workingInode, state.locked);
        const pageOpts: DotRenderPageOptions = {
            url: page.pageURI,
            mode: state.mode,
            viewAs: this.dotRenderHTMLService.getDotEditPageViewAsParams(viewAs)
        };
        const pageMode$: Observable<DotRenderedPage> =
            state.mode !== undefined ? this.dotRenderHTMLService.get(pageOpts) : observableOf(null);

        return lockUnlock$.pipe(
            mergeMap(() =>
                pageMode$.pipe(
                    map(
                        (updatedPage: DotRenderedPage) =>
                            new DotRenderedPageState(this.loginService.auth.loginAsUser || this.loginService.auth.user, updatedPage)
                    )
                )
            )
        );
    }

    /**
     * Get page state
     *
     * @param {string} url
     * @param {number} [languageId]
     * @memberof DotPageStateService
     */
    reload(url: string, languageId?: number): void {
        this.get(url, languageId)
            .pipe(take(1))
            .subscribe((page: DotRenderedPageState) => {
                this.reload$.next(page);
            });
    }

    /**
     * Get page state
     *
     * @param {string} url
     * @param {number} [languageId]
     * @returns {Observable<DotRenderedPageState>}
     * @memberof DotPageStateService
     */
    get(url: string, languageId?: number): Observable<DotRenderedPageState> {
        const options: DotRenderPageOptions = {
            url: url
        };

        if (languageId) {
            options.viewAs = {
                language_id: languageId
            };
        }

        return this.dotRenderHTMLService
            .get(options)
            .pipe(
                map(
                    (page: DotRenderedPage) =>
                        new DotRenderedPageState(this.loginService.auth.loginAsUser || this.loginService.auth.user, page)
                )
            );
    }

    private getLockMode(workingInode: string, lock: boolean): Observable<string> {
        if (lock === true) {
            return this.dotContentletLockerService.lock(workingInode).pipe(pluck('message'));
        } else if (lock === false) {
            return this.dotContentletLockerService.unlock(workingInode).pipe(pluck('message'));
        }

        return observableOf(null);
    }
}
