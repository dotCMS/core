import { of } from 'rxjs';

import { TestBed } from '@angular/core/testing';

import { DotPropertiesService } from '@dotcms/data-access';

import {
    ANALYTICS_SHOW_ENGAGEMENT_DASHBOARD_FLAG,
    dotAnalyticsEngagementResolver
} from './dot-analytics-engagement.resolver';

describe('dotAnalyticsEngagementResolver', () => {
    let dotPropertiesServiceSpy: { getKey: jest.Mock };

    beforeEach(() => {
        dotPropertiesServiceSpy = { getKey: jest.fn() };

        TestBed.configureTestingModule({
            providers: [{ provide: DotPropertiesService, useValue: dotPropertiesServiceSpy }]
        });
    });

    it('should return true when flag is "true"', (done) => {
        dotPropertiesServiceSpy.getKey.mockReturnValue(of('true'));

        const result = TestBed.runInInjectionContext(() =>
            dotAnalyticsEngagementResolver(null, null)
        );

        if (result instanceof Object && 'subscribe' in result) {
            result.subscribe((val) => {
                expect(dotPropertiesServiceSpy.getKey).toHaveBeenCalledWith(
                    ANALYTICS_SHOW_ENGAGEMENT_DASHBOARD_FLAG
                );
                expect(val).toBe(true);
                done();
            });
        }
    });

    it('should return false when flag is not "true"', (done) => {
        dotPropertiesServiceSpy.getKey.mockReturnValue(of('false'));

        const result = TestBed.runInInjectionContext(() =>
            dotAnalyticsEngagementResolver(null, null)
        );

        if (result instanceof Object && 'subscribe' in result) {
            result.subscribe((val) => {
                expect(dotPropertiesServiceSpy.getKey).toHaveBeenCalledWith(
                    ANALYTICS_SHOW_ENGAGEMENT_DASHBOARD_FLAG
                );
                expect(val).toBe(false);
                done();
            });
        }
    });
});
