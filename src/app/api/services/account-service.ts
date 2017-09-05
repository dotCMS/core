import { CoreWebService, ResponseView } from 'dotcms-js/dotcms-js';
import { Observable } from 'rxjs/Rx';
import { RequestMethod } from '@angular/http';
import { Injectable } from '@angular/core';

@Injectable()
export class AccountService {
    constructor(private coreWebService: CoreWebService) {}

    public updateUser(user: AccountUser): Observable<ResponseView> {
        return this.coreWebService.requestView({
            body: user,
            method: RequestMethod.Put,
            url: 'v1/users/current'
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
