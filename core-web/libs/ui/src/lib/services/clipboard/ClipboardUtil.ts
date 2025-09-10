import { Injectable } from '@angular/core';

/**
 * Clipboard utils
 *
 * @export
 * @class DotClipboardUtil
 */
@Injectable()
export class DotClipboardUtil {
    /**
     * Copy the passed string to the clipboard
     *
     * @param string text
     * @returns Promise<boolean>
     * @memberof DotClipboardUtil
     */
    async copy(text: string): Promise<boolean> {
        try {
            await navigator.clipboard.writeText(text);

            return true;
        } catch {
            return this.fallbackCopyToClipboard(text);
        }
    }

    /**
     * Fallback method for older browsers that don't support the Clipboard API
     * @private
     */
    private fallbackCopyToClipboard(text: string): Promise<boolean> {
        return new Promise((resolve) => {
            try {
                const txtArea = document.createElement('textarea');
                txtArea.style.position = 'fixed';
                txtArea.style.top = '0';
                txtArea.style.left = '0';
                txtArea.style.opacity = '0';
                txtArea.value = text;
                document.body.appendChild(txtArea);
                txtArea.select();

                const result = document.execCommand('copy');
                document.body.removeChild(txtArea);
                resolve(result);
            } catch {
                resolve(false);
            }
        });
    }
}
