import { pluck } from 'rxjs/operators';
import { CoreWebService } from 'dotcms-js';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { RequestMethod } from '@angular/http';
import { DotRenderedPage } from '@portlets/dot-edit-page/shared/models/dot-rendered-page.model';
import { PageMode } from '@portlets/dot-edit-page/shared/models/page-mode.enum';
import { DotEditPageViewAs } from '@models/dot-edit-page-view-as/dot-edit-page-view-as.model';
import { DotLanguage } from '@models/dot-language/dot-language.model';
import { DotPersona } from '@models/dot-persona/dot-persona.model';
import { DotDevice } from '@models/dot-device/dot-device.model';

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
     * @param string url
     * @param DotEditPageViewAs viewAsConfig
     * @returns Observable<DotRenderedPage>
     * @memberof DotRenderHTMLService
     */
    getEdit(url: string, viewAsConfig?: DotEditPageViewAs): Observable<DotRenderedPage> {
        return this.get({
            url: url,
            mode: PageMode.EDIT,
            viewAs: this.getDotEditPageViewAsParams(viewAsConfig)
        });
    }

    /**
     * Get the page HTML in preview mode
     *
     * @param string url
     * @param DotEditPageViewAs viewAsConfig
     * @returns Observable<DotRenderedPage>
     * @memberof DotRenderHTMLService
     */
    getPreview(url: string, viewAsConfig?: DotEditPageViewAs): Observable<DotRenderedPage> {
        return this.get({
            url: url,
            mode: PageMode.PREVIEW,
            viewAs: this.getDotEditPageViewAsParams(viewAsConfig)
        });
    }

    /**
     * Get the page HTML in live mode
     *
     * @param string url
     * @param DotEditPageViewAs viewAsConfig
     * @returns Observable<DotRenderedPage>
     * @memberof DotRenderHTMLService
     */
    getLive(url: string, viewAsConfig?: DotEditPageViewAs): Observable<DotRenderedPage> {
        return this.get({
            url: url,
            mode: PageMode.LIVE,
            viewAs: this.getDotEditPageViewAsParams(viewAsConfig)
        });
    }

    public get(options: DotRenderPageOptions): Observable<DotRenderedPage> {
        const params: RenderPageServerParam = {
            ...options.mode ? { mode: this.getPageModeString(options.mode) } : {},
            ...(options.viewAs ? this.getOptionalViewAsParams(options.viewAs) : {})
        };

        return this.coreWebService
            .requestView({
                method: RequestMethod.Get,
                url: `v1/page/render/${options.url.replace(/^\//, '')}`,
                params: params
            })
            .pipe(pluck('entity'));
    }

    public getDotEditPageViewAsParams(viewAs: DotEditPageViewAs): DotEditPageViewAsParams {
        return viewAs
            ? {
                  persona_id: this.getPropertyValue(viewAs.persona, 'identifier'),
                  language_id: this.getPropertyValue(viewAs.language, 'id'),
                  device_inode: this.getPropertyValue(viewAs.device, 'inode')
              }
            : null;
    }

    private getPropertyValue(
        object: DotPersona | DotLanguage | DotDevice,
        propertyName: string
    ): any {
        return object ? object[propertyName] : null;
    }

    // tslint:disable-next-line:cyclomatic-complexity
    private getOptionalViewAsParams(viewAsConfig: DotEditPageViewAsParams) {
        const options: any = {
            ...viewAsConfig.persona_id ? { 'com.dotmarketing.persona.id': viewAsConfig.persona_id } : {},
            ...viewAsConfig.device_inode ? { 'device_inode': viewAsConfig.device_inode } : {}
        };

        if (viewAsConfig.language_id ) {
            options.language_id = viewAsConfig.language_id;
        }

        return options;
    }

    private getPageModeString(pageMode: PageMode): string {
        const pageModeString = {};
        pageModeString[PageMode.EDIT] = 'EDIT_MODE';
        pageModeString[PageMode.PREVIEW] = 'PREVIEW_MODE';
        pageModeString[PageMode.LIVE] = 'ADMIN_MODE';

        return pageModeString[pageMode];
    }
}

export interface DotRenderPageOptions {
    url: string;
    mode?: PageMode;
    viewAs?: DotEditPageViewAsParams;
}

interface DotEditPageViewAsParams {
    persona_id?: string;
    language_id?: number;
    device_inode?: string;
}

interface RenderPageServerParam {
    persona_id?: string;
    language_id?: string;
    device_inode?: string;
    mode: string;
}
