import { Observable } from 'rxjs';

import { Injectable } from '@angular/core';

import { pluck, shareReplay } from 'rxjs/operators';

import { CoreWebService, DotRequestOptionsArgs } from '@dotcms/dotcms-js';
import { DotCopyContent, DotCMSContentlet } from '@dotcms/dotcms-models';

export const DEFAULT_PERSONALIZATION = 'dot:default';

@Injectable({
    providedIn: 'root'
})
export class DotCopyContentService {
    constructor(private readonly coreWebService: CoreWebService) {}

    /**
     *
     *
     * @param string pageId
     * @return {*}
     * @memberof DotEditPageService
     */
    copyContentInPage(body: DotCopyContent): Observable<DotCMSContentlet> {
        const requestOptions: DotRequestOptionsArgs = {
            method: 'PUT',
            body: {
                ...body,
                personalization: body.personalization || DEFAULT_PERSONALIZATION
            },
            url: `api/v1/page/copyContent`
        };

        return this.coreWebService.requestView(requestOptions).pipe(pluck('entity'), shareReplay());
    }
}
