import { APIRequestContext, expect } from '@playwright/test';
import { admin1 } from '@utils/credentials';
import { generateBase64Credentials } from '@utils/generateBase64Credential';

/**
 * A category as returned by `/api/v1/categories`.
 */
export interface Category {
    inode: string;
    key: string;
    categoryName: string;
}

function authHeaders() {
    return {
        Authorization: generateBase64Credentials(admin1.username, admin1.password)
    };
}

/**
 * Creates a category, top-level unless a parent inode is given.
 *
 * @param request - Playwright APIRequestContext
 * @param data - Name and key of the category, and optionally the inode of its parent
 * @returns The created category
 */
export async function createCategory(
    request: APIRequestContext,
    data: { name: string; key: string; parent?: string }
): Promise<Category> {
    const response = await request.post('/api/v1/categories', {
        data: {
            categoryName: data.name,
            key: data.key,
            categoryVelocityVarName: data.key,
            active: true,
            sortOrder: 0,
            ...(data.parent ? { parent: data.parent } : {})
        },
        headers: authHeaders()
    });

    expect(response.status()).toBe(200);

    return (await response.json()).entity;
}

/**
 * Deletes categories by inode. Children must come before their parent.
 *
 * @param request - Playwright APIRequestContext
 * @param inodes - Inodes of the categories to delete
 */
export async function deleteCategories(request: APIRequestContext, inodes: string[]) {
    const response = await request.delete('/api/v1/categories', {
        data: inodes,
        headers: authHeaders()
    });

    expect(response.status()).toBe(200);
}
