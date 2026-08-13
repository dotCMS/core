import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

import { DotCMSResponse } from '@dotcms/dotcms-models';

import {
    DotRoleDetail,
    DotRoleFormValue,
    DotRoleMember,
    DotRoleNode
} from '../models/dot-roles.models';

/**
 * Portlet-scoped data access for the Roles and Tools Angular Beta.
 *
 * The shared `DotRolesService` under `@dotcms/data-access` covers the
 * legacy read paths used by other portlets (roles-per-user hierarchy).
 * This service owns the endpoints the Roles portlet specifically consumes.
 *
 * Blocked write flows (Edit / Delete role, Grant / Remove user) are called
 * out in the individual methods; they resolve to the dedicated v1 endpoints
 * once #36936, #36937, #36938, #36939 land.
 */
@Injectable()
export class DotRolesPortletService {
    #http = inject(HttpClient);

    /** GET /v1/roles — root roles (optionally with first-level children). */
    loadRootRoles(loadChildren = true): Observable<DotRoleNode[]> {
        return this.#http
            .get<DotCMSResponse<DotRoleNode[]>>(`/api/v1/roles?loadChildrenRoles=${loadChildren}`)
            .pipe(map((response) => response.entity ?? []));
    }

    /** GET /v1/roles/{roleId} — role detail (optionally expand a subtree). */
    loadRoleById(roleId: string, loadChildren = true): Observable<DotRoleDetail> {
        return this.#http
            .get<
                DotCMSResponse<DotRoleDetail>
            >(`/api/v1/roles/${roleId}?loadChildrenRoles=${loadChildren}`)
            .pipe(map((response) => response.entity));
    }

    /**
     * GET /v1/roles/{roleId}/rolehierarchyanduserroles — members of the role
     * annotated with inheritance metadata so the Users tab can render the
     * `GRANTED FROM` chip.
     */
    loadRoleMembers(roleId: string): Observable<DotRoleMember[]> {
        return this.#http
            .get<
                DotCMSResponse<DotRoleMember[]>
            >(`/api/v1/roles/${roleId}/rolehierarchyanduserroles`)
            .pipe(map((response) => response.entity ?? []));
    }

    /** POST /v1/roles — create role (Add Role dialog). */
    createRole(form: DotRoleFormValue): Observable<DotRoleDetail> {
        return this.#http
            .post<DotCMSResponse<DotRoleDetail>>('/api/v1/roles', form)
            .pipe(map((response) => response.entity));
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
