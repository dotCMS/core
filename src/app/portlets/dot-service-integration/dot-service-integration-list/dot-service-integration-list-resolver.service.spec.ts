import { of as observableOf } from 'rxjs';
import { async } from '@angular/core/testing';
import { ActivatedRouteSnapshot } from '@angular/router';
import { DOTTestBed } from '../../../test/dot-test-bed';
import { DotServiceIntegrationService } from '@services/dot-service-integration/dot-service-integration.service';
import { DotServiceIntegrationListResolver } from './dot-service-integration-list-resolver.service';

class IntegrationServiceMock {
    get() {}
}

const activatedRouteSnapshotMock: any = jasmine.createSpyObj<ActivatedRouteSnapshot>(
    'ActivatedRouteSnapshot',
    ['toString']
);
activatedRouteSnapshotMock.paramMap = {};

describe('DotServiceIntegrationListResolver', () => {
    let dotIntegrationService: DotServiceIntegrationService;
    let dotServiceIntegrationListResolver: DotServiceIntegrationListResolver;

    beforeEach(async(() => {
        const testbed = DOTTestBed.configureTestingModule({
            providers: [
                DotServiceIntegrationListResolver,
                { provide: DotServiceIntegrationService, useClass: IntegrationServiceMock },
                {
                    provide: ActivatedRouteSnapshot,
                    useValue: activatedRouteSnapshotMock
                }
            ]
        });
        dotIntegrationService = testbed.get(DotServiceIntegrationService);
        dotServiceIntegrationListResolver = testbed.get(DotServiceIntegrationListResolver);
    }));

    it('should get and return a content type', () => {
        const response = [
            {
                configurationsCount: 0,
                key: 'google-calendar',
                name: 'Google Calendar',
                description: "It's a tool to keep track of your life's events",
                iconUrl: '/dA/d948d85c-3bc8-4d85-b0aa-0e989b9ae235/photo/surfer-profile.jpg'
            },
            {
                configurationsCount: 1,
                key: 'asana',
                name: 'Asana',
                description: "It's asana to keep track of your asana events",
                iconUrl: '/dA/792c7c9f-6b6f-427b-80ff-1643376c9999/photo/mountain-persona.jpg'
            }
        ];

        spyOn(dotIntegrationService, 'get').and.returnValue(observableOf(response));

        dotServiceIntegrationListResolver.resolve().subscribe((fakeContentType: any) => {
            expect(fakeContentType).toEqual(response);
        });
        expect(dotIntegrationService.get).toHaveBeenCalled();
    });
});
