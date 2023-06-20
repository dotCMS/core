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

type HslObject = { hue: string; saturation: string; lightness: string };

function parseHSL(hslString: string): HslObject {
    // Use regex to match HSL values with their units
    const regex = /hsl\((\d+deg),\s*(\d+%),\s*(\d+)%\)/;
    const match = hslString.match(regex);

    // Check if HSL values were found
    if (match) {
        return {
            hue: match[1],
            saturation: match[2],
            lightness: match[3]
        };
    } else {
        // Handle case where input was not in correct format
        throw new Error('Input is not a valid HSL color string');
    }
}

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

    private setColorBackground(el: HTMLElement, color: string): void {
        const colorBackground: TinyColor = new TinyColor(color);

        if (colorBackground.isValid) {
            el.style.setProperty('--color-background', colorBackground.toHexString().toUpperCase());
        }
    }

    private setColor(el: HTMLElement, hex: string, type: string): void {
        const color = new TinyColor(hex);

        if (color.isValid) {
            const baseColor = ShadeGenerator.hue(hex).shade('100').hsl();
            const baseColorHsl = parseHSL(baseColor);

            el.style.setProperty(`--color-${type}-h`, baseColorHsl.hue);
            el.style.setProperty(`--color-${type}-s`, baseColorHsl.saturation);

            this.setShades(el, hex, type);
            this.setOpacities(el, baseColorHsl.saturation, type);
        }
    }

    private setShades(el: HTMLElement, hex: string, type: string) {
        const shades = ShadeGenerator.hue(hex).shadesMap('hsl');

        for (const shade in dictionary) {
            const color = shades[dictionary[shade]];
            el.style.setProperty(`--color-palette-${type}-${shade}`, color);
        }
    }

    private setOpacities(el: HTMLElement, saturation: string, type: string) {
        for (let i = 1; i < 10; i++) {
            el.style.setProperty(
                `--color-palette-${type}-op-${i}0`,
                `hsla(var(--color-primary-h), var(--color-primary-s), ${saturation}, 0.${i})`
            );
        }
    }
}
