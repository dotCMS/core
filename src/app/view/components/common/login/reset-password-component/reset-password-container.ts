import {Router, ActivatedRoute} from '@angular/router';
import {Component, ViewEncapsulation} from '@angular/core';
import {LoginService} from '../../../../../api/services/login-service';
import {ResetPasswordComponent} from './reset-password-component';

@Component({
    directives: [ResetPasswordComponent],
    encapsulation: ViewEncapsulation.Emulated,
    moduleId: __moduleName, // REQUIRED to use relative path in styleUrls
    pipes: [],
    providers: [LoginService],
    selector: 'dot-reset-password-container',
    styleUrls: [],
    template: `
        <dot-reset-password-component
            [token]="token"
            [message]="message"
            (changePassword)="changePassword($event)">
        </dot-reset-password-component>
    `
})
export class ResetPasswordContainer {

    private message: string = '';
    private login: string = '';
    private token: string = '';

    private changePasswordSuccessfully: string;

    constructor(private loginService: LoginService, private router: Router, private route: ActivatedRoute) {
        this.route.params.pluck('token').subscribe(token => {
            this.token = token;
        });

        this.loginService.getLoginFormInfo('', ['message.forgot.password.password.updated']).subscribe((data) => {
            let dataI18n = data.i18nMessagesMap;
            let entity = data.entity;

            this.changePasswordSuccessfully = dataI18n['message.forgot.password.password.updated'];
        }, (error) => {
            console.log(error);
        });
    }

    public changePassword(changePasswordData: ChangePasswordData): void {
        this.cleanMessage();
        this.loginService.changePassword(changePasswordData.password, changePasswordData.token)
            .subscribe( result =>{
                //alert(this.resetPasswordSuccessMessage);
                // TODO need to use internationalization
                alert( this.changePasswordSuccessfully );
                this.goToLogin();
            }, (error) => {
                this.message = error.errorsMessages;
            });
    }

    private goToLogin(): void {
        this.router.navigate(['/public/login',{ 'changedPassword': true }]);
    }

    private cleanMessage(): void {
        this.message = '';
    }
}

export interface ChangePasswordData{
    token: string,
    password: string
}
