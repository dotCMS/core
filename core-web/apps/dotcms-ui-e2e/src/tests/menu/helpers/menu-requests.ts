import { Page, Request } from '@playwright/test';

const MENU_PATH = '/api/v1/menu';

/** True for the navigation menu request: a `GET` whose path ends with `/api/v1/menu`. */
function isMenuRequest(request: Request): boolean {
    if (request.method() !== 'GET') {
        return false;
    }

    return new URL(request.url()).pathname.endsWith(MENU_PATH);
}

export interface MenuRequestTracker {
    /** Number of menu requests seen since the tracker was created or last reset. */
    count(): number;
    /** Starts counting again from zero (call it after the load you want to assert on). */
    reset(): void;
}

/**
 * Counts `GET /api/v1/menu` requests the page sends. Attach it **before** navigating so the load
 * itself is counted, then `reset()` once the load has settled to assert on later traffic.
 */
export function trackMenuRequests(page: Page): MenuRequestTracker {
    let seen = 0;
    page.on('request', (request) => {
        if (isMenuRequest(request)) {
            seen += 1;
        }
    });

    return {
        count: () => seen,
        reset: () => {
            seen = 0;
        }
    };
}

/** Makes every menu request fail at the network level until {@link restoreMenuRequests} runs. */
export async function failMenuRequests(page: Page): Promise<void> {
    await page.route(`**${MENU_PATH}`, (route) => route.abort());
}

/**
 * Serves this page a menu without the given portlets, as if the user's layouts did not hold them.
 * Only this page's menu changes, so nothing is altered on the instance and tests running in
 * parallel do not see it. Groups left empty are dropped, as the backend does.
 */
export async function hidePortletsFromMenu(page: Page, portletIds: string[]): Promise<void> {
    await page.route(`**${MENU_PATH}`, async (route) => {
        const response = await route.fetch();
        const body = await response.json();
        const entity = (body.entity as { menuItems: { id: string }[] }[])
            .map((group) => ({
                ...group,
                menuItems: group.menuItems.filter((item) => !portletIds.includes(item.id))
            }))
            .filter((group) => group.menuItems.length > 0);

        await route.fulfill({ response, json: { ...body, entity } });
    });
}

/** Lets menu requests through again after {@link failMenuRequests}. */
export async function restoreMenuRequests(page: Page): Promise<void> {
    await page.unroute(`**${MENU_PATH}`);
}
