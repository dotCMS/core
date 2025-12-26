import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DotHttpErrorManagerService } from '@dotcms/data-access';
import { MockDotHttpErrorManagerService } from '@dotcms/utils-testing';

import { DotAccountService, DotAccountUser } from './dot-account-service';

describe('DotAccountService', () => {
    let service: DotAccountService;
    let httpTesting: HttpTestingController;
    let dotHttpErrorManagerService: DotHttpErrorManagerService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                provideHttpClient(),
                provideHttpClientTesting(),
                DotAccountService,
                {
                    provide: DotHttpErrorManagerService,
                    useClass: MockDotHttpErrorManagerService
                }
            ]
        });

        service = TestBed.inject(DotAccountService);
        httpTesting = TestBed.inject(HttpTestingController);
        dotHttpErrorManagerService = TestBed.inject(DotHttpErrorManagerService);
    });

    afterEach(() => {
        httpTesting.verify();
    });

    it('Should update user data', () => {
        const user: DotAccountUser = {
            userId: '1',
            givenName: 'Test',
            surname: 'test',
            currentPassword: 'Password',
            email: 'test@test.com'
        };
        service.updateUser(user).subscribe();

        const req = httpTesting.expectOne('/api/v1/users/current');
        expect(req.request.method).toBe('PUT');
        expect(req.request.body).toEqual(user);
        req.flush({});
    });

    it('Should do the put request to add the getting starter portlet to menu', () => {
        service.addStarterPage().subscribe((response) => {
            expect(response).toEqual('ok');
        });

        const req = httpTesting.expectOne('/api/v1/toolgroups/gettingstarted/_addtouser');
        expect(req.request.method).toBe('PUT');
        req.flush({ entity: 'ok' });
    });

    it('should handle error on addStarterPage and return null', () => {
        jest.spyOn(dotHttpErrorManagerService, 'handle');

        service.addStarterPage().subscribe((response) => {
            expect(response).toBeNull();
        });

        const req = httpTesting.expectOne('/api/v1/toolgroups/gettingstarted/_addtouser');
        req.flush(null, { status: 400, statusText: 'Bad Request' });

        expect(dotHttpErrorManagerService.handle).toHaveBeenCalled();
    });

    it('Should do the put request to remove the getting starter portlet from menu', () => {
        service.removeStarterPage().subscribe((response) => {
            expect(response).toEqual('ok');
        });

        const req = httpTesting.expectOne('/api/v1/toolgroups/gettingstarted/_removefromuser');
        expect(req.request.method).toBe('PUT');
        req.flush({ entity: 'ok' });
    });

    it('should handle error on removeStarterPage and return null', () => {
        jest.spyOn(dotHttpErrorManagerService, 'handle');

        service.removeStarterPage().subscribe((response) => {
            expect(response).toBeNull();
        });

        const req = httpTesting.expectOne('/api/v1/toolgroups/gettingstarted/_removefromuser');
        req.flush(null, { status: 400, statusText: 'Bad Request' });

        expect(dotHttpErrorManagerService.handle).toHaveBeenCalled();
    });
});
