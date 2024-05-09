import { inject } from "@angular/core";
import { ActivatedRouteSnapshot, ResolveFn, RouterStateSnapshot } from "@angular/router";
import { from, map } from "rxjs";
import { DOTCMS_CLIENT_TOKEN } from "../dotcms-client-token";

export const DotCMSPageResolver: ResolveFn<any> = (
    route: ActivatedRouteSnapshot,
    _state: RouterStateSnapshot,
  ) => {
    const client = inject(DOTCMS_CLIENT_TOKEN);

    const url = route.url.map(segment => segment.path).join('/');
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
      languageId: queryParams['language_id'],
    }

    const pageRequest = client.page.get(pageProps).then((resp: any) => resp.entity);
    const naRequest = client.nav.get(navProps).then((resp: any) => resp.entity);
    
    return from(Promise.all([ pageRequest, naRequest])).pipe(map(([page, nav]) => ({ page, nav }) ))
  };
  
