import { of, Subject } from 'rxjs';

import { Injectable } from '@angular/core';
@Injectable()
export class MockDotRouterJestService {
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
    pageLeaveRequest$ = new Subject<boolean>();
    canDeactivateRoute$ = of(true);

    requestPageLeave = () => {
        this.pageLeaveRequest$.next(true);
    };
    replaceQueryParams = () => {
        /* noop */
    };
    getPortletId = () => {
        return 'test';
    };
    goToEditContentType = () => {
        /* noop */
    };
    goToEditContentlet = () => {
        /* noop */
    };
    goToEditPage = () => {
        /* noop */
    };
    goToEditTask = () => {
        /* noop */
    };
    goToForgotPassword = () => {
        /* noop */
    };
    goToLogin = () => {
        /* noop */
    };
    goToContent = () => {
        /* noop */
    };
    goToCreateContent = () => {
        /* noop */
    };
    goToPreviousUrl = () => {
        /* noop */
    };
    goToStarter = () => {
        /* noop */
    };
    doLogOut = () => {
        /* noop */
    };
    goToMain = () => {
        /* noop */
    };
    goToURL = () => {
        /* noop */
    };
    gotoPortlet = () => {
        /* noop */
    };
    goToAppsConfiguration = () => {
        /* noop */
    };
    goToUpdateAppsConfiguration = () => {
        /* noop */
    };
    goToSiteBrowser = () => {
        /* noop */
    };
    isCurrentPortletCustom = () => {
        /* noop */
    };
    isJSPPortlet = () => {
        /* noop */
    };
    reloadCurrentPortlet = () => {
        /* noop */
    };
    goToEditTemplate = () => {
        /* noop */
    };
    allowRouteDeactivation = () => {
        /* noop */
    };
    forbidRouteDeactivation = () => {
        /* noop */
    };
    isEditPage() {
        /* */
    }
}
