import { Observable } from 'rxjs';

import { Injectable } from '@angular/core';

import { pluck } from 'rxjs/operators';

import { DotSessionStorageService } from '@dotcms/data-access';
import { CoreWebService } from '@dotcms/dotcms-js';
import { DotPage, DotPageContainer } from '@dotcms/dotcms-models';

import { DotPageContent } from '../../shared/models/dot-page-content.model';

@Injectable()
export class DotContainerContentletService {
    constructor(
        private coreWebService: CoreWebService,
        private dotSessionStorageService: DotSessionStorageService
    ) {}

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
        const currentVariantName = this.dotSessionStorageService.getVariationId();
        const defaultUrl = `v1/containers/content/${content.identifier}?containerId=${container.identifier}&pageInode=${page.inode}`;

        const url = !currentVariantName
            ? defaultUrl
            : `${defaultUrl}&variantName=${currentVariantName}`;

        return this.coreWebService
            .requestView({
                url: url
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
