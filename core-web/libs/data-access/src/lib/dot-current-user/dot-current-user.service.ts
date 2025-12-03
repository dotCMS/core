import { Observable } from 'rxjs';

import { Injectable, inject } from '@angular/core';

import { map, take } from 'rxjs/operators';

import { CoreWebService } from '@dotcms/dotcms-js';
import {
    DotCurrentUser,
    DotPermissionsType,
    PermissionsType,
    UserPermissions
} from '@dotcms/dotcms-models';
import { formatMessage } from '@dotcms/utils';
@Injectable()
export class DotCurrentUserService {
    private coreWebService = inject(CoreWebService);

    private currentUsersUrl = 'v1/users/current/';
    private userPermissionsUrl = 'v1/permissions/_bypermissiontype?userid={0}';
    private porletAccessUrl = 'v1/portlet/{0}/_doesuserhaveaccess';

    // TODO: We need to update the LoginService to get the userId in the User object
    /**
     * Get logged user and userId.
     * @returns Observable<DotCurrentUser>
     * @memberof DotCurrentUserService
     */
    getCurrentUser(): Observable<DotCurrentUser> {
        return this.coreWebService
            .request<DotCurrentUser>({
                url: this.currentUsersUrl
            })
            .pipe(map((res: DotCurrentUser) => res));
    }

    /**
     * Returns User portlet permissions data
     * @param string userId
     * @param UserPermissions[] permissions
     * @param PermissionsType[] permissionsType
     * @returns Observable<DotPermissionsType>
     * @memberof DotCurrentUserService
     */
    getUserPermissions(
        userId: string,
        permissions: UserPermissions[] = [],
        permissionsType: PermissionsType[] = []
    ): Observable<DotPermissionsType> {
        let url = permissions.length
            ? `${this.userPermissionsUrl}&permission={1}`
            : this.userPermissionsUrl;
        url = permissionsType.length ? `${url}&permissiontype={2}` : url;

        const permissionsUrl = formatMessage(url, [
            userId,
            permissions.join(','),
            permissionsType.join(',')
        ]);

        return this.coreWebService
            .requestView({
                url: permissionsUrl
            })
            .pipe(
                take(1),
                map((x) => x?.entity)
            );
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
            .pipe(
                take(1),
                map((x) => x?.entity?.response)
            );
    }
}
