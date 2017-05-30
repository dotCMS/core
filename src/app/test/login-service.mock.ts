import { User, Auth } from '../api/services/login-service';
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

    get auth(): Auth {
        return this.mockAuth;
    }

    get auth$(): Observable<Auth> {
        return Observable.of(this.mockAuth);
    }
}