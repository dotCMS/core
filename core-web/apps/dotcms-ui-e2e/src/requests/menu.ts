import { APIRequestContext, expect } from '@playwright/test';
import { admin1 } from '@utils/credentials';
import { generateBase64Credentials } from '@utils/generateBase64Credential';

interface MenuItem {
    id: string;
    url: string;
}

interface MenuLayout {
    menuItems: MenuItem[];
}

/**
 * Returns the id of the layout that holds a portlet, taken from the portlet's menu url
 * (`/c/portal/layout?p_l_id=<layout id>&p_p_id=<portlet id>...`).
 *
 * The portlet is found by its id, not by the name of its menu, which is a label that can change.
 *
 * @param request - Playwright APIRequestContext
 * @param portletId - Id of a portlet that opens through the legacy portal layout (not an Angular route)
 */
export async function getLayoutIdForPortlet(
    request: APIRequestContext,
    portletId: string
): Promise<string> {
    const response = await request.get('/api/v1/menu', {
        headers: {
            Authorization: generateBase64Credentials(admin1.username, admin1.password)
        }
    });

    expect(response.status()).toBe(200);

    const { entity } = (await response.json()) as { entity: MenuLayout[] };
    const url = entity
        .flatMap((layout) => layout.menuItems)
        .find((item) => item.id === portletId)?.url;

    const layoutId = url?.match(/p_l_id=([^&]+)/)?.[1];

    expect(layoutId, `no layout url found for portlet "${portletId}"`).toBeTruthy();

    return layoutId as string;
}
