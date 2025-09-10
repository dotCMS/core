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

    set = jasmine.createSpy('set').and.returnValue(this.mockLoginInfo);
    get = jasmine.createSpy('get').and.returnValue(this.mockLoginInfo);
    update = jasmine.createSpy('update');
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
