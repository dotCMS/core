import { APIRequestContext, expect } from '@playwright/test';
import { admin1 } from '@utils/credentials';
import { generateBase64Credentials } from '@utils/generateBase64Credential';

function authHeaders() {
    return {
        Authorization: generateBase64Credentials(admin1.username, admin1.password),
        'Content-Type': 'application/json'
    };
}

/**
 * Dismisses every notification this user currently has.
 *
 * Test setup, not teardown. The notification bell is durable and per user, so a run inherits every
 * notification the last one left: an assertion that "a notification arrived" passes against a panel
 * full of yesterday's, and there is nothing in a bulk upload's notification text — counts only, no
 * file names — that could tell them apart. Clearing first is what makes the assertion mean
 * something.
 */
export async function clearNotifications(request: APIRequestContext): Promise<void> {
    const listed = await request.get('/api/v1/notification/getNotifications/offset/0/limit/100', {
        headers: authHeaders()
    });
    expect(listed.ok()).toBeTruthy();

    const ids: string[] = ((await listed.json())?.entity?.notifications ?? []).map(
        (notification: { id: string }) => notification.id
    );

    if (!ids.length) {
        return;
    }

    await request.put('/api/v1/notification/delete', {
        data: { items: ids },
        headers: authHeaders()
    });
}
