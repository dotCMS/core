import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { pluck, take } from 'rxjs/operators';
import { CoreWebService } from '@dotcms/dotcms-js';
import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { DotActionBulkRequestOptions } from '@models/dot-action-bulk-request-options/dot-action-bulk-request-options.model';
import { DotActionBulkResult } from '@models/dot-action-bulk-result/dot-action-bulk-result.model';

interface DotActionRequestOptions {
    contentType?: string;
    data: { [key: string]: string };
    action: ActionToFire;
    individualPermissions?: { [key: string]: string[] };
}

enum ActionToFire {
    NEW = 'NEW',
    PUBLISH = 'PUBLISH',
    EDIT = 'EDIT'
}

@Injectable()
export class DotWorkflowActionsFireService {
    constructor(private coreWebService: CoreWebService) {}

    /**
     * Fire a workflow action over a contentlet
     *
     * @param {string} inode
     * @param {string} actionId
     * @param {{ [key: string]: string }} data
     * @returns Observable<DotCMSContentlet> // contentlet
     * @memberof DotWorkflowActionsFireService
     */
    fireTo<T = { [key: string]: string }>(
        inode: string,
        actionId: string,
        data?: T
    ): Observable<DotCMSContentlet> {
        return this.coreWebService
            .requestView({
                body: data,
                method: 'PUT',
                url: `v1/workflow/actions/${actionId}/fire?inode=${inode}&indexPolicy=WAIT_FOR`
            })
            .pipe(pluck('entity'));
    }

    /**
     * Fire a workflow action over a contentlet
     *
     * @param {DotActionBulkRequestOptions} data
     * @returns Observable<DotActionBulkResult> // contentlet
     * @memberof DotWorkflowActionsFireService
     */
    bulkFire(data: DotActionBulkRequestOptions): Observable<DotActionBulkResult> {
        return this.coreWebService
            .requestView({
                body: data,
                method: 'PUT',
                url: `/api/v1/workflow/contentlet/actions/bulk/fire`
            })
            .pipe(pluck('entity'));
    }

    /**
     * Fire a "NEW" action over the content type received with the specified data
     *
     * @param {contentType} string
     * @param {[key: string]: string} data
     * @returns Observable<T>
     *
     * @memberof DotWorkflowActionsFireService
     */
    newContentlet<T>(contentType: string, data: { [key: string]: string }): Observable<T> {
        return this.request<T>({ contentType, data, action: ActionToFire.NEW });
    }

    /**
     * Fire a "PUBLISH" action over the content type received with the specified data
     *
     * @template T
     * @param {string} contentType
     * @param {{ [key: string]: string}} data
     * @returns {Observable<T>}
     * @memberof DotWorkflowActionsFireService
     */
    publishContentlet<T>(
        contentType: string,
        data: { [key: string]: string },
        individualPermissions?: { [key: string]: string[] }
    ): Observable<T> {
        return this.request<T>({
            contentType,
            data,
            action: ActionToFire.PUBLISH,
            individualPermissions
        });
    }
    /**
     * Fire an "EDIT" action over the content type received with the specified data
     *
     * @template T
     * @param {string} contentType
     * @param {{ [key: string]: unknown}} data
     * @return {*}  {Observable<T>}
     * @memberof DotWorkflowActionsFireService
     */
    saveContentlet<T>(data: { [key: string]: string }): Observable<T> {
        return this.request<T>({
            data,
            action: ActionToFire.EDIT
        });
    }

    /**
     * Fire a "PUBLISH" action over the content type received and append the wait for index attr
     *
     * @template T
     * @param {string} contentType
     * @param {{ [key: string]: unknown}} data
     * @returns {Observable<T>}
     * @memberof DotWorkflowActionsFireService
     */
    publishContentletAndWaitForIndex<T>(
        contentType: string,
        data: { [key: string]: string | number },
        individualPermissions?: { [key: string]: string[] }
    ): Observable<T> {
        return this.publishContentlet(
            contentType,
            {
                ...data,
                ...{ indexPolicy: 'WAIT_FOR' }
            },
            individualPermissions
        );
    }

    private request<T>({
        contentType,
        data,
        action,
        individualPermissions
    }: DotActionRequestOptions): Observable<T> {
        const contentlet = contentType ? { contentType: contentType, ...data } : data;
        const bodyRequest = individualPermissions
            ? { contentlet, individualPermissions }
            : { contentlet };

        return this.coreWebService
            .requestView({
                method: 'PUT',
                url: `v1/workflow/actions/default/fire/${action}${
                    data.inode ? `?inode=${data.inode}` : ''
                }`,
                body: bodyRequest
            })
            .pipe(take(1), pluck('entity'));
    }
}
