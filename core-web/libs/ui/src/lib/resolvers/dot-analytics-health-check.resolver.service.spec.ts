import { of } from 'rxjs';

import { TestBed } from '@angular/core/testing';

import { DotExperimentsService } from '@dotcms/data-access';
import { HealthStatusTypes } from '@dotcms/dotcms-models';
import { dotAnalyticsHealthCheckResolver } from '@dotcms/ui';

describe('dotAnalyticsHealthCheckResolver', () => {
    let dotExperimentsService: jasmine.SpyObj<DotExperimentsService>;

    beforeEach(() => {
        const spy = jasmine.createSpyObj('DotExperimentsService', ['healthCheck']);

        TestBed.configureTestingModule({
            providers: [{ provide: DotExperimentsService, useValue: spy }]
        });

        dotExperimentsService = TestBed.inject(
            DotExperimentsService
        ) as jasmine.SpyObj<DotExperimentsService>;
    });

    it('should return HealthStatusTypes.OK when healthCheck is successful', (done) => {
        dotExperimentsService.healthCheck.and.returnValue(of(HealthStatusTypes.OK));

        dotAnalyticsHealthCheckResolver(null, null).subscribe((result) => {
            expect(result).toBe(HealthStatusTypes.OK);
            done();
        });
    });

    it('should return HealthStatusTypes.NOT_CONFIGURED when healthCheck is not configured', (done) => {
        dotExperimentsService.healthCheck.and.returnValue(of(HealthStatusTypes.NOT_CONFIGURED));

        dotAnalyticsHealthCheckResolver(null, null).subscribe((result) => {
            expect(result).toBe(HealthStatusTypes.NOT_CONFIGURED);
            done();
        });
    });

    it('should return HealthStatusTypes.CONFIGURATION_ERROR when healthCheck has configuration error', (done) => {
        dotExperimentsService.healthCheck.and.returnValue(
            of(HealthStatusTypes.CONFIGURATION_ERROR)
        );

        dotAnalyticsHealthCheckResolver(null, null).subscribe((result) => {
            expect(result).toBe(HealthStatusTypes.CONFIGURATION_ERROR);
            done();
        });
    });
});
