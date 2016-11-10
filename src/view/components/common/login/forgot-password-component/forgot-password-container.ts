import {Component, ViewEncapsulation} from '@angular/core';
import {ForgotPasswordComponent} from './forgot-password-component';
import {LoginService} from '../../../../../api/services/login-service';
import {ResponseView} from '../../../../../api/services/response-view';
import {DotRouterService} from '../../../../../api/services/dot-router-service';

@Component({
    directives: [ForgotPasswordComponent],
    encapsulation: ViewEncapsulation.Emulated,
    moduleId: __moduleName, // REQUIRED to use relative path in styleUrls
    selector: 'dot-forgot-password-container',
    template: `
        <dot-forgot-password-component
            [message]="message"
            (cancel)="goToLogin()"
            (recoverPassword)="recoverPassword($event)"
        ></dot-forgot-password-component>
    `,
})
export class ForgotPasswordContainer {

    private message: string = '';
    private email: string = '';

    constructor( private loginService: LoginService, private router: DotRouterService) {

    }

    recoverPassword(forgotPasswordLogin: string): void {
        this.message = '';
        this.email = forgotPasswordLogin;

        this.loginService.recoverPassword(forgotPasswordLogin).subscribe((resp: ResponseView) => {
            this.goToLogin();
        }, (resp: ResponseView) => {
            if (!resp.existError('a-new-password-has-been-sent-to-x')) {
                this.message = resp.errorsMessages;
            } else {
                this.goToLogin();
            }
        });
    }

    goToLogin(): void {
        this.router.goToLogin({'resetEmailSent': true, 'resetEmail': this.email});
    }
}
