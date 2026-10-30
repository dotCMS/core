import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { BaseAnalyticsEvent, EVENT_TYPES } from '@dotcms/dotcms-models';
import { WINDOW } from '@dotcms/utils';

export const CONTENT_ANALYTICS_EVENT_API = '/api/v1/analytics/content/event';
export const DOT_ANALYTICS_SRC = 'dotAnalytics';
@Injectable()
export class DotAnalyticsTrackerService {
    #http = inject(HttpClient);
    #window = inject(WINDOW);

    track<T>(event: EVENT_TYPES, payload: T) {
        const eventData: BaseAnalyticsEvent & T = {
            ...payload,
            event_type: event,
            utc_time: new Date().toISOString(),
            local_tz_offset: new Date().getTimezoneOffset(),
            doc_path: this.#window.location.pathname,
            doc_host: this.#window.location.hostname,
            src: DOT_ANALYTICS_SRC
        };

        return this.#http.post(CONTENT_ANALYTICS_EVENT_API, eventData).subscribe();
    }
}
