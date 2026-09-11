import { of } from 'rxjs';
import { Mock, vi } from 'vitest';

import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot } from '@angular/router';

import { HealthStatusTypes } from '@dotcms/dotcms-models';
import { DotAnalyticsService } from '@dotcms/portlets/dot-analytics/data-access';

import { analyticsHealthGuard } from './analytics-health.guard';

describe('analyticsHealthGuard', () => {
    let mockRouter: Router;
    let mockRouteSnapshot: ActivatedRouteSnapshot;
    let mockAnalyticsService: DotAnalyticsService;

    const mockStateSnapshot = {} as RouterStateSnapshot;

    beforeEach(() => {
        mockRouter = {
            navigate: vi.fn(),
            // Echoes back its args so tests can assert the guard's return value directly.
            createUrlTree: vi.fn((commands, extras) => ({ commands, extras }))
        } as unknown as Router;

        // isEnterprise is resolved on the parent 'analytics' route and merges down into this
        // snapshot's `data` — the guard reads it from here, not from an injected ActivatedRoute.
        mockRouteSnapshot = {
            data: { isEnterprise: true }
        } as unknown as ActivatedRouteSnapshot;

        mockAnalyticsService = {
            healthCheck: vi.fn()
        } as unknown as DotAnalyticsService;

        TestBed.configureTestingModule({
            providers: [
                { provide: Router, useValue: mockRouter },
                { provide: DotAnalyticsService, useValue: mockAnalyticsService }
            ]
        });
    });

    it('should allow access when health status is AVAILABLE', () =>
        new Promise<void>((done) => {
            (mockAnalyticsService.healthCheck as Mock).mockReturnValue(
                of(HealthStatusTypes.AVAILABLE)
            );

            TestBed.runInInjectionContext(() => {
                const result = analyticsHealthGuard(mockRouteSnapshot, mockStateSnapshot);

                if (result && typeof result === 'object' && 'subscribe' in result) {
                    result.subscribe((canActivate) => {
                        expect(canActivate).toBe(true);
                        expect(mockRouter.navigate).not.toHaveBeenCalled();
                        done();
                    });
                }
            });
        }));

    it('should return a UrlTree to the error page when health status is NOT_AVAILABLE', () =>
        new Promise<void>((done) => {
            (mockAnalyticsService.healthCheck as Mock).mockReturnValue(
                of(HealthStatusTypes.NOT_AVAILABLE)
            );

            TestBed.runInInjectionContext(() => {
                const result = analyticsHealthGuard(mockRouteSnapshot, mockStateSnapshot);

                if (result && typeof result === 'object' && 'subscribe' in result) {
                    result.subscribe((canActivate) => {
                        expect(mockRouter.createUrlTree).toHaveBeenCalledWith(
                            ['/analytics/error'],
                            {
                                queryParams: {
                                    status: HealthStatusTypes.NOT_AVAILABLE,
                                    isEnterprise: true
                                }
                            }
                        );
                        expect(canActivate).toEqual({
                            commands: ['/analytics/error'],
                            extras: {
                                queryParams: {
                                    status: HealthStatusTypes.NOT_AVAILABLE,
                                    isEnterprise: true
                                }
                            }
                        });
                        expect(mockRouter.navigate).not.toHaveBeenCalled();
                        done();
                    });
                }
            });
        }));

    it('should handle missing isEnterprise data by defaulting to true', () =>
        new Promise<void>((done) => {
            (mockAnalyticsService.healthCheck as Mock).mockReturnValue(
                of(HealthStatusTypes.NOT_AVAILABLE)
            );
            mockRouteSnapshot.data = {};

            TestBed.runInInjectionContext(() => {
                const result = analyticsHealthGuard(mockRouteSnapshot, mockStateSnapshot);

                if (result && typeof result === 'object' && 'subscribe' in result) {
                    result.subscribe((canActivate) => {
                        expect(mockRouter.createUrlTree).toHaveBeenCalledWith(
                            ['/analytics/error'],
                            {
                                queryParams: {
                                    status: HealthStatusTypes.NOT_AVAILABLE,
                                    isEnterprise: true
                                }
                            }
                        );
                        expect(canActivate).toBeTruthy();
                        done();
                    });
                }
            });
        }));

    it('should pass isEnterprise false when it is set to false', () =>
        new Promise<void>((done) => {
            (mockAnalyticsService.healthCheck as Mock).mockReturnValue(
                of(HealthStatusTypes.NOT_AVAILABLE)
            );
            mockRouteSnapshot.data = { isEnterprise: false };

            TestBed.runInInjectionContext(() => {
                const result = analyticsHealthGuard(mockRouteSnapshot, mockStateSnapshot);

                if (result && typeof result === 'object' && 'subscribe' in result) {
                    result.subscribe((canActivate) => {
                        expect(mockRouter.createUrlTree).toHaveBeenCalledWith(
                            ['/analytics/error'],
                            {
                                queryParams: {
                                    status: HealthStatusTypes.NOT_AVAILABLE,
                                    isEnterprise: false
                                }
                            }
                        );
                        expect(canActivate).toBeTruthy();
                        done();
                    });
                }
            });
        }));
});
