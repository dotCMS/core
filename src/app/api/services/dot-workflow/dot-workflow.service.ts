import { Injectable } from '@angular/core';
import { CoreWebService } from 'dotcms-js/dotcms-js';
import { Observable } from 'rxjs/Observable';
import { RequestMethod } from '@angular/http';
import { DotWorkflow } from '../../../shared/models/dot-workflow/dot-workflow.model';
import { DotWorkflowAction } from '../../../shared/models/dot-workflow-action/dot-workflow-action.model';
import { switchMap, take } from 'rxjs/operators';

/**
 * Provide util methods to get Workflows.
 * @export
 * @class DotWorkflowService
 */
@Injectable()
export class DotWorkflowService {
    constructor(private coreWebService: CoreWebService) {}

    /**
     * Method to get Workflows
     * @param {string} id
     * @returns {Observable<SelectItem[]>}
     * @memberof DotWorkflowService
     */
    get(): Observable<DotWorkflow[]> {
        return this.coreWebService
            .requestView({
                method: RequestMethod.Get,
                url: 'v1/workflow/schemes'
            })
            .pluck('entity');
    }

    /**
     * Get the System default workflow
     *
     * @returns {Observable<DotWorkflow>}
     * @memberof DotWorkflowService
     */
    getSystem(): Observable<DotWorkflow> {
        return this.get().pipe(
            switchMap((workflows: DotWorkflow[]) => workflows.filter((workflow: DotWorkflow) => workflow.system)),
            take(1)
        );
    }

    /**
     * Returns the wokflow or workflow actions for a page asset
     *
     * @param {string} inode
     * @returns {Observable<DotWorkflowAction[]>}
     * @memberof DotWorkflowService
     */
    getContentWorkflowActions(inode: string): Observable<DotWorkflowAction[]> {
        return this.coreWebService
            .requestView({
                method: RequestMethod.Get,
                url: `v1/workflow/contentlet/${inode}/actions`
            })
            .pluck('entity');
    }

    /**
     * Updates the workflow actions for a page asset
     *
     * @param {string} inode
     * @returns {Observable<any>} // contentlet
     * @memberof DotWorkflowService
     */
    fireWorkflowAction(inode: string, actionId: string): Observable<any> {
        return this.coreWebService
            .requestView({
                method: RequestMethod.Put,
                url: `v1/workflow/actions/${actionId}/fire?inode=${inode}`
            })
            .pluck('entity');
    }
}
