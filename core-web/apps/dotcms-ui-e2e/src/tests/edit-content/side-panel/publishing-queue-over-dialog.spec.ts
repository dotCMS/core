import { expect, test } from '@playwright/test';
import {
    createBundleWithAssets,
    deleteBundle,
    futureDate,
    pushBundle,
    resolveEnvironmentId
} from '@requests/bundles';
import { createContentlet } from '@requests/contentlets';
import { type ContentType, createFakeContentType, deleteContentType } from '@requests/contentType';
import { ensurePortletInMenu } from '@requests/menu';
import { Portlet } from '@utils/portlets';
import { uniqueSuffix } from '@utils/utils';

import { SidePanel } from './helpers/side-panel';

/**
 * #38303, US6: a contentlet in a Publishing Queue bundle opens from the bundle's asset list in the
 * side panel, above the asset-list dialog. Typing reaches the editor (the dialog no longer traps
 * focus), and Escape closes the panel first, leaving the dialog open on the same bundle.
 *
 * This is the Angular Publishing Queue (`publishing-queue-beta`), which lists pushed bundles; the
 * bundle is pushed with a date days ahead so it stays queued and is never sent.
 */

/** The Angular Publishing Queue's portlet id, also the first segment of its route. */
const PUBLISHING_QUEUE_PORTLET = 'publishing-queue-beta';

test.describe('Publishing Queue opens bundle content in the side panel over its dialog', () => {
    let contentType: ContentType | null = null;
    let bundleId: string | null = null;
    let bundleName: string;
    let title: string;

    test.beforeEach(async ({ request }) => {
        await ensurePortletInMenu(request, PUBLISHING_QUEUE_PORTLET);

        const suffix = uniqueSuffix();
        // createFakeContentType gives it a Title text field and the new editor.
        contentType = await createFakeContentType(request, {
            name: `E2ESidePanelBundle${suffix}`,
            metadata: { CONTENT_EDITOR2_ENABLED: true }
        });

        title = `Bundled content ${suffix}`;
        const contentlet = await createContentlet(request, {
            contentType: contentType.variable,
            title
        });

        bundleName = `e2e-38303-${suffix}`;
        bundleId = await createBundleWithAssets(request, bundleName, [contentlet.identifier]);
        await pushBundle(request, bundleId, futureDate(), await resolveEnvironmentId(request));
    });

    test.afterEach(async ({ request }) => {
        // The bundle first: deleting the content does not remove it or its queue row.
        if (bundleId) {
            await deleteBundle(request, bundleId);
            bundleId = null;
        }

        if (contentType) {
            await deleteContentType(request, contentType.id);
            contentType = null;
        }
    });

    test('opens bundle content above the asset list and closes back to it with Escape @critical', async ({
        page
    }) => {
        const sidePanel = new SidePanel(page);
        const assetListDialog = page.getByRole('dialog').filter({
            has: page.getByTestId('pq-asset-list-dialog')
        });
        // The header shortens long names; its `title` always carries the whole bundle name.
        const assetListTitle = assetListDialog.getByTestId('pq-asset-list-title');
        const typed = `${title} typed over the dialog`;

        await page.goto(Portlet.PublishingQueueBeta);
        await page.getByTestId('pq-search-input').fill(bundleName);
        const bundleRow = page.getByTestId('pq-bundles-row').filter({ hasText: bundleName });
        await bundleRow.getByTestId('pq-bundles-items-btn').click();
        await expect(assetListDialog).toBeVisible();
        await expect(assetListTitle).toHaveAttribute('title', bundleName);

        await assetListDialog.getByTestId('pq-asset-list-row').filter({ hasText: title }).click();
        await sidePanel.expectOpen();
        expect(await sidePanel.isOnTop(), 'the side panel is not above the dialog').toBe(true);

        // Keystrokes, not `fill()`: a focus trap left on the dialog would swallow them.
        const titleField = sidePanel.front.getByTestId('title');
        await titleField.fill('');
        await titleField.click();
        await page.keyboard.type(typed);
        await expect(titleField).toHaveValue(typed);

        // Saved so the form is pristine: with unsaved changes Escape asks before closing (US3).
        await sidePanel.form.save();

        await page.keyboard.press('Escape');
        await sidePanel.expectClosed();
        await expect(assetListDialog).toBeVisible();
        await expect(assetListTitle).toHaveAttribute('title', bundleName);
    });
});
