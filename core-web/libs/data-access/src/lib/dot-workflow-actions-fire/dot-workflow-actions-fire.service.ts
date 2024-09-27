import { Observable } from 'rxjs';

import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { pluck, take } from 'rxjs/operators';

import {
    DotActionBulkRequestOptions,
    DotCMSContentlet,
    DotActionBulkResult
} from '@dotcms/dotcms-models';

interface DotActionRequestOptions {
    contentType?: string;
    data: { [key: string]: string };
    action: ActionToFire;
    individualPermissions?: { [key: string]: string[] };
    formData?: FormData;
}

export interface DotFireActionOptions<T> {
    actionId: string;
    inode?: string;
    data?: T;
}

enum ActionToFire {
    NEW = 'NEW',
    DESTROY = 'DESTROY',
    PUBLISH = 'PUBLISH',
    EDIT = 'EDIT'
}

@Injectable()
export class DotWorkflowActionsFireService {
    private readonly BASE_URL = '/api/v1/workflow';
    private readonly httpClient = inject(HttpClient);
    private readonly defaultHeaders = new HttpHeaders()
        .set('Accept', '*/*')
        .set('Content-Type', 'application/json');

    /**
     *  Fire a workflow action over a contentlet
     *
     * @template T
     * @param {DotFireActionOptions<T>} options
     * @return {*}  {Observable<DotCMSContentlet>}
     * @memberof DotWorkflowActionsFireService
     */
    fireTo<T = Record<string, string>>(
        options: DotFireActionOptions<T>
    ): Observable<DotCMSContentlet> {
        const { actionId, inode, data } = options;
        const queryInode = inode ? `inode=${inode}&` : '';

        return this.httpClient
            .put(
                `${this.BASE_URL}/actions/${actionId}/fire?${queryInode}indexPolicy=WAIT_FOR`,
                data,
                { headers: this.defaultHeaders }
            )
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
        return this.httpClient
            .put(`${this.BASE_URL}/contentlet/actions/bulk/fire`, data, {
                headers: this.defaultHeaders
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
    newContentlet<T>(
        contentType: string,
        data: { [key: string]: string },
        formData?: FormData
    ): Observable<T> {
        return this.request<T>({ contentType, data, action: ActionToFire.NEW, formData });
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
     * Fire an "DELETE" action over the content type received with the specified data
     *
     * @template T
     * @param {{ [key: string]: unknown}} data
     * @return {*}  {Observable<T>}
     * @memberof DotWorkflowActionsFireService
     */
    deleteContentlet<T>(data: { [key: string]: string }): Observable<T> {
        return this.request<T>({
            data,
            action: ActionToFire.DESTROY
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
        individualPermissions,
        formData
    }: DotActionRequestOptions): Observable<T> {
        let url = `${this.BASE_URL}/actions/default/fire/${action}`;

        const contentlet = contentType ? { contentType: contentType, ...data } : data;
        const bodyRequest = individualPermissions
            ? { contentlet, individualPermissions }
            : { contentlet };

        if (data['inode']) {
            url += `?inode=${data['inode']}`;
        }

        if (formData) {
            formData.append('json', JSON.stringify(bodyRequest));
        }

        return this.httpClient
            .put(url, formData ? formData : bodyRequest, {
                headers: formData ? new HttpHeaders() : this.defaultHeaders
            })
            .pipe(take(1), pluck('entity'));
    }
}
