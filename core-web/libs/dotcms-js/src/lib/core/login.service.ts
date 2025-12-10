import { Observable, of, Subject } from 'rxjs';

import { HttpResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map, pluck, tap } from 'rxjs/operators';

import { DotLoginInformation, SESSION_STORAGE_VARIATION_KEY } from '@dotcms/dotcms-models';

import { CoreWebService } from './core-web.service';
import { DotcmsEventsService } from './dotcms-events.service';
import { HttpCode } from './util/http-code';

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
@Injectable({
    providedIn: 'root'
})
export class LoginService {
    private coreWebService = inject(CoreWebService);
    private dotcmsEventsService = inject(DotcmsEventsService);

    currentUserLanguageId = '';
    private country = '';
    private lang = '';
    private urls: Record<string, string>;

    constructor() {
        const dotcmsEventsService = this.dotcmsEventsService;

        this._loginAsUsersList$ = new Subject<User[]>();

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
        dotcmsEventsService.subscribeTo('SESSION_DESTROYED').subscribe(() => {
            this.logOutUser();
            this.clearExperimentPersistence();
        });

        dotcmsEventsService.subscribeTo('SESSION_LOGOUT').subscribe(() => {
            this.clearExperimentPersistence();
        });
    }

    private _auth$: Subject<Auth> = new Subject<Auth>();

    get auth$(): Observable<Auth> {
        return this._auth$.asObservable();
    }

    private _logout$ = new Subject<void>();

    get logout$() {
        return this._logout$.asObservable();
    }

    private _auth: Auth;

    get auth(): Auth {
        return this._auth;
    }

    private _loginAsUsersList$: Subject<User[]>;

    get loginAsUsersList$(): Observable<User[]> {
        return this._loginAsUsersList$.asObservable();
    }

    get isLogin$(): Observable<boolean> {
        if (!!this.auth && !!this.auth.user) {
            return of(true);
        }

        return this.loadAuth().pipe(map((auth) => !!auth && !!auth.user));
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
                map((auth: Auth) => this.getFullAuth(auth))
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
                body: {
                    messagesKey: i18nKeys,
                    language: this.lang,
                    country: this.country
                },
                method: 'POST',
                url: this.urls.serverInfo
            })
            .pipe(map((res) => res.entity));
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
                        user: this._auth.user
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
                        user: response.entity
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
                        user: this._auth.user
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
        this._auth = this.getFullAuth(auth);
        this._auth$.next(this.getFullAuth(auth));

        this.currentUserLanguageId = auth.user.languageId;

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

    private getFullAuth(auth: Auth): Auth {
        const isLoginAs = !!auth.loginAsUser || !!Object.keys(auth.loginAsUser || {}).length;

        return {
            ...auth,
            isLoginAs
        };
    }

    private clearExperimentPersistence() {
        sessionStorage.removeItem(SESSION_STORAGE_VARIATION_KEY);
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
    admin: boolean;
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
