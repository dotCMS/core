import { expect, it, describe } from '@jest/globals';
import { createServiceFactory, SpectatorService } from '@ngneat/spectator/jest';

import { HttpClient, HttpHandler } from '@angular/common/http';

import { EVENT_TYPES } from '@dotcms/dotcms-models';
import { WINDOW } from '@dotcms/utils';

import {
    CONTENT_ANALYTICS_EVENT_API,
    DOT_ANALYTICS_SRC,
    DotAnalyticsTrackerService
} from './dot-analytics-tracker.service';

describe('DotAnalyticsTrackerService', () => {
    let spectator: SpectatorService<DotAnalyticsTrackerService>;
    const mockWindow = {
        location: {
            pathname: '/test/path',
            hostname: 'test.host.com'
        }
    };

    const createService = createServiceFactory({
        service: DotAnalyticsTrackerService,
        providers: [
            HttpClient,
            HttpHandler,
            {
                provide: WINDOW,
                useValue: mockWindow
            }
        ]
    });

    beforeEach(() => {
        spectator = createService();
    });

    it('should track event', () => {
        const httpClient = spectator.inject(HttpClient);
        const spy = jest.spyOn(httpClient, 'post');

        const event = {
            test: 'Some test data',
            anotherTest: 'Another test data'
        };

        spectator.service.track(EVENT_TYPES.UVE_CALENDAR_CHANGE, event);

        expect(spy).toHaveBeenCalledWith(
            CONTENT_ANALYTICS_EVENT_API,
            expect.objectContaining({
                event_type: EVENT_TYPES.UVE_CALENDAR_CHANGE,
                test: 'Some test data',
                anotherTest: 'Another test data',
                utc_time: expect.any(String),
                local_tz_offset: expect.any(Number),
                doc_path: '/test/path',
                doc_host: 'test.host.com',
                src: DOT_ANALYTICS_SRC
            })
        );
    });
});
