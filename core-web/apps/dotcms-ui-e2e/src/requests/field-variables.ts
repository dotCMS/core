import { APIRequestContext, expect } from '@playwright/test';
import { admin1 } from '@utils/credentials';
import { generateBase64Credentials } from '@utils/generateBase64Credential';

/**
 * Creates a field variable on a content type field.
 * All field types support field variables — this is a shared util.
 *
 * @param request - Playwright API request context
 * @param contentTypeId - Content type ID
 * @param fieldId - Field ID within the content type
 * @param key - Variable key (e.g. 'showFields')
 * @param value - Variable value (e.g. 'title,bio')
 */
export async function createFieldVariable(
    request: APIRequestContext,
    contentTypeId: string,
    fieldId: string,
    key: string,
    value: string
): Promise<void> {
    const response = await request.post(
        `/api/v1/contenttype/${contentTypeId}/fields/id/${fieldId}/variables`,
        {
            data: {
                key,
                value,
                clazz: 'com.dotcms.contenttype.model.field.FieldVariable',
                fieldId
            },
            headers: {
                Authorization: generateBase64Credentials(admin1.username, admin1.password)
            }
        }
    );
    if (response.status() !== 200) {
        const errorBody = await response.json().catch(() => response.statusText());
        console.error('createFieldVariable failed:', JSON.stringify(errorBody, null, 2));
    }
    expect(response.status()).toBe(200);
}
