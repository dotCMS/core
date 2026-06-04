import { Observable, of } from 'rxjs';

import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map, switchMap, take } from 'rxjs/operators';

import {
    ContentTypeWorkflowSchemesView,
    DotCMSWorkflow,
    DotCMSWorkflowStatus,
    WorkflowStep
} from '@dotcms/dotcms-models';

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
        return this.httpClient
            .get<{ entity: DotCMSWorkflow[] }>(`${this.WORKFLOW_URL}/schemes`)
            .pipe(map((x) => x?.entity));
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
            .get<{
                entity: {
                    contentTypeSchemes: DotCMSWorkflow[];
                    schemes: DotCMSWorkflow[];
                };
            }>(`${this.WORKFLOW_URL}/schemes/schemescontenttypes/${contentTypeId}`)
            .pipe(map((x) => x?.entity));
    }

    /**
     * Get the workflow schemes assigned to the given content types, deduped by id.
     *
     * @param {string[]} contentTypeIds
     * @return {*}  {Observable<DotCMSWorkflow[]>}
     * @memberof DotWorkflowService
     */
    getSchemesByContentTypes(contentTypeIds: string[]): Observable<DotCMSWorkflow[]> {
        if (!contentTypeIds.length) {
            return of([]);
        }

        const params = contentTypeIds.reduce(
            (acc, id) => acc.append('contentTypeIds', id),
            new HttpParams()
        );

        return this.httpClient
            .get<{
                entity: ContentTypeWorkflowSchemesView[];
            }>(`${this.WORKFLOW_URL}/contenttypes/schemes`, { params })
            .pipe(
                map((response) => {
                    const schemesById = new Map<string, DotCMSWorkflow>();
                    (response?.entity ?? []).forEach((view) =>
                        view.schemes?.forEach((scheme) => schemesById.set(scheme.id, scheme))
                    );

                    return Array.from(schemesById.values());
                })
            );
    }

    /**
     * Get the steps that belong to a workflow scheme.
     *
     * @param {string} schemeId
     * @return {*}  {Observable<WorkflowStep[]>}
     * @memberof DotWorkflowService
     */
    getSteps(schemeId: string): Observable<WorkflowStep[]> {
        return this.httpClient
            .get<{ entity: WorkflowStep[] }>(`${this.WORKFLOW_URL}/schemes/${schemeId}/steps`)
            .pipe(map((x) => x?.entity));
    }

    /**
     * Get the current workflow status for Contentlet given its inode
     *
     * @param {string} inode
     * @return {*}  {Observable<DotCMSWorkflowStatus>}
     * @memberof DotWorkflowService
     */
    getWorkflowStatus(inode: string): Observable<DotCMSWorkflowStatus> {
        return this.httpClient
            .get<{ entity: DotCMSWorkflowStatus }>(`${this.WORKFLOW_URL}/status/${inode}`)
            .pipe(map((x) => x?.entity));
    }
}
