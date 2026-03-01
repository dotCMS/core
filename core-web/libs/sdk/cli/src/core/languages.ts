/**
 * Language utilities — shared language map fetching for CLI commands.
 */

import consola from 'consola';

import { get } from './http';

import type { LanguageEntry, LanguageMap } from './types';
import type { $Fetch } from 'ofetch';

/**
 * Fetch the language map from the server.
 * Maps languageId → language code (e.g., "1" → "en-US").
 */
export async function fetchLanguageMap(client: $Fetch): Promise<LanguageMap> {
    try {
        const response = await get<{ entity: LanguageEntry[] }>(client, '/api/v2/languages');
        const map: LanguageMap = {};
        for (const lang of response.entity) {
            const code = lang.countryCode
                ? `${lang.languageCode}-${lang.countryCode}`
                : lang.languageCode;
            map[String(lang.id)] = code;
        }
        return map;
    } catch {
        consola.warn('Could not fetch languages, using default (1 → en-US).');
        return { '1': 'en-US' };
    }
}
