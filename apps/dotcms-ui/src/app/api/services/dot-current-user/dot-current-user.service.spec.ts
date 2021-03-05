import { DotCurrentUserService } from './dot-current-user.service';
import { TestBed, getTestBed } from '@angular/core/testing';
import { CoreWebService } from '@dotcms/dotcms-js';
import { CoreWebServiceMock } from '@tests/core-web.service.mock';
import { HttpTestingController, HttpClientTestingModule } from '@angular/common/http/testing';
import {
    DotCurrentUser,
    DotPermissionsType,
    PermissionsType,
    UserPermissions
} from '@shared/models/dot-current-user/dot-current-user';

describe('DotCurrentUserService', () => {
    let injector: TestBed;
    let dotCurrentUserService: DotCurrentUserService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                DotCurrentUserService
            ]
        });
        injector = getTestBed();
        dotCurrentUserService = injector.get(DotCurrentUserService);
        httpMock = injector.get(HttpTestingController);
    });

    it('should get logged user', () => {
        const mockCurrentUserResponse = {
            email: 'admin@dotcms.com',
            givenName: 'TEST',
            roleId: 'e7d23sde-5127-45fc-8123-d424fd510e3',
            surnaname: 'User',
            userId: 'testId'
        };
        dotCurrentUserService.getCurrentUser().subscribe((user: DotCurrentUser) => {
            expect(user).toEqual(mockCurrentUserResponse);
        });

        const req = httpMock.expectOne('v1/users/current/');
        expect(req.request.method).toBe('GET');
        req.flush(mockCurrentUserResponse);
    });

    it('should get user has access to specific Portlet', () => {
        const portlet = 'test';
        dotCurrentUserService.hasAccessToPortlet(portlet).subscribe((hasAccess: boolean) => {
            expect(hasAccess).toEqual(true);
        });

        const req = httpMock.expectOne(`v1/portlet/${portlet}/_doesuserhaveaccess`);
        expect(req.request.method).toBe('GET');
        req.flush({
            entity: {
                response: true
            }
        });
    });

    it('should get user Permissions data', () => {
        const response = {
            STRUCTURES: { canRead: true, canWrite: true },
            HTMLPAGES: { canRead: true, canWrite: true },
            TEMPLATES: { canRead: true, canWrite: true },
            CONTENTLETS: { canRead: true, canWrite: true }
        };
        const userId = 'test';
        dotCurrentUserService
            .getUserPermissions(userId)
            .subscribe((permissions: DotPermissionsType) => {
                expect(permissions).toEqual(response);
            });

        const req = httpMock.expectOne(`v1/permissions/_bypermissiontype?userid=${userId}`);
        expect(req.request.method).toBe('GET');
        req.flush({
            entity: response
        });
    });

    it('should get user Permissions data from specific portlets and permission types', () => {
        const userId = 'test';
        dotCurrentUserService
            .getUserPermissions(userId, [UserPermissions.WRITE], [PermissionsType.HTMLPAGES])
            .subscribe();

        const req = httpMock.expectOne(
            `v1/permissions/_bypermissiontype?userid=${userId}&permission=${UserPermissions.WRITE}&permissiontype=${PermissionsType.HTMLPAGES}`
        );
        expect(req.request.method).toBe('GET');
        req.flush({});
    });

    afterEach(() => {
        httpMock.verify();
    });
});
