import { Injectable } from '@angular/core';
import * as moment from 'moment';

@Injectable()
export class FormatDateService {
    constructor() {}

    setLang(lang: string, messages: any): void {
        // Only "creating" the language once
        if (moment.locale(lang) !== lang) {
            moment.defineLocale(lang, { relativeTime: messages } || {});
        }
        moment.locale(lang);
    }

    getRelative(time): string {
        return moment(parseInt(time, 10)).fromNow();
    }
}
