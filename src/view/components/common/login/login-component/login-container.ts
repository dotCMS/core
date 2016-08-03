
import {Component, EventEmitter, Inject, Input, Output, ViewEncapsulation} from '@angular/core';
import {LoginService} from "../../../../../api/services/login-service";
import { Router } from '@ngrx/router';
import {LoginComponent} from "./login-component";

@Component({
    directives: [LoginComponent],
    encapsulation: ViewEncapsulation.Emulated,
    moduleId: __moduleName, // REQUIRED to use relative path in styleUrls
    pipes: [],
    providers: [LoginService],
    selector: 'dot-login-container',
    styleUrls: [],
    template: `
        <dot-login-component
            [message]="message"
            (login)="logInUser($event)"
            (recoverPassword)="showForgotPassword()"
        >
        </dot-login-component>
    `,
})
export class LoginContainer{
    private message:string;

    constructor(private loginService: LoginService, private router: Router) {

    }

    logInUser(loginData:LoginData): void {
      this.loginService.logInUser(loginData.login, loginData.password, loginData.remenberMe, loginData.language).subscribe((result:any) => {
          console.log();
            if (result.errors.length > 0) {
                this.message = result.errors[0].message;
            } else {
                this.message = '';
                this.router.go('/main');
                //TODO: this window.location.reload should be removed once the menu and router injection update issue is fixed
                //window.location.reload();
            }
        }, (error) => {
            if (error.response.status === 400 || error.response.status === 401) {
                this.message = error.getErrorMessage();
            } else {
                console.log(error);
            }
        });
    }

    /**
     * Display the forgot password card
     */
    showForgotPassword(): void {
        this.router.go('/login/fogotPassword');
    }
}

export interface LoginData{
    login:string,
    password:string,
    remenberMe:boolean,
    language:string
}