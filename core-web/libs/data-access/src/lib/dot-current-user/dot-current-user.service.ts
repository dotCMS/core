import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map, take } from 'rxjs/operators';

import {
    DotCMSResponse,
    DotCurrentUser,
    DotPermissionsType,
    PermissionsType,
    UserPermissions
} from '@dotcms/dotcms-models';
import { formatMessage } from '@dotcms/utils';

@Injectable()
export class DotCurrentUserService {
    private http = inject(HttpClient);

    private currentUsersUrl = '/api/v1/users/current/';
    private userPermissionsUrl = '/api/v1/permissions/_bypermissiontype?userid={0}';
    private porletAccessUrl = '/api/v1/portlet/{0}/_doesuserhaveaccess';

    // TODO: We need to update the LoginService to get the userId in the User object
    /**
     * Get logged user and userId.
     * @returns Observable<DotCurrentUser>
     * @memberof DotCurrentUserService
     */
    getCurrentUser(): Observable<DotCurrentUser> {
        return this.http.get<DotCurrentUser>(this.currentUsersUrl);
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

        return this.http.get<DotCMSResponse<DotPermissionsType>>(permissionsUrl).pipe(
            take(1),
            map((response) => response.entity)
        );
    }

    /**
     * Verifies if current User has access to a specific portlet Id
     * @param string portletid
     * @returns Observable<boolean>
     * @memberof DotCurrentUserService
     */
    hasAccessToPortlet(portletid: string): Observable<boolean> {
        return this.http
            .get<
                DotCMSResponse<{ response: boolean }>
            >(this.porletAccessUrl.replace('{0}', portletid))
            .pipe(
                take(1),
                map((response) => response.entity.response)
            );
    }
}
