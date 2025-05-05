import { inject, Injectable } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { forkJoin, from, Observable, of, shareReplay } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

import { getPageRequestParams } from '@dotcms/client';
import { getUVEState } from '@dotcms/uve';

import { DotcmsNavigationItem } from '@dotcms/angular';

import { PageError } from '../pages.component';
import { DOTCMS_CLIENT_TOKEN } from '../../app.config';

import { BlockEditorContent, Contentlet, DotCMSPageAsset, DotCMSURLContentMap } from '@dotcms/types';

export interface BlogContentlet extends DotCMSURLContentMap {
    blogContent: BlockEditorContent;
}

export interface BlogPageAsset extends DotCMSPageAsset {
    urlContentMap: BlogContentlet;
}

export interface PageResponse {
    page: BlogPageAsset | null;
    error?: PageError;
}

export interface PageAndNavResponse extends PageResponse {
    nav: DotcmsNavigationItem | null;
}

@Injectable({
    providedIn: 'root'
})
export class PageService {
    private readonly client = inject(DOTCMS_CLIENT_TOKEN);
    private navObservable!: Observable<DotcmsNavigationItem | null>;

    /**
     * Get the page and navigation for the given route and config.
     *
     * @param {ActivatedRoute} route
     * @param {*} config
     * @return {*}  {(Observable<{ page: DotCMSPageAsset | { error: PageError }; nav: DotcmsNavigationItem }>)}
     * @memberof PageService
     */
    getPageAndNavigation(route: ActivatedRoute, config: any): Observable<PageAndNavResponse> {
        if (!this.navObservable) {
            this.navObservable = this.fetchNavigation(route);
        }

        return forkJoin({
            nav: this.navObservable,
            pageAsset: this.fetchPage(route, config)
        }).pipe(
            map(({ nav, pageAsset }) => {
                const { page, error } = pageAsset;

                return { nav, page, error };
            })
        );
    }

    private fetchNavigation(route: ActivatedRoute): Observable<DotcmsNavigationItem | null> {
        return from(
            this.client.nav
                .get({
                    path: '/',
                    depth: 2,
                    languageId: route.snapshot.params['languageId'] || 1
                })
                .then((response) => (response as any).entity)
                .catch((e) => {
                    console.error(`Error fetching navigation: ${e.message}`);
                    return null;
                })
        ).pipe(shareReplay(1));
    }

    private fetchPage(route: ActivatedRoute, config: any): Observable<PageAndNavResponse> {
        const params = route.snapshot.queryParams;
        const url = route.snapshot.url.map((segment) => segment.path).join('/');
        const path = url || '/';

        const pageParams = getPageRequestParams({
            path,
            params
        });

        return from(this.client.page.get({ ...pageParams, ...config.params })).pipe(
            map((page: any) => {
                if (!page?.layout) {
                    return {
                        page: null,
                        error: {
                            message:
                                'You might be using an advanced template, or your dotCMS instance might lack an enterprise license.',
                            status: 'Page without layout'
                        }
                    };
                }

                return { page, error: null };
            }),
            catchError((error) => {
                // If the page is not found and we are inside the editor, return an empty object
                // The editor will get the working/unpublished page
                if (error.status === 404 && getUVEState()) {
                    return of({ page: {}, error: null } as any);
                }

                return of({ page: null, error });
            })
        );
    }
}
