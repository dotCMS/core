import { pluck, switchMap, take } from 'rxjs/operators';
import { Injectable } from '@angular/core';
import { CoreWebService } from '@dotcms/dotcms-js';
import { Observable } from 'rxjs';
import { DotCMSWorkflow } from '@dotcms/dotcms-models';

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
}
