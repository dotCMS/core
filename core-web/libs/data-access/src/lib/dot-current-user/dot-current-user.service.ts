import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map, shareReplay } from 'rxjs/operators';

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
    readonly #http = inject(HttpClient);

    readonly #URL_CURRENT_USER = '/api/v1/users/current/';
    readonly #URL_USER_PERMISSIONS = '/api/v1/permissions/_bypermissiontype?userid={0}';
    readonly #URL_PORLET_ACCESS = '/api/v1/portlet/{0}/_doesuserhaveaccess';
    readonly #URL_MENU = '/api/v1/menu';

    #menuCache$: Observable<DotCMSResponse<Array<{ menuItems: Array<{ id: string }> }>>> | null =
        null;

    // TODO: We need to update the LoginService to get the userId in the User object
    /**
     * Get logged user and userId.
     * @returns Observable<DotCurrentUser>
     * @memberof DotCurrentUserService
     */
    getCurrentUser(): Observable<DotCurrentUser> {
        return this.#http.get<DotCurrentUser>(this.#URL_CURRENT_USER);
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
            .get<DotCMSResponse<DotPermissionsType>>(permissionsUrl)
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
                DotCMSResponse<{ response: boolean }>
            >(this.#URL_PORLET_ACCESS.replace('{0}', portletid))
            .pipe(map((res) => res.entity.response));
    }

    /**
     * Checks if a portlet is present in the current user's menu (toolgroups).
     * Unlike hasAccessToPortlet, this does not bypass for CMS Admin — if the
     * portlet is not in any of the user's toolgroups, it returns false.
     * @param portletId the portlet identifier to look up
     * @returns Observable<boolean>
     */
    isPortletInMenu(portletId: string): Observable<boolean> {
        if (!this.#menuCache$) {
            this.#menuCache$ = this.#http
                .get<DotCMSResponse<Array<{ menuItems: Array<{ id: string }> }>>>(this.#URL_MENU)
                .pipe(shareReplay(1));
        }

        return this.#menuCache$.pipe(
            map((res) =>
                res.entity.some((menu) => menu.menuItems.some((item) => item.id === portletId))
            )
        );
    }
}
