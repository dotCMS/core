import { DotCurrentUserService } from './dot-current-user.service';
import { TestBed, getTestBed } from '@angular/core/testing';
import { CoreWebService } from 'dotcms-js';
import { CoreWebServiceMock } from 'projects/dotcms-js/src/lib/core/core-web.service.mock';
import { HttpTestingController, HttpClientTestingModule } from '@angular/common/http/testing';
import { DotCurrentUser } from '@shared/models/dot-current-user/dot-current-user';

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

    afterEach(() => {
        httpMock.verify();
    });
});
