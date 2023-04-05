import { Observable } from 'rxjs';

import { Injectable } from '@angular/core';

import { pluck } from 'rxjs/operators';

import { CoreWebService } from '@dotcms/dotcms-js';
import { DotCMSWorkflowAction, DotCMSWorkflow, DotCMSContentlet } from '@dotcms/dotcms-models';

export enum DotRenderMode {
    LISTING = 'LISTING',
    EDITING = 'EDITING'
}

export interface DotCMSPageWorkflowState {
    actions: DotCMSWorkflowAction[];
    page: DotCMSContentlet;
}

@Injectable()
export class DotWorkflowsActionsService {
    constructor(private coreWebService: CoreWebService) {}

    /**
     * Return a list of actions based on the workflows received
     *
     * @param {DotCMSWorkflow[]} [workflows=[]]
     * @returns {Observable<DotCMSWorkflowAction[]>}
     * @memberof DotWorkflowsActionsService
     */
    getByWorkflows(workflows: DotCMSWorkflow[] = []): Observable<DotCMSWorkflowAction[]> {
        return this.coreWebService
            .requestView({
                method: 'POST',
                url: '/api/v1/workflow/schemes/actions/NEW',
                body: {
                    schemes: workflows.map(this.getWorkFlowId)
                }
            })
            .pipe(pluck('entity'));
    }

    /**
     * Returns the workflow actions of the passed inode
     *
     * @param {string} inode
     * @param {DotRenderMode} [renderMode]
     * @returns {Observable<DotCMSWorkflowAction[]>}
     * @memberof DotWorkflowsActionsService
     */
    getByInode(inode: string, renderMode?: DotRenderMode): Observable<DotCMSWorkflowAction[]> {
        const renderModeQuery = renderMode ? `?renderMode=${renderMode}` : '';

        return this.coreWebService
            .requestView({
                url: `v1/workflow/contentlet/${inode}/actions${renderModeQuery}`
            })
            .pipe(pluck('entity'));
    }

    /**
     * Returns the workflow actions of the passed url, hostId and language
     *
     * @param { host_id: string; language_id: string; url: string; renderMode?: DotRenderMode; } params
     * @param {string} host_id
     * @param {string} language_id
     * @param {string} url
     * @param {DotRenderMode} [renderMode]
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

    private getWorkFlowId(workflow: DotCMSWorkflow): string {
        return workflow && workflow.id;
    }
}
