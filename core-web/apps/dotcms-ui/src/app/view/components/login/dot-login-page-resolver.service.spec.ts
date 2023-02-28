import { of } from 'rxjs';

import { Injectable } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { DotLoginPageResolver } from '@components/login/dot-login-page-resolver.service';
import { DotLoginPageStateService } from '@components/login/shared/services/dot-login-page-state.service';
import { mockLoginFormResponse } from '@dotcms/utils-testing';

@Injectable()
export class MockDotLoginPageStateService {
    mockLoginInfo = of({
        ...mockLoginFormResponse,
        i18nMessagesMap: {
            ...mockLoginFormResponse.i18nMessagesMap,
            emailAddressLabel: 'Email Address'
        }
    });

    set = jest.fn(() => this.mockLoginInfo);
    get = jest.fn(() => this.mockLoginInfo);
    update = jest.fn();
}

describe('DotLoginPageResolver', () => {
    let dotLoginPageStateService: DotLoginPageStateService;
    let dotLoginPageResolver: DotLoginPageResolver;

    beforeEach(() => {
        const testbed = TestBed.configureTestingModule({
            providers: [
                DotLoginPageResolver,
                {
                    provide: DotLoginPageStateService,
                    useClass: MockDotLoginPageStateService
                }
            ],
            imports: []
        });
        dotLoginPageResolver = testbed.get(DotLoginPageResolver);
        dotLoginPageStateService = testbed.get(DotLoginPageStateService);
    });

    it('should set the dotLoginPageStateService with the correct values ', () => {
        dotLoginPageResolver.resolve();
        expect(dotLoginPageStateService.set).toHaveBeenCalledWith('');
    });
});
