import { of } from 'rxjs';

import { HttpTestingController, HttpClientTestingModule } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DotLicenseService } from './dot-license.service';

describe('DotLicenseService', () => {
    let dotLicenseService: DotLicenseService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [DotLicenseService]
        });
        dotLicenseService = TestBed.inject(DotLicenseService);
        httpMock = TestBed.inject(HttpTestingController);
    });

    it('should call the BE with correct endpoint url and method', () => {
        dotLicenseService.isEnterprise().subscribe();

        const req = httpMock.expectOne('/api/v1/appconfiguration');
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

        const req = httpMock.expectOne('/api/v1/appconfiguration');
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

        const req = httpMock.expectOne('/api/v1/appconfiguration');
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

        const req = httpMock.expectOne('/api/v1/appconfiguration');
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
        jest.spyOn(dotLicenseService, 'isEnterprise').mockReturnValue(of(true));
        let result: boolean;
        dotLicenseService
            .canAccessEnterprisePortlet('/whatever')
            .subscribe((res) => (result = res));

        expect(result).toBe(true);
    });

    it('should return true when URL is not enterprise and user do not has license', () => {
        jest.spyOn(dotLicenseService, 'isEnterprise').mockReturnValue(of(false));
        let result: boolean;
        dotLicenseService
            .canAccessEnterprisePortlet('/whatever')
            .subscribe((res) => (result = res));

        expect(result).toBe(true);
    });

    it('should return false when URL is enterprise and user do not has license', () => {
        jest.spyOn(dotLicenseService, 'isEnterprise').mockReturnValue(of(false));
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

    it('should not make any request if license is setted', () => {
        dotLicenseService.setLicense({
            displayServerId: 'test',
            isCommunity: false,
            level: 300,
            levelName: 'test level'
        });

        dotLicenseService.isEnterprise().subscribe((result) => {
            expect(result).toBe(true);
        });

        httpMock.expectNone('/api/v1/appconfiguration');
    });

    it('should fetch the license when calling updateLicense', () => {
        dotLicenseService.updateLicense();

        const req = httpMock.expectOne('/api/v1/appconfiguration');
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

    afterEach(() => {
        httpMock.verify();
    });
});
