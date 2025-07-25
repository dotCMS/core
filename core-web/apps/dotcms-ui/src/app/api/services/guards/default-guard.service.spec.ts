import { TestBed } from '@angular/core/testing';

import { DOTTestBed } from '@dotcms/app/test/dot-test-bed';
import { DotRouterService } from '@dotcms/data-access';

import { DefaultGuardService } from './default-guard.service';

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
