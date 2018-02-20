import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Observable';
import { CoreWebService } from 'dotcms-js/core/core-web.service';
import { RequestMethod } from '@angular/http';
import { DotPageContainer } from '../../dot-edit-page/shared/models/dot-page-container.model';
import { DotPageContent } from '../../dot-edit-page/shared/models/dot-page-content.model';

@Injectable()
export class DotContainerContentletService {
    constructor(private coreWebService: CoreWebService) {}

    getContentletToContainer(container: DotPageContainer, content: DotPageContent): Observable<string> {
        return this.coreWebService
            .requestView({
                method: RequestMethod.Get,
                url: `v1/containers/${container.identifier}/uuid/${container.uuid}/content/${content.identifier}`
            })
            .pluck('bodyJsonObject', 'render');
    }

    /**
     * Save a page's content
     * @param pageId Page's ID
     * @param model content model
     */
    saveContentlet(pageId: string, content: DotPageContainer[]): Observable<string> {
        return this.coreWebService
            .requestView({
                method: RequestMethod.Post,
                body: content,
                url: `v1/page/${pageId}/content`
            })
            .pluck('entity');
    }
}
