import { of } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DotPropertiesService } from '@dotcms/data-access';
import { FeaturedFlags } from '@dotcms/dotcms-models';

import { editContentGuard } from './edit-content.guard';

describe('EditContentGuard', () => {
    let dotPropertiesService: DotPropertiesService;

    const setup = (dotPropertiesServiceMock: unknown) => {
        TestBed.configureTestingModule({
            providers: [
                editContentGuard,
                {
                    provide: DotPropertiesService,
                    useValue: dotPropertiesServiceMock
                },
                HttpClient
            ],
            imports: [HttpClientTestingModule]
        });

        dotPropertiesService = TestBed.inject(DotPropertiesService);
        spyOn(dotPropertiesService, 'getKey').and.callThrough();

        return TestBed.runInInjectionContext(editContentGuard);
    };

    it('should allow access to Edit Content new form', (done) => {
        const guard = setup({
            getKey: () => of('true')
        });

        expect(dotPropertiesService.getKey).toHaveBeenCalledWith(
            FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLE
        );

        guard.subscribe((result) => {
            expect(result).toBe(true);
            done();
        });
    });

    it('should deny access to Edit Content new form', (done) => {
        const guard = setup({
            getKey: () => of('false')
        });

        expect(dotPropertiesService.getKey).toHaveBeenCalledWith(
            FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLE
        );

        guard.subscribe((result) => {
            expect(result).toBe(false);
            done();
        });
    });
});
