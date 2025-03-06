import { Observable, of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { TestBed, waitForAsync } from '@angular/core/testing';

import { DotCurrentUserService } from '@dotcms/data-access';
import { CoreWebService } from '@dotcms/dotcms-js';
import { DotPermissionsType, PermissionsType, UserPermissions } from '@dotcms/dotcms-models';
import { CoreWebServiceMock } from '@dotcms/utils-testing';

import { DotStarterResolver } from './dot-starter-resolver.service';

export const CurrentUserDataMock = {
    admin: true,
    email: 'admin@dotcms.com',
    givenName: 'TEST',
    roleId: 'e7d23sde-5127-45fc-8123-d424fd510e3',
    surname: 'User',
    userId: 'testId'
};

const permissionsData: DotPermissionsType = {
    STRUCTURES: { canRead: true, canWrite: true },
    HTMLPAGES: { canRead: true, canWrite: true },
    TEMPLATES: { canRead: true, canWrite: true },
    CONTENTLETS: { canRead: true, canWrite: true }
};
export class DotCurrentUserServiceMock {
    getCurrentUser() {
        return of(CurrentUserDataMock);
    }

    getUserPermissions(
        _userId: string,
        _permissions: UserPermissions[],
        _permissionsType: PermissionsType[]
    ): Observable<DotPermissionsType> {
        return of(permissionsData);
    }
}

describe('DotStarterResolver', () => {
    let dotStarterResolver: DotStarterResolver;

    beforeEach(waitForAsync(() => {
        const testbed = TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                { provide: DotCurrentUserService, useClass: DotCurrentUserServiceMock },
                DotStarterResolver
            ]
        });
        dotStarterResolver = testbed.inject(DotStarterResolver);
    }));

    it('should get and return user & permissions data', () => {
        dotStarterResolver.resolve().subscribe(({ user, permissions }) => {
            expect(user).toEqual(CurrentUserDataMock);
            expect(permissions).toEqual(permissionsData);
        });
    });
});
