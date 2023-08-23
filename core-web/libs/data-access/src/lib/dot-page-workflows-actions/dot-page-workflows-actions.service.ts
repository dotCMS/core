import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { pluck } from 'rxjs/operators';

import { DotRenderMode } from '@dotcms/data-access';
import { DotCMSWorkflowAction, DotCMSContentlet } from '@dotcms/dotcms-models';

export interface DotCMSPageWorkflowState {
    actions: DotCMSWorkflowAction[];
    page: DotCMSContentlet;
}

@Injectable()
export class DotPageWorkflowsActionsService {
    constructor(private http: HttpClient) {}

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
        return this.http
            .post('/api/v1/page/actions', {
                host_id: params.host_id,
                language_id: params.language_id,
                url: params.url,
                renderMode: params.renderMode
            })
            .pipe(pluck('entity'));
    }
}
