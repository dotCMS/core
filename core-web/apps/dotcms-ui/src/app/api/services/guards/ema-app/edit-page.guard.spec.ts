import { Observable, of } from 'rxjs';

import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';

import { DotPropertiesService, EmaAppConfigurationService } from '@dotcms/data-access';

import { editPageGuard } from './edit-page.guard';

describe('EditPageGuard', () => {
    let emaAppConfigurationService: jasmine.SpyObj<EmaAppConfigurationService>;
    let router: Router;
    let properties: jasmine.SpyObj<DotPropertiesService>;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const state: RouterStateSnapshot = {} as any;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [RouterTestingModule],
            providers: [
                {
                    provide: EmaAppConfigurationService,
                    useValue: {
                        get: jasmine.createSpy('get')
                    }
                },
                {
                    provide: Router,
                    useValue: {
                        navigate: jasmine.createSpy('navigate')
                    }
                },
                {
                    provide: DotPropertiesService,
                    useValue: {
                        getFeatureFlag: jasmine.createSpy('getFeatureFlag')
                    }
                }
            ]
        });

        emaAppConfigurationService = TestBed.inject(
            EmaAppConfigurationService
        ) as jasmine.SpyObj<EmaAppConfigurationService>;
        router = TestBed.inject(Router);
        properties = TestBed.inject(DotPropertiesService) as jasmine.SpyObj<DotPropertiesService>;
    });

    it('should navigate to "edit-ema" when FEATURE_FLAG_NEW_EDIT_PAGE is true', async () => {
        properties.getFeatureFlag.and.returnValue(of(true));

        emaAppConfigurationService.get.and.returnValue(
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
            queryParams: { url: '/some-url' }
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
        } as any;

        const result = await TestBed.runInInjectionContext(
            () => editPageGuard(route, state) as Observable<boolean>
        );

        result.subscribe((canActivate) => {
            expect(router.navigate).toHaveBeenCalledWith(['edit-ema'], {
                queryParams: {
                    url: 'some-url',
                    'com.dotmarketing.persona.id': 'modes.persona.no.persona',
                    language_id: 1
                }
            });
            expect(canActivate).toBe(false);
        });
    });

    it('should navgate to "edit-ema" when have a EMA App configuration', async () => {
        properties.getFeatureFlag.and.returnValue(of(false));
        emaAppConfigurationService.get.and.returnValue(
            of({
                pattern: 'some-pattern',
                url: 'https://example.com',
                options: {
                    authenticationToken: '12345',
                    additionalOption1: 'value1',
                    additionalOption2: 'value2'
                    // Add more key-value pairs as needed
                }
            })
        );

        const route: ActivatedRouteSnapshot = {
            queryParams: { url: '/some-url' }
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
        } as any;

        const result = await TestBed.runInInjectionContext(
            () => editPageGuard(route, state) as Observable<boolean>
        );

        result.subscribe((canActivate) => {
            expect(router.navigate).toHaveBeenCalledWith(['edit-ema'], {
                queryParams: {
                    url: 'some-url',
                    'com.dotmarketing.persona.id': 'modes.persona.no.persona',
                    language_id: 1
                }
            });
            expect(canActivate).toBe(false);
        });
    });

    it('should not update the queryParams on navigate', async () => {
        properties.getFeatureFlag.and.returnValue(of(true));
        emaAppConfigurationService.get.and.returnValue(
            of({
                pattern: 'some-pattern',
                url: 'https://example.com',
                options: {
                    authenticationToken: '12345',
                    additionalOption1: 'value1',
                    additionalOption2: 'value2'
                    // Add more key-value pairs as needed
                }
            })
        );

        const route: ActivatedRouteSnapshot = {
            queryParams: {
                url: '/some-url',
                language_id: 2,
                'com.dotmarketing.persona.id': 'some.real.persona'
            }
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
        } as any;

        const result = await TestBed.runInInjectionContext(
            () => editPageGuard(route, state) as Observable<boolean>
        );

        result.subscribe(() => {
            expect(router.navigate).toHaveBeenCalledWith(['edit-ema'], {
                queryParams: {
                    url: 'some-url',
                    language_id: 2,
                    'com.dotmarketing.persona.id': 'some.real.persona'
                }
            });
        });
    });

    it('should return true when FEATURE_FLAG_NEW_EDIT_PAGE is false', async () => {
        properties.getFeatureFlag.and.returnValue(of(false));
        const route: ActivatedRouteSnapshot = {
            queryParams: { url: '/some-url' }
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
        } as any;

        const result = await TestBed.runInInjectionContext(
            () => editPageGuard(route, state) as Observable<boolean>
        );
        result.subscribe((canActivate) => {
            expect(router.navigate).not.toHaveBeenCalled();
            expect(canActivate).toBe(true);
        });
    });
});
