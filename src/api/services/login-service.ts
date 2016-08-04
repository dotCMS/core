/**
 * Created by oswaldogallango on 7/11/16.
 */
import {Injectable} from '@angular/core';
import {Observable} from 'rxjs/Rx';
import {ApiRoot} from '../persistence/ApiRoot';
import {RequestMethod} from '@angular/http';
import {CoreWebService} from '../services/core-web-service';
import { Router } from '@ngrx/router';

/**
 * This Service get the server configuration to display in the login component
 * and execute the login and forgot password routines
 */
@Injectable()
export class LoginService  {
    private user:User;
    private serverInfo: Array<any>;
    private userAuthURL: string;
    private serverInfoURL: string;
    private recoverPasswordURL: string;
    private logoutURL: string;
    private changePasswordURL: string;

    private lang: string = '';
    private country: string = '';

    constructor(_apiRoot: ApiRoot, public coreWebService: CoreWebService, private router: Router) {

        this.userAuthURL = `${_apiRoot.baseUrl}api/v1/authentication`;
        this.serverInfoURL = `${_apiRoot.baseUrl}api/v1/loginform`;
        this.recoverPasswordURL = `${_apiRoot.baseUrl}api/v1/forgotpassword`;
        this.logoutURL = `${_apiRoot.baseUrl}api/v1/logout`;
        this.changePasswordURL = `${_apiRoot.baseUrl}api/v1/changePassword`;
    }

    /**
     * Get the server information to configure the login component
     * @param language language and country to get the internationalized messages
     * @param i18nKeys array of message key to internationalize
     * @returns {Observable<any>} Observable with an array of internationalized messages and server configuration info
     */
    public getLoginFormInfo(language: string, i18nKeys: Array<string>): Observable<any> {
        this.setLanguage(language);

        let body = JSON.stringify({'messagesKey': i18nKeys, 'language': this.lang, 'country': this.country});

        return this.coreWebService.requestView({
            body: body,
            method: RequestMethod.Post,
            url: this.serverInfoURL,
        });
    }

    /**
     * Executes the call to the login rest api
     * @param login User email or user id
     * @param password User password
     * @param rememberMe boolean indicating if the user want to use or not the remenber me option
     * @param language string with the language and country code, ex: en_US
     * @returns an array with the user if the user loggedIn successfully or the error message
     */
    public logInUser(login: string, password: string, rememberMe: boolean, language: string): Observable<{errors: string[], entity: Object, messages: string[], i18nMessagesMap: Object}> {
        this.setLanguage(language);

        let body = JSON.stringify({'userId': login, 'password': password, 'rememberMe': rememberMe, 'language': this.lang, 'country': this.country});

        return Observable.create(observer => { 
            this.coreWebService.requestView({
                body: body,
                method: RequestMethod.Post,
                url: this.userAuthURL
            }).subscribe(response =>{ 
                this.user = response.entity; 
                observer.next(response); 
            }, error => observer.error(error));
        });
    }

    /**
     * Call the logout rest api
     * @returns {Observable<any>}
     */
    public logOutUser(): Observable<any> {
        this.router.go('/login/login');

        return this.coreWebService.requestView({
            method: RequestMethod.Get,
            url: this.logoutURL,
        });

    }

    /**
     * Executes the call to the recover passwrod rest api
     * @param email User email address
     * @returns an array with message indicating if the recover password was successfull
     * or if there is an error
     */
    public recoverPassword(login: string): Observable<any> {
        let body = JSON.stringify({'userId': login});

        return this.coreWebService.requestView({
            body: body,
            method: RequestMethod.Post,
            url: this.recoverPasswordURL,
        });
    }

    /**
     * update the language and country variables from the string
     * @param language string containing the languag and country
     */
    private setLanguage(language: string): void {
        if (language !== undefined && language !== '') {
            let languageDesc = language.split('_');
            this.lang = languageDesc[0];
            this.country = languageDesc[1];
        }
    }

    public changePassword(login:string, password:string, token:string): Observable<any> {
        let body = JSON.stringify({'userId': login, 'password': password, 'token': token});

        return this.coreWebService.requestView({
            body: body,
            method: RequestMethod.Post,
            url: this.changePasswordURL,
        });
    }
    public getLoginUser():User{
        return this.user;
    }
}

export interface User{
    birthday:number //Timestamp
    lastName:string
    comments:string
    timeZoneId:string
    languageId:string
    active:boolean
    fullName:string
    lastLoginDate:number //Timestamp
    failedLoginAttempts:number
    userId:string
    lastLoginIP:string
    firstName:string
    companyId:string
    modificationDate:number //Timestamp
    emailAddress:string
    deleteInProgress:boolean
    nickname:string
    middleName:string
    female:boolean
    actualCompanyId:string
    male:boolean
    createDate:number //Timestamp
    deleteDate:number //Timestamp
}
