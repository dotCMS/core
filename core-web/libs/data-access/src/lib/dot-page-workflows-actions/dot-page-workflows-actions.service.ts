import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

import { DotCMSResponse } from '@dotcms/dotcms-js';
import { DotCMSWorkflowAction, DotCMSContentlet } from '@dotcms/dotcms-models';

import { DotRenderMode } from '../dot-workflows-actions/dot-workflows-actions.service';

export interface DotCMSPageWorkflowState {
    actions: DotCMSWorkflowAction[];
    page: DotCMSContentlet;
}
@Injectable()
export class DotPageWorkflowsActionsService {
    private http = inject(HttpClient);

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
            .post<DotCMSResponse<DotCMSPageWorkflowState>>('/api/v1/page/actions', {
                host_id: params.host_id,
                language_id: params.language_id,
                url: params.url,
                renderMode: params.renderMode
            })
            .pipe(map((x: DotCMSResponse<DotCMSPageWorkflowState>) => x?.entity));
    }
}
