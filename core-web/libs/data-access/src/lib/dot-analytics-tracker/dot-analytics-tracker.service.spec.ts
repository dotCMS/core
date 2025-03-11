import { TestBed } from '@angular/core/testing';

import { DotAnalyticsTrackerService } from './dot-analytics-tracker.service';

describe('DotAnalyticsTrackerService', () => {
    let service: DotAnalyticsTrackerService;

    beforeEach(() => {
        TestBed.configureTestingModule({});
        service = TestBed.inject(DotAnalyticsTrackerService);
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });
});
