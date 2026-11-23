import { APIRequestContext, expect } from '@playwright/test';
import { admin1 } from '@utils/credentials';
import { generateBase64Credentials } from '@utils/generateBase64Credential';

/** A language the instance has, as `/api/v2/languages` lists it. */
export interface Language {
    id: number;
    isoCode: string;
    language: string;
    defaultLanguage: boolean;
}

/**
 * Lists the instance's languages.
 *
 * @param request The API context.
 * @returns Every language, the default one included.
 */
export async function getLanguages(request: APIRequestContext): Promise<Language[]> {
    const response = await request.get('/api/v2/languages', {
        headers: { Authorization: generateBase64Credentials(admin1.username, admin1.password) }
    });
    expect(response.status()).toBe(200);

    return (await response.json()).entity as Language[];
}

/** What `POST /api/v2/languages` creates a language from. */
export interface LanguageForm {
    languageCode: string;
    countryCode: string;
    language: string;
    country: string;
}

/**
 * A second language for specs that need one on an instance that has only the default, as the empty starter CI runs. The demo starter has other languages, so it never gets this one.
 *
 * Specs keep it once created: specs on parallel workers may share it, and the CI instance is disposable.
 */
export const E2E_SECOND_LANGUAGE: LanguageForm = {
    languageCode: 'it',
    countryCode: 'IT',
    language: 'Italian',
    country: 'Italy'
};

/**
 * Creates a language, or finds the one with the same tag. The endpoint answers 200 in both cases, a language another worker created a moment earlier included, with a "Language already exists." message for the second: either way the language is there.
 *
 * @param request The API context.
 * @param form The language's codes and names.
 * @returns The language.
 */
export async function createLanguage(
    request: APIRequestContext,
    form: LanguageForm
): Promise<Language> {
    const response = await request.post('/api/v2/languages', {
        data: form,
        headers: { Authorization: generateBase64Credentials(admin1.username, admin1.password) }
    });
    expect(response.status()).toBe(200);

    return (await response.json()).entity as Language;
}

/**
 * The first language that is not the default, created from {@link E2E_SECOND_LANGUAGE} when the instance has none. It is never deleted: another worker's spec may be using it.
 *
 * @param request The API context.
 * @returns The language, or `undefined` when there is none and creating one failed.
 */
export async function findOrCreateSecondLanguage(
    request: APIRequestContext
): Promise<Language | undefined> {
    const existing = (await getLanguages(request)).find((language) => !language.defaultLanguage);
    if (existing) {
        return existing;
    }

    return createLanguage(request, E2E_SECOND_LANGUAGE).catch(() => undefined);
}
