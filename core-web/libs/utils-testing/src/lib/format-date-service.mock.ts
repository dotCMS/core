import { format } from 'date-fns';
import { enUS } from 'date-fns/locale';

import { DotLocaleOptions } from '@dotcms/dotcms-models';

export class DotFormatDateServiceMock {
    private _localeOptions?: DotLocaleOptions;
    get localeOptions(): DotLocaleOptions {
        return this._localeOptions ?? { locale: enUS };
    }

    set localeOptions(locale: DotLocaleOptions) {
        this._localeOptions = locale;
    }

    setLang(_lang: string) {
        /* */
    }

    isValid(_date: string, _formatPattern: string) {
        /* */
    }

    format(date: Date, formatPattern: string) {
        return format(date, formatPattern);
    }

    getRelative(_time: string): string {
        return '1 hour ago';
    }

    getUTC(time: Date = new Date()): Date {
        const utcTime = new Date(time.getTime() + time.getTimezoneOffset() * 60000);

        return utcTime;
    }

    differenceInCalendarDays(_dateLeft: Date, _dateRight: Date): number {
        return 1;
    }
    getDateFromTimestamp(_time: number, _userDateFormatOptions?: Intl.DateTimeFormatOptions) {
        return '10/10/2020 10:10 PM';
    }
}
