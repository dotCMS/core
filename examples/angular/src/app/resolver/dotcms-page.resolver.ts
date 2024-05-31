import { Inject, Injectable, InjectionToken, inject } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve } from '@angular/router';
import {
  DOTCMS_CLIENT_TOKEN,
  DotCMSPageAsset,
  DotcmsNavigationItem,
  PageContextService,
} from '@dotcms/angular';

interface DotCMSPageResolverResponse {
  pageAsset: DotCMSPageAsset;
  nav: DotcmsNavigationItem;
}

@Injectable({
  providedIn: 'root',
})
export class DotCMSPageResolver implements Resolve<DotCMSPageResolverResponse> {
  // constructor(@Inject(DOTCMS_CLIENT_TOKEN) private client: DotCmsClient) {}
  // some = new InjectionToken('some');
  // someToken = inject(this.some) as any;
  client = inject(DOTCMS_CLIENT_TOKEN as any) as any;
  pageService = inject(PageContextService);

  async resolve(route: ActivatedRouteSnapshot) {
    const url = route.url.map((segment) => segment.path).join('/');
    const queryParams = route.queryParams;

    const pageProps = {
      path: url || 'index',
      language_id: queryParams['language_id'],
      mode: queryParams['mode'],
      variantName: queryParams['variantName'],
      'com.dotmarketing.persona.id':
        queryParams['com.dotmarketing.persona.id'] || '',
    };

    const navProps = {
      path: '/',
      depth: 2,
      languageId: queryParams['language_id'],
    };

    const pageRequest = this.client.page.get(pageProps) as Promise<{
      entity: DotCMSPageAsset;
    }>;
    const navRequest = this.client.nav.get(navProps) as Promise<{
      entity: DotcmsNavigationItem;
    }>;

    const [pageResponse, navResponse] = await Promise.all([
      pageRequest,
      navRequest,
    ]);

    const pageAsset = pageResponse.entity;
    const nav = navResponse.entity;

    return { pageAsset, nav };
  }
}
