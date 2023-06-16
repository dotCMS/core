import { TinyColor } from '@ctrl/tinycolor';

import { Injectable } from '@angular/core';

import { DotUiColors } from '@dotcms/dotcms-js';

type HSL = [number, number, number];

function hexToHSL(hex: string): HSL {
    let r = 0;
    let g = 0;
    let b = 0;

    if (hex.length == 4) {
        r = parseInt(hex[1] + hex[1], 16);
        g = parseInt(hex[2] + hex[2], 16);
        b = parseInt(hex[3] + hex[3], 16);
    } else if (hex.length == 7) {
        r = parseInt(hex[1] + hex[2], 16);
        g = parseInt(hex[3] + hex[4], 16);
        b = parseInt(hex[5] + hex[6], 16);
    }

    r /= 255;
    g /= 255;
    b /= 255;
    const cmin = Math.min(r, g, b);
    const cmax = Math.max(r, g, b);
    const delta = cmax - cmin;
    let h = 0;
    let s = 0;
    let l = 0;

    if (delta == 0) h = 0;
    else if (cmax == r) h = ((g - b) / delta) % 6;
    else if (cmax == g) h = (b - r) / delta + 2;
    else h = (r - g) / delta + 4;

    h = Math.round(h * 60);

    if (h < 0) h += 360;

    l = (cmax + cmin) / 2;
    s = delta == 0 ? 0 : delta / (1 - Math.abs(2 * l - 1));
    s = +(s * 100).toFixed(1);
    l = +(l * 100).toFixed(1);

    return [h, s, l];
}

function generateShades(hex: string): string[] {
    const hsl = hexToHSL(hex);
    const h = hsl[0];
    const s = hsl[1];

    const shades: string[] = [];
    for (let i = 0; i < 10; i++) {
        let lightness = 88 - i * 10;
        if (lightness < 4) {
            lightness = 4;
        }

        shades.push(`hsl(${h},${s}%,${lightness}%)`);
    }

    return shades;
}

@Injectable()
export class DotUiColorsService {
    private currentColors: DotUiColors;
    private readonly COL_MAIN_LIGHTEN_VAL = 12;

    /**
     * Set CSS variables colors
     *
     * @param DotUiColors colors
     * @memberof DotUiColorsService
     */
    setColors(el: HTMLElement, colors?: DotUiColors): void {
        this.currentColors = colors || this.currentColors;

        this.setColorMain(el, this.currentColors.primary);
        this.setColorSec(el, this.currentColors.secondary);
        this.setColorBackground(el, this.currentColors.background);
    }

    private getDefaultsColors(el: HTMLElement): DotUiColors {
        const values = window.getComputedStyle(el);

        return {
            primary: values.getPropertyValue('--color-main'),
            secondary: values.getPropertyValue('--color-sec'),
            background: values.getPropertyValue('--color-background')
        };
    }

    private getRgbString(color: TinyColor): string {
        const rgb = color.toRgb();

        return `${rgb.r}, ${rgb.g}, ${rgb.b}`;
    }

    private setColorBackground(el: HTMLElement, color: string): void {
        const colorBackground: TinyColor = new TinyColor(color);

        if (colorBackground.isValid && this.getDefaultsColors(el).background !== color) {
            el.style.setProperty('--color-background', colorBackground.toHexString().toUpperCase());
        }
    }

    private setColorMain(el: HTMLElement, color: string): void {
        const shades = generateShades(color);

        shades.forEach((shade, index) => {
            const shadeIndex = index + 1;
            el.style.setProperty(`--color-palette-primary-${shadeIndex}00`, shade);
        });

        // const colorMain = new TinyColor(color);

        // if (colorMain.isValid && this.getDefaultsColors(el).primary !== color) {
        //     const colorMainMod = `#${colorMain
        //         .lighten(this.COL_MAIN_LIGHTEN_VAL)
        //         .toHex()
        //         .toUpperCase()}`;

        //     el.style.setProperty('--color-main', colorMain.toHexString().toUpperCase());
        //     el.style.setProperty('--color-main_mod', colorMainMod);
        //     el.style.setProperty('--color-main_rgb', this.getRgbString(colorMain));
        // }
    }

    private setColorSec(el: HTMLElement, color: string): void {
        const colorSec = new TinyColor(color);

        if (colorSec.isValid && this.getDefaultsColors(el).secondary !== color) {
            el.style.setProperty('--color-sec', colorSec.toHexString().toUpperCase());
            el.style.setProperty('--color-sec_rgb', this.getRgbString(colorSec));
        }
    }
}
