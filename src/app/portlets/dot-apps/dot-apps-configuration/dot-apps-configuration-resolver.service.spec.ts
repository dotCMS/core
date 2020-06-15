import { of as observableOf } from 'rxjs';
import { async } from '@angular/core/testing';
import { ActivatedRouteSnapshot } from '@angular/router';
import { DOTTestBed } from '../../../test/dot-test-bed';
import { DotAppsService } from '@services/dot-apps/dot-apps.service';
import { DotAppsConfigurationResolver } from './dot-apps-configuration-resolver.service';

class AppsServicesMock {
    getConfigurationList(_serviceKey: string) {}
}

const activatedRouteSnapshotMock: any = jasmine.createSpyObj<ActivatedRouteSnapshot>(
    'ActivatedRouteSnapshot',
    ['toString']
);
activatedRouteSnapshotMock.paramMap = {};

describe('DotAppsConfigurationListResolver', () => {
    let dotAppsServices: DotAppsService;
    let dotAppsConfigurationListResolver: DotAppsConfigurationResolver;

    beforeEach(async(() => {
        const testbed = DOTTestBed.configureTestingModule({
            providers: [
                DotAppsConfigurationResolver,
                { provide: DotAppsService, useClass: AppsServicesMock },
                {
                    provide: ActivatedRouteSnapshot,
                    useValue: activatedRouteSnapshotMock
                }
            ]
        });
        dotAppsServices = testbed.get(DotAppsService);
        dotAppsConfigurationListResolver = testbed.get(
            DotAppsConfigurationResolver
        );
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
        spyOn(dotAppsServices, 'getConfigurationList').and.returnValue(observableOf(response));

        dotAppsConfigurationListResolver
            .resolve(activatedRouteSnapshotMock)
            .subscribe((fakeContentType: any) => {
                expect(fakeContentType).toEqual(response);
            });
        expect(dotAppsServices.getConfigurationList).toHaveBeenCalledWith('123');
    });
});
