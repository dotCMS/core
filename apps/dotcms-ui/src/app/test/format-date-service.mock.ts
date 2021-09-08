import { format } from 'date-fns';
export class DotFormatDateServiceMock {
    private _localeOptions: any;

    constructor() {}

    get localeOptions(): any {
        return this._localeOptions;
    }

    set localeOptions(locale: any) {
        this._localeOptions = locale;
    }

    setLang(lang: string) {}

    isValid(date: string, formatPattern: string) {}

    format(date: Date, formatPattern: string) {
        return format(date, formatPattern);
    }

    getRelative(time): string {
        return '1 hour ago'
    }
}
