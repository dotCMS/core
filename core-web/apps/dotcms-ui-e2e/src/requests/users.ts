import { APIRequestContext, expect } from '@playwright/test';
import { admin1 } from '@utils/credentials';
import { generateBase64Credentials } from '@utils/generateBase64Credential';

/** A user a test creates and removes. */
export interface TestUser {
    id: string;
    firstName: string;
    lastName: string;
    email: string;
    password: string;
}

const adminHeaders = () => ({
    Authorization: generateBase64Credentials(admin1.username, admin1.password),
    'Content-Type': 'application/json'
});

/** The starter administrator's id: the user whose account takes over what a deleted user owned. */
const STARTER_ADMIN_ID = 'dotcms.org.1';

/**
 * Creates a back-end user with the `CMS Administrator` role. The empty starter ships only the
 * administrator, so a test that needs a second person signing in creates one.
 *
 * The `CMS Administrator` role holds every layout but `Getting Started`, which belongs to the
 * starter administrator's own role, so this user's navigation is not the administrator's.
 *
 *   POST /api/v1/users -> 200 (CMS Administrator)
 */
export async function createBackendUser(request: APIRequestContext, user: TestUser): Promise<void> {
    const response = await request.post('/api/v1/users', {
        data: {
            userId: user.id,
            firstName: user.firstName,
            lastName: user.lastName,
            email: user.email,
            password: user.password,
            active: true,
            roles: ['CMS Administrator', 'DOTCMS_BACK_END_USER']
        },
        headers: adminHeaders()
    });
    expect(response.status()).toBe(200);
}

/**
 * Removes a user created by {@link createBackendUser}. Call it from `finally` so a failing test
 * does not leave the user on the instance.
 *
 *   DELETE /api/v1/users/{userId}?replacementUserId=... -> 200
 */
export async function deleteUser(request: APIRequestContext, userId: string): Promise<void> {
    const response = await request.delete(
        `/api/v1/users/${userId}?replacementUserId=${STARTER_ADMIN_ID}`,
        { headers: adminHeaders() }
    );
    expect(response.status()).toBe(200);
}
