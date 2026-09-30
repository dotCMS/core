import { APIRequestContext, expect } from '@playwright/test';
import { admin1 } from '@utils/credentials';
import { generateBase64Credentials } from '@utils/generateBase64Credential';

function authHeaders() {
    return {
        Authorization: generateBase64Credentials(admin1.username, admin1.password),
        'Content-Type': 'application/json'
    };
}

/** A notification as the listing returns it, as far as the tests read it. */
interface ListedNotification {
    id: string;
    message?: string;
}

async function listNotifications(request: APIRequestContext): Promise<ListedNotification[]> {
    const listed = await request.get('/api/v1/notification/getNotifications/offset/0/limit/100', {
        headers: authHeaders()
    });
    expect(listed.ok()).toBeTruthy();

    return (await listed.json())?.entity?.notifications ?? [];
}

/**
 * The ids of the notifications that exist now, so a test can later tell its own from older ones.
 *
 * Taken instead of clearing them: tests run in parallel as the same user, and clearing deletes
 * another test's notification before it has been checked.
 */
export async function listNotificationIds(request: APIRequestContext): Promise<string[]> {
    return (await listNotifications(request)).map((notification) => notification.id);
}

/**
 * Waits for a notification with the given text that did not exist when `knownIds` was taken.
 *
 * @param request the API context
 * @param knownIds the ids {@link listNotificationIds} returned before the test acted
 * @param text text the new notification's message contains
 */
export async function expectNewNotification(
    request: APIRequestContext,
    knownIds: string[],
    text: string
): Promise<void> {
    await expect
        .poll(
            async () =>
                (await listNotifications(request)).some(
                    (notification) =>
                        !knownIds.includes(notification.id) &&
                        !!notification.message?.includes(text)
                ),
            { timeout: 60000 }
        )
        .toBe(true);
}
