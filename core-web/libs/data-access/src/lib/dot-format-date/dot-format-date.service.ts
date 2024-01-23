import { differenceInCalendarDays, format, formatDistanceStrict, isValid, parse } from 'date-fns';
import { format as formatTZ, utcToZonedTime } from 'date-fns-tz';

import { inject, Injectable } from '@angular/core';

import { DotcmsConfigService, DotTimeZone, LoginService } from '@dotcms/dotcms-js';
import { DotLocaleOptions } from '@dotcms/dotcms-models';

const DEFAULT_ISO_LOCALE = 'en-US';
const INVALID_DATE_MSG = 'Invalid date';

// Created outside of the service so it can be used on date.validator.ts
export function _isValid(date: string, formatPattern: string) {
    return isValid(parse(date, formatPattern, new Date()));
}

@Injectable({
    providedIn: 'root'
})
export class DotFormatDateService {
    private loginService: LoginService = inject(LoginService);

    private defaultDateFormatOptions: Intl.DateTimeFormatOptions = {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        hour12: true
    };

    private _systemTimeZone!: DotTimeZone;

    constructor(dotcmsConfigService: DotcmsConfigService) {
        dotcmsConfigService
            .getSystemTimeZone()
            .subscribe((timezone) => (this._systemTimeZone = timezone));
    }

    private _localeOptions!: DotLocaleOptions;

    get localeOptions(): DotLocaleOptions {
        return this._localeOptions;
    }

    set localeOptions(locale: DotLocaleOptions) {
        this._localeOptions = locale;
    }

    /**
     * @deprecated
     * please do not use more date-fns use instead Intl.DateTimeFormat
     * @param languageId
     */
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
     * Transform a timestamp to a date string using Intl.DateTimeFormat
     * taking in consideration the user's language selected when logged in.
     *
     * @param {number} timestamp
     * @param userDateFormatOptions
     * @returns {Date}
     */
    getDateFromTimestamp(
        timestamp: number,
        userDateFormatOptions?: Intl.DateTimeFormatOptions
    ): string {
        if (!this.isValidTimestamp(timestamp)) {
            console.error('Invalid timestamp provided:', timestamp);

            return INVALID_DATE_MSG;
        }

        try {
            const options = userDateFormatOptions || this.defaultDateFormatOptions;
            const formatter = new Intl.DateTimeFormat(this.getLocaleISOSelectedAtLogin(), options);

            return formatter.format(new Date(timestamp)).replace(/\s+/g, ' ').trim(); // space normalization
        } catch (error) {
            console.error('Error formatting date:', error);

            return INVALID_DATE_MSG;
        }
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
        return new Date(time.getTime() + time.getTimezoneOffset() * 60000);
    }

    /**
     * Checks if the given value is a valid timestamp.
     *
     * @param {number} value - The value to be checked.
     * @return {boolean} - Returns true if the value is a valid timestamp, otherwise false.
     */
    isValidTimestamp(value: number): boolean {
        // Check if the value is of type number
        if (typeof value !== 'number') {
            return false;
        }

        if (value < 0 || value > 1e13) {
            // 1e13 covers up to the year 2286
            return false;
        }

        // Attempt to construct a Date object with the timestamp
        const date = new Date(value);

        // and NaN if it is not. `isNaN` checks if the value is NaN.
        return !isNaN(date.getTime());
    }

    /**
     * Converts the given locale string to the ISO 639-1 format.
     *
     * @return {string} - The converted locale string in the ISO 639-1 format.
     * @private
     */
    private getLocaleISOSelectedAtLogin(): string {
        const languageLoggedIn = this.loginService.currentUserLanguageId;

        return languageLoggedIn ? languageLoggedIn.replace('_', '-') : DEFAULT_ISO_LOCALE;
    }
}
