import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
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
