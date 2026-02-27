/* eslint-disable @typescript-eslint/no-explicit-any */

import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed, waitForAsync } from '@angular/core/testing';
import { ActivatedRouteSnapshot, ParamMap } from '@angular/router';

import { DotCurrentUserService, DotSystemConfigService, DotAppsService } from '@dotcms/data-access';
import { DotApp } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';
import { DotCurrentUserServiceMock } from '@dotcms/utils-testing';

import { DotAppsConfigurationResolver } from './dot-apps-configuration-resolver.service';

class AppsServicesMock {
    getConfigurationList(_serviceKey: string) {
        return of({});
    }
}

const createMockParamMap = (params: Record<string, string>): ParamMap => ({
    has: (key: string) => key in params,
    get: (key: string) => params[key] || null,
    getAll: (key: string) => (params[key] ? [params[key]] : []),
    keys: Object.keys(params)
});

const activatedRouteSnapshotMock: any = {
    paramMap: createMockParamMap({})
};

describe('DotAppsConfigurationResolver', () => {
    let dotAppsServices: DotAppsService;
    let dotAppsConfigurationResolver: DotAppsConfigurationResolver;

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            providers: [
                DotAppsConfigurationResolver,
                { provide: DotAppsService, useClass: AppsServicesMock },
                {
                    provide: ActivatedRouteSnapshot,
                    useValue: activatedRouteSnapshotMock
                },
                {
                    provide: DotSystemConfigService,
                    useValue: { getSystemConfig: () => of({}) }
                },
                {
                    provide: DotCurrentUserService,
                    useClass: DotCurrentUserServiceMock
                },
                GlobalStore,
                provideHttpClient(),
                provideHttpClientTesting()
            ]
        });
        dotAppsServices = TestBed.inject(DotAppsService);
        dotAppsConfigurationResolver = TestBed.inject(DotAppsConfigurationResolver);
    }));

    it('should get and return apps with configurations', () => {
        const response: DotApp = {
            allowExtraParams: true,
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
                },
                {
                    configured: false,
                    id: '456',
                    name: 'host.example.com'
                }
            ]
        };

        activatedRouteSnapshotMock.paramMap = createMockParamMap({ appKey: '123' });
        jest.spyOn(dotAppsServices, 'getConfigurationList').mockReturnValue(of(response));

        dotAppsConfigurationResolver
            .resolve(activatedRouteSnapshotMock)
            .subscribe((fakeContentType: any) => {
                expect(fakeContentType).toEqual(response);
            });
        expect(dotAppsServices.getConfigurationList).toHaveBeenCalledWith('123');
        expect(dotAppsServices.getConfigurationList).toHaveBeenCalledTimes(1);
    });
});
