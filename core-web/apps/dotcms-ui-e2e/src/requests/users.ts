import { APIRequestContext, expect } from '@playwright/test';

/** A user a test creates and removes. */
export interface TestUser {
    id: string;
    firstName: string;
    lastName: string;
    email: string;
    password: string;
}

/**
 * Signs `request` in the way the login form does, so the calls that follow carry that session.
 *
 * For a test that runs without the suite's saved login. The server refuses a password sent as
 * Basic authentication by default (`ALLOW_BASIC_AUTH` accepts API tokens only), so a request
 * context with no session cookie has to sign in before it can call the REST API.
 *
 *   POST /api/v1/authentication -> 200
 */
export async function signIn(
    request: APIRequestContext,
    username: string,
    password: string
): Promise<void> {
    const response = await request.post('/api/v1/authentication', {
        data: { userId: username, password, rememberMe: false, backEndLogin: true }
    });
    expect(response.status()).toBe(200);
}

/** The starter administrator's id: the user whose account takes over what a deleted user owned. */
const STARTER_ADMIN_ID = 'dotcms.org.1';

/**
 * Creates a back-end user with the `CMS Administrator` role. The empty starter ships only the
 * administrator, so a test that needs a second person signing in creates one.
 *
 * `request` must carry an administrator's session: the suite's saved login, or {@link signIn}.
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
        }
    });
    expect(response.status()).toBe(200);
}

/**
 * Removes a user created by {@link createBackendUser}. Call it from `finally` so a failing test
 * does not leave the user on the instance. `request` must carry an administrator's session.
 *
 *   DELETE /api/v1/users/{userId}?replacementUserId=... -> 200
 */
export async function deleteUser(request: APIRequestContext, userId: string): Promise<void> {
    const response = await request.delete(
        `/api/v1/users/${userId}?replacementUserId=${STARTER_ADMIN_ID}`
    );
    expect(response.status()).toBe(200);
}
