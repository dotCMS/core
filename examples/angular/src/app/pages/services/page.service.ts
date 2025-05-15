import { inject, Injectable } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { forkJoin, from, Observable, of, shareReplay } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

import { getUVEState } from '@dotcms/uve';

import {
    DotcmsNavigationItem,
    DotCMSPageAsset,
    DotCMSPageRequestParams,
    DotCMSComposedPageResponse
} from '@dotcms/types';

import { DOTCMS_CLIENT_TOKEN } from '../../app.config';
import { PageError } from '../../shared/models';

export interface PageResponse<TPage extends DotCMSPageAsset, TContent> {
    response?: DotCMSComposedPageResponse<{ pageAsset: TPage; content: TContent }>;
    error?: PageError;
}

export interface PageWithNavigation<TPage extends DotCMSPageAsset, TContent>
    extends PageResponse<TPage, TContent> {
    nav?: DotcmsNavigationItem;
}

@Injectable({
    providedIn: 'root'
})
export class PageService {
    private readonly client = inject(DOTCMS_CLIENT_TOKEN);

    /**
     * Get the page and navigation for the given route and config.
     *
     * @param {ActivatedRoute} route
     * @param {*} config
     * @return {*}  {(Observable<{ page: DotCMSPageAsset | { error: PageError }; nav: DotcmsNavigationItem }>)}
     * @memberof PageService
     */
    getPageAsset<TPage extends DotCMSPageAsset, TContent>(
        route: ActivatedRoute,
        extraQueries?: DotCMSPageRequestParams['graphql']
    ): Observable<PageWithNavigation<TPage, TContent>> {
        return this.fetchPage<TPage, TContent>(route, extraQueries).pipe(
            map((pageAsset) => {
                const { response, error } = pageAsset;

                return { response, error };
            })
        );
    }

    private fetchPage<TPage extends DotCMSPageAsset, TContent>(
        route: ActivatedRoute,
        extraQueries: DotCMSPageRequestParams['graphql'] = {}
    ): Observable<PageResponse<TPage, TContent>> {
        const params = route.snapshot.queryParams;
        const url = route.snapshot.url.map((segment) => segment.path).join('/');
        const path = url || '/';

        return from(
            this.client.page.get<{ pageAsset: TPage; content: TContent }>(path, {
                ...params,
                graphql: {
                    ...extraQueries
                }
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
                    response: response as DotCMSComposedPageResponse<{
                        pageAsset: TPage;
                        content: TContent;
                    }>
                };
            }),
            catchError((error) => {
                // If the page is not found and we are inside the editor, return an empty object
                // The editor will get the working/unpublished page

                // REMIND ME TO REVISIT THIS
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
