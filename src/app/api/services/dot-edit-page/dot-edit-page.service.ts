import { pluck } from 'rxjs/operators';
import { Injectable } from '@angular/core';
import { CoreWebService } from 'dotcms-js';
import { DotPageContainer } from '@models/dot-page-container/dot-page-container.model';
import { Observable } from 'rxjs';

@Injectable()
export class DotEditPageService {
    constructor(private coreWebService: CoreWebService) {}

    /**
     * Save a page's content
     *
     * @param string pageId
     * @param DotPageContainer[] content
     * @returns Observable<string>
     * @memberof DotContainerContentletService
     */
    save(pageId: string, content: DotPageContainer[]): Observable<string> {
        return this.coreWebService
            .requestView({
                method: 'POST',
                body: content,
                url: `v1/page/${pageId}/content`
            })
            .pipe(pluck('entity'));
    }
}
