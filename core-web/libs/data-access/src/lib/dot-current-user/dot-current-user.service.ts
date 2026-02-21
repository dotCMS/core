import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

import {
    DotCMSAPIResponse,
    DotCurrentUser,
    DotPermissionsType,
    PermissionsType,
    UserPermissions
} from '@dotcms/dotcms-models';
import { formatMessage } from '@dotcms/utils';
@Injectable()
export class DotCurrentUserService {
    readonly #http = inject(HttpClient);

    readonly #URL_CURRENT_USER = '/api/v1/users/current/';
    readonly #URL_USER_PERMISSIONS = '/api/v1/permissions/_bypermissiontype?userid={0}';
    readonly #URL_PORLET_ACCESS = '/api/v1/portlet/{0}/_doesuserhaveaccess';

    // TODO: We need to update the LoginService to get the userId in the User object
    /**
     * Get logged user and userId.
     * @returns Observable<DotCurrentUser>
     * @memberof DotCurrentUserService
     */
    getCurrentUser(): Observable<DotCurrentUser> {
        return this.#http
            .get<DotCurrentUser>(this.#URL_CURRENT_USER)
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
            ? `${this.#URL_USER_PERMISSIONS}&permission={1}`
            : this.#URL_USER_PERMISSIONS;
        url = permissionsType.length ? `${url}&permissiontype={2}` : url;

        const permissionsUrl = formatMessage(url, [
            userId,
            permissions.join(','),
            permissionsType.join(',')
        ]);

        return this.#http
            .get<DotCMSAPIResponse<DotPermissionsType>>(permissionsUrl)
            .pipe(map((res) => res.entity));
    }

    /**
     * Verifies if current User has access to a specific portlet Id
     * @param string portletid
     * @returns Observable<boolean>
     * @memberof DotCurrentUserService
     */
    hasAccessToPortlet(portletid: string): Observable<boolean> {
        return this.#http
            .get<
                DotCMSAPIResponse<{ response: boolean }>
            >(this.#URL_PORLET_ACCESS.replace('{0}', portletid))
            .pipe(map((res) => res.entity.response));
    }
}
