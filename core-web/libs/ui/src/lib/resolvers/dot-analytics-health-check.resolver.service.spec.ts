import { of } from 'rxjs';

import { TestBed } from '@angular/core/testing';

import { DotExperimentsService } from '@dotcms/data-access';
import { HealthStatusTypes } from '@dotcms/dotcms-models';

import { DotAnalyticsHealthCheckResolver } from './dot-analytics-health-check.resolver.service';

describe('DotAnalyticsHealthCheckResolver', () => {
    let resolver: DotAnalyticsHealthCheckResolver;
    let dotExperimentsService: jasmine.SpyObj<DotExperimentsService>;

    beforeEach(() => {
        const spy = jasmine.createSpyObj('DotExperimentsService', ['healthCheck']);

        TestBed.configureTestingModule({
            providers: [
                DotAnalyticsHealthCheckResolver,
                { provide: DotExperimentsService, useValue: spy }
            ]
        });

        resolver = TestBed.inject(DotAnalyticsHealthCheckResolver);
        dotExperimentsService = TestBed.inject(
            DotExperimentsService
        ) as jasmine.SpyObj<DotExperimentsService>;
    });

    it('should return true when healthCheck returns OK', (done) => {
        dotExperimentsService.healthCheck.and.returnValue(of(HealthStatusTypes.OK));

        resolver.resolve().subscribe((result) => {
            expect(result).toBe(true);
            done();
        });
    });

    it('should return false when healthCheck does not return OK', (done) => {
        dotExperimentsService.healthCheck.and.returnValue(of(HealthStatusTypes.NOT_CONFIGURED));

        resolver.resolve().subscribe((result) => {
            expect(result).toBe(false);
            done();
        });
    });
});
