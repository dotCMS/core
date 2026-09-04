import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { FEATURE_FLAG_NOT_FOUND, FeaturedFlags } from '@dotcms/dotcms-models';

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
            providers: [provideHttpClient(), provideHttpClientTesting(), DotPropertiesService]
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

    it('should get boolean-prefixed key using the unprefixed response field', (done) => {
        const key = 'boolean:REPORT_ISSUE_INCLUDE_USER_PII';
        const apiResponse = {
            entity: {
                REPORT_ISSUE_INCLUDE_USER_PII: false
            }
        };

        service.getKey(key).subscribe((response) => {
            expect(response).toBe(false);
            done();
        });

        const req = httpMock.expectOne(`/api/v1/configuration/config?keys=${key}`);
        expect(req.request.method).toBe('GET');
        req.flush(apiResponse);
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
            expect(response[FeaturedFlags.FEATURE_FLAG_EDIT_URL_CONTENT_MAP]).toBe(true);
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

    it('should get feature flags as booleans when API returns JSON boolean values', (done) => {
        const featureFlags = [FeaturedFlags.FEATURE_FLAG_UVE_STYLE_EDITOR];
        const apiResponse: { entity: Record<string, string | boolean> } = {
            entity: {
                [FeaturedFlags.FEATURE_FLAG_UVE_STYLE_EDITOR]: true
            }
        };

        service.getFeatureFlags(featureFlags).subscribe((response) => {
            expect(response[FeaturedFlags.FEATURE_FLAG_UVE_STYLE_EDITOR]).toBe(true);
            done();
        });
        const req = httpMock.expectOne(`/api/v1/configuration/config?keys=${featureFlags.join()}`);
        expect(req.request.method).toBe('GET');
        req.flush(apiResponse);
    });

    it('should get feature flag as true when API returns JSON boolean true', (done) => {
        const featureFlag = FeaturedFlags.FEATURE_FLAG_UVE_STYLE_EDITOR;
        const apiResponse: { entity: Record<string, string | boolean> } = {
            entity: { [featureFlag]: true }
        };

        service.getFeatureFlag(featureFlag).subscribe((response) => {
            expect(response).toBe(true);
            done();
        });
        const req = httpMock.expectOne(`/api/v1/configuration/config?keys=${featureFlag}`);
        expect(req.request.method).toBe('GET');
        req.flush(apiResponse);
    });

    it('should get feature flag as false when API returns JSON boolean false', (done) => {
        const featureFlag = FeaturedFlags.FEATURE_FLAG_UVE_STYLE_EDITOR;
        const apiResponse: { entity: Record<string, string | boolean> } = {
            entity: { [featureFlag]: false }
        };

        service.getFeatureFlag(featureFlag).subscribe((response) => {
            expect(response).toBe(false);
            done();
        });
        const req = httpMock.expectOne(`/api/v1/configuration/config?keys=${featureFlag}`);
        expect(req.request.method).toBe('GET');
        req.flush(apiResponse);
    });

    it('should get fresh feature flag value, coercing the same way as getFeatureFlag', (done) => {
        const featureFlag = FeaturedFlags.FEATURE_FLAG_UVE_STYLE_EDITOR;
        const apiResponse: { entity: Record<string, string | boolean> } = {
            entity: { [featureFlag]: true }
        };

        service.getFreshFeatureFlag(featureFlag).subscribe((response) => {
            expect(response).toBe(true);
            done();
        });
        const req = httpMock.expectOne(`/api/v1/configuration/config?keys=${featureFlag}`);
        expect(req.request.method).toBe('GET');
        req.flush(apiResponse);
    });

    it('should get fresh feature flag as true when not found', (done) => {
        const featureFlag = FeaturedFlags.FEATURE_FLAG_ANNOUNCEMENTS;
        const apiResponse = {
            entity: { [FeaturedFlags.FEATURE_FLAG_ANNOUNCEMENTS]: 'NOT_FOUND' }
        };

        service.getFreshFeatureFlag(featureFlag).subscribe((response) => {
            expect(response).toEqual(true);
            done();
        });
        const req = httpMock.expectOne(`/api/v1/configuration/config?keys=${featureFlag}`);
        expect(req.request.method).toBe('GET');
        req.flush(apiResponse);
    });

    it('should not reuse the featureFlagCache — every call re-fetches from the server', () => {
        const featureFlag = FeaturedFlags.DOTFAVORITEPAGE_FEATURE_ENABLE;

        service.getFreshFeatureFlag(featureFlag).subscribe();
        service.getFreshFeatureFlag(featureFlag).subscribe();

        const reqs = httpMock.match(`/api/v1/configuration/config?keys=${featureFlag}`);
        expect(reqs.length).toBe(2);
        reqs.forEach((req) => req.flush(fakeResponse));
    });

    describe('FEATURE_FLAG_EXPERIMENTS_PORTLET (#37005)', () => {
        const flag = FeaturedFlags.FEATURE_FLAG_EXPERIMENTS_PORTLET;
        const url = `/api/v1/configuration/config?keys=${flag}`;

        it('should read the shipped default of false as false', (done) => {
            service.getFreshFeatureFlag(flag).subscribe((response) => {
                expect(response).toBe(false);
                done();
            });
            httpMock.expectOne(url).flush({ entity: { [flag]: false } });
        });

        it('should read an operator-enabled switch as true', (done) => {
            service.getFreshFeatureFlag(flag).subscribe((response) => {
                expect(response).toBe(true);
                done();
            });
            httpMock.expectOne(url).flush({ entity: { [flag]: true } });
        });

        it('should read the string "false" as false, so an env-var-driven config still resolves', (done) => {
            service.getFreshFeatureFlag(flag).subscribe((response) => {
                expect(response).toBe(false);
                done();
            });
            httpMock.expectOne(url).flush({ entity: { [flag]: 'false' } });
        });

        // Pinned deliberately, and NOT the behaviour #37005 wants: an unset property resolves to
        // ENABLED, per the documented platform rule. This is exactly why the explicit
        // `FEATURE_FLAG_EXPERIMENTS_PORTLET=false` in dotmarketing-config.properties is required
        // rather than decorative — declaring the switch without shipping a value delivers it on,
        // which is FR-013 inverted. If this test ever starts expecting `false`, the shared reader's
        // contract changed and every other flag's default changed with it.
        it('should read an absent property as TRUE — which is why the shipped false is required', (done) => {
            service.getFreshFeatureFlag(flag).subscribe((response) => {
                expect(response).toBe(true);
                done();
            });
            httpMock.expectOne(url).flush({ entity: { [flag]: FEATURE_FLAG_NOT_FOUND } });
        });

        // The same failure mode from the other side: a key omitted from ConfigurationResource's
        // WHITE_LIST is absent from the response body, `getKey` substitutes NOT_FOUND, and the
        // switch reads as enabled with nothing logged. The backend guard is
        // ConfigurationResourceTest#getConfigVariables_experimentsPortletFlagIsWhitelisted_isPresentInResponse.
        it('should read a key missing from the response as TRUE — the WHITE_LIST omission trap', (done) => {
            service.getFreshFeatureFlag(flag).subscribe((response) => {
                expect(response).toBe(true);
                done();
            });
            httpMock.expectOne(url).flush({ entity: {} });
        });
    });

    afterEach(() => {
        httpMock.verify();
    });
});
