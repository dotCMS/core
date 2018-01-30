import { CoreWebService } from 'dotcms-js/dotcms-js';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Observable';
import { RequestMethod } from '@angular/http';
import { DotRenderedPage } from '../../../portlets/dot-edit-page/shared/models/dot-rendered-page.model';
import { PageMode } from '../../../portlets/dot-edit-content/components/dot-edit-page-toolbar/dot-edit-page-toolbar.component';
import { DotEditPageState } from '../../../shared/models/dot-edit-page-state/dot-edit-page-state.model';
import { DotRenderedPageState } from '../../../portlets/dot-edit-page/shared/models/dot-rendered-page-state.model';

/**
 * Provide util methods to get a edit page html
 * @export
 * @class EditPageService
 */

@Injectable()
export class EditPageService {
    constructor(private coreWebService: CoreWebService) {}

    /**
     * Get the page HTML in edit mode
     *
     * @param {string} url
     * @returns {Observable<DotRenderedPage>}
     * @memberof EditPageService
     */
    getEdit(url: string): Observable<DotRenderedPage> {
        return this.get(url, PageMode.EDIT);
    }

    /**
     * Get the page HTML in preview mode
     *
     * @param {string} url
     * @returns {Observable<DotRenderedPage>}
     * @memberof EditPageService
     */
    getPreview(url: string): Observable<DotRenderedPage> {
        return this.get(url, PageMode.PREVIEW);
    }

    /**
     * Get the page HTML in live mode
     *
     * @param {string} url
     * @returns {Observable<DotRenderedPage>}
     * @memberof EditPageService
     */
    getLive(url: string): Observable<DotRenderedPage> {
        return this.get(url, PageMode.LIVE);
    }

    /**
     * Lock a content asset
     *
     * @param {string} inode
     * @returns {Observable<any>}
     * @memberof PageViewService
     */
    lock(inode: string): Observable<any> {
        return this.coreWebService
            .requestView({
                method: RequestMethod.Put,
                url: `content/lock/inode/${inode}`
            })
            .pluck('bodyJsonObject');
    }

    /**
     * Unlock a content asset
     *
     * @param {string} inode
     * @returns {Observable<any>}
     * @memberof PageViewService
     */
    unlock(inode: string): Observable<any> {
        return this.coreWebService
            .requestView({
                method: RequestMethod.Put,
                url: `content/unlock/inode/${inode}`
            })
            .pluck('bodyJsonObject');
    }

    /**
     * Set the page state
     *
     * @param {DotRenderedPage} page
     * @param {DotEditPageState} state
     * @returns {Observable<any>}
     * @memberof EditPageService
     */
    setPageState(page: DotRenderedPage, state: DotEditPageState): Observable<DotRenderedPageState> {
        const lockUnlock: Observable<string> = this.getLockMode(page.liveInode, state.locked);
        const pageMode: Observable<DotRenderedPage> = state.mode !== undefined ? this.getPageMode(state.mode)(page.pageUri) : null;

        return this.getStateRequest(lockUnlock, pageMode);
    }

    private get(url: string, mode: PageMode): Observable<DotRenderedPage> {
        return this.coreWebService
            .requestView({
                method: RequestMethod.Get,
                url: `v1/page/renderHTML/${url.replace(/^\//, '')}?mode=${mode}`
            })
            .pluck('bodyJsonObject')
            .map((dotRenderedPage: DotRenderedPage) => {
                return {
                    ...dotRenderedPage,
                    locked: !!dotRenderedPage.lockedBy
                };
            });
    }

    private getLockMode(liveInode: string, lock: boolean): Observable<string> {
        if (lock && lock === true) {
            return this.lock(liveInode).pluck('message');
        } else if (lock === false) {
            return this.unlock(liveInode).pluck('message');
        } else {
            return null;
        }
    }

    private getPageMode(mode: PageMode): (string) => Observable<DotRenderedPage> {
        const map = {};
        map[PageMode.PREVIEW] = (url: string) => this.getPreview(url);
        map[PageMode.EDIT] = (url: string) => this.getEdit(url);
        map[PageMode.LIVE] = (url: string) => this.getLive(url);

        return map[mode];
    }

    private getStateRequest(
        lockUnlock: Observable<string>,
        pageMode: Observable<DotRenderedPage>
    ): Observable<DotRenderedPageState> {
        if (lockUnlock && pageMode) {
            return lockUnlock.mergeMap((lockState: string) => {
                return pageMode.map((dotRenderedPage: DotRenderedPage) => {
                    return {
                        dotRenderedPage: dotRenderedPage,
                        lockState: lockState
                    };
                });
            });
        } else if (lockUnlock) {
            return lockUnlock.map((lockState: string) => {
                return {
                    lockState: lockState
                };
            });
        } else if (pageMode) {
            return pageMode.map((dotRenderedPage: DotRenderedPage) => {
                return {
                    dotRenderedPage: dotRenderedPage
                };
            });
        }
    }
}
