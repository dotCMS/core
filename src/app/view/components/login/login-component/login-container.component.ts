import { Component, ViewEncapsulation } from '@angular/core';
import { HttpRequestUtils, LoginService, LoggerService, HttpCode, ResponseView } from 'dotcms-js/dotcms-js';
import { DotRouterService } from '../../../../api/services/dot-router-service';

@Component({
    encapsulation: ViewEncapsulation.Emulated,
    providers: [HttpRequestUtils],
    selector: 'dot-login-container',
    template: `
        <dot-login-component
            [message]="message"
            [isLoginInProgress] = "isLoginInProgress"
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
        private loginService: LoginService,
        private router: DotRouterService,
        private httprequestUtils: HttpRequestUtils,
        private loggerService: LoggerService
    ) {
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
        this.message = '';

        this.loginService
            .loginUser(
                loginData.login,
                loginData.password,
                loginData.rememberMe,
                loginData.language
            )
            .subscribe(
                (result: any) => {
                    this.message = '';
                    this.router.goToMain();
                },
                (error: ResponseView) => {
                    if (
                        error.status === HttpCode.BAD_REQUEST ||
                        error.status === HttpCode.UNAUTHORIZED
                    ) {
                        this.message = error.errorsMessages || this.getErrorMessage(error.response.json().error);
                    } else {
                        this.loggerService.debug(error);
                    }
                    this.isLoginInProgress = false;
                }
            );
    }

    /**
     * Display the forgot password card
     */
    showForgotPassword(): void {
        this.router.goToForgotPassword();
    }

    private getErrorMessage(origMessage: string): string {
        const split = origMessage.split(':');
        return split[2];
    }
}

export interface LoginData {
    login: string;
    password: string;
    rememberMe: boolean;
    language: string;
}
