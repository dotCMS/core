import { of } from 'rxjs';

import { TestBed } from '@angular/core/testing';

import { LoginService } from '@dotcms/dotcms-js';
import { DotLoginInformation } from '@dotcms/dotcms-models';
import { LoginServiceMock, mockLoginFormResponse } from '@dotcms/utils-testing';

import { DotLoginPageStateService, LOGIN_LABELS } from './dot-login-page-state.service';

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
        jest.spyOn(loginService, 'getLoginFormInfo').mockReturnValue(of(mockLoginFormResponse));
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
