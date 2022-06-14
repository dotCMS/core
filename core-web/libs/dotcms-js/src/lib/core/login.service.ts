/**
 * Created by oswaldogallango on 7/11/16.
 */
import { CoreWebService } from './core-web.service';
import { Injectable } from '@angular/core';
import { Observable, Subject, of } from 'rxjs';
import { HttpCode } from './util/http-code';
import { pluck, tap, map } from 'rxjs/operators';
import { DotcmsEventsService } from './dotcms-events.service';
import { HttpResponse } from '@angular/common/http';
import { ResponseView } from './util/response-view';
import { DotLoginInformation } from '@dotcms/dotcms-models';

export interface DotLoginParams {
    login: string;
    password: string;
    rememberMe: boolean;
    language: string;
    backEndLogin: boolean;
}

export const LOGOUT_URL = '/dotAdmin/logout';

/**
 * This Service get the server configuration to display in the login component
 * and execute the login and forgot password routines
 */
@Injectable()
export class LoginService {
    private _auth$: Subject<Auth> = new Subject<Auth>();
    private _logout$: Subject<any> = new Subject<any>();
    private _auth: Auth;
    private _loginAsUsersList$: Subject<User[]>;
    private country = '';
    private lang = '';
    private urls: Record<string, string>;

    constructor(
        private coreWebService: CoreWebService,
        private dotcmsEventsService: DotcmsEventsService
    ) {
        this._loginAsUsersList$ = <Subject<User[]>>new Subject();
        this.urls = {
            changePassword: 'v1/changePassword',
            getAuth: 'v1/authentication/logInUser',
            loginAs: 'v1/users/loginas',
            logout: 'v1/logout',
            logoutAs: 'v1/users/logoutas',
            recoverPassword: 'v1/forgotpassword',
            serverInfo: 'v1/loginform',
            userAuth: 'v1/authentication',
            current: '/api/v1/users/current/'
        };

        // when the session is expired/destroyed
        dotcmsEventsService.subscribeTo('SESSION_DESTROYED').subscribe(() => this.logOutUser());
    }

    /**
     * Get current logged in user
     *
     * @return {*}  {Observable<CurrentUser>}
     * @memberof LoginService
     */
    getCurrentUser(): Observable<CurrentUser> {
        return this.coreWebService
            .request<CurrentUser>({
                url: this.urls.current
            })
            .pipe(
                map((res: HttpResponse<CurrentUser>) => res)
            ) as unknown as Observable<CurrentUser>;
    }

    get loginAsUsersList$(): Observable<User[]> {
        return this._loginAsUsersList$.asObservable();
    }

    get auth$(): Observable<Auth> {
        return this._auth$.asObservable();
    }

    get logout$(): Observable<any> {
        return this._logout$.asObservable();
    }

    get auth(): Auth {
        return this._auth;
    }

    get isLogin$(): Observable<boolean> {
        if (!!this.auth && !!this.auth.user) {
            return of(true);
        }

        return this.loadAuth().pipe(map((auth) => !!auth && !!auth.user));
    }

    /**
     * Return the login status
     *
     * @return {*}  {Observable<Auth>}
     * @memberof LoginService
     */
    loadAuth(): Observable<Auth> {
        return this.coreWebService
            .requestView({
                url: this.urls.getAuth
            })
            .pipe(
                pluck('entity'),
                tap((auth: Auth) => {
                    if (auth.user) {
                        this.setAuth(auth);
                    }
                }),
                map((auth: Auth) => auth)
            );
    }

    /**
     * Change password
     *
     * @param {string} password
     * @param {string} token
     * @return {*}  {Observable<string>}
     * @memberof LoginService
     */
    changePassword(password: string, token: string): Observable<string> {
        const body = JSON.stringify({ password: password, token: token });

        return this.coreWebService
            .requestView({
                body: body,
                method: 'POST',
                url: this.urls.changePassword
            })
            .pipe(pluck('entity'));
    }

    /**
     * Get the server information to configure the login component
     *
     * @param {string} language
     * @param {Array<string>} i18nKeys
     * @return {*}  {Observable<DotLoginInformation>}
     * @memberof LoginService
     */
    getLoginFormInfo(language: string, i18nKeys: Array<string>): Observable<DotLoginInformation> {
        this.setLanguage(language);

        return this.coreWebService
            .requestView<DotLoginInformation>({
                body: { messagesKey: i18nKeys, language: this.lang, country: this.country },
                method: 'POST',
                url: this.urls.serverInfo
            })
            .pipe(pluck('bodyJsonObject'));
    }

