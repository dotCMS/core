import { Injectable } from '@angular/core';
import { TinyColor } from '@ctrl/tinycolor';

export interface DotUiColors {
    primary: string;
    secondary: string;
    background: string;
}

@Injectable()
export class DotUiColorsService {
    private html: HTMLElement;
    private readonly COL_MAIN_LIGHTEN_VAL = 12;

    constructor() {}

    /**
     * Set css variables colors
     *
     * @param {DotUiColors} colors
     * @memberof DotUiColorsService
     */
    setColors(colors: DotUiColors): void {
        this.html = document.querySelector('html');
        this.setColorMain(colors.primary);
        this.setColorSec(colors.secondary);
        this.setColorBackground(colors.background);
    }

    private getDefaultsColors(): DotUiColors {
        const values = window.getComputedStyle(this.html);

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

    private setColorBackground(color: string): void {
        if (this.getDefaultsColors().background !== color) {
            this.html.style.setProperty('--color-background', color);
        }
    }

    private setColorMain(color: string): void {
        if (this.getDefaultsColors().primary !== color) {
            const colorMain = new TinyColor(color);
            const colorMainMod = `#${colorMain
                .lighten(this.COL_MAIN_LIGHTEN_VAL)
                .toHex()
                .toUpperCase()}`;

            this.html.style.setProperty('--color-main', color);
            this.html.style.setProperty('--color-main_mod', colorMainMod);
            this.html.style.setProperty('--color-main_rgb', this.getRgbString(colorMain));
        }
    }

    private setColorSec(color: string): void {
        if (this.getDefaultsColors().secondary !== color) {
            const colorSec = new TinyColor(color);

            this.html.style.setProperty('--color-sec', color);
            this.html.style.setProperty('--color-sec_rgb', this.getRgbString(colorSec));
        }
    }
}
