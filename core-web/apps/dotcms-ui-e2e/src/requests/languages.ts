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
 * A second language for specs that need one on an instance that has only the default, as the empty starter CI runs. The demo starter does not carry it.
 */
export const E2E_SECOND_LANGUAGE: LanguageForm = {
    languageCode: 'it',
    countryCode: 'IT',
    language: 'Italian',
    country: 'Italy'
};

/**
 * Creates a language. The endpoint answers 200 for a language whose tag the instance already has too, with that language and a "Language already exists." message, so `created` tells the two apart: only a language the caller created is the caller's to delete.
 *
 * @param request The API context.
 * @param form The language's codes and names.
 * @returns The language, and whether this call created it.
 */
export async function createLanguage(
    request: APIRequestContext,
    form: LanguageForm
): Promise<{ language: Language; created: boolean }> {
    const response = await request.post('/api/v2/languages', {
        data: form,
        headers: { Authorization: generateBase64Credentials(admin1.username, admin1.password) }
    });
    expect(response.status()).toBe(200);

    const body = (await response.json()) as {
        entity: Language;
        messages?: { message: string }[];
    };
    const existed = (body.messages ?? []).some(
        ({ message }) => message === 'Language already exists.'
    );

    return { language: body.entity, created: !existed };
}

/**
 * Deletes a language. A language that still has content cannot be deleted: delete the content first.
 *
 * @param request The API context.
 * @param id The language's id.
 */
export async function deleteLanguage(request: APIRequestContext, id: number): Promise<void> {
    const response = await request.delete(`/api/v2/languages/${id}`, {
        headers: { Authorization: generateBase64Credentials(admin1.username, admin1.password) }
    });
    expect(response.status()).toBe(200);
}

/**
 * The first language that is not the default, created from {@link E2E_SECOND_LANGUAGE} when the instance has none.
 *
 * @param request The API context.
 * @returns The language, or `undefined` when there is none and creating one failed; `created` is true only when this call created it.
 */
export async function findOrCreateSecondLanguage(
    request: APIRequestContext
): Promise<{ language: Language | undefined; created: boolean }> {
    const existing = (await getLanguages(request)).find((language) => !language.defaultLanguage);
    if (existing) {
        return { language: existing, created: false };
    }

    return createLanguage(request, E2E_SECOND_LANGUAGE).catch(() => ({
        language: undefined,
        created: false
    }));
}
