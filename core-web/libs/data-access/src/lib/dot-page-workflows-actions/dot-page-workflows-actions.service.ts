import { Observable } from 'rxjs';

import { Injectable } from '@angular/core';

import { pluck } from 'rxjs/operators';

import { DotRenderMode } from '@dotcms/data-access';
import { CoreWebService } from '@dotcms/dotcms-js';
import { DotCMSWorkflowAction, DotCMSContentlet } from '@dotcms/dotcms-models';

export interface DotCMSPageWorkflowState {
    actions: DotCMSWorkflowAction[];
    page: DotCMSContentlet;
}

@Injectable()
export class DotPageWorkflowsActionsService {
    constructor(private coreWebService: CoreWebService) {}

    /**
     * Returns the workflow actions of the passed url, hostId and language
     *
     * @param { host_id: string; language_id: string; url: string; renderMode?: DotRenderMode; } params
     * @returns {Observable<DotCMSPageWorkflowState>}
     * @memberof DotWorkflowsActionsService
     */
    getByUrl(params: {
        host_id: string;
        language_id: string;
        url: string;
        renderMode?: DotRenderMode;
    }): Observable<DotCMSPageWorkflowState> {
        return this.coreWebService
            .requestView({
                method: 'POST',
                url: `v1/page/actions`,
                body: {
                    host_id: params.host_id,
                    language_id: params.language_id,
                    url: params.url,
                    renderMode: params.renderMode
                }
            })
            .pipe(pluck('entity'));
    }
}
