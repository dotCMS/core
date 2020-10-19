import { Injectable } from '@angular/core';
import { CoreWebService } from 'dotcms-js';
import { Observable } from 'rxjs';
import { DotCurrentUser } from '@models/dot-current-user/dot-current-user';
import { map, pluck, take } from 'rxjs/operators';

@Injectable()
export class DotCurrentUserService {
    private currentUsersUrl = 'v1/users/current/';
    private porletAccessUrl = 'v1/portlet/{0}/_doesuserhaveaccess';

    constructor(private coreWebService: CoreWebService) {}

    // TODO: We need to update the LoginService to get the userId in the User object
    /**
     * Get logged user and userId.
     * @returns Observable<DotCurrentUser>
     * @memberof DotCurrentUserService
     */
    getCurrentUser(): Observable<DotCurrentUser> {
        return this.coreWebService.request({
            url: this.currentUsersUrl
        }).pipe(map((res: any) => <DotCurrentUser>res));
    }

    /**
     * Verifies if current User has access to a specific portlet Id
     * @param string portletid
     * @returns Observable<boolean>
     * @memberof DotCurrentUserService
     */
    hasAccessToPortlet(portletid: string): Observable<boolean> {
        return this.coreWebService
            .requestView({
                url: this.porletAccessUrl.replace('{0}', portletid)
            })
            .pipe(take(1), pluck('entity', 'response'));
    }
}
