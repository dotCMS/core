// dot-analytics-health-check.resolver.service.spec.ts

import { of } from 'rxjs';
import { vi } from 'vitest';

import { EnvironmentInjector, runInInjectionContext } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { RouterStateSnapshot } from '@angular/router';

import { DotExperimentsService } from '@dotcms/data-access';
import { HealthStatusTypes } from '@dotcms/dotcms-models';

import { dotAnalyticsHealthCheckResolver } from './dot-analytics-health-check.resolver.service';

describe('dotAnalyticsHealthCheckResolver', () => {
    let dotExperimentsService: DotExperimentsService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                {
                    provide: DotExperimentsService,
                    useValue: {
                        healthCheck: vi
                            .fn()
                            .mockReturnValue(of({ health: HealthStatusTypes.OK, tier: 'FULL' }))
                    }
                }
            ]
        });

        dotExperimentsService = TestBed.inject(DotExperimentsService);
    });

    it('should extract and return only the health field as HealthStatusTypes', () => {
        const resolver = runInInjectionContext(TestBed.inject(EnvironmentInjector), () =>
            dotAnalyticsHealthCheckResolver(null, {} as RouterStateSnapshot)
        );

        vi.spyOn(dotExperimentsService, 'healthCheck').mockReturnValue(
            of({ health: HealthStatusTypes.OK, tier: 'FULL' })
        );

        resolver.subscribe((healthStatus) => {
            expect(healthStatus).toBe(HealthStatusTypes.OK);
        });
    });
});
