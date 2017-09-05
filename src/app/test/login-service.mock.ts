import { User, Auth } from 'dotcms-js/dotcms-js';
import { Observable } from 'rxjs/Observable';

export class LoginServiceMock {
    public mockUser: User = {
        emailAddress: 'admin@dotcms.com',
        firstName: 'Admin',
        lastName: 'Admin',
        loggedInDate: 123456789,
        userId: '123'
    };

    public mockAuth: Auth = {
        loginAsUser: this.mockUser,
        user: this.mockUser
    };

    private watchUserFunc: Function;

    get auth(): Auth {
        return this.mockAuth;
    }

    get auth$(): Observable<Auth> {
        return Observable.of(this.mockAuth);
    }

    public watchUser(func: Function): void {
        this.watchUserFunc = func;
    }

    public tiggerWatchUser(): void {
        this.watchUserFunc(this.mockAuth);
    }
}
