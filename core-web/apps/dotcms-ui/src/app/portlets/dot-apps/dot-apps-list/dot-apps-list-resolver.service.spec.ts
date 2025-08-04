/* eslint-disable @typescript-eslint/no-explicit-any */

import { of as observableOf, of } from 'rxjs';

import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';

import { DotLicenseService } from '@dotcms/data-access';

import { DotAppsListResolver } from './dot-apps-list-resolver.service';
import { appsResponse, AppsServicesMock } from './dot-apps-list.component.spec';

import { DotAppsService } from '../../../api/services/dot-apps/dot-apps.service';

class DotLicenseServicesMock {
    canAccessEnterprisePortlet(_url: string) {
        of(true);
    }
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
    let dotAppsService: DotAppsService;
    let dotAppsListResolver: DotAppsListResolver;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                DotAppsListResolver,
                { provide: DotLicenseService, useClass: DotLicenseServicesMock },
                { provide: DotAppsService, useClass: AppsServicesMock },
                {
                    provide: ActivatedRouteSnapshot,
                    useValue: activatedRouteSnapshotMock
                }
            ]
        });
        dotAppsService = TestBed.inject(DotAppsService);
        dotLicenseServices = TestBed.inject(DotLicenseService);
        dotAppsListResolver = TestBed.inject(DotAppsListResolver);
    });

    it('should get if portlet can be accessed', () => {
        spyOn(dotLicenseServices, 'canAccessEnterprisePortlet').and.returnValue(observableOf(true));
        spyOn(dotAppsService, 'get').and.returnValue(of(appsResponse));

        dotAppsListResolver
            .resolve(activatedRouteSnapshotMock, routerStateSnapshotMock)
            .subscribe((resolverData: any) => {
                expect(resolverData).toEqual({
                    apps: appsResponse,
                    isEnterpriseLicense: true
                });
            });
        expect(dotLicenseServices.canAccessEnterprisePortlet).toHaveBeenCalledWith('/apps');
    });
});
