import { NEVER, of } from 'rxjs';
import { vi } from 'vitest';

import { Injectable } from '@angular/core';

@Injectable()
export class MockDotRouterService {
    get currentPortlet() {
        return {
            url: 'this/is/an/url',
            id: '123-567'
        };
    }

    get portletReload$() {
        return of('some-id');
    }

    get queryParams() {
        return {};
    }

    get currentSavedURL(): string {
        return this._currentSavedURL;
    }

    set currentSavedURL(url: string) {
        this._currentSavedURL = url;
    }

    get storedRedirectUrl(): string {
        return this._storedRedirectUrl;
    }

    set storedRedirectUrl(url: string) {
        this._storedRedirectUrl = url;
    }

    _storedRedirectUrl = '';
    _currentSavedURL = '';
    pageLeaveRequest$ = NEVER;
    canDeactivateRoute$ = of(true);
    replaceQueryParams = vi.fn();
    getPortletId = vi.fn().mockReturnValue('test');
    goToEditContentType = vi.fn();
    goToEditContentlet = vi.fn();
    goToEditPage = vi.fn();
    goToEditTask = vi.fn();
    goToForgotPassword = vi.fn();
    goToLogin = vi.fn();
    goToContent = vi.fn();
    goToCreateContent = vi.fn();
    goToPreviousUrl = vi.fn();
    goToStarter = vi.fn();
    doLogOut = vi.fn();
    goToMain = vi.fn();
    goToURL = vi.fn();
    gotoPortlet = vi.fn().mockImplementation(() => new Promise((resolve) => resolve(true)));
    goToAppsConfiguration = vi.fn();
    goToUpdateAppsConfiguration = vi.fn();
    goToSiteBrowser = vi.fn();
    isCurrentPortletCustom = vi.fn();
    isCustomPortlet = vi.fn().mockReturnValue(false);
    isJSPPortlet = vi.fn();
    reloadCurrentPortlet = vi.fn();
    goToEditTemplate = vi.fn();
    allowRouteDeactivation = vi.fn();
    forbidRouteDeactivation = vi.fn();
    goToEditContainer = vi.fn();
    isEditPage() {
        /* */
    }
}
