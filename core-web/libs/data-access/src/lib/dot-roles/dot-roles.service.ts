import { Observable } from 'rxjs';

import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map, switchMap } from 'rxjs/operators';

import { DotCMSResponse, DotRole, DotRoleFormValue, DotToolGroup } from '@dotcms/dotcms-models';

import {
    LegacyRoleSearchResponse,
    sanitizeRoleForm,
    unwrapLegacySearchNode
} from './dot-roles.adapters';

import { DotMessageService } from '../dot-messages/dot-messages.service';

const CURRENT_USER_KEY = 'CMS Anonymous';

/**
 * Page size requested per role when composing an effective member list.
 *
 * `GET /v1/roles/{roleId}/users` is server-paged and defaults to
 * `per_page=40`, but callers rendering *effective* membership need each role
 * whole: the union of a role's direct grants and everything inherited from its
 * ancestors is assembled client-side, and a server page of one ancestor is not
 * a page of that union.
 *
 * A deliberate ceiling, not a bridge: only an endpoint that resolves
 * inheritance server-side could lift it, which #37070 explicitly declined in
 * order to keep this a pure "direct members" resource.
 */
export const ROLE_MEMBERS_PAGE_SIZE = 500;

/** Wire response for DELETE /v1/roles/{roleId} — matches `RoleDeletionView`. */
export interface DotRoleDeletionResult {
    readonly deleted: boolean;
    readonly roleId: string;
    /** How many users had the role at the moment of the (cascading) deletion. */
    readonly usersAffected: number;
}

/** Minimal user payload returned by grant / member endpoints. */
export interface DotRoleMemberUser {
    readonly userId: string;
    readonly email?: string;
    readonly fullName?: string;
}

/** Wire response for POST /v1/roles/{roleId}/users/{userId} — `RoleUserGrantView`. */
export interface DotRoleUserGrantResult {
    readonly granted: boolean;
    readonly roleId: string;
    readonly user: DotRoleMemberUser;
}

/** Per-user skip entry from the bulk-removal response. */
export interface DotRoleUsersRemovalSkip {
    readonly userId: string;
    /** `not_found` | `inherited` | `error` — mirrors `SkippedUserView` constants. */
    readonly reason: 'not_found' | 'inherited' | 'error';
}

/** Wire response for DELETE /v1/roles/{roleId}/users — `RoleUsersRemovalView`. */
export interface DotRoleUsersRemovalResult {
    readonly removedUserIds: string[];
    readonly skipped: DotRoleUsersRemovalSkip[];
}

/** Standard dotCMS user row, as returned by the role members endpoint. */
export interface DotRoleUserResult {
    readonly userId: string;
    readonly firstName?: string;
    readonly lastName?: string;
    readonly emailAddress?: string;
}

@Injectable({
    providedIn: 'root'
})
export class DotRolesService {
    private dotMessageService = inject(DotMessageService);
    private http = inject(HttpClient);

    /**
     * Return list of roles associated to specific role .
     * @param {string} roleId
     * @returns Observable<DotRole[]>
     * @memberof DotRolesService
     */
    get(roleId: string, roleHierarchy: boolean): Observable<DotRole[]> {
        return this.http
            .get<
                DotCMSResponse<DotRole[]>
            >(`/api/v1/roles/${roleId}/rolehierarchyanduserroles?roleHierarchyForAssign=${roleHierarchy}`)
            .pipe(
                map((response) => response.entity),
                map(this.processRolesResponse.bind(this))
            );
    }

    /**
     * Return list of roles.
     * @returns Observable<DotRole[]>
     * @memberof DotRolesService
     */
    search(): Observable<DotRole[]> {
        return this.http.get<DotCMSResponse<DotRole[]>>('/api/v1/roles/_search').pipe(
            map((response) => response.entity),
            map(this.processRolesResponse.bind(this))
        );
    }

    /**
     * GET /v1/roles?loadChildrenRoles= — the root roles of the hierarchy.
     *
     * With `loadChildren` the response nests each root's direct children,
     * but only **two levels** are hydrated per request: grandchildren come
     * back with an empty `roleChildren` even when they exist. Callers that
     * need deeper levels compose that themselves on top of {@link getById} —
     * lazily per expansion for a tree view, eagerly for a picker that needs
     * the whole hierarchy up front. That strategy is a consumer concern and
     * lives in the consuming lib, not here.
     */
    getRoots(loadChildren = true): Observable<DotRole[]> {
        return this.http
            .get<DotCMSResponse<DotRole[]>>('/api/v1/roles', {
                params: new HttpParams().set('loadChildrenRoles', String(loadChildren))
            })
            .pipe(map((response) => response.entity ?? []));
    }

    /**
     * GET /v1/roles/{roleId}?loadChildrenRoles= — a single role.
     *
     * Same two-level hydration caveat as {@link getRoots}.
     */
    getById(roleId: string, loadChildren = false): Observable<DotRole> {
        return this.http
            .get<
                DotCMSResponse<DotRole>
            >(`/api/v1/roles/${encodeURIComponent(roleId)}`, { params: new HttpParams().set('loadChildrenRoles', String(loadChildren)) })
            .pipe(map((response) => response.entity));
    }

