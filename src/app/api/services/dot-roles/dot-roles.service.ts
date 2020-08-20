import { Injectable } from '@angular/core';
import { CoreWebService } from 'dotcms-js';
import { Observable } from 'rxjs';
import { RequestMethod } from '@angular/http';
import { map, pluck } from 'rxjs/operators';
import { DotRole } from '@models/dot-role/dot-role.model';
import { DotMessageService } from '@services/dot-message/dot-messages.service';

const CURRENT_USER_KEY = 'CMS Anonymous';

@Injectable()
export class DotRolesService {
    constructor(
        private dotMessageService: DotMessageService,
        private coreWebService: CoreWebService
    ) {}

    /**
     * Return list of roles associated to specific role .
     * @param {string} roleId
     * @returns Observable<DotRole[]>
     * @memberof DotRolesService
     */
    get(roleId: string, roleHierarchy: boolean): Observable<DotRole[]> {
        return this.coreWebService
            .requestView({
                method: RequestMethod.Get,
                url: `/api/v1/roles/${roleId}/rolehierarchyanduserroles?roleHierarchyForAssign=${roleHierarchy}`
            })
            .pipe(
                pluck('entity'),
                map((roles: DotRole[]) =>
                    roles
                        .filter((role: DotRole) => role.roleKey !== 'anonymous')
                        .map((role: DotRole) => {
                            if (role.roleKey === CURRENT_USER_KEY) {
                                role.name = this.dotMessageService.get('current-user');
                            } else if (role.user) {
                                role.name = `${role.name} (${this.dotMessageService.get('user')})`;
                            }
                            return role;
                        })
                )
            );
    }
}
