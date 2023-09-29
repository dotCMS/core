import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { CoreWebService } from '@dotcms/dotcms-js';
import { FeaturedFlags } from '@dotcms/dotcms-models';
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

    it('should get key', (done) => {
        const key = 'key1';
        expect(service).toBeTruthy();

        service.getKey(key).subscribe((response) => {
            expect(response).toEqual(fakeResponse.entity.key1);
            done();
        });
        const req = httpMock.expectOne(`/api/v1/configuration/config?keys=${key}`);
        expect(req.request.method).toBe('GET');
        req.flush(fakeResponse);
    });

    it('should get ky as a list', (done) => {
        const key = 'list';
        expect(service).toBeTruthy();

        service.getKeyAsList(key).subscribe((response) => {
            expect(response).toEqual(fakeResponse.entity.list);
            done();
        });
        const req = httpMock.expectOne(`/api/v1/configuration/config?keys=list:${key}`);
        expect(req.request.method).toBe('GET');
        req.flush(fakeResponse);
    });

    it('should get keys', (done) => {
        const keys = ['key1', 'key2'];
        const apiResponse = {
            entity: {
                key1: 'test',
                key2: 'test2'
            }
        };

        service.getKeys(keys).subscribe((response) => {
            expect(response).toEqual(apiResponse.entity);
            done();
        });
        const req = httpMock.expectOne(`/api/v1/configuration/config?keys=${keys.join()}`);
        expect(req.request.method).toBe('GET');
        req.flush(apiResponse);
    });

    it('should get feature flag value', (done) => {
        const featureFlag = 'featureFlag';
        expect(service).toBeTruthy();

        service.getFeatureFlagValue(featureFlag).subscribe((response) => {
            expect(response).toEqual(true);
            done();
        });
        const req = httpMock.expectOne(`/api/v1/configuration/config?keys=${featureFlag}`);
        expect(req.request.method).toBe('GET');
        req.flush(fakeResponse);
    });

    it('should get feature flag values', (done) => {
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
            done();
        });
        const req = httpMock.expectOne(`/api/v1/configuration/config?keys=${featureFlags.join()}`);
        expect(req.request.method).toBe('GET');
        req.flush(apiResponse);
    });

    describe('getKey', () => {
        it('should return the value of a key if it exists in featureConfig', (done) => {
            const key = 'existingKey';
            const value = 'existingValue';
            service.featureConfig = { [key]: value };
            service.getKey(key).subscribe((result) => {
                expect(result).toEqual(value);
                done();
            });
            httpMock.expectNone(`/api/v1/configuration/config?keys=${key}`);
        });

        it('should make an HTTP request if the key does not exist in featureConfig', (done) => {
            const key = 'nonExistingKey';
            const value = 'nonExistingValue';
            service.getKey(key).subscribe((result) => {
                expect(result).toEqual(value);
                done();
            });
            const req = httpMock.expectOne(`/api/v1/configuration/config?keys=${key}`);
            expect(req.request.method).toEqual('GET');
            req.flush({ entity: { [key]: value } });
        });
    });

    describe('getKeys', () => {
        it('should return the values of all keys if they exist in featureConfig', (done) => {
            const keys = ['existingKey1', 'existingKey2'];
            const values = { [keys[0]]: 'existingValue1', [keys[1]]: 'existingValue2' };
            service.featureConfig = values;
            service.getKeys(keys).subscribe((result) => {
                expect(result).toEqual(values);
                done();
            });
        });

        it('should make an HTTP request if any key does not exist in featureConfig', (done) => {
            const keys = ['existingKey', 'nonExistingKey'];
            const values = { [keys[0]]: 'existingValue', [keys[1]]: 'nonExistingValue' };
            service.getKeys(keys).subscribe((result) => {
                expect(result).toEqual(values);
                done();
            });
            const req = httpMock.expectOne(`/api/v1/configuration/config?keys=${keys.join()}`);
            expect(req.request.method).toEqual('GET');
            req.flush({ entity: values });
        });
    });

    describe('getKeyAsList', () => {
        it('should make an HTTP request and return the value of a key as a list', (done) => {
            const key = 'listKey';
            const value = ['value1', 'value2'];
            service.getKeyAsList(key).subscribe((result) => {
                expect(result).toEqual(value);
                done();
            });
            const req = httpMock.expectOne(`/api/v1/configuration/config?keys=list:${key}`);
            expect(req.request.method).toEqual('GET');
            req.flush({ entity: { [key]: value } });
        });
    });

    describe('loadConfig', () => {
        it('should load the configuration for the feature flags', () => {
            const keys = Object.values(FeaturedFlags);
            const values = { key1: 'value1', key2: 'value2' };

            service.loadConfig();

            const req = httpMock.expectOne(`/api/v1/configuration/config?keys=${keys.join()}`);
            expect(req.request.method).toBe('GET');
            req.flush({ entity: values });

            expect(service.featureConfig).toEqual(values);
        });
    });

    afterEach(() => {
        httpMock.verify();
    });
});
