import { TinyColor } from '@ctrl/tinycolor';
import { updatePrimaryPalette } from '@primeuix/themes';
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

/**
 * Service for managing UI colors in dotCMS
 *
 * Handles color updates for two different approaches:
 *
 * **1. PrimeNG Theme (Modern Angular Components)**
 *    - Uses `updatePrimaryPalette()` to dynamically update PrimeNG design tokens
 *    - Updates CSS variables: `--p-primary-50` through `--p-primary-950`
 *    - **Primary color only for PrimeNG semantic tokens**: PrimeNG components using
 *      `severity="secondary"` use semantic tokens from the preset (not updatable at runtime)
 *    - Used by: PrimeNG components with semantic tokens (primary only)
 *
 * **2. CSS Variables (Angular Components & JSP Portlets)**
 *    - Sets CSS custom properties on HTML elements (main app or iframes)
 *    - Variables: `--color-palette-primary-*`, `--color-palette-secondary-*`, etc.
 *    - **Both primary and secondary**: Both colors are updated via CSS variables
 *    - Used by:
 *      - Angular components using CSS variables directly (e.g., `bg-(--color-palette-secondary-200)`)
 *      - Custom PrimeNG styles (e.g., `.p-badge.p-badge-secondary` uses `$color-palette-secondary`)
 *      - Legacy JSP portlets loaded in iframes (dotcms.css, dotai.css)
 *
 * **Important Notes:**
 * - **Primary color**: Updated in both PrimeNG semantic tokens (dynamically) AND CSS variables
 * - **Secondary color**: Updated in CSS variables (dynamically) for Angular components and custom styles
 *   - CSS variables: `--color-palette-secondary-*` are updated dynamically and used by Angular components
 *   - PrimeNG semantic tokens: Components using `severity="secondary"` use preset value (not updatable)
 * - **Background color**: Updated ONLY in CSS variables
 *
 * **APIs:**
 * - `setColors(el, colors?)` - Updates both PrimeNG theme and legacy CSS variables
 * - `getColors()` - Gets current colors synchronously
 *
 * **Color Sources:**
 * - Server config: `/api/v1/appconfiguration` (user-defined colors)
 * - Fallback: `DEFAULT_COLORS` if server fails or user not authenticated
 */
@Injectable()
export class DotUiColorsService {
    private currentColors: DotUiColors = DEFAULT_COLORS;

    /**
     * Sets colors and updates both approaches:
     * 1. PrimeNG theme (via updatePrimaryPalette) - for PrimeNG semantic tokens
     *    - Only primary color is updated (secondary uses preset, not updatable)
     * 2. CSS variables (on HTML element) - for Angular components and JSP portlets
     *    - Both primary and secondary colors are updated via CSS variables
     *    - Used by Angular components directly and custom PrimeNG styles
     *
     * @param el - HTML element to set CSS variables on
     *            - Main app: `document.documentElement`
     *            - Iframes: `iframe.contentDocument.documentElement` (for JSP portlets)
     * @param colors - Optional. Uses current colors or defaults if not provided
     */
    setColors(el: HTMLElement, colors?: DotUiColors): void {
        this.currentColors = colors || this.currentColors;

        // Approach 1: Update PrimeNG theme for Angular components
        this.updatePrimeNGColors(this.currentColors);

        // Approach 2: Set legacy CSS variables for JSP portlets
        this.setColor(el, this.currentColors.primary, 'primary');
        this.setColor(el, this.currentColors.secondary, 'secondary');
        this.setColorBackground(el, this.currentColors.background);
    }

    /**
     * Gets current colors synchronously
     */
    getColors(): DotUiColors {
        return this.currentColors;
    }

