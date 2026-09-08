import { Injectable } from '@angular/core';

/**
 * Miscellaneous Color utility methods.
 */
@Injectable()
export class ColorUtil {
    /**
     * Check color brightness.
     *
     * @param color color to check, it could be in hex or rgb format
     * @return brightness value from 0 to 255, where 255 is brigthest.
     * @see http://www.webmasterworld.com/forum88/9769.htm
     */
    public getBrightness(color: string): number {
        const isHexCode = color.indexOf('#') !== -1;

        if (isHexCode) {
            // strip off any leading #
            color = color.replace('#', '');
        } else {
            color = this.rgb2hex(color);
        }

        const c_r = parseInt(color.substr(0, 2), 16);
        const c_g = parseInt(color.substr(2, 2), 16);
        const c_b = parseInt(color.substr(4, 2), 16);

        return (c_r * 299 + c_g * 587 + c_b * 114) / 1000;
    }

    /**
     * Return true if hexCode is a bright color
     * @param color color to check, it could be in hex or rgb format
     */

    public isBrightness(color: string): boolean {
        return this.getBrightness(color) > 138;
    }

    /**
     * Convert RGB color format to hex color format, for example, if you have rgb(0,0,0) return #000
     * @see http://stackoverflow.com/questions/1740700/get-hex-value-rather-than-rgb-value-using-jquery
     */
    public rgb2hex(rgb: string): string {
        if (rgb.search('rgb') === -1) {
            return rgb;
        }

        // Held in a local rather than reassigned over the parameter, which is how a
        // `RegExpMatchArray` came to be sitting in a `string`. A value containing "rgb" that is not
        // a well-formed `rgb()`/`rgba()` does not match, and used to throw on `parts[1]`; it now
        // comes back unchanged, like anything else this cannot convert.
        const parts = rgb.match(/^rgba?\((\d+),\s*(\d+),\s*(\d+)(?:,\s*(\d+(\.\d+)?))?\)$/);

        if (!parts) {
            return rgb;
        }

        return '#' + this.hex(parts[1]) + this.hex(parts[2]) + this.hex(parts[3]);
    }

    private hex(x: string): string {
        return ('0' + parseInt(x, 10).toString(16)).slice(-2);
    }
}
