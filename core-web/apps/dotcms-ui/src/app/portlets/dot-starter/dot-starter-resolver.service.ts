import { Observable } from 'rxjs';
import { Injectable } from '@angular/core';
import { Resolve } from '@angular/router';
import { DotCurrentUserService } from '@dotcms/data-access';

import { map, mergeMap } from 'rxjs/operators';
import {
    DotCurrentUser,
    DotPermissionsType,
    UserPermissions,
    PermissionsType
} from '@dotcms/dotcms-models';

/**
 * Returns user's data and permissions
 *
 * @export
 * @class DotStarterResolver
 * @implements {Resolve<Observable<{ user: DotCurrentUser, permissions: DotPermissionsType }>>}
 */
@Injectable()
export class DotStarterResolver
    implements Resolve<Observable<{ user: DotCurrentUser; permissions: DotPermissionsType }>>
{
    constructor(private dotCurrentUserService: DotCurrentUserService) {}

    resolve(): Observable<{ user: DotCurrentUser; permissions: DotPermissionsType }> {
        return this.dotCurrentUserService.getCurrentUser().pipe(
            mergeMap((user: DotCurrentUser) => {
                return this.dotCurrentUserService
                    .getUserPermissions(
                        user.userId,
                        [UserPermissions.WRITE],
                        [
                            PermissionsType.HTMLPAGES,
                            PermissionsType.STRUCTURES,
                            PermissionsType.TEMPLATES,
                            PermissionsType.CONTENTLETS
                        ]
                    )
                    .pipe(
                        map((permissionsType: DotPermissionsType) => {
                            return { user, permissions: permissionsType };
                        })
                    );
            })
        );
    }
}
