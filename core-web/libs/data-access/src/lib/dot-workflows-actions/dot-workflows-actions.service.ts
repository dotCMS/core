import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

import { DotCMSResponse } from '@dotcms/dotcms-js';
import {
    DotCMSContentletWorkflowActions,
    DotCMSWorkflow,
    DotCMSWorkflowAction
} from '@dotcms/dotcms-models';

export enum DotRenderMode {
    LOCKED = 'LOCKED',
    LISTING = 'LISTING',
    ARCHIVED = 'ARCHIVED',
    UNPUBLISHED = 'UNPUBLISHED',
    PUBLISHED = 'PUBLISHED',
    UNLOCKED = 'UNLOCKED',
    NEW = 'NEW',
    EDITING = 'EDITING'
}

interface DotApiResponse<T> {
    entity: T;
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
            .post<DotApiResponse<DotCMSWorkflowAction[]>>(`${this.BASE_URL}/schemes/actions/NEW`, {
                schemes: workflows.map(this.getWorkFlowId)
            })
            .pipe(map((res) => res?.entity));
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
            .get<DotApiResponse<DotCMSWorkflowAction[]>>(
                `${this.BASE_URL}/contentlet/${inode}/actions${renderModeQuery}`
            )
            .pipe(map((res) => res?.entity));
    }

    /**
     * Returns the workflow actions of the passed contentType
     * @param {string} inode
     * @param {DotRenderMode} [renderMode]
     * @returns {Observable<DotCMSWorkflowAction[]>}
     * @memberof DotWorkflowsActionsService
     */
    getDefaultActions(contentTypeId: string): Observable<DotCMSContentletWorkflowActions[]> {
        return this.httpClient
            .get<
                DotCMSResponse<DotCMSContentletWorkflowActions[]>
            >(`${this.BASE_URL}/initialactions/contenttype/${contentTypeId}`)
            .pipe(map((res) => res?.entity || []));
    }

    private getWorkFlowId(workflow: DotCMSWorkflow): string {
        return workflow && workflow.id;
    }

    /**
     * Returns the workflow actions of the passed content type name
     *
     * @param {string} contentTypeName
     * @returns {Observable<DotCMSWorkflowActions>}
     */
    getWorkFlowActions(contentTypeName: string): Observable<DotCMSContentletWorkflowActions[]> {
        return this.httpClient
            .get<
                DotCMSResponse<DotCMSContentletWorkflowActions[]>
            >(`${this.BASE_URL}/defaultactions/contenttype/${contentTypeName}`)
            .pipe(map((res) => res?.entity || []));
    }
}
