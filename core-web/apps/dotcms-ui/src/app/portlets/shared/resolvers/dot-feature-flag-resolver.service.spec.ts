import { of } from 'rxjs';

import { HttpClientModule } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot } from '@angular/router';

import { DotPropertiesService } from '@dotcms/data-access';

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

    it('should return an observable of boolean values', () => {
        const route: ActivatedRouteSnapshot = {
            data: {
                featuredFlagsToCheck: ['flag1', 'flag2']
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

        const expectedFlags = {
            flag1: 'true',
            flag2: 'false'
        };

        const expectedFlagsResult = {
            flag1: true,
            flag2: false
        };

        spyOn(dotConfigurationService, 'getKeys').and.returnValue(of(expectedFlags));

        resolver.resolve(route).subscribe((result) => {
            expect(result).toEqual(expectedFlagsResult);
        });
    });
});
