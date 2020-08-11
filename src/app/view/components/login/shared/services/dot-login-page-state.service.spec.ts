import { DotLoginPageStateService, LOGIN_LABELS } from './dot-login-page-state.service';
import { LoginService } from 'dotcms-js';
import { LoginServiceMock, mockLoginFormResponse } from '@tests/login-service.mock';
import { of } from 'rxjs';
import { DotLoginInformation } from '@models/dot-login';
import { TestBed } from '@angular/core/testing';

describe('DotLoginPageStateServiceService', () => {
    let dotloginPageStateService: DotLoginPageStateService;
    let loginService: LoginService;

    beforeEach(() => {
        const testbed = TestBed.configureTestingModule({
            providers: [
                DotLoginPageStateService,
                { provide: LoginService, useClass: LoginServiceMock }
            ],
            imports: []
        });

        dotloginPageStateService = testbed.get(DotLoginPageStateService);
        loginService = testbed.get(LoginService);
        spyOn(loginService, 'getLoginFormInfo').and.returnValue(
            of({ bodyJsonObject: mockLoginFormResponse })
        );
    });

    it('should set new value to dotLoginInformation$ and call service correctly', () => {
        dotloginPageStateService.set('es_ES').subscribe();
        expect(loginService.getLoginFormInfo).toHaveBeenCalledWith('es_ES', LOGIN_LABELS);
        dotloginPageStateService.get().subscribe((loginInfo: DotLoginInformation) => {
            expect(loginInfo.entity).toEqual(mockLoginFormResponse.entity);
            expect(loginInfo.i18nMessagesMap['emailAddressLabel']).toEqual('Email Address');
        });
    });

    it('should update value of dotLoginInformation$', () => {
        dotloginPageStateService.update('es_ES');
        dotloginPageStateService.get().subscribe((loginInfo: DotLoginInformation) => {
            expect(loginInfo.entity).toEqual(mockLoginFormResponse.entity);
            expect(loginInfo.i18nMessagesMap['emailAddressLabel']).toEqual('Email Address');
        });
    });
});
