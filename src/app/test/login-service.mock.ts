import { User, Auth } from 'dotcms-js/dotcms-js';
import { Observable } from 'rxjs/Observable';
import { Subject } from 'rxjs/Subject';

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

const mockAuth: Auth = {
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

    loginUser(): Observable<User> {
        return Observable.of(mockUserWithRedirect);
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
        return Observable.of({
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
