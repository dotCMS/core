import { Injectable } from '@angular/core';
import { isValid, format, formatDistanceToNowStrict, parse } from 'date-fns';
import { utcToZonedTime, format as formatTZ } from 'date-fns-tz';
import { DotcmsConfigService, DotTimeZone } from '@dotcms/dotcms-js';
interface DotLocaleOptions {
    locale: Locale;
}

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
     * Gets relative strict time from on a specific date passed
     *
     * @param {string} time
     * @returns {string}
     * @memberof DotFormatDateService
     */
    getRelative(time: string): string {
        return formatDistanceToNowStrict(new Date(parseInt(time, 10)), {
            ...this.localeOptions,
            addSuffix: true
        });
    }
}
