import { of } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree } from '@angular/router';

import { DotCurrentUserService, DotPropertiesService } from '@dotcms/data-access';
import { FeaturedFlags } from '@dotcms/dotcms-models';

import { permissionsTabGuard, styleEditorTabGuard } from './dot-content-type-tabs.guard';

const BASE_URL = '/content-types-angular/edit/123';
const STYLE_EDITOR_URL = `${BASE_URL}/style-editor`;
const PERMISSIONS_URL = `${BASE_URL}/permissions`;
const FIELDS_URL = `${BASE_URL}/fields`;

const mockRoute = {} as ActivatedRouteSnapshot;

function mockState(url: string): RouterStateSnapshot {
    return { url } as RouterStateSnapshot;
}

describe('styleEditorTabGuard', () => {
    let dotPropertiesService: DotPropertiesService;
    let router: Router;

    const setup = (featureFlagEnabled: boolean) => {
        TestBed.configureTestingModule({
            providers: [
                HttpClient,
                {
                    provide: DotPropertiesService,
                    useValue: { getFeatureFlag: jest.fn().mockReturnValue(of(featureFlagEnabled)) }
                },
                {
                    provide: Router,
                    useValue: {
                        parseUrl: jest.fn((url: string) => ({ url }) as unknown as UrlTree)
                    }
                }
            ],
            imports: [HttpClientTestingModule]
        });

        dotPropertiesService = TestBed.inject(DotPropertiesService);
        router = TestBed.inject(Router);
    };

    it('should allow access when feature flag is enabled', (done) => {
        setup(true);

        TestBed.runInInjectionContext(() =>
            styleEditorTabGuard(mockRoute, mockState(STYLE_EDITOR_URL))
        ).subscribe((result) => {
            expect(result).toBe(true);
            expect(dotPropertiesService.getFeatureFlag).toHaveBeenCalledWith(
                FeaturedFlags.FEATURE_FLAG_UVE_STYLE_EDITOR
            );
            done();
        });
    });

    it('should redirect to fields when feature flag is disabled', (done) => {
        setup(false);

        TestBed.runInInjectionContext(() =>
            styleEditorTabGuard(mockRoute, mockState(STYLE_EDITOR_URL))
        ).subscribe((result) => {
            expect(router.parseUrl).toHaveBeenCalledWith(FIELDS_URL);
            expect(result).not.toBe(false);
            done();
        });
    });

    it('should preserve query params in the redirect url', (done) => {
        setup(false);
        const urlWithQuery = `${STYLE_EDITOR_URL}?foo=bar`;

        TestBed.runInInjectionContext(() =>
            styleEditorTabGuard(mockRoute, mockState(urlWithQuery))
        ).subscribe(() => {
            expect(router.parseUrl).toHaveBeenCalledWith(`${FIELDS_URL}?foo=bar`);
            done();
        });
    });
});

describe('permissionsTabGuard', () => {
    let dotCurrentUserService: DotCurrentUserService;
    let router: Router;

    const setup = (hasAccess: boolean) => {
        TestBed.configureTestingModule({
            providers: [
                HttpClient,
                {
                    provide: DotCurrentUserService,
                    useValue: {
                        hasAccessToPortlet: jest.fn().mockReturnValue(of(hasAccess))
                    }
                },
                {
                    provide: Router,
                    useValue: {
                        parseUrl: jest.fn((url: string) => ({ url }) as unknown as UrlTree)
                    }
                }
            ],
            imports: [HttpClientTestingModule]
        });

        dotCurrentUserService = TestBed.inject(DotCurrentUserService);
        router = TestBed.inject(Router);
    };

    it('should allow access when user has permissions portlet access', (done) => {
        setup(true);

        TestBed.runInInjectionContext(() =>
            permissionsTabGuard(mockRoute, mockState(PERMISSIONS_URL))
        ).subscribe((result) => {
            expect(result).toBe(true);
            expect(dotCurrentUserService.hasAccessToPortlet).toHaveBeenCalledWith('permissions');
            done();
        });
    });

    it('should redirect to fields when user lacks permissions portlet access', (done) => {
        setup(false);

        TestBed.runInInjectionContext(() =>
            permissionsTabGuard(mockRoute, mockState(PERMISSIONS_URL))
        ).subscribe((result) => {
            expect(router.parseUrl).toHaveBeenCalledWith(FIELDS_URL);
            expect(result).not.toBe(false);
            done();
        });
    });

    it('should preserve query params in the redirect url', (done) => {
        setup(false);
        const urlWithQuery = `${PERMISSIONS_URL}?foo=bar`;

        TestBed.runInInjectionContext(() =>
            permissionsTabGuard(mockRoute, mockState(urlWithQuery))
        ).subscribe(() => {
            expect(router.parseUrl).toHaveBeenCalledWith(`${FIELDS_URL}?foo=bar`);
            done();
        });
    });
});
