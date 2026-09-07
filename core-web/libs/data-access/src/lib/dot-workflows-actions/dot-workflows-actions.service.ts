import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

import {
    DotBulkActionRequest,
    DotBulkActionView,
    DotCMSContentletWorkflowActions,
    DotCMSResponse,
    DotCMSWorkflow,
    DotCMSWorkflowAction
} from '@dotcms/dotcms-models';

import { withDerivedActionInputs } from './dot-workflows-actions.utils';

/**
 * Fills in the `actionInputs` the default/initial-action endpoints do not send (#36883).
 *
 * Applied to `getDefaultActions` and `getWorkFlowActions` only — those return
 * `WorkflowDefaultActionView`, which wraps the raw action and omits the array. `getByInode`,
 * `getByWorkflows` and `getBulkActions` are deliberately left alone: their endpoints build it
 * server-side, and deriving over them would hide a backend regression rather than surface it.
 */
const deriveInputsOnEach = (
    schemeActions: DotCMSContentletWorkflowActions[]
): DotCMSContentletWorkflowActions[] =>
    schemeActions.map((schemeAction) => ({
        ...schemeAction,
        action: withDerivedActionInputs(schemeAction.action)
    }));

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
            .pipe(map((x: { entity?: DotCMSWorkflowAction[] }) => x?.entity)) as Observable<
            DotCMSWorkflowAction[]
        >;
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
            .pipe(map((x: { entity?: DotCMSWorkflowAction[] }) => x?.entity)) as Observable<
            DotCMSWorkflowAction[]
        >;
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
            .pipe(
                map((x) => x?.entity),
                map((res) => res || []),
                map(deriveInputsOnEach)
            );
    }

    /**
     * Returns the workflow actions available for a set of contentlets, grouped by scheme and step,
     * each with the number of selected contentlets it applies to.
     *
     * Backing endpoint: `POST /api/v1/workflow/contentlet/actions/bulk`. Supply either
     * `contentletIds` (contentlet **inodes**, despite the property name) or a Lucene `query` for
     * selections that span pages.
     *
     * Note that an action's `count` is an upper bound when `conditionPresent` is true — the backend
     * does not evaluate the action's Velocity condition while aggregating.
     *
     * @param {DotBulkActionRequest} request
     * @returns {Observable<DotBulkActionView>}
     * @memberof DotWorkflowsActionsService
     */
    getBulkActions(request: DotBulkActionRequest): Observable<DotBulkActionView> {
        return this.httpClient
            .post<
                DotCMSResponse<DotBulkActionView>
            >(`${this.BASE_URL}/contentlet/actions/bulk`, request)
            .pipe(map((response) => response?.entity ?? { schemes: [] }));
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
            .pipe(
                map((x) => x?.entity),
                map((res) => res || []),
                map(deriveInputsOnEach)
            );
    }
}
