import { Injectable } from '@angular/core';
import { CoreWebService } from 'dotcms-js/dotcms-js';
import { RequestMethod } from '@angular/http';
import { DotPageContainer } from '../../../portlets/dot-edit-page/shared/models/dot-page-container.model';
import { Observable } from 'rxjs/Observable';

@Injectable()
export class DotEditPageService {

    constructor(private coreWebService: CoreWebService) {}

    /**
     * Save a page's content
     *
     * @param {string} pageId
     * @param {DotPageContainer[]} content
     * @returns {Observable<string>}
     * @memberof DotContainerContentletService
     */
    save(pageId: string, content: DotPageContainer[]): Observable<string> {
        return this.coreWebService
            .requestView({
                method: RequestMethod.Post,
                body: content,
                url: `v1/page/${pageId}/content`
            })
            .pluck('entity');
    }
}
