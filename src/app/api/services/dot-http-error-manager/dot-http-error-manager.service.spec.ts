import { DotDialogService } from '../dot-dialog/dot-dialog.service';
import { LoginService } from 'dotcms-js/dotcms-js';
import { DOTTestBed } from '../../../test/dot-test-bed';
import { DotRouterService } from '../dot-router/dot-router.service';
import { RouterTestingModule } from '@angular/router/testing';
import { DotMessageService } from '../dot-messages-service';
import { MockDotMessageService } from '../../../test/dot-message-service.mock';
import { DotHttpErrorManagerService } from './dot-http-error-manager.service';
import { mockResponseView } from '../../../test/response-view.mock';

describe('DotHttpErrorManagerService', () => {
    let service: DotHttpErrorManagerService;
    let dotRouterService: DotRouterService;
    let dotDialogService: DotDialogService;
    let loginService: LoginService;
    let result: any;

    const messageServiceMock = new MockDotMessageService({
        'dot.common.http.error.403.header': '403 Header',
        'dot.common.http.error.403.message': '403 Message',
        'dot.common.http.error.500.header': '500 Header',
        'dot.common.http.error.500.message': '500 Message'
    });

    beforeEach(() => {
        const testbed = DOTTestBed.configureTestingModule({
            providers: [
                DotHttpErrorManagerService,
                {
                    provide: LoginService,
                    useValue: {
                        auth: {
                            user: {
                                emailAddress: 'admin@dotcms.com',
                                firstName: 'Admin',
                                lastName: 'Admin',
                                loggedInDate: 123456789,
                                userId: '123'
                            }
                        }
                    }
                },
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                }
            ],
            imports: [RouterTestingModule]
        });
        service = testbed.get(DotHttpErrorManagerService);
        dotRouterService = testbed.get(DotRouterService);
        dotDialogService = testbed.get(DotDialogService);
        loginService = testbed.get(LoginService);
    });

    it('should handle 401 error when user is login we use 403', () => {
        spyOn(dotDialogService, 'alert');

        service.handle(mockResponseView(401)).subscribe(res => {
            result = res;
        });

        expect(result).toEqual({
            redirected: false,
        });
        expect(dotDialogService.alert).toHaveBeenCalledWith({
            message: '403 Message',
            header: '403 Header'
        });
    });

    it('should handle 401 error when user is logout and redirect to login', () => {

        loginService.auth.user = null;
        spyOn(dotRouterService, 'goToLogin');
        spyOn(dotDialogService, 'alert');

        service.handle(mockResponseView(401)).subscribe(res => {
            result = res;
        });

        expect(result).toEqual({
            redirected: true,
        });
        expect(dotDialogService.alert).not.toHaveBeenCalled();
        expect(dotRouterService.goToLogin).toHaveBeenCalledTimes(1);
    });

    it('should handle 403 error', () => {
        spyOn(dotDialogService, 'alert');

        service.handle(mockResponseView(403)).subscribe(res => {
            result = res;
        });

        expect(result).toEqual({
            redirected: false,
        });
        expect(dotDialogService.alert).toHaveBeenCalledWith({
            message: '403 Message',
            header: '403 Header'
        });
    });

    it('should handle 500 error', () => {
        spyOn(dotDialogService, 'alert');

        service.handle(mockResponseView(500)).subscribe(res => {
            result = res;
        });

        expect(result).toEqual({
            redirected: false,
        });
        expect(dotDialogService.alert).toHaveBeenCalledWith({
            message: '500 Message',
            header: '500 Header'
        });
    });
});
