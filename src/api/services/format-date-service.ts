import {Injectable} from '@angular/core';
import moment from 'moment';

@Injectable()
// TODO: probably a good idea to create a interface for this, will revisit this when we defined the architecture for the lang
export class FormatDate {

    constructor() {
    }

    setLang(lang: string, messages: Object):void {
        // Only "creating" the language once
        if (moment.locale(lang) !== lang) {
            moment.defineLocale(lang, {relativeTime: messages} || {});
        }
        moment.locale(lang);
    }

    getRelative(time):string {
        return moment(parseInt(time, 10)).fromNow();
    }

}

