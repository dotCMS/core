import { expect } from '@playwright/test';
import { type ContentType, createFakeContentType, deleteContentType } from '@requests/contentType';

import { ContentDriveBasics } from './helpers/content-drive-basics';
import { type DriveSeed, removeSeed, seedContent, seedDrive } from './helpers/content-drive-seed';

import { test } from '../../fixtures/content-drive.fixture';

/**
 * Journey: narrowing Content Drive by content type. The chip's own panel is unit tested; this
 * checks that the query the server runs for the selection returns that type's rows and no other.
 */
test.describe('Content Drive filters', () => {
    let seed: DriveSeed | null = null;
    let otherType: ContentType | null = null;

    test.afterEach(async ({ request }) => {
        if (otherType) {
            await deleteContentType(request, otherType.id).catch(() => undefined);
            otherType = null;
        }
        await removeSeed(request, seed);
        seed = null;
    });

    test('lists only the content types chosen in the filter @critical', async ({
        adminPage,
        apiHelpers,
        request,
        testSuffix
    }) => {
        const site = await apiHelpers.getDefaultSite();
        seed = await seedDrive(request, site.hostname, 'e2e-filter', testSuffix);
        // A second type with the same fields, so its content files into the same folder.
        otherType = await createFakeContentType(request, {
            name: `E2E filter other ${testSuffix}`,
            fields: seed.contentType.fields.map(({ id: _id, ...field }) => field)
        });

        const kept = `Kept ${testSuffix}`;
        const filteredOut = `Filtered out ${testSuffix}`;
        await seedContent(request, seed, kept);
        await seedContent(request, { ...seed, contentType: otherType }, filteredOut);

        const basics = new ContentDriveBasics(adminPage);
        await basics.drive.goTo();
        await basics.drive.openFolder(seed.folderName);
        await basics.drive.expectListContainsTitle(kept);
        await basics.drive.expectListContainsTitle(filteredOut);

        await basics.chooseContentType(seed.contentType.name);

        await expect(basics.drive.row(filteredOut)).toHaveCount(0, { timeout: 20000 });
        await basics.drive.expectListContainsTitle(kept);
    });
});
