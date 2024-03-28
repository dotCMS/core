import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { CoreWebService } from '@dotcms/dotcms-js';
import { FEATURE_FLAG_NOT_FOUND, FeaturedFlags } from '@dotcms/dotcms-models';
import { CoreWebServiceMock } from '@dotcms/utils-testing';

import { DotPropertiesService } from './dot-properties.service';

const fakeResponse = {
    entity: {
        key1: 'data',
        list: ['1', '2'],
        [FeaturedFlags.DOTFAVORITEPAGE_FEATURE_ENABLE]: 'true'
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
        const featureFlag = FeaturedFlags.DOTFAVORITEPAGE_FEATURE_ENABLE;
        expect(service).toBeTruthy();

        service.getFeatureFlag(featureFlag).subscribe((response) => {
            expect(response).toEqual(true);
            done();
        });
        const req = httpMock.expectOne(`/api/v1/configuration/config?keys=${featureFlag}`);
        expect(req.request.method).toBe('GET');
        req.flush(fakeResponse);
    });

    it('should get feature flag values', (done) => {
        const featureFlags = [
            FeaturedFlags.DOTFAVORITEPAGE_FEATURE_ENABLE,
            FeaturedFlags.FEATURE_FLAG_EDIT_URL_CONTENT_MAP
        ];
        const apiResponse = {
            entity: {
                [FeaturedFlags.DOTFAVORITEPAGE_FEATURE_ENABLE]: 'true',
                [FeaturedFlags.FEATURE_FLAG_EDIT_URL_CONTENT_MAP]: FEATURE_FLAG_NOT_FOUND
            }
        };

        service.getFeatureFlags(featureFlags).subscribe((response) => {
            expect(response[FeaturedFlags.DOTFAVORITEPAGE_FEATURE_ENABLE]).toBe(true);
            expect(response[FeaturedFlags.FEATURE_FLAG_EDIT_URL_CONTENT_MAP]).toBe(
                FEATURE_FLAG_NOT_FOUND
            );
            done();
        });
        const req = httpMock.expectOne(`/api/v1/configuration/config?keys=${featureFlags.join()}`);
        expect(req.request.method).toBe('GET');
        req.flush(apiResponse);
    });

    it('should get feature flag value as true when not found', (done) => {
        const featureFlag = FeaturedFlags.FEATURE_FLAG_ANNOUNCEMENTS;
        const apiResponse = {
            entity: {
                [FeaturedFlags.FEATURE_FLAG_ANNOUNCEMENTS]: 'NOT_FOUND'
            }
        };

        service.getFeatureFlag(featureFlag).subscribe((response) => {
            expect(response).toEqual(true);
            done();
        });
        const req = httpMock.expectOne(`/api/v1/configuration/config?keys=${featureFlag}`);
        expect(req.request.method).toBe('GET');
        req.flush(apiResponse);
    });

    afterEach(() => {
        httpMock.verify();
    });
});
