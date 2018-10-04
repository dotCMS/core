import { Component, ViewEncapsulation } from '@angular/core';
import {
    HttpRequestUtils,
    LoginService,
    LoggerService,
    HttpCode,
    User
} from 'dotcms-js/dotcms-js';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { DotLoadingIndicatorService } from '../../_common/iframe/dot-loading-indicator/dot-loading-indicator.service';

@Component({
    encapsulation: ViewEncapsulation.Emulated,
    providers: [HttpRequestUtils],
    selector: 'dot-login-container',
    template: `
        <dot-login-component
            [message]="message"
            [isLoginInProgress]="isLoginInProgress"
            (login)="logInUser($event)"
            (recoverPassword)="showForgotPassword()"
            [passwordChanged]="passwordChanged"
            [resetEmailSent]="resetEmailSent"
            [resetEmail]="resetEmail"
        >
        </dot-login-component>
    `
})
export class LoginContainerComponent {
    public isLoginInProgress = false;
    public message: string;
    public passwordChanged = false;
    public resetEmail = '';
    public resetEmailSent = false;

    constructor(
        private dotRouterService: DotRouterService,
        private httprequestUtils: HttpRequestUtils,
        private loginService: LoginService,
        private dotLoadingIndicatorService: DotLoadingIndicatorService,
        private loggerService: LoggerService
    ) {
        // this.dotLoadingIndicatorService.hide();
        // TODO: change the httpRequestUtils.getQueryParams() with an NG2 method equivalent to QueryParams on NGRX.
        const queryParams: Map<string, any> = this.httprequestUtils.getQueryParams();
        if (<boolean>queryParams.get('changedPassword')) {
            this.passwordChanged = queryParams.get('changedPassword');
        } else if (<boolean>queryParams.get('resetEmailSent')) {
            this.resetEmailSent = queryParams.get('resetEmailSent');
            this.resetEmail = decodeURIComponent(queryParams.get('resetEmail'));
        }
    }

    logInUser(loginData: LoginData): void {
        this.isLoginInProgress = true;
        this.dotLoadingIndicatorService.show();
        this.message = '';

        this.loginService
            .loginUser(
                loginData.login,
                loginData.password,
                loginData.rememberMe,
                loginData.language
            )
            .subscribe(
                (user: User) => {
                    this.message = '';
                    this.dotLoadingIndicatorService.hide();
                    this.dotRouterService.goToMain(user['editModeUrl']);
                },
                (error: any) => {
                    if (this.isBadRequestOrUnathorized(error.status)) {
                        this.message = JSON.parse(error._body).errors[0].message;
                    } else {
                        this.loggerService.debug(error);
                    }
                    this.isLoginInProgress = false;
                    this.dotLoadingIndicatorService.hide();
                }
            );
    }

    /**
     * Display the forgot password card
     */
    showForgotPassword(): void {
        this.dotRouterService.goToForgotPassword();
    }

    private isBadRequestOrUnathorized(status: number) {
        return status === HttpCode.BAD_REQUEST || status === HttpCode.UNAUTHORIZED;
    }
}

export interface LoginData {
    login: string;
    password: string;
    rememberMe: boolean;
    language: string;
}
