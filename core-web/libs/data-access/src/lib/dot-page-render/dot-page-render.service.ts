import { Observable } from 'rxjs';

import { Injectable, inject } from '@angular/core';
import { Params } from '@angular/router';

import { map } from 'rxjs/operators';

import { CoreWebService } from '@dotcms/dotcms-js';
import {
    DotPageMode,
    DotPersona,
    DotDevice,
    DotPageRenderParameters,
    DotPageRenderOptions,
    DotPageRenderOptionsViewAs,
    DotPageRenderRequestParams
} from '@dotcms/dotcms-models';

import { DotSessionStorageService } from '../dot-session-storage/dot-session-storage.service';

/**
 * Get a render page with the received params
 *
 * @export
 * @class DotPageRenderService
 */
@Injectable()
export class DotPageRenderService {
    private coreWebService = inject(CoreWebService);
    private readonly dotSessionStorageService = inject(DotSessionStorageService);

    /**
     * Verifies if a use can access a page based on the path param
     *
     * @param {Params} queryParams
     * @returns {Observable<boolean>}
     * @memberof DotPageRenderService
     */
    checkPermission(queryParams: Params): Observable<boolean> {
        return this.coreWebService
            .requestView({
                body: {
                    ...queryParams
                },
                method: 'POST',
                url: `v1/page/_check-permission`
            })
            .pipe(map((x) => x?.entity));
    }

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
            .pipe(map((x) => x?.entity));
    }

    private getOptionalViewAsParams(
        viewAsConfig: DotPageRenderOptionsViewAs = {},
        mode: DotPageMode = DotPageMode.PREVIEW
    ): DotPageRenderRequestParams {
        return {
            ...this.getPersonaParam(viewAsConfig.persona),
            ...this.getDeviceParam(viewAsConfig.device),
            ...this.getLanguageParam(viewAsConfig.language),
            ...this.getModeParam(mode),
            ...{
                variantName: this.dotSessionStorageService.getVariationId()
            }
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
