import { DOTTestBed } from '../../../test/dot-test-bed';
import { DotLicenseService } from './dot-license.service';
import { ConnectionBackend, ResponseOptions, Response } from '@angular/http';
import { MockBackend } from '@angular/http/testing';
import { of } from 'rxjs';

let lastConnection: any;

function mockConnectionLicenseResponse(levelNumber: number): void {
    return lastConnection.mockRespond(
        new Response(
            new ResponseOptions({
                body: {
                    entity: {
                        config: {
                            license: {
                                level: levelNumber
                            }
                        }
                    }
                }
            })
        )
    );
}

describe('DotLicenseService', () => {
    beforeEach(() => {
        this.injector = DOTTestBed.resolveAndCreate([DotLicenseService]);
        this.dotLicenseService = this.injector.get(DotLicenseService);
        this.backend = this.injector.get(ConnectionBackend) as MockBackend;
        this.backend.connections.subscribe((connection: any) => (lastConnection = connection));
    });

    it('should call the BE with correct endpoint url and method', () => {
        this.dotLicenseService.isEnterprise().subscribe();
        mockConnectionLicenseResponse(100);

        expect(lastConnection.request.method).toBe(0); // 0 is GET method
        expect(lastConnection.request.url).toContain(`v1/appconfiguration`);
    });

    it('should return a false response because license is 100 = Community', () => {
        let result;
        this.dotLicenseService.isEnterprise().subscribe((res) => (result = res));
        mockConnectionLicenseResponse(100);

        expect(result).toBe(false);
    });

    it('should return a true response because license is equal to 200', () => {
        let result: boolean;
        this.dotLicenseService.isEnterprise().subscribe((res) => (result = res));
        mockConnectionLicenseResponse(200);

        expect(result).toBe(true);
    });

    it('should return a true response because license is equal to 400', () => {
        let result: boolean;
        this.dotLicenseService.isEnterprise().subscribe((res) => (result = res));
        mockConnectionLicenseResponse(400);

        expect(result).toBe(true);
    });

    it('should return true with any URL and user has license', () => {
        spyOn(this.dotLicenseService, 'isEnterprise').and.returnValue(of(true));
        let result: boolean;
        this.dotLicenseService
            .canAccessEnterprisePortlet('/whatever')
            .subscribe((res) => (result = res));

        expect(result).toBe(true);
    });

    it('should return true when URL is not enterprise and user do not has license', () => {
        spyOn(this.dotLicenseService, 'isEnterprise').and.returnValue(of(false));
        let result: boolean;
        this.dotLicenseService
            .canAccessEnterprisePortlet('/whatever')
            .subscribe((res) => (result = res));

        expect(result).toBe(true);
    });

    it('should return false when URL is enterprise and user do not has license', () => {
        spyOn(this.dotLicenseService, 'isEnterprise').and.returnValue(of(false));
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
            return this.dotLicenseService
                .canAccessEnterprisePortlet(url)
                .subscribe((res) => expect(res).toBe(false));
        });
    });
});
