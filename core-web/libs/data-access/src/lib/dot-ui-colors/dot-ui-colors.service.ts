import { TinyColor } from '@ctrl/tinycolor';
import { palette, updatePrimaryPalette } from '@primeuix/themes';

import { Injectable } from '@angular/core';

import { DotUiColors } from '@dotcms/dotcms-js';

type ColorType = 'primary' | 'secondary';

/** PrimeNG palette() returns these eleven steps; we map them 1:1 to our 100-900 contract. */
type PrimeNgPalette = Record<string, string>;

/**
 * Maps PrimeNG's 50-950 palette steps to dotCMS's legacy 100-900 CSS-variable contract.
 * Both consumers (PrimeNG components and the JSP/Dojo iframe) read from the SAME palette()
 * output, so a `--p-primary-500` token and a `--color-palette-primary-500` variable always
 * resolve to the identical color.
 */
const PALETTE_TO_LEGACY_SHADE: Record<string, string> = {
    '100': '50',
    '200': '100',
    '300': '200',
    '400': '300',
    '500': '500',
    '600': '600',
    '700': '700',
    '800': '800',
    '900': '900'
};

export const DEFAULT_COLORS = {
    primary: '#426BF0',
    secondary: '#7042F0',
    background: '#FFFFFF'
};

/**
 * Service for managing UI colors in dotCMS.
 *
 * A single generator — PrimeNG's `palette()` — produces one 50-950 scale per color, which
 * feeds two consumers so they never diverge:
 *
 * **1. PrimeNG design tokens (modern Angular components)**
 *    - `updatePrimaryPalette()` updates `--p-primary-50` through `--p-primary-950` at runtime.
 *    - Primary only: PrimeNG has no `updateSecondaryPalette()`, so `severity="secondary"`
 *      components use the value baked into the preset (see theme.config.ts).
 *
 * **2. Legacy CSS variables (Angular components & JSP/Dojo portlets)**
 *    - `--color-palette-{primary|secondary}-{100..900}` plus opacity variants `-op-{10..90}`.
 *    - Both primary and secondary are updated here.
 *    - Set on the main app's `documentElement` and on each JSP iframe's `documentElement`,
 *      because the iframe has its own document and cannot read the parent's `--p-*` tokens.
 *
 * Background color is written only as `--color-background`.
 *
 * Color source: `/api/v1/appconfiguration` (user-defined), falling back to `DEFAULT_COLORS`.
 */
@Injectable()
export class DotUiColorsService {
    private currentColors: DotUiColors = DEFAULT_COLORS;

    /**
     * Sets colors and updates both consumers from one generated palette.
     *
     * @param el - Element to set CSS variables on: the main app's `document.documentElement`,
     *             or an iframe's `contentDocument.documentElement` for JSP portlets.
     * @param colors - Optional; falls back to the current colors when omitted.
     */
    setColors(el: HTMLElement, colors?: DotUiColors): void {
        this.currentColors = colors || this.currentColors;

        this.updatePrimeNGColors(this.currentColors.primary);

        this.setColor(el, this.currentColors.primary, 'primary');
        this.setColor(el, this.currentColors.secondary, 'secondary');
        this.setColorBackground(el, this.currentColors.background);
    }

    /**
     * Gets current colors synchronously.
     */
    getColors(): DotUiColors {
        return this.currentColors;
    }

    /**
     * Generates the default PrimeNG palette for the initial preset (theme.config.ts),
     * keeping the build-time preset in sync with runtime updates.
     */
    static getDefaultPrimeNGPalette(): PrimeNgPalette {
        return generatePalette(DEFAULT_COLORS.primary);
    }

    /**
     * Updates PrimeNG primary design tokens. PrimeNG only supports runtime updates for the
     * primary palette; secondary components fall back to the preset value (theme.config.ts).
     *
     * @private
     */
    private updatePrimeNGColors(primary: string): void {
        try {
            updatePrimaryPalette(generatePalette(primary));
        } catch (error) {
            // Silently fail if the PrimeNG theming API is not ready (e.g. during SSR).
            console.warn('Failed to update PrimeNG colors:', error);
        }
    }

    /**
     * Writes the legacy `--color-palette-{type}-*` CSS variables from the same palette()
     * output PrimeNG uses, plus the `-h`/`-s` and opacity variables the JSP CSS depends on.
     *
     * @private
     */
    private setColor(el: HTMLElement, hex: string, type: ColorType): void {
        const color = new TinyColor(hex);

        if (!color.isValid) {
            return;
        }

        const generated = generatePalette(hex);
        const { h, s, l } = color.toHsl();

        // HSL pieces consumed by the JSP/Dojo CSS (e.g. to compose runtime opacities).
        el.style.setProperty(`--color-${type}-h`, `${Math.round(h)}deg`);
        el.style.setProperty(`--color-${type}-s`, `${Math.round(s * 100)}%`);

        this.setShades(el, generated, type);
        this.setOpacities(el, Math.round(l * 100), type);
    }

    /**
     * Maps the PrimeNG palette to the legacy 100-900 CSS variables.
     *
     * @private
     */
    private setShades(el: HTMLElement, generated: PrimeNgPalette, type: ColorType): void {
        Object.entries(PALETTE_TO_LEGACY_SHADE).forEach(([legacyShade, paletteStep]) => {
            el.style.setProperty(`--color-palette-${type}-${legacyShade}`, generated[paletteStep]);
        });
    }

    /**
     * Generates the `-op-{10..90}` opacity variables. They reference `--color-{type}-h/-s`
     * by name (so the JSP CSS resolves them at use time) and carry the color's real lightness.
     *
     * @private
     */
    private setOpacities(el: HTMLElement, lightness: number, type: ColorType): void {
        for (let i = 1; i < 10; i++) {
            el.style.setProperty(
                `--color-palette-${type}-op-${i}0`,
                `hsla(var(--color-${type}-h), var(--color-${type}-s), ${lightness}%, 0.${i})`
            );
        }
    }

    /**
     * Sets the background color CSS variable.
     *
     * @private
     */
    private setColorBackground(el: HTMLElement, color: string): void {
        const background = new TinyColor(color);

        if (background.isValid) {
            el.style.setProperty('--color-background', background.toHexString().toUpperCase());
        }
    }
}

/**
 * Single palette generator for the whole service: PrimeNG's `palette()` produces a
 * perceptually-tuned 50-950 scale whose `500` step is the input color itself. Falls back to
 * the default primary palette when given an invalid color.
 */
function generatePalette(hex: string): PrimeNgPalette {
    const color = new TinyColor(hex);

    // palette() returns a ColorScale object for a color input (string only for token
    // expressions, which we never pass), so the cast is safe.
    const source = color.isValid ? hex : DEFAULT_COLORS.primary;
    const scale = palette(source) as PrimeNgPalette;

    // Preserve the exact input hex at 500; palette() lowercases, so normalize to the source.
    return { ...scale, '500': new TinyColor(source).toHexString() };
}
