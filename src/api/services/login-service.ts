/**
 * Created by oswaldogallango on 7/11/16.
 */

import {ApiRoot} from '../persistence/ApiRoot';
import {CoreWebService} from '../services/core-web-service';
import {Http} from '@angular/http';
import {Injectable} from '@angular/core';
import {Observable} from 'rxjs/Rx';
import {RequestMethod} from '@angular/http';
import {Router} from '@angular/router';
import {Subject} from 'rxjs/Subject';

/**
 * This Service get the server configuration to display in the login component
 * and execute the login and forgot password routines
 */
@Injectable()
export class LoginService extends CoreWebService {

    private _auth$: Subject<Auth> = new Subject<Auth>();
    private _auth: Auth;
    private _loginAsUsersList$: Subject<User[]>;
    private country: string = '';
    private lang: string = '';
    private loginAsUserList: User[];
    private urls: any;

    constructor(apiRoot: ApiRoot, http: Http, private router: Router) {
        super(apiRoot, http);

        this._loginAsUsersList$ = <Subject<User[]>>new Subject();
        this.loginAsUserList = [];
        this.urls = {
            changePassword: 'v1/changePassword',
            getAuth: 'v1/authentication/logInUser',
            loginAs: 'v1/users/loginas/userid',
            loginAsUserList: 'v1/users/loginAsData',
            logout: 'v1/logout',
            logoutAs: 'v1/users/logoutas',
            recoverPassword: 'v1/forgotpassword',
            serverInfo: 'v1/loginform',
            userAuth: 'v1/authentication'
        };
    }

    get loginAsUsersList$(): Observable<User[]> {
        return this._loginAsUsersList$.asObservable();
    }

    get auth$(): Observable<Auth> {
        return this._auth$.asObservable();
    }

    get auth(): Auth {
        return this._auth;
    }

    get isLogin(): boolean{
        return this.auth && this.auth.user;
    }

    /**
     * Load _auth information.
     * @returns {Observable<any>}
     */
    public loadAuth(): Observable<Auth> {
        return this.requestView({
            method: RequestMethod.Get,
            url: this.urls.getAuth
        }).pluck('entity').map(auth => {
            if (auth) {
                this.setAuth(auth);
            }
            return auth;
        });
    }

    /**
     * Change password
     * @param password
     * @param token
     * @returns {Observable<any>}
     */
    // TODO: is a common practice in JavaScript that when there is more than 2 params change it to a Object:
    // params.login, params.password and params.token in this case.
    public changePassword(password: string, token: string): Observable<any> {
        let body = JSON.stringify({'password': password, 'token': token});

        return this.requestView({
            body: body,
            method: RequestMethod.Post,
            url: this.urls.changePassword,
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
     * Get login as user list
     * @returns {Observable<User[]>}
     */
    public getLoginAsUsersList(): Observable<User[]> {
        return Observable.create(observer => {
            if (this.loginAsUserList.length) {
                observer.next(this.loginAsUserList);
            } else {
                this.loadLoginAsUsersList();
                let loginAsUsersListSub = this._loginAsUsersList$.subscribe(res => {
                    observer.next(res);
                    loginAsUsersListSub.unsubscribe();
                });
            }
        });
    }

    /**
     * Get the server information to configure the login component
     * @param language language and country to get the internationalized messages
     * @param i18nKeys array of message key to internationalize
     * @returns {Observable<any>} Observable with an array of internationalized messages and server configuration info
     */
    public getLoginFormInfo(language: string, i18nKeys: Array<string>): Observable<any> {
        this.setLanguage(language);

        return this.requestView({
            body: {'messagesKey': i18nKeys, 'language': this.lang, 'country': this.country},
            method: RequestMethod.Post,
            url: this.urls.serverInfo,
        });
    }

    /**
     * Request and store the login as _auth list.
     */
    public loadLoginAsUsersList(): void {
        this.requestView({
            method: RequestMethod.Get,
            url: this.urls.loginAsUserList
        }).pluck('entity', 'users').subscribe(data => {
            this.loginAsUserList = data;
            this._loginAsUsersList$.next(this.loginAsUserList);
        });
    }

    /**
     * Get a user from the login as user list
     * @param userId
     * @returns {User[]}
     */
    public getLoginAsUser(userId: string): User {
        return this.loginAsUserList.filter(item => item.userId === userId)[0];
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
            url: `${this.urls.loginAs}/${options.userId}${options.password ? `/pwd/${options.password}` : ''}`
        }).map((res) => {
            if (!res.entity.loginAs) {
                throw res.errorsMessages;
            }
            let loginAsUser = this.getLoginAsUser(options.userId);
            this.setAuth({
                loginAsUser: loginAsUser,
                user: this._auth.user
            });
            return res;
        }).pluck('entity', 'loginAs');
    }

    /**
     * Executes the call to the login rest api
     * @param login User email or user id
     * @param password User password
     * @param rememberMe boolean indicating if the _auth want to use or not the remenber me option
     * @param language string with the language and country code, ex: en_US
     * @returns an array with the user if the user logged in successfully or the error message
     */
    public loginUser(login: string, password: string, rememberMe: boolean, language: string): Observable<User> {
        this.setLanguage(language);

        return this.requestView({
            body: {'userId': login, 'password': password, 'rememberMe': rememberMe, 'language': this.lang, 'country': this.country},
            method: RequestMethod.Post,
            url: this.urls.userAuth,
        }).map(response => {
            let auth = {
                loginAsUser: null,
                user: response.entity
            };
            this.setAuth(auth);
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
            url: `${this.urls.logoutAs}`
        }).map((res) => {
            this.setAuth({
                loginAsUser: null,
                user: this._auth.user
            });
            return res;
        });
    }

    /**
     * Call the logout rest api
     * @returns {Observable<any>}
     */
    public logOutUser(): Observable<any> {

        return this.requestView({
            method: RequestMethod.Get,
            url: this.urls.logout,
        }).map(response => {
            let nullAuth = {
                loginAsUser: null,
                user: null
            };
            this.setAuth(nullAuth);
            this.router.navigate(['/public/login']);
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

        return this.requestView({
            body: {'userId': login},
            method: RequestMethod.Post,
            url: this.urls.recoverPassword,
        });
    }

    /**
     * Subscribe to ser change and call received function on change.
     * @param func function will call when user change
     */
    public watchUser(func: Function) {
        if (this.auth) {
            func(this.auth);
        }

        this.auth$.subscribe(auth => {
            if (auth.user) {
                func(auth);
            }
        });
    }

    /**
     * Set logged_auth and update auth Observable
     * @param _auth
     */
    private setAuth(auth: Auth): void {
        this._auth = auth;
        this._auth$.next(auth);
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
        } else {
            this.lang = '';
            this.country = '';
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

export interface Auth {
    user: User,
    loginAsUser: User
}