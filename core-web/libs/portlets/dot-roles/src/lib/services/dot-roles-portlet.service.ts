import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

import { DotRoleUserResult } from '@dotcms/data-access';
import { DotCMSResponse } from '@dotcms/dotcms-models';

/**
 * `UserResource.filter` defaults `per_page=40` — without an explicit override
 * the Grant popover only ever offers the first 40 candidates. Keeps realistic
 * dotCMS installs whole without hammering the endpoint.
 */
export const USER_FILTER_PAGE_SIZE = 500;

/**
 * What is left of this portlet's own data access after the roles surface moved
 * to the shared `DotRolesService`: one user-search call.
 *
 * `/v1/users/filter` is a **users** endpoint, not a roles one, so it does not
 * belong in `DotRolesService`. It lives here until there is a shared users
 * service in `data-access` to host it — `dot-users` has its own
 * `DotUsersService`, but portlet-to-portlet imports are not allowed.
 */
@Injectable({ providedIn: 'root' })
export class DotRolesPortletService {
    readonly #http = inject(HttpClient);

    /**
     * GET /v1/users/filter?query=X — free-text user search for the Grant
     * popover. An empty `query` returns the first page, which seeds the picker
     * when it opens.
     */
    searchUsers(query: string): Observable<DotRoleUserResult[]> {
        const params = new URLSearchParams({ per_page: String(USER_FILTER_PAGE_SIZE) });
        if (query) {
            params.set('query', query);
        }

        return this.#http
            .get<DotCMSResponse<DotRoleUserResult[]>>(`/api/v1/users/filter?${params}`)
            .pipe(map((response) => response.entity ?? []));
    }
}
