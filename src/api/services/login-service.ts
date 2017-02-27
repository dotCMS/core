/**
 * Created by oswaldogallango on 7/11/16.
 */
import {CoreWebService} from '../services/core-web-service';
import {RequestMethod} from '@angular/http';
import {Injectable} from '@angular/core';
import {Observable} from 'rxjs/Rx';
import {Router} from '@angular/router';
import {Subject} from 'rxjs/Subject';
import {DotcmsEventsService} from './dotcms-events-service';
import {LoggerService} from './logger.service';

/**
 * This Service get the server configuration to display in the login component
 * and execute the login and forgot password routines
 */
@Injectable()
export class LoginService {

    private _auth$: Subject<Auth> = new Subject<Auth>();
    private _logout$: Subject<> = new Subject<>();
    private _auth: Auth;
    private _loginAsUsersList$: Subject<User[]>;
    private country: string = '';
    private lang: string = '';
    private loginAsUserList: User[];
    private urls: any;

    constructor(private router: Router, private coreWebService: CoreWebService,
                private dotcmsEventsService: DotcmsEventsService,
                private loggerService: LoggerService) {

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

        coreWebService.subscribeTo(401).subscribe(() => this.logOutUser().subscribe(() => {}));

        // when the session is expired/destroyed
        dotcmsEventsService.subscribeTo('SESSION_DESTROYED').pluck('data').subscribe( date => {

            this.loggerService.debug('Processing session destroyed: ', date);
            this.loggerService.debug('User Logged In Date: ', this.auth.user.loggedInDate);
            // if the destroyed event happens after the logged in date, so proceed!
            if (this.isLogoutAfterLastLogin(date)) {
                this.logOutUser().subscribe(() => {});
            }
        });
    }

    private isLogoutAfterLastLogin(date): boolean {
        return this.auth.user && this.auth.user.loggedInDate && date && Number(date) > Number(this.auth.user.loggedInDate);
    }

    get loginAsUsersList$(): Observable<User[]> {
        return this._loginAsUsersList$.asObservable();
    }

    get auth$(): Observable<Auth> {
        return this._auth$.asObservable();
    }

    get logout$(): Observable<> {
        return this._logout$.asObservable();
    }

    get auth(): Auth {
        return this._auth;
    }

    get isLogin$(): Observable<boolean>{
        return Observable.create(obs => {
            if (this.auth && this.auth.user) {
                obs.next(true);
            } else {
                this.loadAuth().subscribe(auth => obs.next(<boolean> auth.user));
            }
        });
    }

    /**
     * Load _auth information.
     * @returns {Observable<any>}
     */
    public loadAuth(): Observable<Auth> {
        return this.coreWebService.requestView({
            method: RequestMethod.Get,
            url: this.urls.getAuth
        }).pluck('entity').map(auth => {

            if (auth.user) {
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

        return this.coreWebService.requestView({
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
            this.loadLoginAsUsersList();
            let loginAsUsersListSub = this._loginAsUsersList$.subscribe(res => {
                observer.next(res);
                loginAsUsersListSub.unsubscribe();
            });
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

        return this.coreWebService.requestView({
            body: {'messagesKey': i18nKeys, 'language': this.lang, 'country': this.country},
            method: RequestMethod.Post,
            url: this.urls.serverInfo,
        });
    }

    /**
     * Request and store the login as _auth list.
     */
    public loadLoginAsUsersList(): void {
        this.coreWebService.requestView({
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
        return this.coreWebService.requestView({
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

        return this.coreWebService.requestView({
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
        return this.coreWebService.requestView({
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

        return this.coreWebService.requestView({
            method: RequestMethod.Get,
            url: this.urls.logout,
        }).map(response => {
            let nullAuth = {
                loginAsUser: null,
                user: null
            };

            this.loggerService.debug("Processing the logOutUser");
            this.setAuth(nullAuth);

            // on logout close the websocket
            this.dotcmsEventsService.destroy();

            this.loggerService.debug("Navigating to Public Login");

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

        return this.coreWebService.requestView({
            body: {'userId': login},
            method: RequestMethod.Post,
            url: this.urls.recoverPassword,
        });
    }

    /**
     * Subscribe to ser change and call received function on change.
     * @param func function will call when user change
     */
    public watchUser(func: Function): void {
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

        // When not logged user we need to fire the observable chain
        if (!auth.user) {
            this._logout$.next();
        }else {
            this.dotcmsEventsService.start();
        }
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
    loggedInDate: number; // Timestamp
}

export interface Auth {
    user: User,
    loginAsUser: User
}