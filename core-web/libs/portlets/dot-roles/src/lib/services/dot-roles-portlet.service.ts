import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

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
     * TODO: replace with `PUT /v1/roles/{roleId}` once #36936 ships.
     *
     * The Edit Role dialog (name / key / description / can-grant / parent)
     * and drag-to-reparent both depend on this endpoint. Until it exists,
     * the Angular portlet cannot mutate an existing role. Consumers should
     * handle the rejected promise / thrown error and surface the message.
     */
    updateRole(_roleId: string, _form: DotRoleFormValue): Observable<never> {
        throw new Error(
            'updateRole is not wired yet — waiting on PUT /v1/roles/{roleId} (issue #36936)'
        );
    }

    /**
     * TODO: replace with `DELETE /v1/roles/{roleId}` once #36939 ships.
     *
     * The Delete Role destructive action in the Edit dialog depends on this.
     * Structured 409 errors (`has_children`, `has_users`, `has_layouts`) will
     * surface once the endpoint exists so the FE can localize the message.
     */
    deleteRole(_roleId: string): Observable<never> {
        throw new Error(
            'deleteRole is not wired yet — waiting on DELETE /v1/roles/{roleId} (issue #36939)'
        );
    }

    /**
     * TODO: replace with `POST /v1/roles/{roleId}/users/{userId}` once
     * #36937 ships. Used by the Grant to User popover on the Users tab.
     */
    grantUserToRole(_roleId: string, _userId: string): Observable<never> {
        throw new Error(
            'grantUserToRole is not wired yet — waiting on POST /v1/roles/{roleId}/users/{userId} (issue #36937)'
        );
    }

    /**
     * TODO: replace with `DELETE /v1/roles/{roleId}/users` once #36938 ships.
     * Used by per-row Remove and bulk-remove on the Users tab.
     */
    removeUsersFromRole(_roleId: string, _userIds: string[]): Observable<never> {
        throw new Error(
            'removeUsersFromRole is not wired yet — waiting on DELETE /v1/roles/{roleId}/users (issue #36938)'
        );
    }

    /**
     * TODO: reparent a role. Blocked on #36936. Drag-and-drop in the roles
     * tree triggers this action, which today has no v1 endpoint (only DWR
     * RoleAjax#updateRole supports it).
     */
    reparentRole(_roleId: string, _newParentId: string | null): Observable<never> {
        throw new Error(
            'reparentRole is not wired yet — waiting on PUT /v1/roles/{roleId} (issue #36936)'
        );
    }
}
