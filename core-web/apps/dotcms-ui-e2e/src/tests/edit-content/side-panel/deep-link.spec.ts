import { iFramesLocators } from '@locators/globalLocators';
import { expect, test } from '@playwright/test';
import { createContentlet } from '@requests/contentlets';
import { type ContentType, createFakeContentType, deleteContentType } from '@requests/contentType';
import { Portlet } from '@utils/portlets';
import { uniqueSuffix } from '@utils/utils';

import { SidePanel } from './helpers/side-panel';

/**
 * #38303, US2: a content link opened on its own (`#/c/content/<inode>`, `#/c/content/new/<type>`)
 * renders the content portlet with the side panel open over it, and closing the panel leaves the
 * author on the portlet, at its own address.
 */

/** Where closing the panel leaves the author: the content portlet's own address. */
const CONTENT_PORTLET_URL = /\/dotAdmin\/#\/c\/content$/;

test.describe('Content links open the side panel over the content portlet', () => {
    let contentType: ContentType | null = null;
    let typeName: string;
    let typeVariable: string;
    let inode: string;
    let title: string;

    test.beforeEach(async ({ request }) => {
        const suffix = uniqueSuffix();
        // createFakeContentType gives it a Title text field and the new editor.
        contentType = await createFakeContentType(request, {
            name: `E2ESidePanelLink${suffix}`,
            metadata: { CONTENT_EDITOR2_ENABLED: true }
        });
        typeName = contentType.name;
        typeVariable = contentType.variable;

        title = `Linked content ${suffix}`;
        const contentlet = await createContentlet(request, {
            contentType: contentType.variable,
            title
        });
        inode = contentlet.inode;
    });

    test.afterEach(async ({ request }) => {
        // Deleting the content type deletes its contentlet.
        if (contentType) {
            await deleteContentType(request, contentType.id);
            contentType = null;
        }
    });

    test('opens content from its link over the portlet and returns to the portlet on close @critical', async ({
        page
    }) => {
        const sidePanel = new SidePanel(page);

        await page.goto(`${Portlet.Content}/${inode}`);

        await expect(page.locator(iFramesLocators.main_iframe)).toBeVisible({ timeout: 20000 });
        await sidePanel.expectOpen();
        await expect(sidePanel.title).toContainText(title);

        await sidePanel.close();
        await sidePanel.expectClosed();
        await expect(page).toHaveURL(CONTENT_PORTLET_URL);
    });

    test('opens a create form from its link over the portlet and returns to the portlet on close', async ({
        page
    }) => {
        const sidePanel = new SidePanel(page);

        await page.goto(`${Portlet.Content}/new/${typeVariable}`);

        await expect(page.locator(iFramesLocators.main_iframe)).toBeVisible({ timeout: 20000 });
        await sidePanel.expectOpen();
        // A create has no title yet, so the panel is headed with the type's name.
        await expect(sidePanel.title).toContainText(typeName);
        await expect(sidePanel.front.getByTestId('title')).toHaveValue('');

        await sidePanel.close();
        await sidePanel.expectClosed();
        await expect(page).toHaveURL(CONTENT_PORTLET_URL);
    });
});
