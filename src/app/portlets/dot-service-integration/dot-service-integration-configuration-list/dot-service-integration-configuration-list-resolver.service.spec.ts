import { of as observableOf } from 'rxjs';
import { async } from '@angular/core/testing';
import { ActivatedRouteSnapshot } from '@angular/router';
import { DOTTestBed } from '../../../test/dot-test-bed';
import { DotServiceIntegrationService } from '@services/dot-service-integration/dot-service-integration.service';
import { DotServiceIntegrationConfigurationListResolver } from './dot-service-integration-configuration-list-resolver.service';
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
    let dotServiceIntegrationConfigurationListResolver: DotServiceIntegrationConfigurationListResolver;
    const messages = {
        'service.integration.configurations': 'Configurations',
        'service.integration.no.configurations': 'No Configurations',
        'service.integration.key': 'Key:',
        'service.integration.add.configurations': 'No configurations',
        'service.integration.no.configurations.message': 'You do not have configurations',
        'service.integration.add.configurations.button': 'Add Configuration',
        'service.integration.confirmation.delete.all.button': 'Delete All',
        'service.integration.confirmation.title': 'Are you sure?',
        'service.integration.confirmation.delete.message': 'Delete this?',
        'service.integration.confirmation.delete.all.message': 'Delete all?',
        'service.integration.confirmation.accept': 'Ok'
    };
    const messageServiceMock = new MockDotMessageService(messages);

    beforeEach(async(() => {
        const testbed = DOTTestBed.configureTestingModule({
            providers: [
                DotServiceIntegrationConfigurationListResolver,
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
            DotServiceIntegrationConfigurationListResolver
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
                    hostId: '123',
                    hostName: 'demo.dotcms.com'
                },
                {
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
                expect(fakeContentType).toEqual([response, messages]);
            });
        expect(dotIntegrationService.getConfiguration).toHaveBeenCalledWith('123');
    });
});
