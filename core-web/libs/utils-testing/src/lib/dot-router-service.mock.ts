import { NEVER, of } from 'rxjs';

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
    replaceQueryParams = jest.fn();
    getPortletId = jest.fn().mockReturnValue('test');
    goToEditContentType = jest.fn();
    goToEditContentlet = jest.fn();
    goToEditPage = jest.fn();
    goToEditTask = jest.fn();
    goToForgotPassword = jest.fn();
    goToLogin = jest.fn();
    goToContent = jest.fn();
    goToCreateContent = jest.fn();
    goToPreviousUrl = jest.fn();
    goToStarter = jest.fn();
    doLogOut = jest.fn();
    goToMain = jest.fn();
    goToURL = jest.fn();
    gotoPortlet = jest.fn().mockImplementation(() => new Promise((resolve) => resolve(true)));
    goToAppsConfiguration = jest.fn();
    goToUpdateAppsConfiguration = jest.fn();
    goToSiteBrowser = jest.fn();
    isCurrentPortletCustom = jest.fn();
    isCustomPortlet = jest.fn().mockReturnValue(false);
    isJSPPortlet = jest.fn();
    reloadCurrentPortlet = jest.fn();
    goToEditTemplate = jest.fn();
    allowRouteDeactivation = jest.fn();
    forbidRouteDeactivation = jest.fn();
    goToEditContainer = jest.fn();
    isEditPage() {
        /* */
    }
}