    /**
     * GET /v1/roles/users/{userIdOrEmail} — every role the user holds.
     *
     * Served by the `includeImplicitRoles = true` overload, so inherited
     * roles come back alongside direct grants with no discriminator between
     * them. Echoing this list into `PUT /v1/users` promotes inherited roles
     * to direct grants — callers doing a save must account for that.
     */
    getForUser(userIdOrEmail: string): Observable<DotRole[]> {
        return this.http
            .get<
                DotCMSResponse<DotRole[]>
            >(`/api/v1/roles/users/${encodeURIComponent(userIdOrEmail)}`)
            .pipe(map((response) => response.entity ?? []));
    }

    /**
     * GET /v1/roles/layouts — every tool group (backend `Layout`) in the
     * system, each enriched with `portletTitles`: the localized, human-readable
     * names of the portlets in `portletIds`, resolved server-side.
     *
     * Despite the path, this is a system-wide catalog, not a per-role read.
     * It is the only endpoint that returns the titles, so anything displaying
     * tool groups reads them from here.
     */
    getAllToolGroups(): Observable<DotToolGroup[]> {
        return this.http
            .get<DotCMSResponse<DotToolGroup[]>>('/api/v1/roles/layouts')
            .pipe(map((response) => response.entity ?? []));
    }

    /**
     * GET /v1/roles/{roleId}/layouts — the tool groups granted **directly** to
     * the role.
     *
     * Direct grants only: the backend resolves this as
     * `from LayoutsRoles where role_id = ?` with no hierarchy walk
     * (`RoleFactoryImpl#loadLayoutIdsForRole`), so effective grants have to be
     * composed by the caller across the ancestor chain — the same shape as
     * {@link getForUser} and the role members endpoint.
     *
     * Items are raw `Layout` objects and carry no `portletTitles`; pair this
     * with {@link getAllToolGroups} for anything rendered.
     */
    getToolGroups(roleId: string): Observable<DotToolGroup[]> {
        return this.http
            .get<
                DotCMSResponse<DotToolGroup[]>
            >(`/api/v1/roles/${encodeURIComponent(roleId)}/layouts`)
            .pipe(map((response) => response.entity ?? []));
    }

    /**
     * POST /v1/roles/layouts — set the tool groups granted to a role.
     *
     * A **full replace** of the role's direct grants, not an append: the
     * backend diffs `toolGroupIds` against what the role currently has, drops
     * the difference and adds the rest (`RoleHelper#saveRoleLayouts`). Callers
     * must send the complete set they want the role to end up with.
     *
     * Only direct grants belong in the payload. Echoing an inherited grant
     * here would silently promote it to a direct grant on this role.
     */
    saveToolGroups(roleId: string, toolGroupIds: string[]): Observable<unknown> {
        return this.http.post('/api/v1/roles/layouts', {
            roleId,
            layoutIds: toolGroupIds
        });
    }

    /**
     * GET /api/role/loadbyname/name/{query}/ — deep search over the hierarchy,
     * returning the full ancestor path of every role whose name matches
     * (case-insensitive substring), so a tree can render matches with their
     * branches expanded. Client-side filtering only sees roles already in
     * memory, which misses unloaded grandchildren.
     *
     * Pre-v1 and marked `deprecated=true` on `RoleResource.loadByName`, but it
     * is the only REST surface today that does the deep search. Replacing it
     * with `GET /v1/roles?search=` is a backend follow-up; kept isolated behind
     * this method so the eventual swap is one file.
     *
     * Named `searchTree` because {@link search} is already taken by the
     * `_search` endpoint, which returns a flat `SmallRoleView` with no parent.
     */
    searchTree(query: string): Observable<DotRole[]> {
        const url = `/api/role/loadbyname/name/${encodeURIComponent(query)}/`;

        return this.http.get<LegacyRoleSearchResponse>(url).pipe(
            map((response) => {
                const items = response?.items ?? [];
                if (items.length === 0) {
                    return [];
                }

                // Drop the synthetic "Roles" wrapper node the legacy
                // serializer puts at the root.
                return (items[0]?.children ?? []).map(unwrapLegacySearchNode);
            })
        );
    }

    /**
     * GET /v1/roles/{roleId}/users — users **directly** granted the role.
     *
     * Inheritance is deliberately out of scope: the endpoint answers "who is
     * directly granted this role" only. Callers needing effective membership
     * walk the ancestor chain themselves — see {@link ROLE_MEMBERS_PAGE_SIZE}
     * for why each role is pulled whole.
     */
    getUsers(roleId: string): Observable<DotRoleUserResult[]> {
        const url =
            `/api/v1/roles/${encodeURIComponent(roleId)}/users` +
            `?per_page=${ROLE_MEMBERS_PAGE_SIZE}`;

        return this.http
            .get<DotCMSResponse<DotRoleUserResult[]>>(url)
            .pipe(map((response) => response.entity ?? []));
    }

