import { Observable } from 'rxjs';

import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

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

/**
 * Full user payload returned by GET /api/v1/users/{userId}. Includes
 * every field User#toMap() populates — additional info, birthday, etc.
 * — which are absent from the list response. `additionalInfo` is a
 * free-form map keyed by whatever the caller decided to store; the
 * dialog treats `prefix`, `suffix`, `title`, `company`, `website` as
 * conventional keys.
 */
export interface DotUserDetail extends DotUserListItem {
    birthday: string | null;
    middleName: string | null;
    nickname: string | null;
    languageId: string | null;
    timeZoneId: string | null;
    male: boolean | null;
    female: boolean | null;
    additionalInfo: Record<string, unknown> | null;
    createDate: number | null;
    modificationDate: number | null;
}

/**
 * Payload accepted by both POST /api/v1/users (create) and PUT
 * /api/v1/users (update). Mirrors com.dotcms.rest.api.v1.user.UserForm.
 *
 * Contract notes:
 * - Create requires `password`; update leaves it optional and skips the
 *   password mutation when omitted.
 * - Omitting `roles` on update means "do not touch role membership".
 *   A non-empty `roles` list REPLACES the user's role membership
 *   entirely (see UserResource#processRoles).
 */
export interface DotUserFormPayload {
    userId?: string;
    firstName: string;
    lastName: string;
    email: string;
    active: boolean;
    password?: string;
    middleName?: string;
    nickName?: string;
    birthday?: string;
    languageId?: string;
    timeZoneId?: string;
    male?: boolean;
    additionalInfo?: Record<string, unknown>;
    roles?: string[];
}

interface DotUserDetailResponse {
    entity: {
        userId: string;
        user: DotUserDetail;
        roleId: string;
    };
}

interface DotUserUpdateResponse {
    entity: {
        userId: string;
        user: DotUserDetail;
        roleId?: string;
    };
}

/**
 * RoleView returned by `GET /api/v1/roles/users/{userIdOrEmail}`. The
 * `roleKey` field is what UserForm on the backend consumes when we
 * send `roles: [...]` on save — create/update call
 * `roleAPI.loadRoleByKey(...)` on each entry.
 */
export interface DotRoleView {
    id: string;
    name?: string;
    description?: string;
    roleKey?: string;
    parent?: string;
    editPermissions?: boolean;
    editUsers?: boolean;
    editLayouts?: boolean;
    locked?: boolean;
    system?: boolean;
    dbfqn?: string;
    fqn?: string;
}

interface DotRolesResponse {
    entity: DotRoleView[];
}

interface DotToolgroupStateResponse {
    entity: {
        message?: boolean;
    };
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
        const paramMap: Array<[string, string | undefined]> = [
            ['query', params.filter],
            ['page', params.page?.toString()],
            ['per_page', params.perPage?.toString()],
            ['orderby', params.orderBy],
            ['direction', params.direction],
            ['includeanonymous', params.includeAnonymous ? 'true' : undefined],
            ['includedefault', params.includeDefault ? 'true' : undefined],
            ['roleKey', params.roleKey]
        ];

        const httpParams = paramMap.reduce(
            (acc, [key, value]) => (value ? acc.set(key, value) : acc),
            new HttpParams()
        );

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

    /**
     * Fetches the full user payload used to hydrate the edit dialog.
     * The list response omits `additionalInfo`, `birthday`, and other
     * profile-only fields, so the dialog must re-fetch even when it
     * already has a `DotUserListItem` from the row click.
     */
    getUser(userId: string): Observable<DotUserDetail> {
        return this.#http
            .get<DotUserDetailResponse>(`/api/v1/users/${encodeURIComponent(userId)}`)
            .pipe(map((response) => response.entity.user));
    }

    createUser(payload: DotUserFormPayload): Observable<DotUserDetail> {
        return this.#http
            .post<DotUserUpdateResponse>('/api/v1/users', payload)
            .pipe(map((response) => response.entity.user));
    }

    updateUser(payload: DotUserFormPayload): Observable<DotUserDetail> {
        return this.#http
            .put<DotUserUpdateResponse>('/api/v1/users', payload)
            .pipe(map((response) => response.entity.user));
    }

    /**
     * Returns every role currently assigned to a user (explicit + system).
     * We rely on this for two things:
     *   1. Hydrating the Access section toggles by checking for
     *      well-known role keys (CMS Administrator, DOTCMS_BACK_END_USER,
     *      DOTCMS_FRONT_END_USER).
     *   2. Preserving the user's other role memberships on save. The
     *      backend `PUT /api/v1/users` replaces the full role list, so
     *      we must send back every role key the user already had, minus
     *      the access-role keys that are now toggled off.
     */
    getUserRoles(userIdOrEmail: string): Observable<DotRoleView[]> {
        return this.#http
            .get<DotRolesResponse>(`/api/v1/roles/users/${encodeURIComponent(userIdOrEmail)}`)
            .pipe(map((response) => response.entity ?? []));
    }

    /**
     * Reads whether the `gettingstarted` layout is assigned to a user.
     * The legacy admin UI ties the "Show Getting Started" checkbox to
     * this same layout via `_addtouser` / `_removefromuser`; we mirror
     * that semantic instead of introducing a new field.
     */
    getGettingStartedState(userId: string): Observable<boolean> {
        return this.#http
            .get<DotToolgroupStateResponse>('/api/v1/toolgroups/gettingstarted/_userHasLayout', {
                params: new HttpParams().set('userid', userId)
            })
            .pipe(map((response) => !!response.entity?.message));
    }

    /**
     * Adds or removes the `gettingstarted` layout on the target user.
     * Independent of `PUT /api/v1/users` — mirrors the two legacy
     * endpoints used by view_users_js_inc.jsp.
     */
    setGettingStarted(userId: string, enabled: boolean): Observable<unknown> {
        const url = enabled
            ? '/api/v1/toolgroups/gettingstarted/_addtouser'
            : '/api/v1/toolgroups/gettingstarted/_removefromuser';

        return this.#http.put(url, null, {
            params: new HttpParams().set('userid', userId)
        });
    }
}
