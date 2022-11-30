import { pluck } from 'rxjs/operators';
import { Injectable } from '@angular/core';
import { CoreWebService, DotRequestOptionsArgs } from '@dotcms/dotcms-js';
import { DotPageContainer } from '@models/dot-page-container/dot-page-container.model';
import { Observable } from 'rxjs';
import { DotWhatChanged } from '@models/dot-what-changed/dot-what-changed.model';
import { DotSessionStorageService } from '@shared/services/dot-session-storage.service';

@Injectable()
export class DotEditPageService {
    constructor(
        private coreWebService: CoreWebService,
        private readonly dotSessionStorageService: DotSessionStorageService
    ) {}

    /**
     * Save a page's content
     *
     * @param string pageId
     * @param DotPageContainer[] content
     * @returns Observable<string>
     * @memberof DotEditPageService
     */
    save(pageId: string, content: DotPageContainer[]): Observable<string> {
        const requestOptions: DotRequestOptionsArgs = {
            method: 'POST',
            body: content,
            url: `v1/page/${pageId}/content`
        };

        const currentVariantName = this.dotSessionStorageService.getVariationId();

        if (currentVariantName) {
            requestOptions.params = {
                variantName: currentVariantName
            };
        }

        return this.coreWebService.requestView(requestOptions).pipe(pluck('entity'));
    }

    /**
     * Get the live and working version markup of specific page.
     *
     * @param string pageId
     * @param string language
     * @returns Observable<DotWhatChanged>
     * @memberof DotEditPageService
     */
    whatChange(pageId: string, languageId: string): Observable<DotWhatChanged> {
        return this.coreWebService
            .requestView({
                url: `v1/page/${pageId}/render/versions?langId=${languageId}`
            })
            .pipe(pluck('entity'));
    }
}
