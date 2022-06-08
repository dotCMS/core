import { TestBed, waitForAsync } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { DotAccountUser, DotAccountService } from './dot-account-service';
import { CoreWebService, LoginService } from '@dotcms/dotcms-js';
import { CoreWebServiceMock } from '@tests/core-web.service.mock';
import { LoginServiceMock } from '@tests/login-service.mock';
import { mockResponseView } from '@dotcms/app/test/response-view.mock';
import { throwError } from 'rxjs';
import { DotHttpErrorManagerService } from './dot-http-error-manager/dot-http-error-manager.service';
import { DotAlertConfirmService } from '@services/dot-alert-confirm';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { MockDotRouterService } from '@tests/dot-router-service.mock';
import { ConfirmationService } from 'primeng/api';
import { DotFormatDateService } from '@services/dot-format-date-service';
import { DotFormatDateServiceMock } from '@dotcms/app/test/format-date-service.mock';

describe('DotAccountService', () => {
    let service: DotAccountService;
    let coreWebService: CoreWebService;
    let httpTestingController: HttpTestingController;
    let dotHttpErrorManagerService: DotHttpErrorManagerService;

    beforeEach(
        waitForAsync(() => {
            TestBed.configureTestingModule({
                providers: [
                    DotAccountService,
                    DotHttpErrorManagerService,
                    DotAlertConfirmService,
                    ConfirmationService,
                    { provide: DotFormatDateService, useClass: DotFormatDateServiceMock },
                    { provide: CoreWebService, useClass: CoreWebServiceMock },
                    { provide: DotRouterService, useClass: MockDotRouterService },
                    {
                        provide: LoginService,
                        useClass: LoginServiceMock
                    }
                ],
                imports: [HttpClientTestingModule]
            });

            service = TestBed.inject(DotAccountService);
            httpTestingController = TestBed.inject(HttpTestingController);
            dotHttpErrorManagerService = TestBed.inject(DotHttpErrorManagerService);
            coreWebService = TestBed.inject(CoreWebService);
        })
    );

    it('Should update user data', () => {
        const user: DotAccountUser = {
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
            return req.url === '/api/v1/toolgroups/gettingstarted/_addtouser';
        });
        expect(reqMock.request.method).toBe('PUT');
        reqMock.flush({});
    });

    it('should throw error on get apps and handle it', () => {
        const error404 = mockResponseView(400);
        spyOn(dotHttpErrorManagerService, 'handle').and.callThrough();
        spyOn(coreWebService, 'requestView').and.returnValue(throwError(error404));

        service.addStarterPage().subscribe();
        expect(dotHttpErrorManagerService.handle).toHaveBeenCalledWith(mockResponseView(400));
    });

    it('Should do the put request to remove the getting starter portlet to menu', () => {
        service.removeStarterPage().subscribe();

        const reqMock = httpTestingController.expectOne((req) => {
            return req.url === '/api/v1/toolgroups/gettingstarted/_removefromuser';
        });
        expect(reqMock.request.method).toBe('PUT');
        reqMock.flush({});
    });

    it('should throw error on get apps and handle it', () => {
        const error404 = mockResponseView(400);
        spyOn(dotHttpErrorManagerService, 'handle').and.callThrough();
        spyOn(coreWebService, 'requestView').and.returnValue(throwError(error404));

        service.removeStarterPage().subscribe();
        expect(dotHttpErrorManagerService.handle).toHaveBeenCalledWith(mockResponseView(400));
    });
});
