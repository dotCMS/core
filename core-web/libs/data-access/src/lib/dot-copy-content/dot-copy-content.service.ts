import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { pluck, shareReplay } from 'rxjs/operators';

import { DotTreeNode, DotCMSContentlet } from '@dotcms/dotcms-models';

export const DEFAULT_PERSONALIZATION = 'dot:default';

const API_ENDPOINT = `/api/v1/page/copyContent`;

@Injectable({
    providedIn: 'root'
})
export class DotCopyContentService {
    private readonly http = inject(HttpClient);

    /**
     *
     * Create a copy of a content in a page.
     * @param {DotTreeNode} data
     * @return {*}  {Observable<DotCMSContentlet>}
     * @memberof DotCopyContentService
     */
    copyInPage(treeNode: DotTreeNode): Observable<DotCMSContentlet> {
        const body = {
            ...treeNode,
            personalization: treeNode?.personalization || DEFAULT_PERSONALIZATION
        };

        return this.http.put(API_ENDPOINT, body).pipe(shareReplay(), pluck('entity'));
    }
}
