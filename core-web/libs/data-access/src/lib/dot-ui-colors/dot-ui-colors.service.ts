import { TinyColor } from '@ctrl/tinycolor';
import ShadeGenerator from 'shade-generator';

import { Injectable } from '@angular/core';

import { updatePrimaryPalette } from '@primeuix/themes';

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
     * Set colors for both PrimeNG theme and legacy CSS variables
     *
     * This method updates:
     * 1. PrimeNG theme using updatePrimaryPalette and updatePreset APIs
     * 2. Legacy CSS custom properties for backward compatibility
     *
     * @param el - HTML element to set CSS variables on (usually document.documentElement)
     * @param colors - Optional colors object. If not provided, uses current colors or defaults
     * @memberof DotUiColorsService
     */
    setColors(el: HTMLElement, colors?: DotUiColors): void {
        this.currentColors = colors || this.currentColors;

        // Update PrimeNG theme colors dynamically
        this.updatePrimeNGColors();

        // Maintain backward compatibility with legacy CSS variables
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

    /**
     * Updates PrimeNG theme colors using the PrimeNG theming API
     * 
     * This method generates a complete color palette (50-950) from the base colors
     * and updates PrimeNG's design tokens dynamically at runtime.
     * 
     * @private
     */
    private updatePrimeNGColors(): void {
        try {
            // Update primary color palette
            const primaryPalette = this.generatePrimeNGPalette(this.currentColors.primary);
            updatePrimaryPalette(primaryPalette);

            // Update secondary color if needed (using updatePreset for more control)
            // Note: PrimeNG doesn't have a built-in secondary color in the preset,
            // but we can store it for future use or custom components
            if (this.currentColors.secondary && this.currentColors.secondary !== DEFAULT_COLORS.secondary) {
                const secondaryPalette = this.generatePrimeNGPalette(this.currentColors.secondary);
                // Store secondary palette for potential future use
                // For now, we only update primary as that's what PrimeNG uses by default
            }
        } catch (error) {
            // Silently fail if PrimeNG theming API is not available
            // This can happen during SSR or if PrimeNG hasn't initialized yet
            console.warn('Failed to update PrimeNG colors:', error);
        }
    }

    /**
     * Generates a complete PrimeNG color palette (50-950) from a base hex color
     * 
     * PrimeNG expects a palette with shades from 50 (lightest) to 950 (darkest),
     * where 500 is typically the base color.
     * 
     * @param hex - Base color in hex format (e.g., '#426BF0')
     * @returns Record with keys '50' through '950' and hex color values
     * @private
     */
    private generatePrimeNGPalette(hex: string): Record<string, string> {
        const color = new TinyColor(hex);

        if (!color.isValid) {
            // Return default palette if color is invalid
            return this.getDefaultPrimeNGPalette();
        }

        // Use ShadeGenerator to create the palette (reliable and consistent)
        // PrimeNG's palette() function may have different return types, so we use
        // ShadeGenerator as the primary method for consistency
        return this.generatePaletteWithShadeGenerator(hex);
    }

    /**
     * Generates palette using ShadeGenerator as fallback
     * 
     * @param hex - Base color in hex format
     * @returns Record with PrimeNG palette format
     * @private
     */
    private generatePaletteWithShadeGenerator(hex: string): Record<string, string> {
        const shades = ShadeGenerator.hue(hex).shadesMap('hex');

        // Map ShadeGenerator output to PrimeNG format (50-950)
        return {
            '50': shades['10'] || this.lighten(hex, 95),
            '100': shades['30'] || this.lighten(hex, 85),
            '200': shades['50'] || this.lighten(hex, 70),
            '300': shades['70'] || this.lighten(hex, 50),
            '400': shades['100'] || this.lighten(hex, 30),
            '500': hex, // Base color
            '600': shades['300'] || this.darken(hex, 10),
            '700': shades['500'] || this.darken(hex, 20),
            '800': shades['800'] || this.darken(hex, 30),
            '900': shades['1000'] || this.darken(hex, 40),
            '950': this.darken(hex, 50)
        };
    }

    /**
     * Returns the default PrimeNG primary palette generated from DEFAULT_COLORS.primary
     * 
     * This method generates the palette using the same logic as generatePrimeNGPalette
     * to ensure consistency between the initial theme config and runtime updates.
     * 
     * @returns Default palette object generated from DEFAULT_COLORS.primary
     * @private
     */
    private getDefaultPrimeNGPalette(): Record<string, string> {
        // Generate palette from DEFAULT_COLORS.primary to ensure consistency
        return this.generatePaletteWithShadeGenerator(DEFAULT_COLORS.primary);
    }

    /**
     * Generates the default PrimeNG primary palette from DEFAULT_COLORS.primary
     * 
     * This is a public static method that can be used in theme.config.ts to ensure
     * the initial preset uses the same default colors as the service.
     * 
     * @returns Default palette object for PrimeNG theme configuration
     */
    static getDefaultPrimeNGPalette(): Record<string, string> {
        // Use the same generation logic as the service instance
        const shades = ShadeGenerator.hue(DEFAULT_COLORS.primary).shadesMap('hex');

        return {
            '50': shades['10'] || new TinyColor(DEFAULT_COLORS.primary).lighten(95).toHexString(),
            '100': shades['30'] || new TinyColor(DEFAULT_COLORS.primary).lighten(85).toHexString(),
            '200': shades['50'] || new TinyColor(DEFAULT_COLORS.primary).lighten(70).toHexString(),
            '300': shades['70'] || new TinyColor(DEFAULT_COLORS.primary).lighten(50).toHexString(),
            '400': shades['100'] || new TinyColor(DEFAULT_COLORS.primary).lighten(30).toHexString(),
            '500': DEFAULT_COLORS.primary, // Base color
            '600': shades['300'] || new TinyColor(DEFAULT_COLORS.primary).darken(10).toHexString(),
            '700': shades['500'] || new TinyColor(DEFAULT_COLORS.primary).darken(20).toHexString(),
            '800': shades['800'] || new TinyColor(DEFAULT_COLORS.primary).darken(30).toHexString(),
            '900': shades['1000'] || new TinyColor(DEFAULT_COLORS.primary).darken(40).toHexString(),
            '950': new TinyColor(DEFAULT_COLORS.primary).darken(50).toHexString()
        };
    }

    /**
     * Lightens a hex color by a percentage
     * 
     * @param hex - Color in hex format
     * @param amount - Percentage to lighten (0-100)
     * @returns Lightened color in hex format
     * @private
     */
    private lighten(hex: string, amount: number): string {
        return new TinyColor(hex).lighten(amount).toHexString();
    }

    /**
     * Darkens a hex color by a percentage
     * 
     * @param hex - Color in hex format
     * @param amount - Percentage to darken (0-100)
     * @returns Darkened color in hex format
     * @private
     */
    private darken(hex: string, amount: number): string {
        return new TinyColor(hex).darken(amount).toHexString();
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

        Object.entries(dictionary).forEach(([shade, shadeKey]) => {
            const color = shades[shadeKey as keyof typeof shades];
            el.style.setProperty(`--color-palette-${type}-${shade}`, color);
        });
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
