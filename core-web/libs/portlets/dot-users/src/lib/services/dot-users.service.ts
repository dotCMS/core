import { Observable } from 'rxjs';

import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { DotCMSAPIResponse } from '@dotcms/dotcms-models';

/**
 * Row shape returned by GET /api/v1/users/filter — mirrors the fields
 * populated by com.liferay.portal.model.User#toMap() on the backend.
 * Kept intentionally partial: only the fields consumed by the list view
 * are declared.
 */
export interface DotUserListItem {
    userId: string;
    id: string;
    firstName: string;
    lastName: string;
    fullName: string;
    name: string;
    emailAddress: string;
    gravitar: string;
    active: boolean;
    admin: boolean;
    backendUser: boolean;
    frontendUser: boolean;
    hasConsoleAccess: boolean;
    lastLoginDate: number | null;
    lastLoginIP: string | null;
    failedLoginAttempts: number | null;
}

export interface DotUsersPaginatedParams {
    filter?: string;
    page?: number;
    perPage?: number;
    orderBy?: string;
    direction?: 'ASC' | 'DESC';
    includeAnonymous?: boolean;
    includeDefault?: boolean;
    /**
     * System role key to constrain the result set to users who hold that role
     * (e.g. `DOTCMS_BACK_END_USER`, `DOTCMS_FRONT_END_USER`). The backend
     * accepts it repeatably or as a comma-separated list; the FE currently
     * uses a single value from the "Filter by" chip.
     */
    roleKey?: string;
}

@Injectable({ providedIn: 'root' })
export class DotUsersService {
    readonly #http = inject(HttpClient);

    getUsersPaginated(
        params: DotUsersPaginatedParams
    ): Observable<DotCMSAPIResponse<DotUserListItem[]>> {
        let httpParams = new HttpParams();

        if (params.filter) {
            httpParams = httpParams.set('query', params.filter);
        }
        if (params.page !== undefined) {
            httpParams = httpParams.set('page', params.page.toString());
        }
        if (params.perPage !== undefined) {
            httpParams = httpParams.set('per_page', params.perPage.toString());
        }
        if (params.orderBy) {
            httpParams = httpParams.set('orderby', params.orderBy);
        }
        if (params.direction) {
            httpParams = httpParams.set('direction', params.direction);
        }
        if (params.includeAnonymous) {
            httpParams = httpParams.set('includeanonymous', 'true');
        }
        if (params.includeDefault) {
            httpParams = httpParams.set('includedefault', 'true');
        }
        if (params.roleKey) {
            httpParams = httpParams.set('roleKey', params.roleKey);
        }

        return this.#http.get<DotCMSAPIResponse<DotUserListItem[]>>('/api/v1/users/filter', {
            params: httpParams
        });
    }

    deleteUser(userId: string, replacementUserId?: string): Observable<unknown> {
        let params = new HttpParams();
        if (replacementUserId) {
            params = params.set('replacementUserId', replacementUserId);
        }

        return this.#http.delete(`/api/v1/users/${encodeURIComponent(userId)}`, { params });
    }
}
