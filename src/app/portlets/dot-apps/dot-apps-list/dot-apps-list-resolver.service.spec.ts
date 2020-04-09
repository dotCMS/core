import { of as observableOf } from 'rxjs';
import { async } from '@angular/core/testing';
import { ActivatedRouteSnapshot } from '@angular/router';
import { DOTTestBed } from '../../../test/dot-test-bed';
import { DotAppsService } from '@services/dot-apps/dot-apps.service';
import { DotAppsListResolver } from './dot-apps-list-resolver.service';

class AppsServicesMock {
    get() {}
}

const activatedRouteSnapshotMock: any = jasmine.createSpyObj<ActivatedRouteSnapshot>(
    'ActivatedRouteSnapshot',
    ['toString']
);
activatedRouteSnapshotMock.paramMap = {};

describe('DotAppsListResolver', () => {
    let dotAppsServices: DotAppsService;
    let dotAppsListResolver: DotAppsListResolver;

    beforeEach(async(() => {
        const testbed = DOTTestBed.configureTestingModule({
            providers: [
                DotAppsListResolver,
                { provide: DotAppsService, useClass: AppsServicesMock },
                {
                    provide: ActivatedRouteSnapshot,
                    useValue: activatedRouteSnapshotMock
                }
            ]
        });
        dotAppsServices = testbed.get(DotAppsService);
        dotAppsListResolver = testbed.get(DotAppsListResolver);
    }));

    it('should get and return an Apps list', () => {
        const response = [
            {
                configurationsCount: 0,
                key: 'google-calendar',
                name: 'Google Calendar',
                description: 'It\'s a tool to keep track of your life\'s events',
                iconUrl: '/dA/d948d85c-3bc8-4d85-b0aa-0e989b9ae235/photo/surfer-profile.jpg'
            },
            {
                configurationsCount: 1,
                key: 'asana',
                name: 'Asana',
                description: 'It\'s asana to keep track of your asana events',
                iconUrl: '/dA/792c7c9f-6b6f-427b-80ff-1643376c9999/photo/mountain-persona.jpg'
            }
        ];

        spyOn(dotAppsServices, 'get').and.returnValue(observableOf(response));

        dotAppsListResolver.resolve().subscribe((fakeContentType: any) => {
            expect(fakeContentType).toEqual(response);
        });
        expect(dotAppsServices.get).toHaveBeenCalled();
    });
});
