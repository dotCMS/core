import { Injectable } from '@angular/core';
/**
 * Check if the browser is ie11
 *
 * @export
 * @class BrowserUtil
 * @deprecated We don't support IE11 anymore
 */
@Injectable()

export class BrowserUtil {
    public isIE11(): boolean {
        return navigator.appName === 'Netscape' && navigator.appVersion.indexOf('Trident') !== -1;
    }
}
