import { Observable, of } from 'rxjs';

import { HttpClientModule } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot } from '@angular/router';

import { DotPropertiesService } from '@dotcms/data-access';
import { FeaturedFlags } from '@dotcms/dotcms-models';

import { DotFeatureFlagResolver } from './dot-feature-flag-resolver.service';

describe('DotFeatureFlagResolver', () => {
    let resolver: DotFeatureFlagResolver;
    let dotConfigurationService: DotPropertiesService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [DotFeatureFlagResolver, DotPropertiesService],
            imports: [HttpClientModule]
        });

        resolver = TestBed.inject(DotFeatureFlagResolver);
        dotConfigurationService = TestBed.inject(DotPropertiesService);
    });

    it('should return an observable of boolean values', (done) => {
        const route: ActivatedRouteSnapshot = {
            data: {
                featuredFlagsToCheck: [
                    FeaturedFlags.FEATURE_FLAG_ANNOUNCEMENTS,
                    FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_CONTENT_TYPE
                ]
            },
            url: [],
            params: {},
            queryParams: {},
            fragment: '',
            outlet: '',
            component: undefined,
            routeConfig: undefined,
            title: '',
            root: new ActivatedRouteSnapshot(),
            parent: new ActivatedRouteSnapshot(),
            firstChild: new ActivatedRouteSnapshot(),
            children: [],
            pathFromRoot: [],
            paramMap: undefined,
            queryParamMap: undefined
        };

        const expectedFlagsResult: Record<string, boolean> = {
            [FeaturedFlags.FEATURE_FLAG_ANNOUNCEMENTS]: true,
            [FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_CONTENT_TYPE]: false
        };

        spyOn(dotConfigurationService, 'getFeatureFlags').and.returnValue(of(expectedFlagsResult));

        (resolver.resolve(route) as Observable<Record<string, boolean>>).subscribe(
            (result: Record<string, boolean>) => {
                expect(dotConfigurationService.getFeatureFlags).toHaveBeenCalledWith([
                    FeaturedFlags.FEATURE_FLAG_ANNOUNCEMENTS,
                    FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_CONTENT_TYPE
                ]);

                expect(result).toEqual(expectedFlagsResult);
                done();
            }
        );
    });
});
