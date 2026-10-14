import { Service } from '@angular/core';
/**
 * Check if the browser is ie11
 *
 * @export
 * @class BrowserUtil
 * @deprecated We don't support IE11 anymore
 */
@Service({ autoProvided: false })
export class BrowserUtil {
    public isIE11(): boolean {
        return navigator.appName === 'Netscape' && navigator.appVersion.indexOf('Trident') !== -1;
    }
}