    /**
     * Approach 1: Updates PrimeNG theme colors dynamically
     * Generates palette (50-950) from base colors and updates PrimeNG design tokens
     *
     * **Important:** This only updates PrimeNG semantic tokens (used by components with
     * `severity="primary"`). PrimeNG supports secondary as a severity type, but only
     * primary can be updated dynamically via `updatePrimaryPalette()`. There is no
     * equivalent `updateSecondaryPalette()` function.
     *
     * **Secondary color behavior:**
     * - PrimeNG semantic tokens: Components using `severity="secondary"` use the preset
     *   value (defined in theme.config.ts), not updatable at runtime
     * - CSS variables: Secondary is updated dynamically via `setColor()` (line 95) and
     *   used by Angular components and custom PrimeNG styles that reference CSS variables
     *
     * @private
     */
    private updatePrimeNGColors(colors: DotUiColors): void {
        try {
            // Update primary color palette for PrimeNG semantic tokens
            // PrimeNG only supports dynamic runtime updates for primary color semantic tokens
            const primaryPalette = this.generatePrimeNGPalette(colors.primary);
            updatePrimaryPalette(primaryPalette);

            // Note: Secondary color semantic tokens are NOT updated here because:
            // 1. PrimeNG doesn't have updateSecondaryPalette() function for runtime updates
            // 2. Secondary semantic tokens can only be defined in the initial preset (theme.config.ts)
            // 3. PrimeNG components using severity="secondary" will use the preset value
            //
            // However, secondary CSS variables ARE updated in setColor() method (line 95),
            // which are used by:
            // - Angular components using CSS variables directly (e.g., bg-(--color-palette-secondary-200))
            // - Custom PrimeNG styles (e.g., .p-badge.p-badge-secondary uses $color-palette-secondary)
            // - Legacy JSP portlets
            //
            // If PrimeNG adds updateSecondaryPalette() in the future, it would be added here
        } catch (error) {
            // Silently fail if PrimeNG theming API is not available
            // This can happen during SSR or if PrimeNG hasn't initialized yet
            console.warn('Failed to update PrimeNG colors:', error);
        }
    }

    /**
     * Generates PrimeNG color palette (50-950) from base hex color
     *
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
     * Generates palette using ShadeGenerator
     *
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
     * Returns default palette from DEFAULT_COLORS.primary
     *
     * @private
     */
    private getDefaultPrimeNGPalette(): Record<string, string> {
        // Generate palette from DEFAULT_COLORS.primary to ensure consistency
        return this.generatePaletteWithShadeGenerator(DEFAULT_COLORS.primary);
    }

    /**
     * Static method to generate default palette for theme.config.ts
     * Ensures initial preset uses same defaults as service
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
     * @private
     */
    private lighten(hex: string, amount: number): string {
        return new TinyColor(hex).lighten(amount).toHexString();
    }

    /**
     * @private
     */
    private darken(hex: string, amount: number): string {
        return new TinyColor(hex).darken(amount).toHexString();
    }

    /**
     * Sets background color CSS variable for JSP portlets
     *
     * @private
     */
    private setColorBackground(el: HTMLElement, color: string): void {
        const colorBackground: TinyColor = new TinyColor(color);

        if (colorBackground.isValid) {
            el.style.setProperty('--color-background', colorBackground.toHexString().toUpperCase());
        }
    }

    /**
     * Approach 2: Sets CSS variables for a color (primary/secondary)
     * Generates HSL base, shades (100-900), and opacities (10-90)
     *
     * Used by:
     * - Angular components using CSS variables directly (e.g., `bg-(--color-palette-secondary-200)`)
     * - Custom PrimeNG styles (e.g., `.p-badge.p-badge-secondary`)
     * - Legacy JSP portlets (dotcms.css, dotai.css)
     *
     * @private
     */
    private setColor(el: HTMLElement, hex: string, type: ColorType): void {
        const color = new TinyColor(hex);

        if (color.isValid) {
            const baseColor = ShadeGenerator.hue(hex).shade('100').hsl();
            const baseColorHsl = parseHSL(baseColor);

            // Set HSL base values (used by JSP CSS)
            el.style.setProperty(`--color-${type}-h`, baseColorHsl.hue);
            el.style.setProperty(`--color-${type}-s`, baseColorHsl.saturation);

            // Generate shades and opacities for JSP portlets
            this.setShades(el, hex, type);
            this.setOpacities(el, baseColorHsl.saturation, type);
        }
    }

    /**
     * Generates CSS variables for color shades (100-900) for JSP portlets
     *
     * @private
     */
    private setShades(el: HTMLElement, hex: string, type: ColorType) {
        const shades = ShadeGenerator.hue(hex).shadesMap('hsl');

        Object.entries(dictionary).forEach(([shade, shadeKey]) => {
            const color = shades[shadeKey as keyof typeof shades];
            el.style.setProperty(`--color-palette-${type}-${shade}`, color);
        });
    }

    /**
     * Generates CSS variables for color opacities (10-90) for JSP portlets
     * Used in dotcms.css and dotai.css for outlines, backgrounds, and shadows
     *
     * @private
     */
    private setOpacities(el: HTMLElement, saturation: string, type: ColorType) {
        for (let i = 1; i < 10; i++) {
            el.style.setProperty(
                `--color-palette-${type}-op-${i}0`,
                `hsla(var(--color-${type}-h), var(--color-${type}-s), ${saturation}, 0.${i})`
            );
        }
    }
}
