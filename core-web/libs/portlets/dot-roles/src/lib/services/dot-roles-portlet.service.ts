import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map, switchMap } from 'rxjs/operators';

import { DotCMSResponse } from '@dotcms/dotcms-models';

import { DotRoleDetail, DotRoleFormValue, DotRoleNode } from '../models/dot-roles.models';

/**
 * User row shape returned by `/v1/users/filter?roleKey=X`. The endpoint
 * accepts `roleKey` (not `roleId`) as the filter parameter. Same wire
 * format used by the dot-users portlet.
 */
export interface DotRoleUserFilterResult {
    readonly userId: string;
    readonly firstName?: string;
    readonly lastName?: string;
    readonly emailAddress?: string;
}

/** Row shape from `/v1/roles/{roleId}/rolehierarchyanduserroles`. */
interface RoleHierarchyEntry {
    readonly id: string;
    readonly name?: string;
    readonly roleKey?: string;
    readonly user?: boolean;
}

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

/**
 * Portlet-scoped data access for the Roles and Tools Angular Beta.
 *
 * Blocked write flows (Edit / Delete role, Grant / Remove user) are called
 * out in the individual methods; they resolve to the dedicated v1 endpoints
 * once #36936, #36937, #36938, #36939 land.
 */
@Injectable()
export class DotRolesPortletService {
    #http = inject(HttpClient);

    /**
     * GET /v1/roles — root roles + their direct children.
     * The backend only fills 2 levels; deeper levels are lazy-loaded per
     * expand via `loadRoleById`.
     */
    loadRootRoles(loadChildren = true): Observable<DotRoleNode[]> {
        return this.#http
            .get<DotCMSResponse<DotRoleNode[]>>(`/api/v1/roles?loadChildrenRoles=${loadChildren}`)
            .pipe(map((response) => response.entity ?? []));
    }

    /**
     * GET /v1/roles/{roleId} — role detail with its direct children.
     * Used both to populate the role detail area on selection and to
     * lazy-load grandchildren when the roles tree expands a node.
     */
    loadRoleById(roleId: string, loadChildren = true): Observable<DotRoleDetail> {
        return this.#http
            .get<
                DotCMSResponse<DotRoleDetail>
            >(`/api/v1/roles/${roleId}?loadChildrenRoles=${loadChildren}`)
            .pipe(map((response) => response.entity));
    }

    /**
     * GET /v1/users/filter?roleKey=X — users granted the given role.
     *
     * Fast path (used when the selected role has a `roleKey`). Returns
     * users with email/name in a single call. The endpoint requires a
     * `roleKey` — roles created via the UI without one need the id-based
     * fallback below.
     */
    loadRoleMembersByKey(roleKey: string): Observable<DotRoleUserFilterResult[]> {
        const url = `/api/v1/users/filter?roleKey=${encodeURIComponent(roleKey)}`;

        return this.#http
            .get<DotCMSResponse<DotRoleUserFilterResult[]>>(url)
            .pipe(map((response) => response.entity ?? []));
    }

