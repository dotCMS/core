import { Observable, forkJoin, of } from 'rxjs';

import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { catchError, map, switchMap } from 'rxjs/operators';

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
 * - A non-empty `roles` list REPLACES the user's role membership
 *   entirely (see UserResource#processRoles). The FE always sends the
 *   full role list echoed back from {@link DotUsersService.getUserRoles}
 *   with Access-toggle deltas applied, so membership is preserved on
 *   every save — omitting `roles` (the alternative "don't touch" mode)
 *   is not used by this UI.
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
 * RoleView returned by the `/api/v1/roles/**` endpoints. `roleKey`
 * is what the backend UserForm expects on save — create/update look
 * each entry up via `roleAPI.loadRoleByKey(...)`. Not every role
 * carries a roleKey (per-user personal roles typically don't), which
 * the Roles tab filters out since they cannot be sent back on save.
 *
 * `roleChildren` is populated by `GET /api/v1/roles?loadChildrenRoles=true`
 * — the root-roles endpoint returns each root with its immediate
 * descendants nested. The service flattens the two levels into a
 * single list with the `parent` field set so callers can walk the
 * hierarchy with a plain parent-id lookup.
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
    roleChildren?: DotRoleView[];
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
     * We rely on this for three things:
     *   1. Hydrating the Access section toggles by checking for
     *      well-known role keys (CMS Administrator, DOTCMS_BACK_END_USER,
     *      DOTCMS_FRONT_END_USER).
     *   2. Seeding the Roles tab's Granted panel with the user's
     *      currently-granted roles.
     *   3. Preserving the user's other role memberships on save. The
     *      backend `PUT /api/v1/users` replaces the full role list, so
     *      we must send back every role key the user already had, minus
     *      the access-role keys that are now toggled off.
     *
     * Note: `/api/v1/roles/users/{id}` is served by the
     * `includeImplicitRoles = true` overload — inherited roles come
     * back alongside direct grants without a discriminator. Echoing
     * that list back into `PUT /api/v1/users` therefore promotes
     * inherited roles to direct grants, which is a big part of the
     * reason we're moving away from sending `roles` on update.
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

    /**
     * Loads every system role for the Roles tab shuttle, with the
     * full parent/child hierarchy resolved. Backend endpoints only
     * return one level of children per call, so we:
     *   1. Fetch the roots via `/api/v1/roles?loadChildrenRoles=true`
     *      → returns roots with their immediate children nested.
     *   2. Recursively fetch `/api/v1/roles/{id}?loadChildrenRoles=true`
     *      for every non-root role we've discovered so far, until a
     *      round yields no new roles.
     *
     * `_search` would be a single call but returns `SmallRoleView`
     * (no parent, no hierarchy), so a client-side tree cannot be
     * reconstructed from it. The recursive walk keeps request count
     * bounded by the number of non-root roles in the system.
     */
    getAllRoles(): Observable<DotRoleView[]> {
        return this.#http
            .get<DotRolesResponse>('/api/v1/roles', {
                params: new HttpParams().set('loadChildrenRoles', 'true')
            })
            .pipe(switchMap((response) => this.expandRoleTree(response.entity ?? [])));
    }

    private expandRoleTree(roots: DotRoleView[]): Observable<DotRoleView[]> {
        const flat: DotRoleView[] = [];
        const seen = new Set<string>();

        for (const root of roots) {
            if (!seen.has(root.id)) {
                flat.push({ ...root, roleChildren: undefined, parent: undefined });
                seen.add(root.id);
            }
            for (const child of root.roleChildren ?? []) {
                if (!seen.has(child.id)) {
                    flat.push({ ...child, roleChildren: undefined, parent: root.id });
                    seen.add(child.id);
                }
            }
        }

        const initialChildren = flat.filter((role) => role.parent);

        return this.expandDescendants(initialChildren, flat, seen);
    }

    private expandDescendants(
        toExpand: DotRoleView[],
        flat: DotRoleView[],
        seen: Set<string>
    ): Observable<DotRoleView[]> {
        if (toExpand.length === 0) {
            return of(flat);
        }

        const requests = toExpand.map((role) =>
            this.#http
                .get<{ entity: DotRoleView }>(`/api/v1/roles/${encodeURIComponent(role.id)}`, {
                    params: new HttpParams().set('loadChildrenRoles', 'true')
                })
                .pipe(
                    map((resp) => ({
                        parentId: role.id,
                        children: resp.entity.roleChildren ?? []
                    })),
                    // A failed lookup for one node shouldn't kill the whole
                    // tree — treat it as "no discovered children" and move on.
                    catchError(() => of({ parentId: role.id, children: [] as DotRoleView[] }))
                )
        );

        return forkJoin(requests).pipe(
            switchMap((results) => {
                const newlyDiscovered: DotRoleView[] = [];
                for (const { parentId, children } of results) {
                    for (const child of children) {
                        if (!seen.has(child.id)) {
                            const role: DotRoleView = {
                                ...child,
                                roleChildren: undefined,
                                parent: parentId
                            };
                            flat.push(role);
                            seen.add(child.id);
                            newlyDiscovered.push(role);
                        }
                    }
                }

                return this.expandDescendants(newlyDiscovered, flat, seen);
            })
        );
    }
}
