import { Observable } from 'rxjs';
import { Injectable } from '@angular/core';
import { CoreWebService, ResponseView } from 'dotcms-js';
import { take, pluck } from 'rxjs/operators';

@Injectable()
export class AccountService {
    constructor(private coreWebService: CoreWebService) {}

    /**
     * Updates user data
     *
     * @param {AccountUser} user
     * @returns {Observable<ResponseView>}
     * @memberof AccountService
     */
    updateUser(user: AccountUser): Observable<ResponseView> {
        return this.coreWebService.requestView({
            body: user,
            method: 'PUT',
            url: 'v1/users/current'
        });
    }

    /**
     * Put request to add the getting starter portlet to menu
     *
     * @returns {Observable<string>}
     * @memberof AccountService
     */
    addStarterPage(): Observable<string> {
        return this.coreWebService
            .requestView({
                method: 'PUT',
                url: '/api/v1/toolgroups/gettingstarted/_addtocurrentuser'
            })
            .pipe(take(1), pluck('entity'));
    }

    /**
     * put request to remove the getting starter portlet to menu
     *
     * @returns {Observable<string>}
     * @memberof AccountService
     */
    removeStarterPage(): Observable<string> {
        return this.coreWebService
            .requestView({
                method: 'PUT',
                url: '/api/v1/toolgroups/gettingstarted/_removefromcurrentuser'
            })
            .pipe(take(1), pluck('entity'));
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