    /**
     * GET /v1/roles/{roleId}/rolehierarchyanduserroles — fallback used when
     * the selected role has no `roleKey`. The endpoint returns a mixed list
     * of Role objects: the role itself (or its ancestors when
     * `roleHierarchyForAssign=true`), and one entry per user assigned,
     * where `role.user === true` and `role.roleKey` holds the userId.
     *
     * We filter for user-roles and map to `DotRoleUserFilterResult`. The
     * response does not carry email — the endpoint returns Role objects,
     * not User objects — so members loaded via this path show empty email.
     *
     * TODO: retire this fallback and the ancestor-walk fan-out in the store
     * once `GET /v1/roles/{roleId}/users` ships (issue #37070). That
     * endpoint will return `List<RoleMemberView>` with email + granted-from
     * metadata in a single call, replacing this whole flow.
     */
    loadRoleMembersById(roleId: string): Observable<DotRoleUserFilterResult[]> {
        const url = `/api/v1/roles/${encodeURIComponent(
            roleId
        )}/rolehierarchyanduserroles?roleHierarchyForAssign=false`;

        return this.#http.get<DotCMSResponse<RoleHierarchyEntry[]>>(url).pipe(
            map((response) => {
                const entries = response.entity ?? [];
                return entries
                    .filter((entry) => entry.user === true)
                    .map((entry) => {
                        const [firstName = '', ...rest] = (entry.name ?? '').split(' ');
                        return {
                            userId: entry.roleKey ?? entry.id,
                            firstName,
                            lastName: rest.join(' '),
                            emailAddress: ''
                        };
                    });
            })
        );
    }

    /**
     * POST /v1/roles — create role (Add Role dialog).
     *
     * The `cms_role.role_key` column has a UNIQUE constraint at the DB
     * level and legacy roles ship with `role_key = ''`, so posting an
     * empty string on new roles reliably hits a `duplicate key value
     * violates unique constraint` from Postgres. We strip empty-string
     * optional fields to `undefined` here (JSON.stringify then omits
     * them), which lets the backend persist NULL and satisfy the
     * uniqueness contract.
     */
    createRole(form: DotRoleFormValue): Observable<DotRoleDetail> {
        return this.#http
            .post<DotCMSResponse<DotRoleDetail>>('/api/v1/roles', this.#sanitizeRoleForm(form))
            .pipe(map((response) => response.entity));
    }

    #sanitizeRoleForm(form: DotRoleFormValue): DotRoleFormValue {
        const trimmedKey = form.roleKey?.trim();
        const trimmedDescription = form.description?.trim();

        return {
            ...form,
            roleKey: trimmedKey ? trimmedKey : undefined,
            description: trimmedDescription ? trimmedDescription : undefined,
            parentRoleId: form.parentRoleId ?? undefined
        };
    }

    /**
     * PUT /v1/roles/{roleId} — update an existing role (Edit Role dialog).
     *
     * Same body shape as `POST /v1/roles`; response mirrors `GET /v1/roles/{roleId}`
     * (a hydrated `RoleView`). Payload sanitization is shared with `createRole` so
     * empty `roleKey` / `description` don't hit the DB UNIQUE constraint on
     * `role_key`. `parentRoleId = null` reparents to root (BE sets
     * `role.setParent(role.getId())`).
     *
     * Backend error semantics — surfaced by the caller via `httpErrorManager`:
     *   400 → cycle in hierarchy (reparent under a descendant)
     *   403 → system/locked role, or admin gate
     *   404 → role or parent role not found
     *   409 → duplicate `roleKey` or duplicate `roleName` under same parent
     */
    updateRole(roleId: string, form: DotRoleFormValue): Observable<DotRoleDetail> {
        return this.#http
            .put<
                DotCMSResponse<DotRoleDetail>
            >(`/api/v1/roles/${encodeURIComponent(roleId)}`, this.#sanitizeRoleForm(form))
            .pipe(map((response) => response.entity));
    }

    /**
     * DELETE /v1/roles/{roleId} — delete a role (#36939).
     *
     * Cascading deletion: the response's `usersAffected` reports how many users
     * had the role at the moment of deletion (their assignments were dropped).
     * Permissions granted to the role and layout / tool-group assignments are
     * also removed. Deletion is rejected by the backend for:
     *   403 → system or locked roles
     *   404 → role not found
     *   409 → role has children, or a workflow action references it
     *
     * Backend errors surface through `httpErrorManager`; the store just
     * returns `null` on failure.
     */
    deleteRole(roleId: string): Observable<DotRoleDeletionResult> {
        return this.#http
            .delete<
                DotCMSResponse<DotRoleDeletionResult>
            >(`/api/v1/roles/${encodeURIComponent(roleId)}`)
            .pipe(map((response) => response.entity));
    }

    /**
     * POST /v1/roles/{roleId}/users/{userId} — grant a role to a user (#36937).
     *
     * Idempotent by design: the BE returns `granted: true` even when the user
     * already held the role (directly or via inheritance) — retries are safe.
     * The user payload in the response is deliberately slim (id / email /
     * fullName); consumers needing more must call the users API.
     *
     * Error semantics:
     *   403 → role's `editUsers` flag is false (workflow / system roles)
     *   404 → role or user not found
     */
    grantUserToRole(roleId: string, userId: string): Observable<DotRoleUserGrantResult> {
        const url = `/api/v1/roles/${encodeURIComponent(roleId)}/users/${encodeURIComponent(
            userId
        )}`;

        return this.#http
            .post<DotCMSResponse<DotRoleUserGrantResult>>(url, null)
            .pipe(map((response) => response.entity));
    }

    /**
     * DELETE /v1/roles/{roleId}/users — bulk-remove members (#36938).
     *
     * Partial-success semantics: the BE returns `removedUserIds` for the
     * users whose direct membership was removed, and a `skipped` list per
     * user for the rest (reason: `not_found` / `inherited` / `error`). The
     * batch never fails as a whole once the role resolves, so consumers
     * should ALWAYS act on both arrays — a 200 does not imply "all removed".
     *
     * Angular's `HttpClient.delete` needs the body under `options.body`; the
     * body wraps `userIds` under `RoleUsersForm` on the BE.
     */
    removeUsersFromRole(roleId: string, userIds: string[]): Observable<DotRoleUsersRemovalResult> {
        return this.#http
            .delete<
                DotCMSResponse<DotRoleUsersRemovalResult>
            >(`/api/v1/roles/${encodeURIComponent(roleId)}/users`, { body: { userIds } })
            .pipe(map((response) => response.entity));
    }

    /**
     * Reparent a role via `PUT /v1/roles/{roleId}`. Drag-and-drop in the tree
     * calls this — it loads the role first (to preserve unrelated fields the
     * form isn't collecting) and PUTs a form that only changes `parentRoleId`.
     * Pass `null` to move to root. Round-trips the loaded detail so the store
     * can splice the updated shape into its tree.
     */
    reparentRole(roleId: string, newParentId: string | null): Observable<DotRoleDetail> {
        return this.loadRoleById(roleId, false).pipe(
            switchMap((role) =>
                this.updateRole(roleId, {
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
}
