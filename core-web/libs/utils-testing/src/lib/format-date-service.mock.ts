import { DotLocaleOptions } from '@dotcms/dotcms-models';
import { format } from 'date-fns';

export class DotFormatDateServiceMock {
    private _localeOptions: DotLocaleOptions;

    get localeOptions(): DotLocaleOptions {
        return this._localeOptions;
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

    getRelative(_time): string {
        return '1 hour ago';
    }
}
