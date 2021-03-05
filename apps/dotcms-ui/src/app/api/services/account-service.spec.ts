import { TestBed, waitForAsync } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AccountService, AccountUser } from './account-service';
import { CoreWebService } from '@dotcms/dotcms-js';
import { CoreWebServiceMock } from '@tests/core-web.service.mock';

describe('AccountService', () => {
    let service: AccountService;
    let httpTestingController: HttpTestingController;

    beforeEach(
        waitForAsync(() => {
            TestBed.configureTestingModule({
                providers: [
                    AccountService,
                    { provide: CoreWebService, useClass: CoreWebServiceMock }
                ],
                imports: [HttpClientTestingModule]
            });

            service = TestBed.inject(AccountService);
            httpTestingController = TestBed.inject(HttpTestingController);
        })
    );

    it('Should update user data', () => {
        const user: AccountUser = {
            userId: '1',
            givenName: 'Test',
            surname: 'test',
            currentPassword: 'Password',
            email: 'test@test.com'
        };
        service.updateUser(user).subscribe();

        const reqMock = httpTestingController.expectOne((req) => {
            return req.url === 'v1/users/current';
        });
        expect(reqMock.request.method).toBe('PUT');
        reqMock.flush({});
    });

    it('Should do the put request to add the getting starter portlet to menu', () => {
        service.addStarterPage().subscribe();

        const reqMock = httpTestingController.expectOne((req) => {
            return req.url === '/api/v1/toolgroups/gettingstarted/_addtocurrentuser';
        });
        expect(reqMock.request.method).toBe('PUT');
        reqMock.flush({});
    });

    it('Should do the put request to remove the getting starter portlet to menu', () => {
        service.removeStarterPage().subscribe();

        const reqMock = httpTestingController.expectOne((req) => {
            return req.url === '/api/v1/toolgroups/gettingstarted/_removefromcurrentuser';
        });
        expect(reqMock.request.method).toBe('PUT');
        reqMock.flush({});
    });
});
