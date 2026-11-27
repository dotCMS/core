import { InjectionToken } from '@angular/core';

import { DotMenuItem } from '@dotcms/dotcms-models';

/**
 * Registers the runtime Angular routes that menu items declare through the init param
 * `angular-module`. The menu feature calls it with every item after each successful load, before
 * the load is reported as ready, so a route guard that waits for the menu never decides before the
 * routes exist. The application provides it; it is optional so the feature also works without it.
 */
export const DOT_MENU_ROUTES_REGISTRAR = new InjectionToken<(items: DotMenuItem[]) => void>(
    'DOT_MENU_ROUTES_REGISTRAR'
);
