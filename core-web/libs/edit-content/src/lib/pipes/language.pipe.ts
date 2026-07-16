import { Pipe, PipeTransform } from '@angular/core';

import { DotLanguage } from '@dotcms/dotcms-models';

/**
 * Pipe to format the language into a human-readable string.
 * Takes a DotLanguage object and returns a formatted string with language name and code.
 * Example: { language: 'English', languageCode: 'en' } -> 'English (en)'
 * Falls back to isoCode if languageCode is empty, or empty parentheses if neither exists.
 */
@Pipe({
    name: 'language'
})
export class LanguagePipe implements PipeTransform {
    /**
     * Transform the language to a string.
     *
     * @param {DotLanguage} language - The language to transform.
     * @returns {string} The transformed language.
     */
    transform(language: DotLanguage): string {
        if (!language?.language) {
            return '';
        }

        const code = language.languageCode || language.isoCode || '';

        if (code) {
            return `${language.language} (${code})`;
        }

        return language.language;
    }
}
