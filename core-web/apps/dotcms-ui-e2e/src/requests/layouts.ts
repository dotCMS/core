import { APIRequestContext, expect } from '@playwright/test';
import { admin1 } from '@utils/credentials';
import { generateBase64Credentials } from '@utils/generateBase64Credential';

/**
 * A navigation section ("layout") as `GET /api/v1/layouts` returns it. `tabOrder` is only present
 * on this admin endpoint; `GET /api/v1/menu` sorts by it server-side and does not send it.
 */
export interface Layout {
    icon: string;
    id: string;
    name: string;
    portletIds: string[];
    portletTitles: string[];
    tabOrder: number;
}

const adminHeaders = () => ({
    Authorization: generateBase64Credentials(admin1.username, admin1.password),
    'Content-Type': 'application/json'
});

/**
 * Lists every navigation section with the tools it holds.
 *
 *   GET /api/v1/layouts -> 200, entity: Layout[]
 */
export async function getLayouts(request: APIRequestContext): Promise<Layout[]> {
    const response = await request.get('/api/v1/layouts', { headers: adminHeaders() });
    expect(response.status()).toBe(200);
    const { entity } = await response.json();

    return entity as Layout[];
}

/**
 * Replaces the full, ordered list of tools of one section. The backend pushes the system event
 * `UPDATE_PORTLET_LAYOUTS` when it succeeds, which is what makes an open admin session reload its
 * menu.
 *
 *   PUT /api/v1/layouts/{layoutId}/portlets   { "portletIds": [...] } -> 200 (CMS Administrator)
 */
export async function setLayoutTools(
    request: APIRequestContext,
    layoutId: string,
    portletIds: string[]
): Promise<void> {
    const response = await request.put(`/api/v1/layouts/${layoutId}/portlets`, {
        data: { portletIds },
        headers: adminHeaders()
    });
    expect(response.status()).toBe(200);
}

/**
 * Puts a section back to the tools it had before a test changed it. Call it from `afterEach`
 * with the `portletIds` read before the change, so a failing test never leaves the instance's
 * navigation altered for the next one.
 */
export async function restoreLayoutTools(
    request: APIRequestContext,
    layoutId: string,
    original: string[]
): Promise<void> {
    await setLayoutTools(request, layoutId, original);
}

/**
 * A group of the signed-in user's navigation as `GET /api/v1/menu` returns it. The groups come
 * already sorted by the user's `tabOrder`; the response carries no `tabOrder` field itself.
 */
export interface MenuGroup {
    id: string;
    name: string;
    tabName: string;
    tabIcon: string;
    url: string;
    menuItems: { id: string; label: string; url: string; angular: boolean; ajax: boolean }[];
}

/**
 * Reads the navigation menu the admin user sees, to compare it with what the page rendered. A
 * direct API read is used instead of the page's own response because the browser drops a response
 * body once the page navigates away.
 *
 *   GET /api/v1/menu -> 200, entity: MenuGroup[]
 */
export async function getMenu(request: APIRequestContext): Promise<MenuGroup[]> {
    const response = await request.get('/api/v1/menu', { headers: adminHeaders() });
    expect(response.status()).toBe(200);
    const { entity } = await response.json();

    return entity as MenuGroup[];
}

/**
 * The admin's menu group that holds `portletId`, so a test names a group by what it contains and
 * not by its label: the starters do not agree on labels (`Site` in a demo instance, `Site Manager`
 * in the empty starter, `Content Model` against `Schema`). When a portlet sits in several groups
 * the first one in on-screen order is returned.
 */
export async function getGroupHolding(
    request: APIRequestContext,
    portletId: string
): Promise<MenuGroup> {
    const group = (await getMenu(request)).find((candidate) =>
        candidate.menuItems.some((item) => item.id === portletId)
    );
    expect(group, `no menu group holds the ${portletId} portlet`).toBeDefined();

    return group as MenuGroup;
}
