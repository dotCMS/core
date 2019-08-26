import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { RequestMethod } from '@angular/http';
import { pluck, take } from 'rxjs/operators';
import { CoreWebService } from 'dotcms-js';
import { DotCMSContentlet } from 'dotcms-models';

@Injectable()
export class DotWorkflowActionsFireService {
    constructor(private coreWebService: CoreWebService) {}

    /**
     * Fire a workflow action over a contentlet
     *
     * @param {string} inode
     * @param {string} actionId
     * @returns Observable<any> // contentlet
     * @memberof DotWorkflowActionsFireService
     */
    fireTo(inode: string, actionId: string): Observable<DotCMSContentlet> {
        return this.coreWebService
            .requestView({
                method: RequestMethod.Put,
                url: `v1/workflow/actions/${actionId}/fire?inode=${inode}`
            })
            .pipe(pluck('entity'));
    }

    /**
     * Fire a "NEW" action over the content type received with the specified data
     *
     * @param {contentType} string
     * @param {[key: string]: any} data
     * @returns Observable<T>
     *
     * @memberof DotWorkflowActionsFireService
     */
    newContentlet<T>(contentType: string, data: { [key: string]: any }): Observable<T> {
        return this.coreWebService
            .requestView({
                method: RequestMethod.Put,
                url: `v1/workflow/actions/default/fire/NEW`,
                body: { contentlet: { contentType: contentType, ...data } }
            })
            .pipe(take(1), pluck('entity'));
    }
}
