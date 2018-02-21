import { User, Auth } from 'dotcms-js/dotcms-js';
import { Observable } from 'rxjs/Observable';
import { Subject } from 'rxjs';

const mockUser: User = {
    emailAddress: 'admin@dotcms.com',
    firstName: 'Admin',
    lastName: 'Admin',
    loggedInDate: 123456789,
    userId: '123'
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

    watchUser(func: Function): void {
        this.watchUserFunc = func;
    }

    tiggerWatchUser(): void {
        this.watchUserFunc(mockAuth);
    }

    triggerNewAuth(auth: Auth) {
        this._auth.next(auth);
    }
}
