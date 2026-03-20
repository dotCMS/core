import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DotLicenseService } from '@dotcms/data-access';

import { DotEnterpriseLicenseResolver } from './dot-enterprise-license-resolver.service';

describe('DotEnterpriseLicenseResolver', () => {
    let service: DotEnterpriseLicenseResolver;
    let dotLicenseService: DotLicenseService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                provideHttpClient(),
                provideHttpClientTesting(),
                DotEnterpriseLicenseResolver,
                DotLicenseService
            ]
        });
        service = TestBed.inject(DotEnterpriseLicenseResolver);
        dotLicenseService = TestBed.inject(DotLicenseService);
    });

    it('should call dotLicenseService', () => {
        jest.spyOn(dotLicenseService, 'isEnterprise').mockReturnValue(of(true));

        service.resolve().subscribe(() => {
            expect(dotLicenseService.isEnterprise).toHaveBeenCalled();
        });
    });
});
