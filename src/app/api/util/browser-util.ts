import {Injectable} from '@angular/core';

@Injectable()
export class BrowserUtil {
    public isIE11(): boolean {
        return navigator.appName === 'Netscape' && navigator.appVersion.indexOf('Trident') !== -1;
    }
}