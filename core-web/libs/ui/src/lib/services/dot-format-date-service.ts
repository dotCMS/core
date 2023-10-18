import { differenceInCalendarDays, format, formatDistanceStrict, isValid, parse } from 'date-fns';
import { format as formatTZ, utcToZonedTime } from 'date-fns-tz';

import { Injectable } from '@angular/core';

import { DotcmsConfigService, DotTimeZone } from '@dotcms/dotcms-js';
import { DotLocaleOptions } from '@dotcms/dotcms-models';

// Created outside of the service so it can be used on date.validator.ts
export function _isValid(date: string, formatPattern: string) {
    return isValid(parse(date, formatPattern, new Date()));
}

@Injectable({
    providedIn: 'root'
})
export class DotFormatDateService {
    private _localeOptions: DotLocaleOptions;
    private _systemTimeZone: DotTimeZone;

    constructor(dotcmsConfigService: DotcmsConfigService) {
        dotcmsConfigService
            .getSystemTimeZone()
            .subscribe((timezone) => (this._systemTimeZone = timezone));
    }

    get localeOptions(): DotLocaleOptions {
        return this._localeOptions;
    }

    set localeOptions(locale: DotLocaleOptions) {
        this._localeOptions = locale;
    }

    async setLang(languageId: string) {
        let [langCode, countryCode] = languageId.replace('_', '-').split('-');
        let localeLang;

        langCode = langCode?.toLowerCase() || 'en';
        countryCode = countryCode?.toLocaleUpperCase() || 'US';

        try {
            localeLang = await import(`date-fns/locale/${langCode}-${countryCode}/index.js`);
        } catch (error) {
            try {
                localeLang = await import(`date-fns/locale/${langCode}/index.js`);
            } catch (error) {
                localeLang = await import(`date-fns/locale/en-US`);
            }
        }

        this.localeOptions = { locale: localeLang.default };
    }

    /**
     * Get the number of calendar days between the given dates. This means that the
     * times are removed from the dates and then the difference in days is calculated.
     *
     * @param {Date} startDate
     * @param {Date} endDate
     * @returns {number}
     * @memberof DotFormatDateService
     */
    differenceInCalendarDays(startDate: Date, endDate: Date): number {
        return differenceInCalendarDays(startDate, endDate);
    }

    /**
     * Checks if a date is valid based on a pattern
     *
     * @param {string} date
     * @param {string} formatPattern
     * @returns {boolean}
     * @memberof DotFormatDateService
     */
    isValid(date: string, formatPattern: string): boolean {
        return _isValid(date, formatPattern);
    }

    /**
     * Format a date based on a pattern
     *
     * @param {Date} date
     * @param {string} formatPattern
     * @returns {string}
     * @memberof DotFormatDateService
     */
    format(date: Date, formatPattern: string): string {
        return format(date, formatPattern, { ...this.localeOptions });
    }

    /**
     * Format a date based on a pattern and in the serverTime
     *
     * @param {Date} date
     * @param {string} formatPattern
     * @returns {string}
     * @memberof DotFormatDateService
     */
    formatTZ(date: Date, formatPattern: string): string {
        const zonedDate = utcToZonedTime(date, this._systemTimeZone.id);

        return formatTZ(zonedDate, formatPattern, { timeZone: this._systemTimeZone.id });
    }

    /**
     * Gets relative strict time from on a specific date passed.
     * Dates must be in UTC format.
     *
     * @param {string} time Formatted as unix timestamp
     * @param {Date} baseDate
     * @returns {string}
     * @memberof DotFormatDateService
     */
    getRelative(time: Date, baseDate: Date = this.getUTC()): string {
        return formatDistanceStrict(time, baseDate, {
            ...this.localeOptions,
            addSuffix: true
        });
    }

    /**
     * Get the UTC date from a given date. You can pass a date or it will use the current date.
     * Dates must be in UTC format. To calculate the relative values according to the server time.
     *
     * @return {*}  {Date}
     * @memberof DotFormatDateService
     */
    getUTC(time: Date = new Date()): Date {
        const utcTime = new Date(time.getTime() + time.getTimezoneOffset() * 60000);

        return utcTime;
    }
}
