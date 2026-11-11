import { APIRequestContext, expect } from '@playwright/test';
import { admin1 } from '@utils/credentials';
import { generateBase64Credentials } from '@utils/generateBase64Credential';

import { Template } from '@models/template.model';

type CreateTemplate = Omit<Template, 'identifier'>;

export async function createTemplate(request: APIRequestContext, data: CreateTemplate) {
    const endpoint = `/api/v1/templates`;
    const response = await request.post(endpoint, {
        data,
        headers: {
            Authorization: generateBase64Credentials(admin1.username, admin1.password)
        }
    });

    expect(response.status()).toBe(200);

    const responseData = await response.json();
    return responseData.entity as Template;
}

export async function getTemplate(request: APIRequestContext, identifier: string) {
    // TemplateResource only exposes /{id}/live and /{id}/working — there's no bare
    // /{id} route. A layout save (TemplateAPIImpl.saveTemplate) only calls
    // VersionableAPI.setWorking, never publishes, so the saved layout lives on the
    // WORKING version, not LIVE.
    const endpoint = `/api/v1/templates/${identifier}/working`;
    const response = await request.get(endpoint, {
        headers: {
            Authorization: generateBase64Credentials(admin1.username, admin1.password)
        }
    });
    expect(response.status()).toBe(200);
    const responseData = await response.json();
    return responseData.entity as Template;
}

/**
 * Removes a template for good: unpublish, archive, then delete, the order the API requires. Each of
 * these bulk endpoints answers 200 even when the template fails, so the failures listed in the body
 * are checked too. A template that pages still reference cannot be deleted: remove those pages
 * first.
 */
export async function removeTemplate(request: APIRequestContext, identifier: string) {
    const headers = { Authorization: generateBase64Credentials(admin1.username, admin1.password) };
    const steps = [
        () => request.put('/api/v1/templates/_unpublish', { data: [identifier], headers }),
        () => request.put('/api/v1/templates/_archive', { data: [identifier], headers }),
        () => request.delete('/api/v1/templates', { data: [identifier], headers })
    ];

    for (const step of steps) {
        const response = await step();
        expect(response.status()).toBe(200);
        expect((await response.json()).entity.fails).toEqual([]);
    }
}
