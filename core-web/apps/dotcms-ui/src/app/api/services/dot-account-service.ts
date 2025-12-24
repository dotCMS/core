import { Observable } from 'rxjs';

import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { catchError, map, take } from 'rxjs/operators';

import { DotHttpErrorManagerService } from '@dotcms/data-access';
import { DotCMSResponse } from '@dotcms/dotcms-models';

@Injectable()
export class DotAccountService {
    private http = inject(HttpClient);
    private httpErrorManagerService = inject(DotHttpErrorManagerService);

    /**
     * Updates user data
     *
     * @param {DotAccountUser} user
     * @returns {Observable<DotCMSResponse<unknown>>}
     * @memberof DotAccountService
     */
    updateUser(user: DotAccountUser): Observable<DotCMSResponse<unknown>> {
        return this.http.put<DotCMSResponse<unknown>>('/api/v1/users/current', user);
    }

    /**
     * Put request to add the getting starter portlet to menu
     *
     * @returns {Observable<string>}
     * @memberof DotAccountService
     */
    addStarterPage(): Observable<string> {
        return this.http
            .put<DotCMSResponse<string>>('/api/v1/toolgroups/gettingstarted/_addtouser', {})
            .pipe(
                take(1),
                map((response) => response.entity),
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
        return this.http
            .put<DotCMSResponse<string>>('/api/v1/toolgroups/gettingstarted/_removefromuser', {})
            .pipe(
                take(1),
                map((response) => response.entity),
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
