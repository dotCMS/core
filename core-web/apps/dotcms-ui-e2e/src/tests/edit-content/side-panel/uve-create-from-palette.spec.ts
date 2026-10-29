import { UveEditorPage } from '@pages';
import { expect, test } from '@playwright/test';
import { type ContentType, createFakeContentType, deleteContentType } from '@requests/contentType';
import { uniqueSuffix } from '@utils/utils';

import { SidePanel } from './helpers/side-panel';
import { createUvePage, removeUvePage, type UvePage, uvePagePath } from './helpers/uve-page';

/**
 * #38303, US1: content created from the UVE palette lands on the page once. The drop opens a create
 * form in the side panel; the first save adds the new contentlet to the container, and saving it
 * again only updates it. Before the move, a second save added it a second time or raised the
 * "Content Already Added" toast.
 */

// The palette and a container wide enough to drop on, side by side.
test.use({ viewport: { width: 1920, height: 1080 } });

/** The UVE's toast when content is added to a container that already holds it. */
const DUPLICATE_TOAST = 'Content Already Added';

test.describe('UVE palette creates content in the side panel', () => {
    let contentType: ContentType | null = null;
    let uvePage: UvePage | null = null;
    let pagePath: string;
    let contentTypeName: string;
    let suffix: string;

    test.beforeEach(async ({ request }) => {
        suffix = uniqueSuffix();
        contentTypeName = `E2ESidePanelPalette${suffix}`;

        // createFakeContentType gives it a Title text field and the new editor.
        contentType = await createFakeContentType(request, {
            name: contentTypeName,
            metadata: { CONTENT_EDITOR2_ENABLED: true }
        });

        uvePage = await createUvePage(request, suffix);
        pagePath = uvePagePath(uvePage);
    });

    test.afterEach(async ({ request }) => {
        if (uvePage) {
            await removeUvePage(request, uvePage);
            uvePage = null;
        }

        // Deleting the content type deletes the contentlet the test created.
        if (contentType) {
            await deleteContentType(request, contentType.id);
            contentType = null;
        }
    });

    test('adds content from the palette once however often it is saved @critical', async ({
        page
    }) => {
        const uve = new UveEditorPage(page);
        const sidePanel = new SidePanel(page);
        const firstTitle = `Palette content ${suffix}`;
        const secondTitle = `${firstTitle} saved again`;

        await uve.open(pagePath);
        await uve.dragContentTypeToEmptyContainer(contentTypeName);

        await sidePanel.expectOpen();
        await sidePanel.form.fillTextField(firstTitle);
        await sidePanel.form.save();
        await expect(uve.contentletHeading(firstTitle)).toBeVisible({ timeout: 30000 });

        await sidePanel.form.fillTextField(secondTitle);
        await sidePanel.form.save();

        // The same contentlet, updated: one heading with the new title, none left with the old.
        await expect(uve.contentletHeading(secondTitle)).toHaveCount(1, { timeout: 30000 });
        await expect(uve.contentletHeading(firstTitle)).toHaveCount(0);
        await expect(page.getByText(DUPLICATE_TOAST)).toHaveCount(0);
        await expect(sidePanel.front).toBeVisible();
    });
});
