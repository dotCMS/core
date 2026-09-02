/* eslint-disable @typescript-eslint/no-explicit-any */

import { of } from 'rxjs';

import { TestBed, waitForAsync } from '@angular/core/testing';
import { ActivatedRouteSnapshot, ParamMap } from '@angular/router';

import { DotAppsService } from '@dotcms/data-access';
import { DotApp } from '@dotcms/dotcms-models';

import { DotAppsConfigurationDetailResolver } from './dot-apps-configuration-detail-resolver.service';

class AppsServicesMock {
    getConfiguration(_appKey: string, _id: string) {
        return of({});
    }
}

const createMockParamMap = (params: Record<string, string>): ParamMap => ({
    has: (key: string) => key in params,
    get: (key: string) => params[key] || null,
    getAll: (key: string) => (params[key] ? [params[key]] : []),
    keys: Object.keys(params)
});

const activatedRouteSnapshotMock = {
    paramMap: createMockParamMap({})
} as unknown as ActivatedRouteSnapshot;

describe('DotAppsConfigurationDetailResolver', () => {
    let dotAppsServices: DotAppsService;
    let dotAppsConfigurationDetailResolver: DotAppsConfigurationDetailResolver;

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            providers: [
                DotAppsConfigurationDetailResolver,
                { provide: DotAppsService, useClass: AppsServicesMock },
                {
                    provide: ActivatedRouteSnapshot,
                    useValue: activatedRouteSnapshotMock
                }
            ]
        });
        dotAppsServices = TestBed.inject(DotAppsService);
        dotAppsConfigurationDetailResolver = TestBed.inject(DotAppsConfigurationDetailResolver);
    }));

    it('should get and return app with configurations', () => {
        const response: DotApp = {
            allowExtraParams: false,
            configurationsCount: 2,
            key: 'google-calendar',
            name: 'Google Calendar',
            description: "It's a tool to keep track of your life's events",
            iconUrl: '/dA/d948d85c-3bc8-4d85-b0aa-0e989b9ae235/photo/surfer-profile.jpg',
            sites: [
                {
                    configured: true,
                    id: '123',
                    name: 'demo.dotcms.com'
                }
            ]
        };

        const queryParams = {
            appKey: 'sampleDescriptor1',
            id: '48190c8c-42c4-46af-8d1a-0cd5db894797'
        };

        (activatedRouteSnapshotMock as any).paramMap = createMockParamMap({
            appKey: queryParams.appKey,
            id: queryParams.id
        });

        jest.spyOn(dotAppsServices, 'getConfiguration').mockReturnValue(of(response));

        dotAppsConfigurationDetailResolver
            .resolve(activatedRouteSnapshotMock)
            .subscribe((fakeContentType) => {
                expect(fakeContentType).toEqual(response);
            });

        expect(dotAppsServices.getConfiguration).toHaveBeenCalledWith(
            queryParams.appKey,
            queryParams.id
        );
    });
});
