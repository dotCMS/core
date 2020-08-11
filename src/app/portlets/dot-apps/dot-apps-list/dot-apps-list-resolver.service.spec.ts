import { of as observableOf } from 'rxjs';
import { async, TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { DotAppsListResolver } from './dot-apps-list-resolver.service';
import { DotLicenseService } from '@services/dot-license/dot-license.service';

class DotLicenseServicesMock {
    canAccessEnterprisePortlet(_url: string) {}
}

const activatedRouteSnapshotMock: any = jasmine.createSpyObj<ActivatedRouteSnapshot>(
    'ActivatedRouteSnapshot',
    ['toString']
);

const routerStateSnapshotMock = jasmine.createSpyObj<RouterStateSnapshot>('RouterStateSnapshot', [
    'toString'
]);
routerStateSnapshotMock.url = '/apps';

describe('DotAppsListResolver', () => {
    let dotLicenseServices: DotLicenseService;
    let dotAppsListResolver: DotAppsListResolver;

    beforeEach(async(() => {
        const testbed = TestBed.configureTestingModule({
            providers: [
                DotAppsListResolver,
                { provide: DotLicenseService, useClass: DotLicenseServicesMock },
                {
                    provide: ActivatedRouteSnapshot,
                    useValue: activatedRouteSnapshotMock
                }
            ]
        });
        dotLicenseServices = testbed.get(DotLicenseService);
        dotAppsListResolver = testbed.get(DotAppsListResolver);
    }));

    it('should get if portlet can be accessed', () => {
        spyOn(dotLicenseServices, 'canAccessEnterprisePortlet').and.returnValue(observableOf(true));

        dotAppsListResolver
            .resolve(activatedRouteSnapshotMock, routerStateSnapshotMock)
            .subscribe((canAccess: any) => {
                expect(canAccess).toEqual(true);
            });
        expect(dotLicenseServices.canAccessEnterprisePortlet).toHaveBeenCalledWith('/apps');
    });
});
