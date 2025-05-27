import { inject, Injectable } from '@angular/core';
import { from, Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

import { getUVEState } from '@dotcms/uve';

import {
    DotCMSPageRequestParams,
    DotCMSComposedPageResponse,
    DotCMSExtendedPageResponse
} from '@dotcms/types';

import { DOTCMS_CLIENT_TOKEN } from '../app.config';
import { PageError } from '../shared/models';

// We extend the DotCMSComposedPageResponse to add an error property
export interface CustomPageResponse<T extends DotCMSExtendedPageResponse> {
    response?: DotCMSComposedPageResponse<T>;
    error?: PageError;
}

@Injectable({
    providedIn: 'root'
})
export class PageService {
    private readonly client = inject(DOTCMS_CLIENT_TOKEN);

    /**
     * Get the page and navigation for the given url and params.
     *
     * @param {string} url
     * @param {DotCMSPageRequestParams} params
     * @return {*}  {Observable<DotCMSCustomPageResponse<TPage, TContent>>}
     * @memberof PageService
     */
    getPageAsset<T extends DotCMSExtendedPageResponse>(
        url: string,
        params: DotCMSPageRequestParams
    ): Observable<CustomPageResponse<T>> {
        return from(
            this.client.page.get<T>(url, {
                ...params
            })
        ).pipe(
            map((response) => {
                if (!response?.pageAsset?.layout) {
                    return {
                        error: {
                            message:
                                'You might be using an advanced template, or your dotCMS instance might lack an enterprise license.',
                            status: 'Page without layout'
                        }
                    };
                }

                return {
                    response
                };
            }),
            catchError((error) => {
                // If the page is not found and we are inside the editor, return an empty object
                // The editor will get the working/unpublished page
                if (error.status === 404 && getUVEState()) {
                    return of({ response: {} } as CustomPageResponse<T>);
                }

                return of({
                    response: {},
                    error
                } as CustomPageResponse<T>);
            })
        );
    }
}
