import { of } from 'rxjs';

import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Route, Router } from '@angular/router';

import { DotExperimentsService } from '@dotcms/data-access';
import { HealthStatusTypes } from '@dotcms/dotcms-models';

import { analyticsHealthGuard, clearAnalyticsHealthCache } from './analytics-health.guard';

describe('analyticsHealthGuard', () => {
    let mockRouter: Router;
    let mockActivatedRoute: ActivatedRoute;
    let mockDotExperimentsService: DotExperimentsService;

    const mockRoute = {} as Route;
    const mockSegments = [];

    beforeEach(() => {
        // Clear cache before each test to ensure isolation
        clearAnalyticsHealthCache();

        mockRouter = {
            navigate: jest.fn()
        } as unknown as Router;

        mockActivatedRoute = {
            snapshot: {
                data: { isEnterprise: true }
            }
        } as unknown as ActivatedRoute;

        mockDotExperimentsService = {
            healthCheck: jest.fn()
        } as unknown as DotExperimentsService;

        TestBed.configureTestingModule({
            providers: [
                { provide: Router, useValue: mockRouter },
                { provide: ActivatedRoute, useValue: mockActivatedRoute },
                { provide: DotExperimentsService, useValue: mockDotExperimentsService }
            ]
        });
    });

    it('should allow access when health status is OK', (done) => {
        (mockDotExperimentsService.healthCheck as jest.Mock).mockReturnValue(
            of(HealthStatusTypes.OK)
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

    it('should redirect to error page when health status is NOT_CONFIGURED', (done) => {
        (mockDotExperimentsService.healthCheck as jest.Mock).mockReturnValue(
            of(HealthStatusTypes.NOT_CONFIGURED)
        );

        TestBed.runInInjectionContext(() => {
            const result = analyticsHealthGuard(mockRoute, mockSegments);

            if (result && typeof result === 'object' && 'subscribe' in result) {
                result.subscribe((canActivate) => {
                    expect(canActivate).toBe(false);
                    expect(mockRouter.navigate).toHaveBeenCalledWith(['/analytics/error'], {
                        queryParams: {
                            status: HealthStatusTypes.NOT_CONFIGURED,
                            isEnterprise: true
                        }
                    });
                    done();
                });
            }
        });
    });

    it('should redirect to error page when health status is CONFIGURATION_ERROR', (done) => {
        (mockDotExperimentsService.healthCheck as jest.Mock).mockReturnValue(
            of(HealthStatusTypes.CONFIGURATION_ERROR)
        );

        TestBed.runInInjectionContext(() => {
            const result = analyticsHealthGuard(mockRoute, mockSegments);

            if (result && typeof result === 'object' && 'subscribe' in result) {
                result.subscribe((canActivate) => {
                    expect(canActivate).toBe(false);
                    expect(mockRouter.navigate).toHaveBeenCalledWith(['/analytics/error'], {
                        queryParams: {
                            status: HealthStatusTypes.CONFIGURATION_ERROR,
                            isEnterprise: true
                        }
                    });
                    done();
                });
            }
        });
    });

    it('should handle missing isEnterprise data by defaulting to true', (done) => {
        (mockDotExperimentsService.healthCheck as jest.Mock).mockReturnValue(
            of(HealthStatusTypes.NOT_CONFIGURED)
        );
        mockActivatedRoute.snapshot.data = {}; // No isEnterprise data

        TestBed.runInInjectionContext(() => {
            const result = analyticsHealthGuard(mockRoute, mockSegments);

            if (result && typeof result === 'object' && 'subscribe' in result) {
                result.subscribe((canActivate) => {
                    expect(canActivate).toBe(false);
                    expect(mockRouter.navigate).toHaveBeenCalledWith(['/analytics/error'], {
                        queryParams: {
                            status: HealthStatusTypes.NOT_CONFIGURED,
                            isEnterprise: true // Should default to true
                        }
                    });
                    done();
                });
            }
        });
    });

    it('should pass isEnterprise false when it is set to false', (done) => {
        (mockDotExperimentsService.healthCheck as jest.Mock).mockReturnValue(
            of(HealthStatusTypes.NOT_CONFIGURED)
        );
        mockActivatedRoute.snapshot.data = { isEnterprise: false };

        TestBed.runInInjectionContext(() => {
            const result = analyticsHealthGuard(mockRoute, mockSegments);

            if (result && typeof result === 'object' && 'subscribe' in result) {
                result.subscribe((canActivate) => {
                    expect(canActivate).toBe(false);
                    expect(mockRouter.navigate).toHaveBeenCalledWith(['/analytics/error'], {
                        queryParams: {
                            status: HealthStatusTypes.NOT_CONFIGURED,
                            isEnterprise: false
                        }
                    });
                    done();
                });
            }
        });
    });
});