    /**
     * Do the login as request and return an Observable.
     *
     * @param {{ user: User; password: string }} userData
     * @return {*}  {Observable<boolean>}
     * @memberof LoginService
     */
    loginAs(userData: { user: User; password: string }): Observable<boolean> {
        return this.coreWebService
            .requestView<{ loginAs: boolean }>({
                body: {
                    password: userData.password,
                    userId: userData.user.userId
                },
                method: 'POST',
                url: this.urls.loginAs
            })
            .pipe(
                map((res) => {
                    if (!res.entity.loginAs) {
                        throw res.errorsMessages;
                    }

                    this.setAuth({
                        loginAsUser: userData.user,
                        user: this._auth.user,
                        isLoginAs: true
                    });
                    return res;
                }),
                pluck('entity', 'loginAs')
            );
    }

    /**
     * Executes the call to the login rest api
     *
     * @param {DotLoginParams} {
     *         login,
     *         password,
     *         rememberMe,
     *         language,
     *         backEndLogin
     *     }
     * @return {*}  {Observable<User>}
     * @memberof LoginService
     */
    loginUser({
        login,
        password,
        rememberMe,
        language,
        backEndLogin
    }: DotLoginParams): Observable<User> {
        this.setLanguage(language);

        return this.coreWebService
            .requestView<User>({
                body: {
                    userId: login,
                    password: password,
                    rememberMe: rememberMe,
                    language: this.lang,
                    country: this.country,
                    backEndLogin: backEndLogin
                },
                method: 'POST',
                url: this.urls.userAuth
            })
            .pipe(
                map((response) => {
                    const auth = {
                        loginAsUser: null,
                        user: response.entity,
                        isLoginAs: false
                    };

                    this.setAuth(auth);
                    this.coreWebService
                        .subscribeToHttpError(HttpCode.UNAUTHORIZED)
                        .subscribe(() => {
                            this.logOutUser();
                        });
                    return response.entity;
                })
            );
    }

    /**
     * Logout "login as" user
     *
     * @return {*}  {Observable<boolean>}
     * @memberof LoginService
     */
    logoutAs(): Observable<boolean> {
        return this.coreWebService
            .requestView<{ logoutAs: boolean }>({
                method: 'PUT',
                url: `${this.urls.logoutAs}`
            })
            .pipe(
                map((res) => {
                    this.setAuth({
                        loginAsUser: null,
                        user: this._auth.user,
                        isLoginAs: true
                    });
                    return res.entity.logoutAs;
                })
            );
    }

    /**
     * Executes the call to the recover passwrod rest api
     *
     * @param {string} login
     * @return {*}  {Observable<string>}
     * @memberof LoginService
     */
    recoverPassword(login: string): Observable<string> {
        return this.coreWebService
            .requestView<string>({
                body: { userId: login },
                method: 'POST',
                url: this.urls.recoverPassword
            })
            .pipe(pluck('entity'));
    }

    /**
     * Subscribe to ser change and call received function on change.
     *
     * @param {(params?: unknown) => void} func
     * @memberof LoginService
     */
    watchUser(func: (params?: unknown) => void): void {
        if (this.auth) {
            func(this.auth);
        }

        this.auth$.subscribe((auth) => {
            if (auth.user) {
                func(auth);
            }
        });
    }

    /**
     * Set logged_auth and update auth Observable
     *
     * @param {Auth} auth
     * @memberof LoginService
     */
    setAuth(auth: Auth): void {
        this._auth = auth;
        this._auth$.next(auth);

        // When not logged user we need to fire the observable chain
        if (!auth.user) {
            this._logout$.next();
        } else {
            this.dotcmsEventsService.start();
        }
    }

    private setLanguage(language: string): void {
        if (language !== undefined && language !== '') {
            const languageDesc = language.split('_');
            this.lang = languageDesc[0];
            this.country = languageDesc[1];
        } else {
            this.lang = '';
            this.country = '';
        }
    }

    private logOutUser(): void {
        window.location.href = `${LOGOUT_URL}?r=${new Date().getTime()}`;
    }
}

export interface CurrentUser {
    email: string;
    givenName: string;
    loginAs: boolean;
    roleId: string;
    surname: string;
    userId: string;
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
    editModeUrl?: string;
}

export interface Auth {
    user: User;
    loginAsUser: User;
    isLoginAs?: boolean;
}
