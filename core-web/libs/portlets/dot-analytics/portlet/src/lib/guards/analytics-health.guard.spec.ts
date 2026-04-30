import { of } from 'rxjs';

import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Route, Router } from '@angular/router';

import { HealthStatusTypes } from '@dotcms/dotcms-models';
import { DotAnalyticsService } from '@dotcms/portlets/dot-analytics/data-access';

import { analyticsHealthGuard } from './analytics-health.guard';

describe('analyticsHealthGuard', () => {
    let mockRouter: Router;
    let mockActivatedRoute: ActivatedRoute;
    let mockAnalyticsService: DotAnalyticsService;

    const mockRoute = {} as Route;
    const mockSegments = [];

    beforeEach(() => {
        mockRouter = {
            navigate: jest.fn()
        } as unknown as Router;

        mockActivatedRoute = {
            snapshot: {
                data: { isEnterprise: true }
            }
        } as unknown as ActivatedRoute;

        mockAnalyticsService = {
            healthCheck: jest.fn(),
            healthCheckWithCache: jest.fn(),
            clearHealthCache: jest.fn()
        } as unknown as DotAnalyticsService;

        TestBed.configureTestingModule({
            providers: [
                { provide: Router, useValue: mockRouter },
                { provide: ActivatedRoute, useValue: mockActivatedRoute },
                { provide: DotAnalyticsService, useValue: mockAnalyticsService }
            ]
        });
    });

    it('should allow access when health status is AVAILABLE', (done) => {
        (mockAnalyticsService.healthCheckWithCache as jest.Mock).mockReturnValue(
            of(HealthStatusTypes.AVAILABLE)
        );

        TestBed.runInInjectionContext(() => {
            const result = analyticsHealthGuard(mockRoute, mockSegments);

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
        (mockAnalyticsService.healthCheckWithCache as jest.Mock).mockReturnValue(
            of(HealthStatusTypes.NOT_AVAILABLE)
        );

        TestBed.runInInjectionContext(() => {
            const result = analyticsHealthGuard(mockRoute, mockSegments);

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
        (mockAnalyticsService.healthCheckWithCache as jest.Mock).mockReturnValue(
            of(HealthStatusTypes.NOT_AVAILABLE)
        );
        mockActivatedRoute.snapshot.data = {};

        TestBed.runInInjectionContext(() => {
            const result = analyticsHealthGuard(mockRoute, mockSegments);

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
        (mockAnalyticsService.healthCheckWithCache as jest.Mock).mockReturnValue(
            of(HealthStatusTypes.NOT_AVAILABLE)
        );
        mockActivatedRoute.snapshot.data = { isEnterprise: false };

        TestBed.runInInjectionContext(() => {
            const result = analyticsHealthGuard(mockRoute, mockSegments);

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
