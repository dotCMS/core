import { Observable, of } from 'rxjs';

import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';

import { DotPropertiesService, EmaAppConfigurationService } from '@dotcms/data-access';

import { editEmaGuard } from './edit-ema.guard';

describe('EditEmaGuard', () => {
    let emaAppConfigurationService: EmaAppConfigurationService;
    let router: Router;
    let properties: DotPropertiesService;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const state: RouterStateSnapshot = {} as any;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [RouterTestingModule],
            providers: [
                {
                    provide: EmaAppConfigurationService,
                    useValue: {
                        get: jest.fn()
                    }
                },
                {
                    provide: Router,
                    useValue: {
                        navigate: jest.fn(),
                        createUrlTree: jest.fn().mockReturnValue('this.is.a.url.tree.mock')
                    }
                },
                {
                    provide: DotPropertiesService,
                    useValue: {
                        getFeatureFlag: jest.fn().mockReturnValue(of(true))
                    }
                }
            ]
        });

        emaAppConfigurationService = TestBed.inject(EmaAppConfigurationService);
        router = TestBed.inject(Router);
        properties = TestBed.inject(DotPropertiesService);
    });

    it('should navigate to "edit-ema" when app is Headless', (done) => {
        jest.spyOn(emaAppConfigurationService, 'get').mockReturnValue(
            of({
                pattern: 'some-pattern',
                url: 'https://example.com',
                options: {
                    authenticationToken: '12345',
                    additionalOption1: 'value1',
                    additionalOption2: 'value2'
                }
            })
        );

        const route: ActivatedRouteSnapshot = {
            firstChild: {
                url: [{ path: 'content' }]
            },
            queryParams: { url: '/some-url' }
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
        } as any;

        const result = TestBed.runInInjectionContext(
            () => editEmaGuard(route, state) as Observable<boolean>
        );

        result.subscribe((canActivate) => {
            expect(router.navigate).toHaveBeenCalledWith(['/edit-ema/content'], {
                queryParams: {
                    'com.dotmarketing.persona.id': 'modes.persona.no.persona',
                    language_id: 1,
                    url: '/some-url'
                },
                replaceUrl: true
            });
            expect(canActivate).toBe(true);
            done();
        });
    });

    it('should navigate to "edit-ema" and sanitize url', (done) => {
        jest.spyOn(emaAppConfigurationService, 'get').mockReturnValue(
            of({
                pattern: 'some-pattern',
                url: 'https://example.com',
                options: {
                    authenticationToken: '12345',
                    additionalOption1: 'value1',
                    additionalOption2: 'value2'
                }
            })
        );

        const route: ActivatedRouteSnapshot = {
            firstChild: {
                url: [{ path: 'content' }]
            },
            queryParams: { url: '/some-url/with-index/index' }
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
        } as any;

        const result = TestBed.runInInjectionContext(
            () => editEmaGuard(route, state) as Observable<boolean>
        );

        result.subscribe((canActivate) => {
            expect(router.navigate).toHaveBeenCalledWith(['/edit-ema/content'], {
                queryParams: {
                    'com.dotmarketing.persona.id': 'modes.persona.no.persona',
                    language_id: 1,
                    url: 'some-url/with-index'
                },
                replaceUrl: true
            });
            expect(canActivate).toBe(true);
            done();
        });
    });

    it('should not update the queryParams on navigate', (done) => {
        jest.spyOn(emaAppConfigurationService, 'get').mockReturnValue(
            of({
                pattern: 'some-pattern',
                url: 'https://example.com',
                options: {
                    authenticationToken: '12345',
                    additionalOption1: 'value1',
                    additionalOption2: 'value2'
                }
            })
        );

        const route: ActivatedRouteSnapshot = {
            firstChild: {
                url: [{ path: 'content' }]
            },
            queryParams: {
                'com.dotmarketing.persona.id': 'some.persona',
                language_id: 2,
                url: '/some-url'
            }
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
        } as any;

        const result = TestBed.runInInjectionContext(
            () => editEmaGuard(route, state) as Observable<boolean>
        );

        result.subscribe((canActivate) => {
            expect(router.navigate).not.toHaveBeenCalled();
            expect(canActivate).toBe(true);
            done();
        });
    });

    it('should navigate to "edit-page" when app is VTL and feature flag is disabled', (done) => {
        jest.spyOn(emaAppConfigurationService, 'get').mockReturnValue(of(null)); // Is VTL
        jest.spyOn(properties, 'getFeatureFlag').mockReturnValue(of(false));

        const route: ActivatedRouteSnapshot = {
            firstChild: {
                url: [{ path: 'content' }]
            },
            queryParams: { url: '/some-url' }
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
        } as any;

        const result = TestBed.runInInjectionContext(
            () => editEmaGuard(route, state) as Observable<boolean>
        );

        result.subscribe((canActivate) => {
            expect(router.navigate).toHaveBeenCalledWith(['/edit-page/content'], {
                queryParams: { url: '/some-url' }
            });
            expect(canActivate).toBe(false);
            done();
        });
    });

    it('should navigate to "edit-ema" when app is VTL and feature flag is enabled', (done) => {
        jest.spyOn(emaAppConfigurationService, 'get').mockReturnValue(of(null)); // Is VTL
        jest.spyOn(properties, 'getFeatureFlag').mockReturnValue(of(true));

        const route: ActivatedRouteSnapshot = {
            firstChild: {
                url: [{ path: 'content' }]
            },
            queryParams: { url: '/some-url' }
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
        } as any;

        const result = TestBed.runInInjectionContext(
            () => editEmaGuard(route, state) as Observable<boolean>
        );

        result.subscribe((canActivate) => {
            expect(router.navigate).toHaveBeenCalledWith(['/edit-ema/content'], {
                queryParams: {
                    url: '/some-url',
                    'com.dotmarketing.persona.id': 'modes.persona.no.persona',
                    language_id: 1
                },
                replaceUrl: true
            });
            expect(canActivate).toBe(true);
            done();
        });
    });
});