    /**
     * POST /v1/roles — create a role.
     *
     * The `cms_role.role_key` column has a UNIQUE constraint and legacy roles
     * ship with `role_key = ''`, so posting an empty string reliably trips a
     * duplicate-key violation from Postgres. {@link sanitizeRoleForm} strips
     * empty optional fields to `undefined` so JSON.stringify omits them and the
     * backend persists NULL instead.
     */
    create(form: DotRoleFormValue): Observable<DotRole> {
        return this.http
            .post<DotCMSResponse<DotRole>>('/api/v1/roles', sanitizeRoleForm(form))
            .pipe(map((response) => response.entity));
    }

    /**
     * PUT /v1/roles/{roleId} — update an existing role.
     *
     * A **full replace**: same body shape as {@link create}, and an omitted
     * field is cleared rather than preserved. `parentRoleId = null` reparents
     * to root (the backend sets `role.setParent(role.getId())`).
     *
     * Backend error semantics, for callers deciding what to surface:
     *   400 → cycle in hierarchy (reparent under a descendant)
     *   403 → system/locked role, or admin gate
     *   404 → role or parent role not found
     *   409 → duplicate `roleKey`, or duplicate name under the same parent
     */
    update(roleId: string, form: DotRoleFormValue): Observable<DotRole> {
        return this.http
            .put<
                DotCMSResponse<DotRole>
            >(`/api/v1/roles/${encodeURIComponent(roleId)}`, sanitizeRoleForm(form))
            .pipe(map((response) => response.entity));
    }

    /**
     * DELETE /v1/roles/{roleId} — delete a role.
     *
     * Cascading: `usersAffected` reports how many users held the role when it
     * went away (their assignments were dropped). Permissions granted to the
     * role and its tool-group assignments are removed too. Rejected for:
     *   403 → system or locked roles
     *   404 → role not found
     *   409 → role has children, or a workflow action references it
     *
     * A 200 with `deleted: false` means the backend accepted the request but
     * declined to delete — check the flag, do not assume success from the
     * status code.
     */
    delete(roleId: string): Observable<DotRoleDeletionResult> {
        return this.http
            .delete<
                DotCMSResponse<DotRoleDeletionResult>
            >(`/api/v1/roles/${encodeURIComponent(roleId)}`)
            .pipe(map((response) => response.entity));
    }

    /**
     * POST /v1/roles/{roleId}/users/{userId} — grant the role to a user.
     *
     * Idempotent by design: returns `granted: true` even when the user already
     * held the role, directly or by inheritance, so retries are safe. The user
     * payload is deliberately slim (id / email / fullName).
     *
     *   403 → the role's `editUsers` flag is false (workflow / system roles)
     *   404 → role or user not found
     */
    grantUser(roleId: string, userId: string): Observable<DotRoleUserGrantResult> {
        const url = `/api/v1/roles/${encodeURIComponent(roleId)}/users/${encodeURIComponent(
            userId
        )}`;

        return this.http
            .post<DotCMSResponse<DotRoleUserGrantResult>>(url, null)
            .pipe(map((response) => response.entity));
    }

    /**
     * DELETE /v1/roles/{roleId}/users — bulk-remove members.
     *
     * Partial success is the normal case: `removedUserIds` holds the users
     * whose direct membership went away, `skipped` holds the rest with a
     * reason (`not_found` / `inherited` / `error`). The batch never fails as a
     * whole once the role resolves, so callers must ALWAYS read both arrays —
     * a 200 does not mean everything was removed.
     *
     * Angular's `HttpClient.delete` needs the body under `options.body`.
     */
    removeUsers(roleId: string, userIds: string[]): Observable<DotRoleUsersRemovalResult> {
        return this.http
            .delete<
                DotCMSResponse<DotRoleUsersRemovalResult>
            >(`/api/v1/roles/${encodeURIComponent(roleId)}/users`, { body: { userIds } })
            .pipe(map((response) => response.entity));
    }

    /**
     * Move a role under a new parent, or to root with `null`.
     *
     * Reads the role first because {@link update} is a full replace: without
     * re-sending `roleKey`, `description` and the edit flags, a reparent would
     * silently wipe them.
     */
    reparent(roleId: string, newParentId: string | null): Observable<DotRole> {
        return this.getById(roleId, false).pipe(
            switchMap((role) =>
                this.update(roleId, {
                    roleName: role.name,
                    roleKey: role.roleKey,
                    parentRoleId: newParentId,
                    canEditUsers: role.editUsers ?? true,
                    canEditPermissions: role.editPermissions ?? true,
                    canEditLayouts: role.editLayouts ?? true,
                    description: role.description
                })
            )
        );
    }

    private processRolesResponse(roles: DotRole[]): DotRole[] {
        return roles
            .filter((role: DotRole) => role.roleKey !== 'anonymous')
            .map((role: DotRole) => {
                if (role.roleKey === CURRENT_USER_KEY) {
                    role.name = this.dotMessageService.get('current-user');
                } else if (role.user) {
                    role.name = `${role.name}`;
                }

                return role;
            });
    }
}
