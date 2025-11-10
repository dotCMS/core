import { Pipe, PipeTransform } from '@angular/core';

/**
 * Transforms a language-region code (e.g., 'en-gb') to standardized format (e.g., 'en-GB').
 * Ensures the language code is lowercase and the region code is uppercase.
 * Returns the original input if it doesn't match the ISO code format.
 *
 * @example
 * {{ 'en-gb' | dotIsoCode }} -> 'en-GB'
 * {{ 'FR-fr' | dotIsoCode }} -> 'fr-FR'
 * {{ 'invalid' | dotIsoCode }} -> 'invalid'
 */
@Pipe({
    name: 'dotIsoCode'
})
export class DotIsoCodePipe implements PipeTransform {
    /**
     * Regular expression for validating ISO codes:
     * - Exactly 2 letters for language code
     * - Hyphen separator
     * - Exactly 2 letters for region code
     */
    private static readonly ISO_CODE_PATTERN = /^[a-zA-Z]{2}-[a-zA-Z]{2}$/;

    /**
     * Transforms the input ISO code to a standardized format.
     * @param value - The input ISO code string (e.g., 'en-gb', 'FR-fr')
     * @returns The formatted ISO code or the original input if invalid
     */
    transform(value: string | null | undefined): string {
        // Return empty string for null/undefined inputs
        if (!value) {
            return '';
        }

        // Return original value if it doesn't match the pattern
        if (!DotIsoCodePipe.ISO_CODE_PATTERN.test(value)) {
            return value;
        }

        // Split once and reuse the parts
        const [languageCode, regionCode] = value.split('-');

        // Return formatted code
        return `${languageCode.toLowerCase()}-${regionCode.toUpperCase()}`;
    }
}
