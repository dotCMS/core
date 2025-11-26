// dot-analytics-health-check.resolver.service.spec.ts

import { of } from 'rxjs';

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
                    useValue: { healthCheck: jest.fn().mockReturnValue(of(HealthStatusTypes.OK)) }
                }
            ]
        });

        dotExperimentsService = TestBed.inject(DotExperimentsService);
    });

    it('should return HealthStatusTypes.OK when healthCheck is successful', () => {
        const resolver = runInInjectionContext(TestBed.inject(EnvironmentInjector), () =>
            dotAnalyticsHealthCheckResolver(null, {} as RouterStateSnapshot)
        );

        jest.spyOn(dotExperimentsService, 'healthCheck').mockReturnValue(of(HealthStatusTypes.OK));

        resolver.subscribe((healthStatus) => {
            expect(healthStatus).toBe(HealthStatusTypes.OK);
        });
    });
});
