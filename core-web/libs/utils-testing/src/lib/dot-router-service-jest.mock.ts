import { of, Subject } from 'rxjs';

import { Injectable } from '@angular/core';

const noop = () => {
    /* */
};

@Injectable()
export class MockDotRouterJestService {
    get currentPortlet() {
        return {
            url: 'this/is/an/url/jest',
            id: '123-567-890'
        };
    }

    get portletReload$() {
        return of('some-id-jest');
    }

    get queryParams() {
        return {
            /* Empty */
        };
    }

    get currentSavedURL(): string {
        return this._currentSavedURL;
    }

    set currentSavedURL(jestURL: string) {
        this._currentSavedURL = jestURL;
    }

    get storedRedirectUrl(): string {
        return this._storedRedirectUrl;
    }

    set storedRedirectUrl(jestURL: string) {
        this._storedRedirectUrl = jestURL;
    }

    _storedRedirectUrl = '';
    _currentSavedURL = '';
    pageLeaveRequest$ = new Subject<boolean>();
    canDeactivateRoute$ = of(true);
    replaceQueryParams = noop;
    goToEditContentType = noop;
    goToEditContentlet = noop;
    goToEditPage = noop;
    goToEditTask = noop;
    goToForgotPassword = noop;
    goToLogin = noop;
    goToContent = noop;
    goToCreateContent = noop;
    goToPreviousUrl = noop;
    goToStarter = noop;
    doLogOut = noop;
    goToMain = noop;
    goToURL = noop;
    gotoPortlet = noop;
    goToAppsConfiguration = noop;
    goToUpdateAppsConfiguration = noop;
    goToSiteBrowser = noop;
    isCurrentPortletCustom = noop;
    isJSPPortlet = noop;
    reloadCurrentPortlet = noop;
    goToEditTemplate = noop;
    allowRouteDeactivation = noop;
    forbidRouteDeactivation = noop;
    isEditPage = noop;

    requestPageLeave = () => {
        this.pageLeaveRequest$.next(true);
    };
    getPortletId = () => {
        return 'test';
    };
}
