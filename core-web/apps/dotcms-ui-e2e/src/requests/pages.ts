import { APIRequestContext, expect } from '@playwright/test';
import { admin1 } from '@utils/credentials';
import { generateBase64Credentials } from '@utils/generateBase64Credential';

import { getSites } from './sites';

export interface Page {
    identifier: string;
    contentType: 'htmlpageasset';
    title: string;
    url: string;
    hostFolder: string;
    template: string;
    friendlyName: string;
    cachettl: number;
    inode: string;
}

type CreatePage = Omit<Page, 'identifier' | 'inode' | 'hostFolder'>;

export async function createPage(request: APIRequestContext, data: CreatePage): Promise<Page> {
    const sites = await getSites(request);
    const defaultSite = sites.find((site) => site.default && !site.systemHost);
    if (!defaultSite) {
        throw new Error('No default site found. Cannot create HTML page.');
    }

    const endpoint = `/api/v1/workflow/actions/default/fire/PUBLISH?indexPolicy=WAIT_FOR`;
    const response = await request.post(endpoint, {
        data: {
            contentlet: {
                ...data,
                hostFolder: defaultSite.identifier
            }
        },
        headers: {
            Authorization: generateBase64Credentials(admin1.username, admin1.password)
        }
    });

    expect(response.status()).toBe(200);

    const responseData = await response.json();
    const results = responseData.entity.results;
    expect(results).toHaveLength(1);

    // The API wraps the contentlet under its content type variable key
    // e.g. results[0] = { "htmlpageasset": { identifier, inode, ... } }
    const key = Object.keys(results[0])[0];
    const page = results[0][key] as Page;

    expect(page.inode).toBeTruthy();
    expect(page.identifier).toBeTruthy();

    return page;
}

export async function executeAction(request: APIRequestContext, actionId: string, inode: string) {
    const endpoint = `/api/v1/workflow/actions/${actionId}/fire?indexPolicy=WAIT_FOR&inode=${inode}`;
    const response = await request.put(endpoint, {
        headers: {
            Authorization: generateBase64Credentials(admin1.username, admin1.password)
        },
        data: {}
    });
    expect(response.status()).toBe(200);
}

export interface Action {
    id: string;
    name: string;
}

export async function getActionsByContentlet(request: APIRequestContext, inode: string) {
    const endpoint = `/api/v1/workflow/contentlet/${inode}/actions?renderMode=LISTING`;
    const response = await request.get(endpoint, {
        headers: {
            Authorization: generateBase64Credentials(admin1.username, admin1.password)
        }
    });
    expect(response.status()).toBe(200);
    const responseData = await response.json();
    return responseData.entity as Action[];
}

export async function actionsPageWorkflow(
    request: APIRequestContext,
    inode: string,
    steps: string[]
) {
    for (const step of steps) {
        const actions = await getActionsByContentlet(request, inode);
        const action = actions.find((a) => a.name === step);
        if (action) {
            await executeAction(request, action.id, inode);
        } else {
            console.warn(`Action not found for workflow step: ${step}. Skipping remaining steps.`);
            break;
        }
    }
}
