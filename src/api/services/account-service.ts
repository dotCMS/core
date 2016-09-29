import {ApiRoot} from '../persistence/ApiRoot';
import {CoreWebService} from './core-web-service';
import {Http} from '@angular/http';
import {Observable} from 'rxjs/Rx';
import {RequestMethod} from '@angular/http';
import {ResponseView} from './response-view';

export class AccountService extends CoreWebService {

    constructor(apiRoot: ApiRoot, http: Http) {
        super(apiRoot, http);
    }

    public updateUser(user: AccountUser): Observable<ResponseView> {
        return this.requestView({
            body: user,
            method: RequestMethod.Put,
            url: 'v1/users/current',
        });
    }
}

export interface AccountUser {
    userId: string;
    givenName: string;
    surname: string;
    newPassword?: string;
    currentPassword: string;
    email: string;
}
