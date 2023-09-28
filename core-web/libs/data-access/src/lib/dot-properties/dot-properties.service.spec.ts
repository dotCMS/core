import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { CoreWebService } from '@dotcms/dotcms-js';
import { CoreWebServiceMock } from '@dotcms/utils-testing';

import { DotPropertiesService } from './dot-properties.service';

const fakeResponse = {
    entity: {
        key1: 'data',
        list: ['1', '2'],
        featureFlag: 'true'
    }
};

describe('DotPropertiesService', () => {
    let service: DotPropertiesService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                DotPropertiesService
            ]
        });
        service = TestBed.inject(DotPropertiesService);
        httpMock = TestBed.inject(HttpTestingController);
    });

    it('should get key', () => {
        const key = 'key1';
        expect(service).toBeTruthy();

        service.getKey(key).subscribe((response) => {
            expect(response).toEqual(fakeResponse.entity.key1);
        });
        const req = httpMock.expectOne(`/api/v1/configuration/config?keys=${key}`);
        expect(req.request.method).toBe('GET');
        req.flush(fakeResponse);
    });

    it('should get ky as a list', () => {
        const key = 'list';
        expect(service).toBeTruthy();

        service.getKeyAsList(key).subscribe((response) => {
            expect(response).toEqual(fakeResponse.entity.list);
        });
        const req = httpMock.expectOne(`/api/v1/configuration/config?keys=list:${key}`);
        expect(req.request.method).toBe('GET');
        req.flush(fakeResponse);
    });

    it('should get keys', () => {
        const keys = ['key1', 'key2'];
        const apiResponse = {
            entity: {
                key1: 'test',
                key2: 'test2'
            }
        };

        service.getKeys(keys).subscribe((response) => {
            expect(response).toEqual(apiResponse.entity);
        });
        const req = httpMock.expectOne(`/api/v1/configuration/config?keys=${keys.join()}`);
        expect(req.request.method).toBe('GET');
        req.flush(apiResponse);
    });

    it('should get feature flag value', () => {
        const featureFlag = 'featureFlag';
        expect(service).toBeTruthy();

        service.getFeatureFlagValue(featureFlag).subscribe((response) => {
            expect(response).toEqual(true);
        });
        const req = httpMock.expectOne(`/api/v1/configuration/config?keys=${featureFlag}`);
        expect(req.request.method).toBe('GET');
        req.flush(fakeResponse);
    });

    it('should get feature flag values', () => {
        const featureFlags = ['featureFlag', 'featureFlag2'];
        const apiResponse = {
            entity: {
                featureFlag: 'true',
                featureFlag2: 'NOT_FOUND'
            }
        };

        service.getFeatureFlagsValues(featureFlags).subscribe((response) => {
            expect(response['featureFlag']).toBe(true);
            expect(response['featureFlag2']).toBe(false);
        });
        const req = httpMock.expectOne(`/api/v1/configuration/config?keys=${featureFlags.join()}`);
        expect(req.request.method).toBe('GET');
        req.flush(apiResponse);
    });

    afterEach(() => {
        httpMock.verify();
    });
});
