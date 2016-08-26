/**
 * Created by oswaldogallango on 7/11/16.
 */
import {Injectable} from '@angular/core';
import {Observable} from 'rxjs/Rx';
import {ApiRoot} from '../persistence/ApiRoot';
import {RequestMethod} from '@angular/http';
import {CoreWebService} from '../services/core-web-service';
import { Router } from '@ngrx/router';
import {Observer} from 'rxjs/Observer';
import {RoutingService} from './routing-service';
import {Subject} from 'rxjs/Subject';
import {Http} from '@angular/http';

/**
 * This Service get the server configuration to display in the login component
 * and execute the login and forgot password routines
 */
@Injectable()
export class LoginService extends CoreWebService {

    private user: User;
    private userAuthURL: string;
    private serverInfoURL: string;
    private recoverPasswordURL: string;
    private logoutURL: string;
    private changePasswordURL: string;

    private lang: string = '';
    private country: string = '';

    private _loginUser$: Subject<User> = new Subject<User>();

    constructor(apiRoot: ApiRoot, http: Http, public coreWebService: CoreWebService, private router: Router) {
        super(apiRoot, http);

        this.userAuthURL = `${apiRoot.baseUrl}api/v1/authentication`;
        this.serverInfoURL = `${apiRoot.baseUrl}api/v1/loginform`;
        this.recoverPasswordURL = `${apiRoot.baseUrl}api/v1/forgotpassword`;
        this.logoutURL = `${apiRoot.baseUrl}api/v1/logout`;
        this.changePasswordURL = `${apiRoot.baseUrl}api/v1/changePassword`;
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
    public logInUser(login: string, password: string, rememberMe: boolean, language: string): Observable<User> {
        this.setLanguage(language);

        let body = JSON.stringify({'userId': login, 'password': password, 'rememberMe': rememberMe, 'language': this.lang, 'country': this.country});

        return this.requestView({
            body: body,
            method: RequestMethod.Post,
            url: this.userAuthURL,
        }).map(response => {
            this.setLogInUser( response.entity );
            return response.entity;
        });
    }

    public setLogInUser( user: User ): void {
        this.user = user;
        this._loginUser$.next( user );
    }

    /**
     * Call the logout rest api
     * @returns {Observable<any>}
     */
    public logOutUser(): Observable<any> {
        this.router.go('/public/login');

        return this.requestView({
            method: RequestMethod.Get,
            url: this.logoutURL,
        }).map( response => {
            this.setLogInUser( null );
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

    public changePassword(login: string, password: string, token: string): Observable<any> {
        let body = JSON.stringify({'userId': login, 'password': password, 'token': token});

        return this.coreWebService.requestView({
            body: body,
            method: RequestMethod.Post,
            url: this.changePasswordURL,
        });
    }

    get loginUser$(): Observable<User> {
        return this._loginUser$.asObservable();
    }

    get loginUser(): User {
        return this.user;
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
}

export interface User {
    active?: boolean;
    actualCompanyId?: string;
    birthday?: number; // Timestamp
    comments?: string;
    companyId?: string;
    createDate?: number; // Timestamp
    deleteDate?: number; // Timestamp
    deleteInProgress?: boolean;
    failedLoginAttempts?: number;
    female?: boolean;
    firstName: string;
    fullName?: string;
    emailAddress: string;
    languageId?: string;
    lastLoginDate?: number; // Timestamp
    lastLoginIP?: string;
    lastName: string;
    male?: boolean;
    middleName?: string;
    modificationDate?: number; // Timestamp
    nickname?: string;
    timeZoneId?: string;
    userId: string;
}
