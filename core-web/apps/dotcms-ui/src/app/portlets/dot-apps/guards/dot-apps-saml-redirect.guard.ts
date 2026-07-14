import { inject } from '@angular/core';
import { CanActivateFn, Router, UrlTree } from '@angular/router';

/** AppSecrets key for SAML, mirrors `DotSamlProxyFactory.SAML_APP_CONFIG_KEY`. */
const SAML_APP_KEY = 'dotsaml-config';

/** Shell path of the dotAuth portlet, registered in `app.routes.ts`. */
const DOT_AUTH_PATH = '/dotAuth';

/**
 * Guards the `/apps/:appKey` routes (list, create, edit): if a user lands on
 * a SAML path — typically from a bookmark or an old docs link — route them to
 * the dotAuth portlet, which is the sole editor for SAML config since phase-3.
 *
 * `dotsaml-config.yml` is kept in place so the SAML runtime's Apps descriptor
 * lookup keeps working; the redirect only affects user navigation.
 */
export const dotAppsSamlRedirectGuard: CanActivateFn = (route): boolean | UrlTree => {
    const appKey = route.paramMap.get('appKey');
    if (appKey === SAML_APP_KEY) {
        return inject(Router).parseUrl(DOT_AUTH_PATH);
    }
    return true;
};
