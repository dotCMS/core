import { PagesListPage } from '@pages';
import { expect, type Locator, type Page, test } from '@playwright/test';
import { uniqueSuffix } from '@utils/utils';

import { SidePanel } from './helpers/side-panel';
import { createUvePage, removeUvePage, type UvePage } from './helpers/uve-page';

/**
 * #38303, T173: the Pages list shows an edit made in the side panel as soon as it is saved,
 * whichever menu opened the panel. The three-dots menu passed a refresh callback; the right-click
 * menu asked for the same menu with none, so an edit from it never refetched the listing.
 */

/** The menus a page row opens its actions from, and how to open each one. */
const MENUS: [string, (page: Page, row: Locator) => Promise<Locator>][] = [
    [
        'the right-click menu',
        async (page, row) => {
            await row.click({ button: 'right' });

            return page.locator('.p-contextmenu');
        }
    ],
    [
        'the three-dots menu',
        async (page, row) => {
            await row.getByTestId('row-action-button').click();

            return page.locator('.p-tieredmenu');
        }
    ]
];

test.describe('The Pages list after an edit in the side panel', () => {
    let uvePage: UvePage | null = null;
    let title: string;

    test.beforeEach(async ({ request }) => {
        uvePage = await createUvePage(request, uniqueSuffix(), { contentTypeIds: [] });
        title = uvePage.page.title;
    });

    test.afterEach(async ({ request }) => {
        if (uvePage) {
            await removeUvePage(request, uvePage);
            uvePage = null;
        }
    });

    /**
     * Saves a new title for the page open in the panel, in whichever editor the page type uses: the
     * new editor's form, or the legacy editor in its iframe. A page's title input carries no
     * `data-testid="title"` of its own (AGENTS.md), so it is found through its field's wrapper.
     */
    const saveTitle = async (sidePanel: SidePanel, newTitle: string): Promise<void> => {
        const newEditorTitle = sidePanel.front.getByTestId('field-title').getByRole('textbox');
        const legacyEditor = sidePanel.front.locator('iframe');
        await expect(newEditorTitle.or(legacyEditor)).toBeVisible({ timeout: 20000 });

        if (await newEditorTitle.isVisible()) {
            await newEditorTitle.fill(newTitle);
            await sidePanel.form.save();
        } else {
            await sidePanel.saveLegacyTextField('title', newTitle);
        }
    };

    for (const [label, openMenu] of MENUS) {
        test(`shows the new title after a save from ${label} @critical`, async ({ page }) => {
            const pagesList = new PagesListPage(page);
            const sidePanel = new SidePanel(page);
            const newTitle = `${title} renamed`;

            await pagesList.navigateTo();
            const row = pagesList.getRowByTitle(title);
            await expect(row).toBeVisible({ timeout: 20000 });

            const menu = await openMenu(page, row);
            await menu.getByRole('menuitem', { name: 'Edit', exact: true }).click();
            await sidePanel.expectOpen();

            await saveTitle(sidePanel, newTitle);

            // The panel stays open over the list; the listing behind it has refetched.
            await expect(pagesList.getRowByTitle(newTitle)).toBeVisible({ timeout: 15000 });
            await expect(sidePanel.front).toBeVisible();
        });
    }
});
