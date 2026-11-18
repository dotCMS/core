import { Observable, of } from 'rxjs';
import { vi } from 'vitest';

import { HttpClient } from '@angular/common/http';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import {
    ActivatedRouteSnapshot,
    CanActivateFn,
    GuardResult,
    Router,
    RouterStateSnapshot,
    UrlTree
} from '@angular/router';

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

// Both guards return an Observable; CanActivateFn only promises the wider MaybeAsync union.
const runGuard = (guard: CanActivateFn, url: string): Observable<GuardResult> =>
    TestBed.runInInjectionContext(() =>
        guard(mockRoute, mockState(url))
    ) as Observable<GuardResult>;

describe('styleEditorTabGuard', () => {
    let dotPropertiesService: DotPropertiesService;
    let router: Router;

    const setup = (featureFlagEnabled: boolean) => {
        TestBed.configureTestingModule({
            providers: [
                HttpClient,
                {
                    provide: DotPropertiesService,
                    useValue: { getFeatureFlag: vi.fn().mockReturnValue(of(featureFlagEnabled)) }
                },
                {
                    provide: Router,
                    useValue: {
                        parseUrl: vi.fn((url: string) => ({ url }) as unknown as UrlTree)
                    }
                }
            ],
            imports: [HttpClientTestingModule]
        });

        dotPropertiesService = TestBed.inject(DotPropertiesService);
        router = TestBed.inject(Router);
    };

    it('should allow access when feature flag is enabled', () =>
        new Promise<void>((done) => {
            setup(true);

            runGuard(styleEditorTabGuard, STYLE_EDITOR_URL).subscribe((result) => {
                expect(result).toBe(true);
                expect(dotPropertiesService.getFeatureFlag).toHaveBeenCalledWith(
                    FeaturedFlags.FEATURE_FLAG_UVE_STYLE_EDITOR
                );
                done();
            });
        }));

    it('should redirect to fields when feature flag is disabled', () =>
        new Promise<void>((done) => {
            setup(false);

            runGuard(styleEditorTabGuard, STYLE_EDITOR_URL).subscribe((result) => {
                expect(router.parseUrl).toHaveBeenCalledWith(FIELDS_URL);
                expect(result).not.toBe(false);
                done();
            });
        }));

    it('should preserve query params in the redirect url', () =>
        new Promise<void>((done) => {
            setup(false);
            const urlWithQuery = `${STYLE_EDITOR_URL}?foo=bar`;

            runGuard(styleEditorTabGuard, urlWithQuery).subscribe(() => {
                expect(router.parseUrl).toHaveBeenCalledWith(`${FIELDS_URL}?foo=bar`);
                done();
            });
        }));
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
                        hasAccessToPortlet: vi.fn().mockReturnValue(of(hasAccess))
                    }
                },
                {
                    provide: Router,
                    useValue: {
                        parseUrl: vi.fn((url: string) => ({ url }) as unknown as UrlTree)
                    }
                }
            ],
            imports: [HttpClientTestingModule]
        });

        dotCurrentUserService = TestBed.inject(DotCurrentUserService);
        router = TestBed.inject(Router);
    };

    it('should allow access when user has permissions portlet access', () =>
        new Promise<void>((done) => {
            setup(true);

            runGuard(permissionsTabGuard, PERMISSIONS_URL).subscribe((result) => {
                expect(result).toBe(true);
                expect(dotCurrentUserService.hasAccessToPortlet).toHaveBeenCalledWith(
                    'permissions'
                );
                done();
            });
        }));

    it('should redirect to fields when user lacks permissions portlet access', () =>
        new Promise<void>((done) => {
            setup(false);

            runGuard(permissionsTabGuard, PERMISSIONS_URL).subscribe((result) => {
                expect(router.parseUrl).toHaveBeenCalledWith(FIELDS_URL);
                expect(result).not.toBe(false);
                done();
            });
        }));

    it('should preserve query params in the redirect url', () =>
        new Promise<void>((done) => {
            setup(false);
            const urlWithQuery = `${PERMISSIONS_URL}?foo=bar`;

            runGuard(permissionsTabGuard, urlWithQuery).subscribe(() => {
                expect(router.parseUrl).toHaveBeenCalledWith(`${FIELDS_URL}?foo=bar`);
                done();
            });
        }));
});
