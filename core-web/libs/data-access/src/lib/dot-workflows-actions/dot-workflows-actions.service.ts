import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map, pluck } from 'rxjs/operators';

import { DotCMSResponse } from '@dotcms/dotcms-js';
import { DotCMSWorkflowAction, DotCMSWorkflow } from '@dotcms/dotcms-models';

export enum DotRenderMode {
    LISTING = 'LISTING',
    EDITING = 'EDITING'
}

@Injectable()
export class DotWorkflowsActionsService {
    private readonly BASE_URL = '/api/v1/workflow';
    private readonly httpClient = inject(HttpClient);

    /**
     * Return a list of actions based on the workflows received
     *
     * @param {DotCMSWorkflow[]} [workflows=[]]
     * @returns {Observable<DotCMSWorkflowAction[]>}
     * @memberof DotWorkflowsActionsService
     */
    getByWorkflows(workflows: DotCMSWorkflow[] = []): Observable<DotCMSWorkflowAction[]> {
        return this.httpClient
            .post(`${this.BASE_URL}/schemes/actions/NEW`, {
                schemes: workflows.map(this.getWorkFlowId)
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

        return this.httpClient
            .get(`${this.BASE_URL}/contentlet/${inode}/actions${renderModeQuery}`)
            .pipe(pluck('entity'));
    }

    /**
     * Returns the workflow actions of the passed contentType
     *
     * @param {string} inode
     * @param {DotRenderMode} [renderMode]
     * @returns {Observable<DotCMSWorkflowAction[]>}
     * @memberof DotWorkflowsActionsService
     */
    getDefaultActions(contentTypeId: string): Observable<DotCMSWorkflowAction[]> {
        return this.httpClient
            .get<
                DotCMSResponse<{ action: DotCMSWorkflowAction; scheme: DotCMSWorkflow }[]>
            >(`${this.BASE_URL}/initialactions/contenttype/${contentTypeId}`)
            .pipe(
                pluck('entity'),
                map((res = []) => {
                    return res.map(({ action }) => {
                        return action;
                    });
                })
            );
    }

    private getWorkFlowId(workflow: DotCMSWorkflow): string {
        return workflow && workflow.id;
    }
}
