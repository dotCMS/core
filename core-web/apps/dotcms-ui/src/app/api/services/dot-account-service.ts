import { Observable } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { catchError, map, pluck, take } from 'rxjs/operators';

import { DotHttpErrorManagerService } from '@dotcms/data-access';
import { CoreWebService, ResponseView } from '@dotcms/dotcms-js';

@Injectable()
export class DotAccountService {
    private coreWebService = inject(CoreWebService);
    private httpErrorManagerService = inject(DotHttpErrorManagerService);

    /**
     * Updates user data
     *
     * @param {DotAccountUser} user
     * @returns {Observable<ResponseView>}
     * @memberof DotAccountService
     */
    updateUser(user: DotAccountUser): Observable<ResponseView> {
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
     * @memberof DotAccountService
     */
    addStarterPage(): Observable<string> {
        return this.coreWebService
            .requestView({
                method: 'PUT',
                url: '/api/v1/toolgroups/gettingstarted/_addtouser'
            })
            .pipe(
                take(1),
                pluck('entity'),
                catchError((error: HttpErrorResponse) => {
                    return this.httpErrorManagerService.handle(error).pipe(
                        take(1),
                        map(() => null)
                    );
                })
            );
    }

    /**
     * put request to remove the getting starter portlet to menu
     *
     * @returns {Observable<string>}
     * @memberof DotAccountService
     */
    removeStarterPage(): Observable<string> {
        return this.coreWebService
            .requestView({
                method: 'PUT',
                url: '/api/v1/toolgroups/gettingstarted/_removefromuser'
            })
            .pipe(
                take(1),
                pluck('entity'),
                catchError((error: HttpErrorResponse) => {
                    return this.httpErrorManagerService.handle(error).pipe(
                        take(1),
                        map(() => null)
                    );
                })
            );
    }
}

export interface DotAccountUser {
    userId: string;
    givenName: string;
    surname: string;
    newPassword?: string;
    currentPassword: string;
    email: string;
}
