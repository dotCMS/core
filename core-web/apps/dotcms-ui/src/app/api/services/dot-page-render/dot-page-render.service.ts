import { pluck } from 'rxjs/operators';
import { CoreWebService } from '@dotcms/dotcms-js';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { DotPageRenderParameters } from '@models/dot-page/dot-rendered-page.model';
import { DotPageMode } from '@models/dot-page/dot-page-mode.enum';

import { DotPersona } from '@shared/models/dot-persona/dot-persona.model';
import { DotDevice } from '@shared/models/dot-device/dot-device.model';
import { Params } from '@angular/router';

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
                url: `v1/page/render/${url.replace(/^\//, '')}`,
                params: { ...extraParams, ...params }
            })
            .pipe(pluck('entity'));
    }

    private getOptionalViewAsParams(
        viewAsConfig: DotPageRenderOptionsViewAs = {},
        mode: DotPageMode
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
                  'com.dotmarketing.persona.id': persona.identifier || null
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

export interface DotPageRenderOptionsViewAs {
    persona?: DotPersona;
    language?: number;
    device?: DotDevice;
}

export interface DotPageRenderOptions {
    url?: string;
    mode?: DotPageMode;
    viewAs?: DotPageRenderOptionsViewAs;
}

interface DotPageRenderRequestParams {
    persona_id?: string;
    language_id?: string;
    device_inode?: string;
    mode?: DotPageMode;
}
