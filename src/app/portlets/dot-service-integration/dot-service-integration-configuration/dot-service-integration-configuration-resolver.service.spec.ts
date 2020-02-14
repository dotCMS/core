import { of as observableOf } from 'rxjs';
import { async } from '@angular/core/testing';
import { ActivatedRouteSnapshot } from '@angular/router';
import { DOTTestBed } from '../../../test/dot-test-bed';
import { DotServiceIntegrationService } from '@services/dot-service-integration/dot-service-integration.service';
import { DotServiceIntegrationConfigurationResolver } from './dot-service-integration-configuration-resolver.service';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { DotMessageService } from '@services/dot-messages-service';

class IntegrationServiceMock {
    getConfiguration(_serviceKey: string) {}
}

const activatedRouteSnapshotMock: any = jasmine.createSpyObj<ActivatedRouteSnapshot>(
    'ActivatedRouteSnapshot',
    ['toString']
);
activatedRouteSnapshotMock.paramMap = {};

describe('DotServiceIntegrationConfigurationListResolver', () => {
    let dotIntegrationService: DotServiceIntegrationService;
    let dotServiceIntegrationConfigurationListResolver: DotServiceIntegrationConfigurationResolver;
    const messages = {
        'service.integration.configurations': 'Configurations',
        'service.integration.no.configurations': 'No Configurations',
        'service.integration.confirmation.delete.all.button': 'Delete All',
        'service.integration.confirmation.title': 'Are you sure?',
        'service.integration.key': 'Key:',
        'service.integration.confirmation.description.show.more': 'Show More',
        'service.integration.confirmation.description.show.less': 'Show Less',
        'service.integration.confirmation.delete.all.message': 'Delete all?',
        'service.integration.confirmation.accept': 'Ok',
        'service.integration.search.placeholder': 'Search'
    };
    const messageServiceMock = new MockDotMessageService(messages);

    beforeEach(async(() => {
        const testbed = DOTTestBed.configureTestingModule({
            providers: [
                DotServiceIntegrationConfigurationResolver,
                { provide: DotMessageService, useValue: messageServiceMock },
                { provide: DotServiceIntegrationService, useClass: IntegrationServiceMock },
                {
                    provide: ActivatedRouteSnapshot,
                    useValue: activatedRouteSnapshotMock
                }
            ]
        });
        dotIntegrationService = testbed.get(DotServiceIntegrationService);
        dotServiceIntegrationConfigurationListResolver = testbed.get(
            DotServiceIntegrationConfigurationResolver
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
        spyOn(dotIntegrationService, 'getConfiguration').and.returnValue(observableOf(response));

        dotServiceIntegrationConfigurationListResolver
            .resolve(activatedRouteSnapshotMock)
            .subscribe((fakeContentType: any) => {
                expect(fakeContentType).toEqual({service: response, messages});
            });
        expect(dotIntegrationService.getConfiguration).toHaveBeenCalledWith('123');
    });
});
