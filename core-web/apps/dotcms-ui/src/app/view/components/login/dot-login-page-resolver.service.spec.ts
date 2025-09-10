import { of } from 'rxjs';

import { Injectable } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { mockLoginFormResponse } from '@dotcms/utils-testing';

import { DotLoginPageResolver } from './dot-login-page-resolver.service';
import { DotLoginPageStateService } from './shared/services/dot-login-page-state.service';

@Injectable()
export class MockDotLoginPageStateService {
    mockLoginInfo = of({
        ...mockLoginFormResponse,
        i18nMessagesMap: {
            ...mockLoginFormResponse.i18nMessagesMap,
            emailAddressLabel: 'Email Address'
        }
    });

    set = jest.fn().mockReturnValue(this.mockLoginInfo);
    get = jest.fn().mockReturnValue(this.mockLoginInfo);
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
        expect(dotLoginPageStateService.set).toHaveBeenCalledTimes(1);
    });
});
