import { Injectable } from '@angular/core';
import { CoreWebService } from '@dotcms/dotcms-js';
import { pluck } from 'rxjs/operators';
import { Observable } from 'rxjs';
import { DotCMSWorkflowAction, DotCMSWorkflow } from '@dotcms/dotcms-models';

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
     * @returns {Observable<DotCMSWorkflowAction[]>}
     * @memberof DotWorkflowsActionsService
     */
    getByInode(inode: string): Observable<DotCMSWorkflowAction[]> {
        return this.coreWebService
            .requestView({
                url: `v1/workflow/contentlet/${inode}/actions`
            })
            .pipe(pluck('entity'));
    }

    private getWorkFlowId(workflow: DotCMSWorkflow): string {
        return workflow && workflow.id;
    }
}
