/* eslint-disable @typescript-eslint/no-explicit-any */

import { of } from 'rxjs';

import { TestBed, waitForAsync } from '@angular/core/testing';
import { ActivatedRouteSnapshot } from '@angular/router';

import { DotAppsConfigurationResolver } from './dot-apps-configuration-resolver.service';

import { DotAppsService } from '../../../api/services/dot-apps/dot-apps.service';

class AppsServicesMock {
    getConfigurationList(_serviceKey: string) {
        of({});
    }
}

const activatedRouteSnapshotMock: any = jasmine.createSpyObj<ActivatedRouteSnapshot>(
    'ActivatedRouteSnapshot',
    ['toString']
);
activatedRouteSnapshotMock.paramMap = {};

describe('DotAppsConfigurationListResolver', () => {
    let dotAppsServices: DotAppsService;
    let dotAppsConfigurationListResolver: DotAppsConfigurationResolver;

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            providers: [
                DotAppsConfigurationResolver,
                { provide: DotAppsService, useClass: AppsServicesMock },
                {
                    provide: ActivatedRouteSnapshot,
                    useValue: activatedRouteSnapshotMock
                }
            ]
        });
        dotAppsServices = TestBed.inject(DotAppsService);
        dotAppsConfigurationListResolver = TestBed.inject(DotAppsConfigurationResolver);
    }));

    it('should get and return apps with configurations', () => {
        const response = {
            integrationsCount: 2,
            serviceKey: 'google-calendar',
            name: 'Google Calendar',
            description: "It's a tool to keep track of your life's events",
            iconUrl: '/dA/d948d85c-3bc8-4d85-b0aa-0e989b9ae235/photo/surfer-profile.jpg',
            hosts: [
                {
                    configured: true,
                    hostId: '123',
                    hostName: 'demo.dotcms.com'
                },
                {
                    configured: false,
                    hostId: '456',
                    hostName: 'host.example.com'
                }
            ]
        };

        activatedRouteSnapshotMock.paramMap.get = () => '123';
        spyOn<any>(dotAppsServices, 'getConfigurationList').and.returnValue(of(response));

        dotAppsConfigurationListResolver
            .resolve(activatedRouteSnapshotMock)
            .subscribe((fakeContentType: any) => {
                expect(fakeContentType).toEqual(response);
            });
        expect(dotAppsServices.getConfigurationList).toHaveBeenCalledWith('123');
    });
});
