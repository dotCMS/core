import { Observable } from 'rxjs';

import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

import { DotCMSResponse, DotRole } from '@dotcms/dotcms-models';

import { DotMessageService } from '../dot-messages/dot-messages.service';

const CURRENT_USER_KEY = 'CMS Anonymous';

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
