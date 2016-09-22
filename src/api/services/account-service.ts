import {ApiRoot} from '../persistence/ApiRoot';
import {CoreWebService} from './core-web-service';
import {Http} from '@angular/http';
import {Observable} from 'rxjs/Rx';
import {RequestMethod} from '@angular/http';
import {ResponseView} from './response-view';

export class AccountService extends CoreWebService {

    private updateAccountURL: string;

    constructor(apiRoot: ApiRoot, http: Http) {
        super(apiRoot, http);
        this.updateAccountURL = 'v1/users/current';
    }

    public updateUser(user: AccountUser): Observable<ResponseView> {
        return this.requestView({
            body: user,
            method: RequestMethod.Put,
            url: this.updateAccountURL,
        });
    }
}

export interface AccountUser{
    userId: string;
    givenName: string;
    surname: string;
    password?: string;
    email: string;
}
