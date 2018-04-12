import { DOTTestBed } from '../../../test/dot-test-bed';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Observable';
import { LoginService } from 'dotcms-js/dotcms-js';
import { DotRouterService } from '../dot-router/dot-router.service';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { ConfigParams } from 'dotcms-js/core/dotcms-config.service';
import { DotLicenseService } from './dot-license.service';
import { ConnectionBackend, ResponseOptions, Response } from '@angular/http';
import { MockBackend } from '@angular/http/testing';

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
        this.dotLicenseService.isEnterpriseLicense().subscribe();
        mockConnectionLicenseResponse(100);

        expect(lastConnection.request.method).toBe(0); // 0 is GET method
        expect(lastConnection.request.url).toContain(`v1/appconfiguration`);
    });

    it('should return a false response because license is 100 = Community', () => {
        let result;
        this.dotLicenseService.isEnterpriseLicense().subscribe((res) => (result = res));
        mockConnectionLicenseResponse(100);

        expect(result).toBe(false);
    });

    it('should return a true response because license is equal to 200', () => {
        let result: boolean;
        this.dotLicenseService.isEnterpriseLicense().subscribe((res) => (result = res));
        mockConnectionLicenseResponse(200);

        expect(result).toBe(true);
    });

    it('should return a true response because license is equal to 400', () => {
        let result: boolean;
        this.dotLicenseService.isEnterpriseLicense().subscribe((res) => (result = res));
        mockConnectionLicenseResponse(400);

        expect(result).toBe(true);
    });
});
