import { of, NEVER } from 'rxjs';

import { Injectable } from '@angular/core';
import {} from 'jasmine';

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
    replaceQueryParams = jasmine.createSpy('replaceQueryParams');
    getPortletId = jasmine.createSpy('getPortletId').and.returnValue('test');
    goToEditContentType = jasmine.createSpy('goToEditContentType');
    goToEditContentlet = jasmine.createSpy('goToEditContentlet');
    goToEditPage = jasmine.createSpy('goToEditPage');
    goToEditTask = jasmine.createSpy('goToEditTask');
    goToForgotPassword = jasmine.createSpy('goToForgotPassword');
    goToLogin = jasmine.createSpy('goToLogin');
    goToContent = jasmine.createSpy('goToContent');
    goToCreateContent = jasmine.createSpy('goToCreateContent');
    goToPreviousUrl = jasmine.createSpy('goToPreviousUrl');
    goToStarter = jasmine.createSpy('goToStarter');
    doLogOut = jasmine.createSpy('doLogOut');
    goToMain = jasmine.createSpy('goToMain');
    goToURL = jasmine.createSpy('goToURL');
    gotoPortlet = jasmine
        .createSpy('gotoPortlet')
        .and.callFake(() => new Promise((resolve) => resolve(true)));
    goToAppsConfiguration = jasmine.createSpy('goToAppsConfiguration');
    goToUpdateAppsConfiguration = jasmine.createSpy('goToUpdateAppsConfiguration');
    goToSiteBrowser = jasmine.createSpy('goToSiteBrowser');
    isCurrentPortletCustom = jasmine.createSpy('isCurrentPortletCustom');
    isJSPPortlet = jasmine.createSpy('isJSPPortlet');
    reloadCurrentPortlet = jasmine.createSpy('reloadCurrentPortlet');
    goToEditTemplate = jasmine.createSpy('goToEditTemplate');
    allowRouteDeactivation = jasmine.createSpy('allowRouteDeactivation');
    forbidRouteDeactivation = jasmine.createSpy('forbidRouteDeactivation');
    goToEditContainer = jasmine.createSpy('goToEditContainer');
    isEditPage() {
        /* */
    }
}
