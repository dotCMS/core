import { pluck } from 'rxjs/operators';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { DotPageContent } from '../../shared/models/dot-page-content.model';
import { CoreWebService } from '@dotcms/dotcms-js';
import { DotPageContainer, DotPage } from '@dotcms/dotcms-models';

@Injectable()
export class DotContainerContentletService {
    constructor(private coreWebService: CoreWebService) {}

    /**
     * Get the HTML of a contentlet inside a container
     *
     * @param DotPageContainer container
     * @param DotPageContent content
     * @param DotPage page
     * @returns Observable<string>
     * @memberof DotContainerContentletService
     */
    getContentletToContainer(
        container: DotPageContainer,
        content: DotPageContent,
        page: DotPage
    ): Observable<string> {
        return this.coreWebService
            .requestView({
                url: `v1/containers/content/${content.identifier}?containerId=${container.identifier}&pageInode=${page.inode}`
            })
            .pipe(pluck('entity', 'render'));
    }

    /**
     * Get the HTML of a form inside a container
     *
     * @param DotPageContainer container
     * @param string formId
     * @returns Observable<string>
     * @memberof DotContainerContentletService
     */
    getFormToContainer(
        container: DotPageContainer,
        formId: string
    ): Observable<{ render: string; content: { [key: string]: string } }> {
        return this.coreWebService
            .requestView({
                url: `v1/containers/form/${formId}?containerId=${container.identifier}`
            })
            .pipe(pluck('entity'));
    }
}
