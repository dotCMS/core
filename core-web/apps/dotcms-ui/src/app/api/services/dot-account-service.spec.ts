import { throwError } from 'rxjs';

import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed, waitForAsync } from '@angular/core/testing';

import { ConfirmationService } from 'primeng/api';

import {
    DotAlertConfirmService,
    DotHttpErrorManagerService,
    DotMessageDisplayService,
    DotRouterService,
    DotFormatDateService
} from '@dotcms/data-access';
import { CoreWebService, LoginService } from '@dotcms/dotcms-js';
import {
    CoreWebServiceMock,
    DotFormatDateServiceMock,
    DotMessageDisplayServiceMock,
    LoginServiceMock,
    MockDotRouterService,
    mockResponseView
} from '@dotcms/utils-testing';

import { DotAccountService, DotAccountUser } from './dot-account-service';

describe('DotAccountService', () => {
    let service: DotAccountService;
    let coreWebService: CoreWebService;
    let httpTestingController: HttpTestingController;
    let dotHttpErrorManagerService: DotHttpErrorManagerService;

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            providers: [
                DotAccountService,
                DotHttpErrorManagerService,
                DotAlertConfirmService,
                ConfirmationService,
                {
                    provide: DotMessageDisplayService,
                    useClass: DotMessageDisplayServiceMock
                },
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
    }));

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
        jest.spyOn(dotHttpErrorManagerService, 'handle');
        jest.spyOn(coreWebService, 'requestView').mockReturnValue(throwError(error404));

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
        jest.spyOn(dotHttpErrorManagerService, 'handle');
        jest.spyOn(coreWebService, 'requestView').mockReturnValue(throwError(error404));

        service.removeStarterPage().subscribe();
        expect(dotHttpErrorManagerService.handle).toHaveBeenCalledWith(mockResponseView(400));
    });
});
