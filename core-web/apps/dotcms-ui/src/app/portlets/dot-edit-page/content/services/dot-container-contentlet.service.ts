import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

import { DotSessionStorageService } from '@dotcms/data-access';
import { DotCMSResponse, DotPage, DotPageContainer, DotPageContent } from '@dotcms/dotcms-models';

@Injectable()
export class DotContainerContentletService {
    private http = inject(HttpClient);
    private dotSessionStorageService = inject(DotSessionStorageService);

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
        const defaultUrl = `/api/v1/containers/content/${content.identifier}?containerId=${container.identifier}&pageInode=${page.inode}`;

        const url = !currentVariantName
            ? defaultUrl
            : `${defaultUrl}&variantName=${currentVariantName}`;

        return this.http
            .get<DotCMSResponse<{ render: string }>>(url)
            .pipe(map((response) => response.entity.render));
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
        return this.http
            .get<
                DotCMSResponse<{ render: string; content: { [key: string]: string } }>
            >(`/api/v1/containers/form/${formId}?containerId=${container.identifier}`)
            .pipe(map((response) => response.entity));
    }
}
