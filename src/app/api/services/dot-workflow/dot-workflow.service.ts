import { pluck, switchMap, take } from 'rxjs/operators';
import { Injectable } from '@angular/core';
import { CoreWebService } from 'dotcms-js';
import { Observable } from 'rxjs';
import { RequestMethod } from '@angular/http';
import { DotCMSWorkflow } from 'dotcms-models';

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
     * @param string id
     * @returns Observable<SelectItem[]>
     * @memberof DotWorkflowService
     */
    get(): Observable<DotCMSWorkflow[]> {
        return this.coreWebService
            .requestView({
                method: RequestMethod.Get,
                url: 'v1/workflow/schemes'
            })
            .pipe(pluck('entity'));
    }

    /**
     * Get the System default workflow
     *
     * @returns Observable<DotWorkflow>
     * @memberof DotWorkflowService
     */
    getSystem(): Observable<DotCMSWorkflow> {
        return this.get().pipe(
            switchMap((workflows: DotCMSWorkflow[]) =>
                workflows.filter((workflow: DotCMSWorkflow) => workflow.system)
            ),
            take(1)
        );
    }

    /**
     * Updates the workflow actions for a page asset
     *
     * @param string inode
     * @returns Observable<any> // contentlet
     * @memberof DotWorkflowService
     */
    fireWorkflowAction(inode: string, actionId: string): Observable<any> {
        return this.coreWebService
            .requestView({
                method: RequestMethod.Put,
                url: `v1/workflow/actions/${actionId}/fire?inode=${inode}`
            })
            .pipe(pluck('entity'));
    }
}
