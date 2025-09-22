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

type ColorType = 'primary' | 'secondary';

export const DEFAULT_COLORS = {
    primary: '#426BF0',
    secondary: '#7042F0',
    background: '#FFFFFF'
};

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
    private currentColors: DotUiColors = DEFAULT_COLORS;

    /**
     * Set CSS variables colors
     *
     * @param DotUiColors colors
     * @memberof DotUiColorsService
     */
    setColors(el: HTMLElement, colors?: DotUiColors): void {
        this.currentColors = colors || this.currentColors;

        if (this.currentColors.primary === DEFAULT_COLORS.primary) {
            this.setDefaultPrimaryColor(el);
        } else {
            this.setColor(el, this.currentColors.primary, 'primary');
        }

        if (this.currentColors.secondary === DEFAULT_COLORS.secondary) {
            this.setDefaultSecondaryColor(el);
        } else {
            this.setColor(el, this.currentColors.secondary, 'secondary');
        }

        this.setColorBackground(el, this.currentColors.background);
    }

    private setColorBackground(el: HTMLElement, color: string): void {
        const colorBackground: TinyColor = new TinyColor(color);

        if (colorBackground.isValid) {
            el.style.setProperty('--color-background', colorBackground.toHexString().toUpperCase());
        }
    }

    private setColor(el: HTMLElement, hex: string, type: ColorType): void {
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

    private setShades(el: HTMLElement, hex: string, type: ColorType) {
        const shades = ShadeGenerator.hue(hex).shadesMap('hsl');

        for (const shade in dictionary) {
            // @ts-expect-error - dictionary is a valid object
            const color = shades[dictionary[shade]];
            el.style.setProperty(`--color-palette-${type}-${shade}`, color);
        }
    }

    private setOpacities(el: HTMLElement, saturation: string, type: ColorType) {
        for (let i = 1; i < 10; i++) {
            el.style.setProperty(
                `--color-palette-${type}-op-${i}0`,
                `hsla(var(--color-primary-h), var(--color-primary-s), ${saturation}, 0.${i})`
            );
        }
    }

    private setDefaultPrimaryColor(el: HTMLElement): void {
        el.style.setProperty(`--color-primary-h`, '226deg');
        el.style.setProperty(`--color-primary-s`, '85%');

        const saturations = [98, 96, 90, 78, 60, 48, 36, 27, 21];

        saturations.forEach((saturation, index) => {
            const level = `${index + 1}00`;
            el.style.setProperty(
                `--color-palette-primary-${level}`,
                `hsl(var(--color-primary-h) var(--color-primary-s) ${saturation}%)`
            );
        });
    }

    private setDefaultSecondaryColor(el: HTMLElement): void {
        el.style.setProperty(`--color-secondary-h`, '256deg');
        el.style.setProperty(`--color-secondary-s`, '85%');

        const saturations = [98, 94, 84, 71, 60, 51, 42, 30, 22];

        saturations.forEach((saturation, index) => {
            const level = `${index + 1}00`;
            el.style.setProperty(
                `--color-palette-secondary-${level}`,
                `hsl(var(--color-secondary-h) var(--color-secondary-s) ${saturation}%)`
            );
        });
    }
}
