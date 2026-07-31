import { of } from 'rxjs';

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
            navigate: jest.fn()
        } as unknown as Router;

        // isEnterprise is resolved on the parent 'analytics' route and merges down into this
        // snapshot's `data` — the guard reads it from here, not from an injected ActivatedRoute.
        mockRouteSnapshot = {
            data: { isEnterprise: true }
        } as unknown as ActivatedRouteSnapshot;

        mockAnalyticsService = {
            healthCheck: jest.fn()
        } as unknown as DotAnalyticsService;

        TestBed.configureTestingModule({
            providers: [
                { provide: Router, useValue: mockRouter },
                { provide: DotAnalyticsService, useValue: mockAnalyticsService }
            ]
        });
    });

    it('should allow access when health status is AVAILABLE', (done) => {
        (mockAnalyticsService.healthCheck as jest.Mock).mockReturnValue(
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
    });

    it('should redirect to error page when health status is NOT_AVAILABLE', (done) => {
        (mockAnalyticsService.healthCheck as jest.Mock).mockReturnValue(
            of(HealthStatusTypes.NOT_AVAILABLE)
        );

        TestBed.runInInjectionContext(() => {
            const result = analyticsHealthGuard(mockRouteSnapshot, mockStateSnapshot);

            if (result && typeof result === 'object' && 'subscribe' in result) {
                result.subscribe((canActivate) => {
                    expect(canActivate).toBe(false);
                    expect(mockRouter.navigate).toHaveBeenCalledWith(['/analytics/error'], {
                        queryParams: {
                            status: HealthStatusTypes.NOT_AVAILABLE,
                            isEnterprise: true
                        }
                    });
                    done();
                });
            }
        });
    });

    it('should handle missing isEnterprise data by defaulting to true', (done) => {
        (mockAnalyticsService.healthCheck as jest.Mock).mockReturnValue(
            of(HealthStatusTypes.NOT_AVAILABLE)
        );
        mockRouteSnapshot.data = {};

        TestBed.runInInjectionContext(() => {
            const result = analyticsHealthGuard(mockRouteSnapshot, mockStateSnapshot);

            if (result && typeof result === 'object' && 'subscribe' in result) {
                result.subscribe((canActivate) => {
                    expect(canActivate).toBe(false);
                    expect(mockRouter.navigate).toHaveBeenCalledWith(['/analytics/error'], {
                        queryParams: {
                            status: HealthStatusTypes.NOT_AVAILABLE,
                            isEnterprise: true
                        }
                    });
                    done();
                });
            }
        });
    });

    it('should pass isEnterprise false when it is set to false', (done) => {
        (mockAnalyticsService.healthCheck as jest.Mock).mockReturnValue(
            of(HealthStatusTypes.NOT_AVAILABLE)
        );
        mockRouteSnapshot.data = { isEnterprise: false };

        TestBed.runInInjectionContext(() => {
            const result = analyticsHealthGuard(mockRouteSnapshot, mockStateSnapshot);

            if (result && typeof result === 'object' && 'subscribe' in result) {
                result.subscribe((canActivate) => {
                    expect(canActivate).toBe(false);
                    expect(mockRouter.navigate).toHaveBeenCalledWith(['/analytics/error'], {
                        queryParams: {
                            status: HealthStatusTypes.NOT_AVAILABLE,
                            isEnterprise: false
                        }
                    });
                    done();
                });
            }
        });
    });
});
