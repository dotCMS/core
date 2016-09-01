/**
 * Created by oswaldogallango on 7/11/16.
 */

import {ApiRoot} from '../persistence/ApiRoot';
import {AppConfigurationService} from '../services/system/app-configuration-service';
import {CoreWebService} from '../services/core-web-service';
import {DotcmsConfig} from '../services/system/dotcms-config';
import {Http} from '@angular/http';
import {Inject, Injectable} from '@angular/core';
import {Observable} from 'rxjs/Rx';
import {RequestMethod} from '@angular/http';
import {Router} from '@ngrx/router';
import {Subject} from 'rxjs/Subject';


/**
 * This Service get the server configuration to display in the login component
 * and execute the login and forgot password routines
 */
@Injectable()
export class LoginService extends CoreWebService {

    private _loginUser$: Subject<User> = new Subject<User>();
    private changePasswordURL: string;
    private country: string = '';
    private _isLoginAs$: Subject<boolean> = new Subject<boolean>();
    private lang: string = '';
    private loginAsUrl: string;
    private logoutAsUrl: string;
    private loginAsUserList: User[];
    private logoutURL: string;
    private recoverPasswordURL: string;
    private serverInfoURL: string;
    private user: User;
    private userAuthURL: string;
    private userListUrl: string;
    private _loginAsUsers$: Subject<User[]>;

    constructor(private appConfigurationService: AppConfigurationService, apiRoot: ApiRoot, http: Http,
                public coreWebService: CoreWebService, private router: Router,
                @Inject('dotcmsConfig') private dotcmsConfig: DotcmsConfig) {
        super(apiRoot, http);

        this._isLoginAs$ = <Subject<boolean>>new Subject();
        this._loginAsUsers$ = <Subject<User[]>>new Subject();
        this.changePasswordURL = `${apiRoot.baseUrl}api/v1/changePassword`;
        this.loginAsUrl = `${apiRoot.baseUrl}api/v1/users/loginas/userid`;
        this.loginAsUserList = [];
        this.logoutAsUrl = `${apiRoot.baseUrl}api/v1/users/logoutas`;
        this.logoutURL = `${apiRoot.baseUrl}api/v1/logout`;
        this.recoverPasswordURL = `${apiRoot.baseUrl}api/v1/forgotpassword`;
        this.serverInfoURL = `${apiRoot.baseUrl}api/v1/loginform`;
        this.userAuthURL = `${apiRoot.baseUrl}api/v1/authentication`;
        this.userListUrl = `${apiRoot.baseUrl}api/v1/users/loginAsData`;
    }

    get isLoginAs$(): Subject<boolean> {
        return this._isLoginAs$.asObservable();
    }

    get loginAsUsers$(): Subject<User[]> {
        return this._loginAsUsers$.asObservable();
    }

    get loginUser$(): Observable<User> {
        return this._loginUser$.asObservable();
    }

    get loginUser(): User {
        return this.user;
    }

    set isLoginAs$(val: boolean): void {
        this._isLoginAs$.next(val);
    }

    /**
     * Change password
     * @param login
     * @param password
     * @param token
     * @returns {Observable<any>}
     */
    // TODO: is a common practice in JavaScript that when there is more than 2 params change it to a Object:
    // params.login, params.password and params.token in this case.
    public changePassword(login: string, password: string, token: string): Observable<any> {
        let body = JSON.stringify({'userId': login, 'password': password, 'token': token});

        return this.coreWebService.requestView({
            body: body,
            method: RequestMethod.Post,
            url: this.changePasswordURL,
        });
    }

    /**
     * Get specific user from the Login as user list
     * @param id
     * @returns {User}
     */
    getLoginAsUser(id: string): User {
        return this.loginAsUserList.filter(user => user.userId === id)[0];
    };

    /**
     * Get the server information to configure the login component
     * @param language language and country to get the internationalized messages
     * @param i18nKeys array of message key to internationalize
     * @returns {Observable<any>} Observable with an array of internationalized messages and server configuration info
     */
    public getLoginFormInfo(language: string, i18nKeys: Array<string>): Observable<any> {
        this.setLanguage(language);

        return this.coreWebService.requestView({
            body: {'messagesKey': i18nKeys, 'language': this.lang, 'country': this.country},
            method: RequestMethod.Post,
            url: this.serverInfoURL,
        });
    }

    /**
     * Request and store the login as user list.
     */
    public loadLoginAsUsers(): void {
        this.requestView({
            method: RequestMethod.Get,
            url: this.userListUrl
        }).pluck('entity', 'users').subscribe(data => {
            this.loginAsUserList = data;
            this._loginAsUsers$.next(this.loginAsUserList);
        });
    }

    /**
     * Get a user from the login as user list
     * @param user
     * @returns {User[]}
     */
    public getLoginAsuser(user: string): User {
        return this.loginAsUserList.filter(item => item.userId === user)[0];
    }

    /**
     * Do the login as request and return an Observable.
     * @param options
     * @returns {Observable<R>}
     */
    // TODO: password in the url is a no-no, fix asap. Sanchez and Jose have an idea.
    public loginAs(options: any): Observable<any> {
        return this.requestView({
            method: RequestMethod.Put,
            url: `${this.loginAsUrl}/${options.userId}${options.password ? `/pwd/${options.password}` : ''}`
        }).map((res) => {
            this.setLogInUser(this.getLoginAsUser(options.userId));
            this.dotcmsConfig.setLoginAsUser(this.getLoginAsUser(options.userId));
            this._isLoginAs$.next(true);
            // Only to update the user logged as in the dotcmsConfig map.
            // this.appConfigurationService.getConfigProperties();
            return res;
        }).pluck('entity', 'loginAs');
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

        return this.requestView({
            body: {'userId': login, 'password': password, 'rememberMe': rememberMe, 'language': this.lang, 'country': this.country},
            method: RequestMethod.Post,
            url: this.userAuthURL,
        }).map(response => {
            this.setLogInUser(response.entity);
            this.dotcmsConfig.setUser(response.entity);
            return response.entity;
        });
    }

    /**
     * Logout "login as" user
     * @returns {Observable<R>}
     */
    public logoutAs(): Observable<any> {
        return this.requestView({
            method: RequestMethod.Put,
            url: `${this.logoutAsUrl}`
        }).map((res) => {
            this.setLogInUser(this.dotcmsConfig.configParams.user);
            this.dotcmsConfig.setLoginAsUser(null);
            this._isLoginAs$.next(false);
            // Only to update the user logged as in the dotcmsConfig map.
            // this.appConfigurationService.getConfigProperties();
            // console.log(this.dotcmsConfig);
            return res;
        });
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
        }).map(response => {
            this.setLogInUser(null);
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
     * Set logged user
     * @param user
     */
    public setLogInUser(user: User): void {
        this.user = user;
        this._loginUser$.next(user);
    }

    /**
     * update the language and country variables from the string
     * @param language string containing the language and country
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
    emailAddress: string;
    failedLoginAttempts?: number;
    female?: boolean;
    firstName: string;
    fullName?: string;
    id?: string;
    languageId?: string;
    lastLoginDate?: number; // Timestamp
    lastLoginIP?: string;
    lastName: string;
    male?: boolean;
    middleName?: string;
    modificationDate?: number; // Timestamp
    name?: string;
    nickname?: string;
    requestPassword?: boolean;
    timeZoneId?: string;
    type?: string;
    userId: string;
    password?: string;
}
