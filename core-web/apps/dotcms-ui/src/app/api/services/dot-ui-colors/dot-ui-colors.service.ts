import { TinyColor } from '@ctrl/tinycolor';
import ShadeGenerator from 'shade-generator';

import { Injectable } from '@angular/core';

import { DotUiColors } from '@dotcms/dotcms-js';

// ShadeGenerator generates 20 colors and we only use 10, so we need to map the colors.
const dictionary = {
    '100': '10',
    '200': '30',
    '300': '50',
    '400': '70',
    '500': '100',
    '600': '300',
    '700': '500',
    '800': '800',
    '900': '1000'
};

@Injectable()
export class DotUiColorsService {
    private currentColors: DotUiColors;

    /**
     * Set CSS variables colors
     *
     * @param DotUiColors colors
     * @memberof DotUiColorsService
     */
    setColors(el: HTMLElement, colors?: DotUiColors): void {
        this.currentColors = colors || this.currentColors;

        this.setColor(el, this.currentColors.primary, 'primary');
        this.setColor(el, this.currentColors.secondary, 'secondary');
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

    private setColorBackground(el: HTMLElement, color: string): void {
        const colorBackground: TinyColor = new TinyColor(color);

        if (colorBackground.isValid && this.getDefaultsColors(el).background !== color) {
            el.style.setProperty('--color-background', colorBackground.toHexString().toUpperCase());
        }
    }

    private setColor(el: HTMLElement, color: string, type: string): void {
        const shades = ShadeGenerator.hue(color).shadesMap('hsl');

        for (const shade in dictionary) {
            const color = shades[dictionary[shade]];
            el.style.setProperty(`--color-palette-${type}-${shade}`, color);
        }
    }
}
