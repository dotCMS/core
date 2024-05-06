import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { pluck, switchMap, take } from 'rxjs/operators';

import { DotCMSWorkflow, DotCMSWorkflowStatus } from '@dotcms/dotcms-models';

/**
 * Provide util methods to get Workflows.
 * @export
 * @class DotWorkflowService
 */
@Injectable()
export class DotWorkflowService {
    private readonly WORKFLOW_URL = '/api/v1/workflow';
    private readonly httpClient: HttpClient = inject(HttpClient);

    /**
     * Method to get Workflows
     * @param string id
     * @returns Observable<SelectItem[]>
     * @memberof DotWorkflowService
     */
    get(): Observable<DotCMSWorkflow[]> {
        return this.httpClient.get(`${this.WORKFLOW_URL}/schemes`).pipe(pluck('entity'));
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
     * Get the Workflow Schema for a ContentType given its inode
     *
     * @param {string} contentTypeId
     * @return {*}
     * @memberof DotWorkflowService
     */
    getSchemaContentType(contentTypeId: string): Observable<{
        contentTypeSchemes: DotCMSWorkflow[];
        schemes: DotCMSWorkflow[];
    }> {
        return this.httpClient
            .get(`${this.WORKFLOW_URL}/schemes/schemescontenttypes/${contentTypeId}`)
            .pipe(pluck('entity'));
    }

    /**
     * Get the current workflow status for Contentlet given its inode
     *
     * @param {string} inode
     * @return {*}  {Observable<DotCMSWorkflowStatus>}
     * @memberof DotWorkflowService
     */
    getWorkflowStatus(inode: string): Observable<DotCMSWorkflowStatus> {
        return this.httpClient.get(`${this.WORKFLOW_URL}/status/${inode}`).pipe(pluck('entity'));
    }
}
