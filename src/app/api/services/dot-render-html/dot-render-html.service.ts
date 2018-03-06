import { CoreWebService } from 'dotcms-js/dotcms-js';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Observable';
import { RequestMethod } from '@angular/http';
import { DotRenderedPage } from '../../../portlets/dot-edit-page/shared/models/dot-rendered-page.model';
import { PageMode } from '../../../portlets/dot-edit-page/shared/models/page-mode.enum';

/**
 * Provide util methods to get a edit page html
 * @export
 * @class DotRenderHTMLService
 */
@Injectable()
export class DotRenderHTMLService {
    constructor(private coreWebService: CoreWebService) {}

    /**
     * Get the page HTML in edit mode
     *
     * @param {string} url
     * @returns {Observable<DotRenderedPage>}
     * @memberof DotRenderHTMLService
     */
    getEdit(url: string): Observable<DotRenderedPage> {
        return this.get(url, PageMode.EDIT);
    }

    /**
     * Get the page HTML in preview mode
     *
     * @param {string} url
     * @returns {Observable<DotRenderedPage>}
     * @memberof DotRenderHTMLService
     */
    getPreview(url: string): Observable<DotRenderedPage> {
        return this.get(url, PageMode.PREVIEW);
    }

    /**
     * Get the page HTML in live mode
     *
     * @param {string} url
     * @returns {Observable<DotRenderedPage>}
     * @memberof DotRenderHTMLService
     */
    getLive(url: string): Observable<DotRenderedPage> {
        return this.get(url, PageMode.LIVE);
    }

    private get(url: string, pageMode: PageMode): Observable<DotRenderedPage> {
        return this.coreWebService
            .requestView({
                method: RequestMethod.Get,
                url: `v1/page/renderHTML/${url.replace(/^\//, '')}?mode=${this.getPageModeString(pageMode)}`
            })
            .pluck('bodyJsonObject');
    }

    private getPageModeString(pageMode: PageMode): string {
        const pageModeString = {};
        pageModeString[PageMode.EDIT] = 'EDIT_MODE';
        pageModeString[PageMode.PREVIEW] = 'PREVIEW_MODE';
        pageModeString[PageMode.LIVE] = 'LIVE';

        return pageModeString[pageMode];
    }
}
