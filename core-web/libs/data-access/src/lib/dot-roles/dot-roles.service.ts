import { Observable } from 'rxjs';

import { Injectable, inject } from '@angular/core';

import { map, pluck } from 'rxjs/operators';

import { CoreWebService } from '@dotcms/dotcms-js';
import { DotRole } from '@dotcms/dotcms-models';

import { DotMessageService } from '../dot-messages/dot-messages.service';

const CURRENT_USER_KEY = 'CMS Anonymous';

@Injectable({
    providedIn: 'root'
})
export class DotRolesService {
    private dotMessageService = inject(DotMessageService);
    private coreWebService = inject(CoreWebService);

    /**
     * Return list of roles associated to specific role .
     * @param {string} roleId
     * @returns Observable<DotRole[]>
     * @memberof DotRolesService
     */
    get(roleId: string, roleHierarchy: boolean): Observable<DotRole[]> {
        return this.coreWebService
            .requestView({
                url: `/api/v1/roles/${roleId}/rolehierarchyanduserroles?roleHierarchyForAssign=${roleHierarchy}`
            })
            .pipe(pluck('entity'), map(this.processRolesResponse.bind(this)));
    }

    /**
     * Return list of roles.
     * @returns Observable<DotRole[]>
     * @memberof DotRolesService
     */
    search(): Observable<DotRole[]> {
        return this.coreWebService
            .requestView({
                url: `/api/v1/roles/_search`
            })
            .pipe(pluck('entity'), map(this.processRolesResponse.bind(this)));
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
