import { Injectable } from '@angular/core';
import { TinyColor } from '@ctrl/tinycolor';
import { DotUiColors } from 'dotcms-js';

@Injectable()
export class DotUiColorsService {
    private currentColors: DotUiColors;
    private readonly COL_MAIN_LIGHTEN_VAL = 12;

    constructor() {}

    /**
     * Set css variables colors
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
        const colorMain = new TinyColor(color);

        if (colorMain.isValid && this.getDefaultsColors(el).primary !== color) {
            const colorMainMod = `#${colorMain
                .lighten(this.COL_MAIN_LIGHTEN_VAL)
                .toHex()
                .toUpperCase()}`;

            el.style.setProperty('--color-main', colorMain.toHexString().toUpperCase());
            el.style.setProperty('--color-main_mod', colorMainMod);
            el.style.setProperty('--color-main_rgb', this.getRgbString(colorMain));
        }
    }

    private setColorSec(el: HTMLElement, color: string): void {
        const colorSec = new TinyColor(color);

        if (colorSec.isValid && this.getDefaultsColors(el).secondary !== color) {
            el.style.setProperty('--color-sec', colorSec.toHexString().toUpperCase());
            el.style.setProperty('--color-sec_rgb', this.getRgbString(colorSec));
        }
    }
}
