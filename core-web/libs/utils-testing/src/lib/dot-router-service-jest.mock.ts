import { of, Subject } from 'rxjs';

export class MockDotRouterJestService {
    jest = undefined;

    constructor(jest: any) {
        this.jest = jest;
    }

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
    replaceQueryParams = this.jest.fn();
    goToEditContentType = this.jest.fn();
    goToEditContentlet = this.jest.fn();
    goToEditPage = this.jest.fn();
    goToEditTask = this.jest.fn();
    goToForgotPassword = this.jest.fn();
    goToLogin = this.jest.fn();
    goToContent = this.jest.fn();
    goToCreateContent = this.jest.fn();
    goToPreviousUrl = this.jest.fn();
    goToStarter = this.jest.fn();
    doLogOut = this.jest.fn();
    goToMain = this.jest.fn();
    goToURL = this.jest.fn();
    gotoPortlet = this.jest.fn();
    goToAppsConfiguration = this.jest.fn();
    goToUpdateAppsConfiguration = this.jest.fn();
    goToSiteBrowser = this.jest.fn();
    isCurrentPortletCustom = this.jest.fn();
    isJSPPortlet = this.jest.fn();
    reloadCurrentPortlet = this.jest.fn();
    goToEditTemplate = this.jest.fn();
    allowRouteDeactivation = this.jest.fn();
    forbidRouteDeactivation = this.jest.fn();
    isEditPage = this.jest.fn();

    requestPageLeave = () => {
        this.pageLeaveRequest$.next(true);
    };
    getPortletId = () => {
        return 'test';
    };
}
