
import {Component, EventEmitter, Inject, Input, Output, ViewEncapsulation} from '@angular/core';
import {LoginService} from "../../../../../api/services/login-service";
import { Router } from '@ngrx/router';
import {LoginComponent} from "./login-component";

@Component({
    directives: [LoginComponent],
    encapsulation: ViewEncapsulation.Emulated,
    moduleId: __moduleName, // REQUIRED to use relative path in styleUrls
    pipes: [],
    providers: [],
    selector: 'dot-login-container',
    styleUrls: [],
    template: `
        <dot-login-component
            [message]="message"
            [isLoginInProgress] = "isLoginInProgress"
            (login)="logInUser($event)"
            (recoverPassword)="showForgotPassword()"
        >
        </dot-login-component>
    `,
})
export class LoginContainer{
    private message:string;
    private isLoginInProgress: boolean = false;

    constructor(private loginService: LoginService, private router: Router) {

    }

    logInUser(loginData:LoginData): void {
        this.isLoginInProgress = true;

        this.loginService.logInUser(loginData.login, loginData.password, loginData.remenberMe, loginData.language).subscribe((result:any) => {
            this.message = '';
            this.router.go('/main');
        }, (error) => {
            if (error.response.status === 400 || error.response.status === 401) {
                this.message = error.getErrorMessage();
            } else {
                console.log(error);
            }
            this.isLoginInProgress = false
            ;
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