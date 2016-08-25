import {Injectable} from '@angular/core';
import moment from 'moment';

@Injectable()
// TODO: probably a good idea to create a interface for this, will revisit this when we defined the architecture for the lang
export class FormatDate {

    constructor() {}

    setLang(lang:string, messages:Object):void {
        moment.locale(lang, messages || {});
    }

    getLang() {
        return moment.locale();
    }

    getRelative(time):string {
        return moment(parseInt(time, 10)).fromNow();
    }

}