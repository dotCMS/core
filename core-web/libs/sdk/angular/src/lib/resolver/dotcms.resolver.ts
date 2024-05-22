/* eslint-disable @typescript-eslint/no-explicit-any */
import { Injectable, inject } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve } from '@angular/router';

import { DOTCMS_CLIENT_TOKEN } from '../dotcms-client-token';
import { DotCMSNavigationItem, DotCMSPageAsset } from '../models';
import { PageContextService } from '../services/dotcms-context/page-context.service';

@Injectable({
    providedIn: 'root'
})
export class DotCMSPageResolverService implements Resolve<any> {
    client = inject(DOTCMS_CLIENT_TOKEN);
    pageContextService = inject(PageContextService);

    async resolve(route: ActivatedRouteSnapshot) {
        const url = route.url.map((segment) => segment.path).join('/');
        const queryParams = route.queryParams;

        const pageProps = {
            path: url || 'index',
            language_id: queryParams['language_id'],
            mode: queryParams['mode'],
            variantName: queryParams['variantName'],
            'com.dotmarketing.persona.id': queryParams['com.dotmarketing.persona.id'] || ''
        };

        const navProps = {
            path: '/',
            depth: 2,
            languageId: queryParams['language_id']
        };

        const pageRequest = this.client.page.get(pageProps) as Promise<{ entity: DotCMSPageAsset }>;
        const navRequest = this.client.nav.get(navProps) as Promise<{
            entity: DotCMSNavigationItem;
        }>;

        const [pageResponse, navResponse] = await Promise.all([pageRequest, navRequest]);

        const pageAsset = pageResponse.entity;
        const nav = navResponse.entity;

        this.pageContextService.setContext(pageAsset);

        return { pageAsset, nav };
    }
}
