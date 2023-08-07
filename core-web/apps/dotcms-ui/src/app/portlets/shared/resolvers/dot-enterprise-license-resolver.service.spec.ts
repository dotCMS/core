import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DotLicenseService } from '@dotcms/data-access';
import { CoreWebService, CoreWebServiceMock } from '@dotcms/dotcms-js';
import { DotEnterpriseLicenseResolver } from '@portlets/shared/resolvers/dot-enterprise-license-resolver.service';

describe('DotEnterpriseLicenseResolver', () => {
    let service: DotEnterpriseLicenseResolver;
    let dotLicenseService: DotLicenseService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [
                DotEnterpriseLicenseResolver,
                DotLicenseService,
                { provide: CoreWebService, useClass: CoreWebServiceMock }
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
