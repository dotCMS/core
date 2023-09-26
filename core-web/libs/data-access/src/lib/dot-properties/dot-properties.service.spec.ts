import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { CoreWebService } from '@dotcms/dotcms-js';
import { ENABLED_FEATURE_FLAGS } from '@dotcms/dotcms-models';
import { CoreWebServiceMock } from '@dotcms/utils-testing';

import { DotPropertiesService } from './dot-properties.service';

const fakeResponse = {
    entity: {
        key1: 'data',
        list: ['1', '2']
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

    describe('getKey', () => {
        it('should return the value of a key if it exists in featureConfig', () => {
            const key = 'existingKey';
            const value = 'existingValue';
            service.featureConfig = { [key]: value };
            service.getKey(key).subscribe((result) => {
                expect(result).toEqual(value);
            });
        });

        it('should make an HTTP request if the key does not exist in featureConfig', () => {
            const key = 'nonExistingKey';
            const value = 'nonExistingValue';
            service.getKey(key).subscribe((result) => {
                expect(result).toEqual(value);
            });
            const req = httpMock.expectOne(`/api/v1/configuration/config?keys=${key}`);
            expect(req.request.method).toEqual('GET');
            req.flush({ entity: { [key]: value } });
        });
    });

    describe('getKeys', () => {
        it('should return the values of all keys if they exist in featureConfig', () => {
            const keys = ['existingKey1', 'existingKey2'];
            const values = { [keys[0]]: 'existingValue1', [keys[1]]: 'existingValue2' };
            service.featureConfig = values;
            service.getKeys(keys).subscribe((result) => {
                expect(result).toEqual(values);
            });
        });

        it('should make an HTTP request if any key does not exist in featureConfig', () => {
            const keys = ['existingKey', 'nonExistingKey'];
            const values = { [keys[0]]: 'existingValue', [keys[1]]: 'nonExistingValue' };
            service.getKeys(keys).subscribe((result) => {
                expect(result).toEqual(values);
            });
            const req = httpMock.expectOne(`/api/v1/configuration/config?keys=${keys.join()}`);
            expect(req.request.method).toEqual('GET');
            req.flush({ entity: values });
        });
    });

    describe('getKeyAsList', () => {
        it('should make an HTTP request and return the value of a key as a list', () => {
            const key = 'listKey';
            const value = ['value1', 'value2'];
            service.getKeyAsList(key).subscribe((result) => {
                expect(result).toEqual(value);
            });
            const req = httpMock.expectOne(`/api/v1/configuration/config?keys=list:${key}`);
            expect(req.request.method).toEqual('GET');
            req.flush({ entity: { [key]: value } });
        });
    });

    describe('loadConfig', () => {
        it('should load the configuration for the feature flags', () => {
            const keys = ENABLED_FEATURE_FLAGS;
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
