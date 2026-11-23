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
