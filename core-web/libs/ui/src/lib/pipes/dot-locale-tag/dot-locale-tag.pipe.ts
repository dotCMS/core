import { Pipe, PipeTransform } from '@angular/core';

import { DotLanguage } from '@dotcms/dotcms-models';

/**
 * Pipe to get the ISO code for a language.
 * Takes a languageId and a map of languages, returns the isoCode.
 * Example: transform(1, languagesMap) -> 'en'
 * Returns '-' if languageId is invalid, languagesMap is not provided, language is not found, or isoCode is missing.
 */
@Pipe({
    name: 'dotLocaleTag',
    standalone: true
})
export class DotLocaleTagPipe implements PipeTransform {
    /**
     * Transform the language id to its ISO code.
     *
     * @param {number} languageId - The language id to transform.
     * @param {Map<number, DotLanguage>} languagesMap - Map of language id to DotLanguage.
     * @returns {string} The ISO code or '-' if not found.
     */
    transform(languageId: number, languagesMap: Map<number, DotLanguage>): string {
        if (!languageId || !languagesMap) {
            return '-';
        }

        const language = languagesMap.get(languageId);

        if (!language || !language.isoCode) {
            return '-';
        }

        return language.isoCode;
    }
}
