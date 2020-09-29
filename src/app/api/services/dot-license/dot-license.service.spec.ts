import { DotLicenseService } from './dot-license.service';
import { TestBed, getTestBed } from '@angular/core/testing';
import { HttpTestingController, HttpClientTestingModule } from '@angular/common/http/testing';
import { CoreWebService } from 'dotcms-js';
import { CoreWebServiceMock } from 'projects/dotcms-js/src/lib/core/core-web.service.mock';
import { of } from 'rxjs';

describe('DotLicenseService', () => {
    let injector: TestBed;
    let dotLicenseService: DotLicenseService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                DotLicenseService
            ]
        });
        injector = getTestBed();
        dotLicenseService = injector.get(DotLicenseService);
        httpMock = injector.get(HttpTestingController);
    });

    it('should call the BE with correct endpoint url and method', () => {
        dotLicenseService.isEnterprise().subscribe();

        const req = httpMock.expectOne('v1/appconfiguration');
        expect(req.request.method).toBe('GET');
        req.flush({
            entity: {
                config: {
                    license: {
                        level: 100
                    }
                }
            }
        });
    });

    it('should return a false response because license is 100 = Community', () => {
        dotLicenseService.isEnterprise().subscribe((result) => {
            expect(result).toBe(false);
        });

        const req = httpMock.expectOne('v1/appconfiguration');
        expect(req.request.method).toBe('GET');
        req.flush({
            entity: {
                config: {
                    license: {
                        level: 100
                    }
                }
            }
        });
    });

    it('should return a true response because license is equal to 200', () => {
        dotLicenseService.isEnterprise().subscribe((result) => {
            expect(result).toBe(true);
        });

        const req = httpMock.expectOne('v1/appconfiguration');
        expect(req.request.method).toBe('GET');
        req.flush({
            entity: {
                config: {
                    license: {
                        level: 200
                    }
                }
            }
        });
    });

    it('should return a true response because license is equal to 400', () => {
        dotLicenseService.isEnterprise().subscribe((result) => {
            expect(result).toBe(true);
        });

        const req = httpMock.expectOne('v1/appconfiguration');
        expect(req.request.method).toBe('GET');
        req.flush({
            entity: {
                config: {
                    license: {
                        level: 400
                    }
                }
            }
        });
    });

    it('should return true with any URL and user has license', () => {
        spyOn(dotLicenseService, 'isEnterprise').and.returnValue(of(true));
        let result: boolean;
        dotLicenseService
            .canAccessEnterprisePortlet('/whatever')
            .subscribe((res) => (result = res));

        expect(result).toBe(true);
    });

    it('should return true when URL is not enterprise and user do not has license', () => {
        spyOn(dotLicenseService, 'isEnterprise').and.returnValue(of(false));
        let result: boolean;
        dotLicenseService
            .canAccessEnterprisePortlet('/whatever')
            .subscribe((res) => (result = res));

        expect(result).toBe(true);
    });

    it('should return false when URL is enterprise and user do not has license', () => {
        spyOn(dotLicenseService, 'isEnterprise').and.returnValue(of(false));
        const urls = [
            '/rules',
            '/c/publishing-queue',
            '/c/site-search',
            '/c/time-machine',
            '/c/workflow-schemes',
            '/c/es-search',
            '/forms',
            '/apps'
        ];
        urls.forEach((url) => {
            return dotLicenseService
                .canAccessEnterprisePortlet(url)
                .subscribe((res) => expect(res).toBe(false));
        });
    });

    afterEach(() => {
        httpMock.verify();
    });
});
