import { pluck } from 'rxjs/operators';
import { CoreWebService } from '@dotcms/dotcms-js';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { Params } from '@angular/router';
import {
    DotPageMode,
    DotPersona,
    DotDevice,
    DotPageRenderParameters,
    DotPageRenderOptions,
    DotPageRenderOptionsViewAs,
    DotPageRenderRequestParams
} from '@dotcms/dotcms-models';

/**
 * Get a render page with the received params
 *
 * @export
 * @class DotPageRenderService
 */
@Injectable()
export class DotPageRenderService {
    constructor(private coreWebService: CoreWebService) {}

    /**
     * Make request to get a rendered page
     *
     * @param {DotPageRenderOptions} options
     * @returns {Observable<DotPageRender>}
     * @memberof DotPageRenderService
     */
    get(
        { viewAs, mode, url }: DotPageRenderOptions,
        extraParams?: Params
    ): Observable<DotPageRenderParameters> {
        const params: DotPageRenderRequestParams = this.getOptionalViewAsParams(viewAs, mode);

        return this.coreWebService
            .requestView({
                url: `v1/page/render/${url?.replace(/^\//, '')}`,
                params: { ...extraParams, ...params }
            })
            .pipe(pluck('entity'));
    }

    private getOptionalViewAsParams(
        viewAsConfig: DotPageRenderOptionsViewAs = {},
        mode: DotPageMode = DotPageMode.PREVIEW
    ): DotPageRenderRequestParams {
        return {
            ...this.getPersonaParam(viewAsConfig.persona),
            ...this.getDeviceParam(viewAsConfig.device),
            ...this.getLanguageParam(viewAsConfig.language),
            ...this.getModeParam(mode)
        };
    }

    private getModeParam(mode: DotPageMode): { [key: string]: DotPageMode } {
        return mode ? { mode } : {};
    }

    private getPersonaParam(persona: DotPersona): { [key: string]: string } {
        return persona
            ? {
                  'com.dotmarketing.persona.id': persona.identifier || ''
              }
            : {};
    }

    private getDeviceParam(device: DotDevice): { [key: string]: string } {
        return device
            ? {
                  device_inode: device.inode
              }
            : {};
    }

    private getLanguageParam(language: number): { [key: string]: string } {
        return language
            ? {
                  language_id: language.toString()
              }
            : {};
    }
}
