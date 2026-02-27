import { of } from 'rxjs';

import { TestBed } from '@angular/core/testing';

import { DotPropertiesService } from '@dotcms/data-access';
import { FeaturedFlags } from '@dotcms/dotcms-models';

import { PagesGuardService } from './pages-guard.service';

// Mock service for DotPropertiesService (replacement for removed dot-edit-page module)
class MockDotPropertiesService {
    getFeatureFlag(_flag: string) {
        return of(false);
    }
}

describe('PagesGuardService', () => {
    let pagesGuardService: PagesGuardService;
    let dotPropertiesService: DotPropertiesService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                PagesGuardService,
                { provide: DotPropertiesService, useClass: MockDotPropertiesService }
            ]
        });

        pagesGuardService = TestBed.inject(PagesGuardService);
        dotPropertiesService = TestBed.inject(DotPropertiesService);
    });

    it('should allow access to Pages Portlets', () => {
        let result: boolean;
        jest.spyOn(dotPropertiesService, 'getFeatureFlag').mockReturnValue(of(true));
        pagesGuardService.canActivate().subscribe((res) => (result = res));
        expect(dotPropertiesService.getFeatureFlag).toHaveBeenCalledWith(
            FeaturedFlags.DOTFAVORITEPAGE_FEATURE_ENABLE
        );
        expect(result).toBe(true);
    });

    it('should deny access to Pages Portlets', () => {
        let result: boolean;
        jest.spyOn(dotPropertiesService, 'getFeatureFlag').mockReturnValue(of(false));
        pagesGuardService.canActivate().subscribe((res) => (result = res));
        expect(dotPropertiesService.getFeatureFlag).toHaveBeenCalledWith(
            FeaturedFlags.DOTFAVORITEPAGE_FEATURE_ENABLE
        );
        expect(result).toBe(false);
    });
});
