import { of } from 'rxjs';

import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DotLicenseService } from '@dotcms/data-access';
import { CoreWebService, CoreWebServiceMock } from '@dotcms/dotcms-js';
import { DotEnterpriseLicenseResolver } from '@dotcms/ui';

describe('DotEnterpriseLicenseResolver', () => {
    let service: DotEnterpriseLicenseResolver;
    let dotLicenseService: DotLicenseService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [],
            providers: [
                DotEnterpriseLicenseResolver,
                DotLicenseService,
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                provideHttpClient(withInterceptorsFromDi()),
                provideHttpClientTesting()
            ]
        });
        service = TestBed.inject(DotEnterpriseLicenseResolver);
        dotLicenseService = TestBed.inject(DotLicenseService);
    });

    it('should call dotLicenseService', () => {
        spyOn(dotLicenseService, 'isEnterprise').and.returnValue(of(true));
        service.resolve().subscribe(() => {
            expect(dotLicenseService.isEnterprise).toHaveBeenCalled();
        });
    });
});
