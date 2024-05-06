import { of, Subject } from 'rxjs';

export class MockDotRouterJestService {
    jest: {
        fn: () => () => unknown;
    };

    replaceQueryParams: () => unknown;
    goToEditContentType: () => unknown;
    goToEditContentlet: () => unknown;
    goToEditPage: () => unknown;
    goToEditTask: () => unknown;
    goToForgotPassword: () => unknown;
    goToLogin: () => unknown;
    goToContent: () => unknown;
    goToCreateContent: () => unknown;
    goToPreviousUrl: () => unknown;
    goToStarter: () => unknown;
    doLogOut: () => unknown;
    goToMain: () => unknown;
    goToURL: () => unknown;
    gotoPortlet: () => unknown;
    goToAppsConfiguration: () => unknown;
    goToUpdateAppsConfiguration: () => unknown;
    goToSiteBrowser: () => unknown;
    isCurrentPortletCustom: () => unknown;
    isJSPPortlet: () => unknown;
    reloadCurrentPortlet: () => unknown;
    goToEditTemplate: () => unknown;
    allowRouteDeactivation: () => unknown;
    forbidRouteDeactivation: () => unknown;
    isEditPage: () => unknown;

    constructor(jest: { fn: () => () => unknown }) {
        this.jest = jest;

        this.replaceQueryParams = this.jest?.fn();
        this.goToEditContentType = this.jest?.fn();
        this.goToEditContentlet = this.jest?.fn();
        this.goToEditPage = this.jest?.fn();
        this.goToEditTask = this.jest?.fn();
        this.goToForgotPassword = this.jest?.fn();
        this.goToLogin = this.jest?.fn();
        this.goToContent = this.jest?.fn();
        this.goToCreateContent = this.jest?.fn();
        this.goToPreviousUrl = this.jest?.fn();
        this.goToStarter = this.jest?.fn();
        this.doLogOut = this.jest?.fn();
        this.goToMain = this.jest?.fn();
        this.goToURL = this.jest?.fn();
        this.gotoPortlet = this.jest?.fn();
        this.goToAppsConfiguration = this.jest?.fn();
        this.goToUpdateAppsConfiguration = this.jest?.fn();
        this.goToSiteBrowser = this.jest?.fn();
        this.isCurrentPortletCustom = this.jest?.fn();
        this.isJSPPortlet = this.jest?.fn();
        this.reloadCurrentPortlet = this.jest?.fn();
        this.goToEditTemplate = this.jest?.fn();
        this.allowRouteDeactivation = this.jest?.fn();
        this.forbidRouteDeactivation = this.jest?.fn();
        this.isEditPage = this.jest?.fn();
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

    requestPageLeave = () => {
        this.pageLeaveRequest$.next(true);
    };
    getPortletId = () => {
        return 'test';
    };
}
