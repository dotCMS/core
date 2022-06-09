import { waitForAsync, TestBed } from '@angular/core/testing';
import { DotStarterResolver } from './dot-starter-resolver.service';
import { DotCurrentUserService } from '@services/dot-current-user/dot-current-user.service';
import { CoreWebServiceMock } from '@tests/core-web.service.mock';
import { CoreWebService } from '@dotcms/dotcms-js';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Observable, of } from 'rxjs';
import {
    DotPermissionsType,
    PermissionsType,
    UserPermissions
} from '@models/dot-current-user/dot-current-user';

const userData = {
    email: 'admin@dotcms.com',
    givenName: 'TEST',
    roleId: 'e7d23sde-5127-45fc-8123-d424fd510e3',
    surnaname: 'User',
    userId: 'testId'
};

const permissionsData: DotPermissionsType = {
    STRUCTURES: { canRead: true, canWrite: true },
    HTMLPAGES: { canRead: true, canWrite: true },
    TEMPLATES: { canRead: true, canWrite: true },
    CONTENTLETS: { canRead: true, canWrite: true }
};
class DotCurrentUserServiceMock {
    getCurrentUser() {
        return of(userData);
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

    beforeEach(
        waitForAsync(() => {
            const testbed = TestBed.configureTestingModule({
                imports: [HttpClientTestingModule],
                providers: [
                    { provide: CoreWebService, useClass: CoreWebServiceMock },
                    { provide: DotCurrentUserService, useClass: DotCurrentUserServiceMock },
                    DotStarterResolver
                ]
            });
            dotStarterResolver = testbed.inject(DotStarterResolver);
        })
    );

    it('should get and return user & permissions data', () => {
        dotStarterResolver.resolve().subscribe(({ user, permissions }) => {
            expect(user).toEqual(userData);
            expect(permissions).toEqual(permissionsData);
        });
    });
});
