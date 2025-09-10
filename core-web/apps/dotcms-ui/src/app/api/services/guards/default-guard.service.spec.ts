import { TestBed } from '@angular/core/testing';

import { DotRouterService } from '@dotcms/data-access';

import { DefaultGuardService } from './default-guard.service';

import { DOTTestBed } from '../../../test/dot-test-bed';

describe('ValidDefaultGuardService', () => {
    let defaultGuardService: DefaultGuardService;
    let dotRouterService: DotRouterService;

    beforeEach(() => {
        DOTTestBed.configureTestingModule({
            providers: [DefaultGuardService]
        });

        defaultGuardService = TestBed.inject(DefaultGuardService);
        dotRouterService = TestBed.inject(DotRouterService);
    });

    it('should redirect to to Main Portlet always', () => {
        const result = defaultGuardService.canActivate();
        expect(dotRouterService.goToMain).toHaveBeenCalled();
        expect(result).toBe(true);
    });
});
