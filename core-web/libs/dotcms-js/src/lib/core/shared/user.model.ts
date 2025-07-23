import { Injectable, LOCALE_ID, inject } from '@angular/core';

import { LoggerService } from '../logger.service';

@Injectable()
export class UserModel {
    private loggerService = inject(LoggerService);

    username: string;
    password: string;
    locale: string;
    suppressAlerts = false;

    constructor() {
        const localeId = inject(LOCALE_ID);

        this.locale = localeId; // default to 'en-US'
        try {
            const url = window.location.search.substring(1);
            this.locale = this.checkQueryForUrl(url);
        } catch (e) {
            this.loggerService.error('Could not set locale from URL.');
        }
    }

    checkQueryForUrl(locationQuery: string): string {
        let locale = this.locale;
        if (locationQuery && locationQuery.length) {
            const q = locationQuery;
            const token = 'locale=';
            const idx = q.indexOf(token);
            if (idx >= 0) {
                let end = q.indexOf('&', idx);
                end = end !== -1 ? end : q.indexOf('#', idx);
                end = end !== -1 ? end : q.length;
                locale = q.substring(idx + token.length, end);
            }
        }

        return locale;
    }
}
