import { Injectable } from '@angular/core';
import { CoreWebService } from 'dotcms-js/dotcms-js';
import { Observable } from 'rxjs/Observable';
import { DotCurrentUser } from '@models/dot-current-user/dot-current-user';
import { RequestMethod } from '@angular/http';

@Injectable()
export class DotCurrentUserService {
    private currentUsersUrl = 'v1/users/current/';

    constructor(private coreWebService: CoreWebService) {}

    // TODO: We need to update the LoginService to get the userId in the User object
    /**
     * Get logged user and userId.
     * @returns {Observable<DotCurrentUser>}
     * @memberof DotCurrentUserService
     */
    getCurrentUser(): Observable<DotCurrentUser> {
        return this.coreWebService.request({
            method: RequestMethod.Get,
            url: this.currentUsersUrl
        });
    }
}
