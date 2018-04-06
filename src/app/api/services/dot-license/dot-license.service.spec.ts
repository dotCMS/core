import { TestBed } from '@angular/core/testing';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Observable';
import { LoginService } from 'dotcms-js/dotcms-js';
import { DotRouterService } from '../dot-router/dot-router.service';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { ConfigParams, DotcmsConfig } from 'dotcms-js/core/dotcms-config.service';
import { DotLicenseService } from './dot-license.service';

@Injectable()
class MockDotcmsConfigService {
    getConfig(): Observable<ConfigParams> {
        return Observable.of({
            license: {
                level: 100
            },
            disabledWebsockets: '',
            websocketReconnectTime: 0,
            websocketEndpoints: '',
            websocketsSystemEventsEndpoint: '',
            websocketBaseURL: '',
            websocketProtocol: '',
            menu: [],
            paginatorRows: 0,
            paginatorLinks: 0,
            emailRegex: ''
        });
    }
}

describe('DotLicenseService', () => {
    let dotCmsConfig: DotcmsConfig;
    let dotLicenseService: DotLicenseService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [DotLicenseService, { provide: DotcmsConfig, useClass: MockDotcmsConfigService }]
        });

        dotCmsConfig = TestBed.get(DotcmsConfig);
        dotLicenseService = TestBed.get(DotLicenseService);
    });

    it('should return a false response because license is 100 = Community', () => {
        let result: boolean;
        dotLicenseService.isEnterpriseLicense().subscribe((res) => (result = res));

        expect(result).toBe(false);
    });

    it('should return a true response because license is equal to 200', () => {
        let result: boolean;

        spyOn(dotCmsConfig, 'getConfig').and.returnValue(
            Observable.of({
                license: {
                    level: 200
                }
            })
        );

        dotLicenseService.isEnterpriseLicense().subscribe((res) => (result = res));
        expect(result).toBe(true);
    });

    it('should return a true response because license is equal to 400', () => {
        let result: boolean;

        spyOn(dotCmsConfig, 'getConfig').and.returnValue(
            Observable.of({
                license: {
                    level: 400
                }
            })
        );

        dotLicenseService.isEnterpriseLicense().subscribe((res) => (result = res));
        expect(result).toBe(true);
    });
});
