import { CoreWebService } from 'dotcms-js/dotcms-js';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Observable';
import { RequestMethod } from '@angular/http';
import { DotRenderedPage } from '../../../portlets/dot-edit-page/shared/models/dot-rendered-page.model';
import { PageMode } from '../../../portlets/dot-edit-page/shared/models/page-mode.enum';
import { DotEditPageViewAs } from '../../../shared/models/dot-edit-page-view-as/dot-edit-page-view-as.model';

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
     * @param {DotEditPageViewAs} viewAsConfig
     * @returns {Observable<DotRenderedPage>}
     * @memberof DotRenderHTMLService
     */
    getEdit(url: string, viewAsConfig?: DotEditPageViewAs): Observable<DotRenderedPage> {
        return this.get(url, PageMode.EDIT, viewAsConfig);
    }

    /**
     * Get the page HTML in preview mode
     *
     * @param {string} url
     * @param {DotEditPageViewAs} viewAsConfig
     * @returns {Observable<DotRenderedPage>}
     * @memberof DotRenderHTMLService
     */
    getPreview(url: string, viewAsConfig?: DotEditPageViewAs): Observable<DotRenderedPage> {
        return this.get(url, PageMode.PREVIEW, viewAsConfig);
    }

    /**
     * Get the page HTML in live mode
     *
     * @param {string} url
     * @param {DotEditPageViewAs} viewAsConfig
     * @returns {Observable<DotRenderedPage>}
     * @memberof DotRenderHTMLService
     */
    getLive(url: string, viewAsConfig?: DotEditPageViewAs): Observable<DotRenderedPage> {
        return this.get(url, PageMode.LIVE, viewAsConfig);
    }

    private get(url: string, pageMode: PageMode, viewAsConfig?: DotEditPageViewAs): Observable<DotRenderedPage> {
        let params = { mode: this.getPageModeString(pageMode) };
        if (viewAsConfig) {
            params = {
                ...params,
                ...this.setOptionalViewAsParams(viewAsConfig)
            };
        }
        return this.coreWebService
            .requestView({
                method: RequestMethod.Get,
                url: `v1/page/renderHTML/${url.replace(/^\//, '')}`,
                params: params
            })
            .pluck('bodyJsonObject');
    }

    private setOptionalViewAsParams(viewAsConfig: DotEditPageViewAs) {
        return {
            language_id: viewAsConfig.language.id,
            ...viewAsConfig.persona ? { 'com.dotmarketing.persona.id': viewAsConfig.persona.identifier } : {},
            ...viewAsConfig.device ? { 'device_inode': viewAsConfig.device.inode } : {}
        };
    }

    private getPageModeString(pageMode: PageMode): string {
        const pageModeString = {};
        pageModeString[PageMode.EDIT] = 'EDIT_MODE';
        pageModeString[PageMode.PREVIEW] = 'PREVIEW_MODE';
        pageModeString[PageMode.LIVE] = 'LIVE';

        return pageModeString[pageMode];
    }
}
