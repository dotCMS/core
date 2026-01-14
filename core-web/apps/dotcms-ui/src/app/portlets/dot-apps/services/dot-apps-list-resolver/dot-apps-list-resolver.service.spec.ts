/* eslint-disable @typescript-eslint/no-explicit-any */

import { of as observableOf, of } from 'rxjs';

import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot } from '@angular/router';

import { DotAppsService, DotLicenseService } from '@dotcms/data-access';
import { DotApp } from '@dotcms/dotcms-models';

import { DotAppsListResolver } from './dot-apps-list-resolver.service';

import { appsResponse } from '../../shared/mocks';

class DotLicenseServicesMock {
    canAccessEnterprisePortlet(_url: string) {
        return of(true);
    }
}

const activatedRouteSnapshotMock: any = {};

describe('DotAppsListResolver', () => {
    let dotLicenseServices: DotLicenseService;
    let dotAppsService: DotAppsService;
    let dotAppsListResolver: DotAppsListResolver;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                DotAppsListResolver,
                { provide: DotLicenseService, useClass: DotLicenseServicesMock },
                {
                    provide: DotAppsService,
                    useValue: { get: jest.fn().mockReturnValue(of(appsResponse)) }
                },
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
        jest.spyOn(dotLicenseServices, 'canAccessEnterprisePortlet').mockReturnValue(
            observableOf(true)
        );
        jest.spyOn(dotAppsService, 'get').mockReturnValue(of(appsResponse));

        dotAppsListResolver.resolve(activatedRouteSnapshotMock).subscribe((apps: DotApp[]) => {
            expect(apps).toEqual(appsResponse);
        });
        expect(dotLicenseServices.canAccessEnterprisePortlet).toHaveBeenCalledWith('/apps');
        expect(dotLicenseServices.canAccessEnterprisePortlet).toHaveBeenCalledTimes(1);
    });
});
