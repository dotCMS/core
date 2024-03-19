import { jest } from '@jest/globals';
import { of, Subject } from 'rxjs';

import { Injectable } from '@angular/core';

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
    replaceQueryParams = jest.fn();
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
    gotoPortlet = jest.fn();
    goToAppsConfiguration = jest.fn();
    goToUpdateAppsConfiguration = jest.fn();
    goToSiteBrowser = jest.fn();
    isCurrentPortletCustom = jest.fn();
    isJSPPortlet = jest.fn();
    reloadCurrentPortlet = jest.fn();
    goToEditTemplate = jest.fn();
    allowRouteDeactivation = jest.fn();
    forbidRouteDeactivation = jest.fn();
    isEditPage = jest.fn();

    requestPageLeave = () => {
        this.pageLeaveRequest$.next(true);
    };
    getPortletId = () => {
        return 'test';
    };
}
