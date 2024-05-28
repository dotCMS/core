import { inject } from '@angular/core';
import { ActivatedRouteSnapshot } from '@angular/router';

import { DOTCMS_CLIENT_TOKEN } from '../dotcms-client-token';
import { DotcmsNavigationItem, DotCMSPageAsset } from '../models';
import { PageContextService } from '../services/dotcms-context/page-context.service';

/**
 * This resolver is used to fetch the page and navigation data from dotCMS.
 *
 * @param {ActivatedRouteSnapshot} route
 * @param {RouterStateSnapshot} _state
 * @return {*}
 */
export const DotCMSPageResolver = async (
    route: ActivatedRouteSnapshot
): Promise<{
    pageAsset: DotCMSPageAsset;
    nav: DotcmsNavigationItem;
}> => {
    const client = inject(DOTCMS_CLIENT_TOKEN);
    const pageContextService = inject(PageContextService);

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

    const pageRequest = client.page.get(pageProps) as Promise<{ entity: DotCMSPageAsset }>;
    const navRequest = client.nav.get(navProps) as Promise<{ entity: DotcmsNavigationItem }>;

    const [pageResponse, navResponse] = await Promise.all([pageRequest, navRequest]);

    const pageAsset = pageResponse.entity;
    const nav = navResponse.entity;

    pageContextService.setContext(pageAsset);

    return { pageAsset, nav };
};
