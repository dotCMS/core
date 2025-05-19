import { inject, Injectable } from '@angular/core';
import { ActivatedRouteSnapshot } from '@angular/router';
import { from, Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

import { getUVEState } from '@dotcms/uve';

import {
    DotcmsNavigationItem,
    DotCMSPageAsset,
    DotCMSPageRequestParams,
    DotCMSComposedPageResponse
} from '@dotcms/types';

import { DOTCMS_CLIENT_TOKEN } from '../app.config';
import { PageError } from '../shared/models';

export interface PageResponse<TPage extends DotCMSPageAsset, TContent> {
    response?: DotCMSComposedPageResponse<{ pageAsset: TPage; content: TContent }>;
    error?: PageError;
}

export interface DotCMSCustomPageResponse<TPage extends DotCMSPageAsset, TContent>
    extends PageResponse<TPage, TContent> {
    nav?: DotcmsNavigationItem;
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
    getPageAsset<TPage extends DotCMSPageAsset, TContent>(
        url: string,
        params: DotCMSPageRequestParams
    ): Observable<DotCMSCustomPageResponse<TPage, TContent>> {
        return from(
            this.client.page.get<{ pageAsset: TPage; content: TContent }>(url, {
                ...params
            })
        ).pipe(
            map((response: DotCMSComposedPageResponse<{ pageAsset: TPage; content: TContent }>) => {
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
                    return of({ response: {} } as PageResponse<TPage, TContent>);
                }

                return of({
                    response: {} as DotCMSComposedPageResponse<{
                        pageAsset: TPage;
                        content: TContent;
                    }>,
                    error
                });
            })
        );
    }
}
