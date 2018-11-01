import { of as observableOf, Observable, Subject } from 'rxjs';
import { User, Auth } from 'dotcms-js';

export const mockUser: User = {
    emailAddress: 'admin@dotcms.com',
    firstName: 'Admin',
    lastName: 'Admin',
    loggedInDate: 123456789,
    userId: '123'
};

const mockUserWithRedirect = {
    ...mockUser,
    editModeUrl: 'redirect/to'
};

export const mockAuth: Auth = {
    loginAsUser: mockUser,
    user: mockUser
};

export class LoginServiceMock {
    _auth: Subject<Auth> = new Subject();
    private watchUserFunc: Function;

    constructor() {
        this._auth.next(mockAuth);
    }

    get auth(): Auth {
        return mockAuth;
    }

    get auth$(): Observable<Auth> {
        return this._auth;
    }

    setAuth(): void {

    }

    loginAs(): Observable<any> {
        return observableOf({});
    }

    logoutAs(): Observable<any> {
        return observableOf({});
    }

    loginUser(): Observable<User> {
        return observableOf(mockUserWithRedirect);
    }

    logOutUser(): Observable<any> {
        return observableOf({});
    }

    watchUser(func: Function): void {
        this.watchUserFunc = func;
    }

    tiggerWatchUser(): void {
        this.watchUserFunc(mockAuth);
    }

    triggerNewAuth(auth: Auth) {
        this._auth.next(auth);
    }

    getLoginFormInfo(): Observable<any> {
        return observableOf({
            i18nMessagesMap: {
                'sign-in': 'Sign in'
            },
            entity: {
                levelName: '',
                languages: [],
                currentLanguage: ''
            }
        });
    }
}
