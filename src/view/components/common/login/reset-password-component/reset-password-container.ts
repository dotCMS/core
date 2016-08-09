import {Component, ViewEncapsulation} from '@angular/core';
import {LoginService} from "../../../../../api/services/login-service";
import { Router, QueryParams, RouteParams } from '@ngrx/router';
import {ResetPasswordComponent} from "./reset-password-component";

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
            [login]="login"
            [message]="message"
            (changePassword)="changePassword($event)">
        </dot-reset-password-component>
    `
})
export class ResetPasswordContainer{

    private message:string = '';
    private login:string = '';
    private token:string = '';

    constructor( private loginService: LoginService, private router: Router,
                 private queryParams: QueryParams, private routeParams: RouteParams) {

    }

    ngOnInit(){
         this.queryParams.pluck<string>('token').distinctUntilChanged()
            .forEach(token => this.token = token);

        this.routeParams.pluck<string>('userId').distinctUntilChanged()
            .forEach(login => this.login = login);
    }

    public changePassword(changePasswordData:ChangePasswordData):void{
        this.loginService.changePassword(changePasswordData.login, changePasswordData.password, changePasswordData.token)
            .subscribe( result =>{
                //alert(this.resetPasswordSuccessMessage);
                // TODO need to use internationalization
                alert('Your password have been successfully changed');
                this.goToLogin();
            }, error => this.message = error.errorsMessages);
    }

    private goToLogin():void{
        this.router.go('/public/login');
    }
}

export interface ChangePasswordData{
    login:string,
    token:string,
    password:string
}
