import { pluck } from 'rxjs/operators';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { RequestMethod } from '@angular/http';
import { DotPageContainer } from '../../shared/models/dot-page-container.model';
import { DotPageContent } from '../../shared/models/dot-page-content.model';
import { ContentType } from '../../../content-types/shared/content-type.model';
import { CoreWebService } from 'dotcms-js';

@Injectable()
export class DotContainerContentletService {
    constructor(private coreWebService: CoreWebService) {}

    /**
     * Get the HTML of a contentlet inside a container
     *
     * @param DotPageContainer container
     * @param DotPageContent content
     * @returns Observable<string>
     * @memberof DotContainerContentletService
     */
    getContentletToContainer(
        container: DotPageContainer,
        content: DotPageContent
    ): Observable<string> {
        return this.coreWebService
            .requestView({
                method: RequestMethod.Get,
                url: `v1/containers/${container.identifier}/content/${content.identifier}`
            })
            .pipe(pluck('entity', 'render'));
    }

    /**
     * Get the HTML of a form inside a container
     *
     * @param DotPageContainer container
     * @param ContentType form
     * @returns Observable<string>
     * @memberof DotContainerContentletService
     */
    getFormToContainer(
        container: DotPageContainer,
        form: ContentType
    ): Observable<{ render: string; content: any }> {
        return this.coreWebService
            .requestView({
                method: RequestMethod.Get,
                url: `v1/containers/${container.identifier}/form/${form.id}`
            })
            .pipe(pluck('entity'));
    }
}
