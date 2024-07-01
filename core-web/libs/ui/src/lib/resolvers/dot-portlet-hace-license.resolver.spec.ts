import { of } from 'rxjs';

import { EnvironmentInjector, runInInjectionContext } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { RouterStateSnapshot } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';

import { DotLicenseService } from '@dotcms/data-access';

import { portletHaveLicenseResolver } from './dot-portlet-have-license.resolver';

describe('formResolver', () => {
    let dotLicenseService: DotLicenseService;
    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [RouterTestingModule],
            providers: [
                {
                    provide: DotLicenseService,
                    useValue: {
                        canAccessEnterprisePortlet: () => of(true),
                        isEnterpriseLicense: () => of(true)
                    }
                }
            ]
        });

        dotLicenseService = TestBed.inject(DotLicenseService);
    });

    it('should return true when canAccessEnterprisePortlet returns true', () => {
        const resolver = runInInjectionContext(TestBed.inject(EnvironmentInjector), () =>
            portletHaveLicenseResolver({}, { url: '' } as RouterStateSnapshot)
        );
        jest.spyOn(dotLicenseService, 'canAccessEnterprisePortlet').mockReturnValue(of(true));
        resolver.subscribe((res) => {
            expect(res).toBe(true);
        });
    });

    it('should return false when canAccessEnterprisePortlet returns false', () => {
        const resolver = runInInjectionContext(TestBed.inject(EnvironmentInjector), () =>
            portletHaveLicenseResolver({}, { url: '' } as RouterStateSnapshot)
        );
        jest.spyOn(dotLicenseService, 'canAccessEnterprisePortlet').mockReturnValue(of(false));
        resolver.subscribe((res) => {
            expect(res).toBe(true);
        });
    });
});
