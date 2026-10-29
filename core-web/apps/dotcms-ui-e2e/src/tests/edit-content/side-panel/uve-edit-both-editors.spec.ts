import { UveEditorPage } from '@pages';
import { expect, test } from '@playwright/test';
import { createContentlet, lockContentlet } from '@requests/contentlets';
import { type ContentType, createFakeContentType, deleteContentType } from '@requests/contentType';
import { createFakePayloadTextField } from '@utils/dot-content-types.mock';
import { uniqueSuffix } from '@utils/utils';

import { SidePanel } from './helpers/side-panel';
import { createUvePage, removeUvePage, type UvePage, uvePagePath } from './helpers/uve-page';

/**
 * #38303, US1: the UVE's pencil (the contentlet toolbar's Full Editor button) opens content in the
 * side panel, in whichever editor its type uses, and the page shows a save while the panel is still
 * open. The iframe dialog the UVE used before (`dot-edit-ema-dialog`) is gone.
 *
 * One page holds a contentlet of each kind, as an author would have it; each test edits one.
 */

// The contentlet toolbar shows the pencil in a row only on contentlets 600px or wider.
test.use({ viewport: { width: 1920, height: 1080 } });

const TITLE_FIELD = 'title';

test.describe('UVE pencil opens content in the side panel', () => {
    let createdTypes: ContentType[] = [];
    let uvePage: UvePage | null = null;
    let pagePath: string;
    let newEditorTitle: string;
    let legacyEditorTitle: string;

    test.beforeEach(async ({ request }) => {
        const suffix = uniqueSuffix();
        const fields = [
            createFakePayloadTextField({ name: 'Title', variable: TITLE_FIELD, sortOrder: 1 })
        ];

        const newEditorType = await createFakeContentType(request, {
            name: `E2ESidePanelNew${suffix}`,
            metadata: { CONTENT_EDITOR2_ENABLED: true },
            fields
        });
        createdTypes.push(newEditorType);

        const legacyEditorType = await createFakeContentType(request, {
            name: `E2ESidePanelLegacy${suffix}`,
            metadata: { CONTENT_EDITOR2_ENABLED: false },
            fields
        });
        createdTypes.push(legacyEditorType);

        newEditorTitle = `New editor content ${suffix}`;
        legacyEditorTitle = `Legacy editor content ${suffix}`;

        const newEditorContent = await createContentlet(request, {
            contentType: newEditorType.variable,
            [TITLE_FIELD]: newEditorTitle
        });
        const legacyEditorContent = await createContentlet(request, {
            contentType: legacyEditorType.variable,
            [TITLE_FIELD]: legacyEditorTitle
        });
        // The legacy editor opens content read-only unless the user holds its lock.
        await lockContentlet(request, legacyEditorContent.identifier);

        uvePage = await createUvePage(request, suffix, [
            newEditorContent.identifier,
            legacyEditorContent.identifier
        ]);
        pagePath = uvePagePath(uvePage);
    });

    test.afterEach(async ({ request }) => {
        if (uvePage) {
            await removeUvePage(request, uvePage);
            uvePage = null;
        }

        // Deleting a content type deletes its contentlets.
        for (const type of createdTypes) {
            await deleteContentType(request, type.id);
        }
        createdTypes = [];
    });

    test('edits new-editor content in the side panel and the page shows the save', async ({
        page
    }) => {
        const uve = new UveEditorPage(page);
        const sidePanel = new SidePanel(page);
        const editedTitle = `${newEditorTitle} edited`;

        await uve.open(pagePath);
        await uve.openFullEditor(newEditorTitle);

        await sidePanel.expectOpen();
        await expect(page.locator('dot-edit-ema-dialog')).toHaveCount(0);

        await sidePanel.form.fillTextField(editedTitle);
        await sidePanel.form.save();

        await expect(uve.contentletHeading(editedTitle)).toBeVisible({ timeout: 30000 });
        await expect(sidePanel.front).toBeVisible();
    });

    test('edits legacy-editor content in the side panel and the page shows the save @critical', async ({
        page
    }) => {
        const uve = new UveEditorPage(page);
        const sidePanel = new SidePanel(page);
        const editedTitle = `${legacyEditorTitle} edited`;

        await uve.open(pagePath);
        await uve.openFullEditor(legacyEditorTitle);

        await sidePanel.expectOpen();
        await expect(page.locator('dot-edit-ema-dialog')).toHaveCount(0);

        await sidePanel.saveLegacyTextField(TITLE_FIELD, editedTitle);

        await expect(uve.contentletHeading(editedTitle)).toBeVisible({ timeout: 30000 });
        await expect(sidePanel.front).toBeVisible();
    });
});
