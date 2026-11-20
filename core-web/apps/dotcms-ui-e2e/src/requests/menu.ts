import { APIRequestContext, expect } from '@playwright/test';
import { admin1 } from '@utils/credentials';
import { generateBase64Credentials } from '@utils/generateBase64Credential';

/** A tab of the admin menu (a layout) and the portlets it lists, as `GET /api/v1/menu` returns it. */
interface MenuTab {
    id: string;
    menuItems: { id: string }[];
}

function authHeaders() {
    return { Authorization: generateBase64Credentials(admin1.username, admin1.password) };
}

/**
 * Makes sure the admin user's menu lists a portlet, adding it to the first layout when no layout
 * does.
 *
 * A portlet declared in `portlet.xml` is not reachable until a layout lists it: the menu guard
 * rejects the route and the app goes to the first portlet instead. A portlet that ships without an
 * upgrade task, like the Angular Publishing Queue (`publishing-queue-beta`), is missing from a clean
 * instance's menu, so a spec that opens it adds it first. The portlet is left in the layout; it is
 * harmless there and found on later runs.
 *
 * @param request - Playwright APIRequestContext
 * @param portletId - The portlet's id, which is also the first segment of its route
 */
export async function ensurePortletInMenu(
    request: APIRequestContext,
    portletId: string
): Promise<void> {
    const menuResponse = await request.get('/api/v1/menu', { headers: authHeaders() });
    expect(menuResponse.status(), 'could not read the admin menu').toBe(200);

    const tabs: MenuTab[] = (await menuResponse.json()).entity ?? [];
    if (tabs.some((tab) => tab.menuItems.some((item) => item.id === portletId))) {
        return;
    }

    expect(tabs.length, 'the admin menu has no layout to add a portlet to').toBeGreaterThan(0);

    const added = await request.put(
        `/api/v1/portlet/custom/${portletId}/_addtolayout/${tabs[0].id}`,
        { headers: authHeaders() }
    );
    expect(added.status(), `failed to add ${portletId} to the menu: ${await added.text()}`).toBe(
        200
    );
}
