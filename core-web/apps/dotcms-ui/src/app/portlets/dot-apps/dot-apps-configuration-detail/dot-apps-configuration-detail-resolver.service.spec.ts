/* eslint-disable @typescript-eslint/no-explicit-any */

import { of } from 'rxjs';

import { TestBed, waitForAsync } from '@angular/core/testing';
import { ActivatedRouteSnapshot } from '@angular/router';

import { DotAppsConfigurationDetailResolver } from './dot-apps-configuration-detail-resolver.service';

import { DotAppsService } from '../../../api/services/dot-apps/dot-apps.service';

class AppsServicesMock {
    getConfiguration(_appKey: string, _id: string) {
        return of({});
    }
}

const activatedRouteSnapshotMock: any = jest.fn<ActivatedRouteSnapshot>('ActivatedRouteSnapshot', [
    'toString'
]);
activatedRouteSnapshotMock.paramMap = {};

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
        const response = {
            integrationsCount: 2,
            appKey: 'google-calendar',
            name: 'Google Calendar',
            description: "It's a tool to keep track of your life's events",
            iconUrl: '/dA/d948d85c-3bc8-4d85-b0aa-0e989b9ae235/photo/surfer-profile.jpg',
            hosts: [
                {
                    configured: true,
                    hostId: '123',
                    hostName: 'demo.dotcms.com'
                }
            ]
        };

        const queryParams = {
            appKey: 'sampleDescriptor1',
            id: '48190c8c-42c4-46af-8d1a-0cd5db894797'
        };

        activatedRouteSnapshotMock.paramMap.get = (param: string) => {
            return param === 'appKey' ? queryParams.appKey : queryParams.id;
        };

        jest.spyOn<any>(dotAppsServices, 'getConfiguration').mockReturnValue(of(response));

        dotAppsConfigurationDetailResolver
            .resolve(activatedRouteSnapshotMock)
            .subscribe((fakeContentType: any) => {
                expect(fakeContentType).toEqual(response);
            });

        expect(dotAppsServices.getConfiguration).toHaveBeenCalledWith(
            queryParams.appKey,
            queryParams.id
        );
    });
});
