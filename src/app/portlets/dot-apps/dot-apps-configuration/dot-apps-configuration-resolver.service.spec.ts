import { of as observableOf } from 'rxjs';
import { async } from '@angular/core/testing';
import { ActivatedRouteSnapshot } from '@angular/router';
import { DOTTestBed } from '../../../test/dot-test-bed';
import { DotAppsService } from '@services/dot-apps/dot-apps.service';
import { DotAppsConfigurationResolver } from './dot-apps-configuration-resolver.service';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { DotMessageService } from '@services/dot-messages-service';

class AppsServicesMock {
    getConfiguration(_serviceKey: string) {}
}

const activatedRouteSnapshotMock: any = jasmine.createSpyObj<ActivatedRouteSnapshot>(
    'ActivatedRouteSnapshot',
    ['toString']
);
activatedRouteSnapshotMock.paramMap = {};

describe('DotAppsConfigurationListResolver', () => {
    let dotAppsServices: DotAppsService;
    let dotAppsConfigurationListResolver: DotAppsConfigurationResolver;
    const messages = {
        'apps.configurations': 'Configurations',
        'apps.no.configurations': 'No Configurations',
        'apps.confirmation.delete.all.button': 'Delete All',
        'apps.confirmation.title': 'Are you sure?',
        'apps.key': 'Key:',
        'apps.confirmation.description.show.more': 'Show More',
        'apps.confirmation.description.show.less': 'Show Less',
        'apps.confirmation.delete.all.message': 'Delete all?',
        'apps.confirmation.accept': 'Ok',
        'apps.search.placeholder': 'Search'
    };
    const messageServiceMock = new MockDotMessageService(messages);

    beforeEach(async(() => {
        const testbed = DOTTestBed.configureTestingModule({
            providers: [
                DotAppsConfigurationResolver,
                { provide: DotMessageService, useValue: messageServiceMock },
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

    it('should get and return service integration with configurations', () => {
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
        spyOn(dotAppsServices, 'getConfiguration').and.returnValue(observableOf(response));

        dotAppsConfigurationListResolver
            .resolve(activatedRouteSnapshotMock)
            .subscribe((fakeContentType: any) => {
                expect(fakeContentType).toEqual({service: response, messages});
            });
        expect(dotAppsServices.getConfiguration).toHaveBeenCalledWith('123');
    });
});
