import { of } from 'rxjs';

import { HttpClient, provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DotPropertiesService } from '@dotcms/data-access';
import { FeaturedFlags } from '@dotcms/dotcms-models';

import { editContentGuard } from './edit-content.guard';

describe('EditContentGuard', () => {
    let dotPropertiesService: DotPropertiesService;

    const setup = (dotPropertiesServiceMock: unknown) => {
        TestBed.configureTestingModule({
            imports: [],
            providers: [
                editContentGuard,
                {
                    provide: DotPropertiesService,
                    useValue: dotPropertiesServiceMock
                },
                HttpClient,
                provideHttpClient(withInterceptorsFromDi()),
                provideHttpClientTesting()
            ]
        });

        dotPropertiesService = TestBed.inject(DotPropertiesService);
        spyOn(dotPropertiesService, 'getFeatureFlag').and.callThrough();

        return TestBed.runInInjectionContext(editContentGuard);
    };

    it('should allow access to Edit Content new form', (done) => {
        const guard = setup({
            getFeatureFlag: () => of(true)
        });

        expect(dotPropertiesService.getFeatureFlag).toHaveBeenCalledWith(
            FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED
        );

        guard.subscribe((result) => {
            expect(result).toBe(true);
            done();
        });
    });

    it('should deny access to Edit Content new form', (done) => {
        const guard = setup({
            getFeatureFlag: () => of(false)
        });

        expect(dotPropertiesService.getFeatureFlag).toHaveBeenCalledWith(
            FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED
        );

        guard.subscribe((result) => {
            expect(result).toBe(false);
            done();
        });
    });
});
