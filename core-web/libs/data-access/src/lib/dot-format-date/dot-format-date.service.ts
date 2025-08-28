import { tz, TZDate } from '@date-fns/tz';
import {
    differenceInCalendarDays,
    format,
    formatDistanceStrict,
    isValid,
    Locale,
    parse
} from 'date-fns';

import { inject, Injectable, Signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';

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
    private loginService = inject(LoginService);
    private dotcmsConfigService = inject(DotcmsConfigService);

    private defaultDateFormatOptions: Intl.DateTimeFormatOptions = {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        hour12: true
    };

    private $systemTimeZone: Signal<DotTimeZone | null> = toSignal(
        this.dotcmsConfigService.getSystemTimeZone(),
        {
            initialValue: null
        }
    );

    private _localeOptions!: DotLocaleOptions;

    get localeOptions(): DotLocaleOptions {
        return this._localeOptions;
    }

    set localeOptions(locale: DotLocaleOptions) {
        this._localeOptions = locale;
    }

    /**
     * @param languageId
     */
    async setLang(languageId: string) {
        let [langCode, countryCode] = languageId.replace('_', '-').split('-');

        langCode = langCode?.toLowerCase() || 'en';
        countryCode = countryCode?.toUpperCase() || 'US';

        // Convert locale format from 'en-US' to 'enUS' for date-fns
        const formatLocaleCode = (lang: string, country?: string) => {
            if (country) {
                return lang + country;
            }
            return lang;
        };

        try {
            // Try with full locale code (e.g., 'enUS')
            const fullLocaleCode = formatLocaleCode(langCode, countryCode);
            const localeModule = (await import('date-fns/locale')) as unknown as Record<
                string,
                Locale
            >;

            if (fullLocaleCode in localeModule) {
                this.localeOptions = { locale: localeModule[fullLocaleCode] };
                return;
            }

            // Try with just language code (e.g., 'en')
            if (langCode in localeModule) {
                this.localeOptions = { locale: localeModule[langCode] };
                return;
            }

            // Fallback to enUS
            this.localeOptions = { locale: localeModule['enUS'] };
        } catch (error) {
            console.warn('Failed to load date locale, falling back to enUS:', error);
            // Final fallback
            const localeModule = (await import('date-fns/locale')) as unknown as Record<
                string,
                Locale
            >;
            this.localeOptions = { locale: localeModule['enUS'] };
        }
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
     * Format a date based on a pattern and in the serverTime using date-fns v4.0 TZDate
     *
     * @param {Date} date
     * @param {string} formatPattern
     * @returns {string}
     * @memberof DotFormatDateService
     */
    formatTZ(date: Date, formatPattern: string): string {
        const systemTimeZone = this.$systemTimeZone();

        if (!systemTimeZone) {
            return INVALID_DATE_MSG;
        }

        try {
            // Using TZDate from @date-fns/tz with date-fns v4.0
            const tzDate = new TZDate(date, systemTimeZone.id);
            return format(tzDate, formatPattern, {
                in: tz(systemTimeZone.id),
                ...this.localeOptions
            });
        } catch (error) {
            console.error('Error formatting date with timezone:', error);
            return INVALID_DATE_MSG;
        }
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
