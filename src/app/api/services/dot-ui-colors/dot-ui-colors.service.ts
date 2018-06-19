import { Injectable } from '@angular/core';
import { TinyColor } from '@ctrl/tinycolor';

export interface DotUiColors {
    primary: string;
    secondary: string;
    background: string;
}

@Injectable()
export class DotUiColorsService {
    private currentColors: DotUiColors;
    private readonly COL_MAIN_LIGHTEN_VAL = 12;

    constructor() {}

    /**
     * Set css variables colors
     *
     * @param {DotUiColors} colors
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
        if (this.getDefaultsColors(el).background !== color) {
            el.style.setProperty('--color-background', color);
        }
    }

    private setColorMain(el: HTMLElement, color: string): void {
        if (this.getDefaultsColors(el).primary !== color) {
            const colorMain = new TinyColor(color);
            const colorMainMod = `#${colorMain
                .lighten(this.COL_MAIN_LIGHTEN_VAL)
                .toHex()
                .toUpperCase()}`;

            el.style.setProperty('--color-main', color);
            el.style.setProperty('--color-main_mod', colorMainMod);
            el.style.setProperty('--color-main_rgb', this.getRgbString(colorMain));
        }
    }

    private setColorSec(el: HTMLElement, color: string): void {
        if (this.getDefaultsColors(el).secondary !== color) {
            const colorSec = new TinyColor(color);

            el.style.setProperty('--color-sec', color);
            el.style.setProperty('--color-sec_rgb', this.getRgbString(colorSec));
        }
    }
}
