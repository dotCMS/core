import { Injectable } from '@angular/core';

/**
 * Clipboard utils
 *
 * @export
 * @class DotClipboardUtil
 */
@Injectable()
export class DotClipboardUtil {
    constructor() {}

    /**
     * Copy the passed string to the clipboard
     *
     * @param string text
     * @returns Promise<boolean>
     * @memberof DotClipboardUtil
     */
    copy(text: string): Promise<boolean> {
        /*
            Aparently this is the only crossbrowser solution so far. If we do this in another place we might have
            to include an npm module.
        */
        const txtArea = document.createElement('textarea');

        txtArea.style.position = 'fixed';
        txtArea.style.top = '0';
        txtArea.style.left = '0';
        txtArea.style.opacity = '0';
        txtArea.value = text;
        document.body.appendChild(txtArea);
        txtArea.select();

        let result;

        return new Promise((resolve, reject) => {
            try {
                result = document.execCommand('copy');
                resolve(result);
            } catch (err) {
                reject(result);
            }
            document.body.removeChild(txtArea);
        });
    }
}
