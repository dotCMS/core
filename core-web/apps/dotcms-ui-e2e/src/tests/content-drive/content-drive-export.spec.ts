import { ContentDrivePage } from '@pages';
import { createFakePayloadTextField } from '@utils/dot-content-types.mock';
import { Portlet } from '@utils/portlets';

import { expect, test } from '../../fixtures/content-drive.fixture';
import { createContentlet, deleteContentlets } from '../../requests/contentlets';
import { createFakeContentType, deleteContentType } from '../../requests/contentType';

/**
 * Journey: exporting the Content Drive listing as a CSV (#37900), filter mode.
 *
 * With nothing selected and the listing filtered to one content type, the toolbar offers Export,
 * and the browser receives a file named the way the Content Search export names it:
 * `<ContentType>_contents_<M_d_yyyy>.csv`.
 *
 * What only a real browser can prove is the last step: that the blob the page receives is handed
 * to the browser as a download under the server's file name. The rows and the headers are the
 * backend suite's (`ContentDriveExportFilterModeTest`) and the Postman "Export" folder's; the
 * button's visibility rules are the component spec's.
 */
test.describe('Content Drive export', () => {
    test('downloads the filtered type as <Type>_contents_<date>.csv @critical', async ({
        adminPage,
        request,
        testSuffix
    }) => {
        // Letters and digits only, so the sanitized type name in the file is this name unchanged.
        const typeName = `CdExport${testSuffix.replace(/[^a-zA-Z0-9]/g, '')}`;
        const contentType = await createFakeContentType(request, {
            name: typeName,
            variable: typeName,
            fields: [createFakePayloadTextField({ name: 'Title', variable: 'title', sortOrder: 1 })]
        });
        const contentletIds: string[] = [];

        try {
            for (const title of [`${typeName} first`, `${typeName} second`]) {
                const contentlet = await createContentlet(request, {
                    contentType: contentType.variable,
                    title
                });
                contentletIds.push(contentlet.identifier);
            }

            const drive = new ContentDrivePage(adminPage);

            // The address carries the filter, so the drive opens already narrowed to the one type.
            await adminPage.goto(
                `${Portlet.ContentDrive}?filters=${encodeURIComponent(
                    `contentType:${contentType.variable}`
                )}`
            );
            await expect(drive.toolbar).toBeVisible({ timeout: 20000 });
            await drive.expectListContainsTitle(`${typeName} first`);

            const exportButton = drive.toolbar.getByTestId('export-button').getByRole('button');
            await expect(exportButton).toBeEnabled({ timeout: 20000 });

            // Armed before the click, or a fast server finishes before anything is listening.
            const download = adminPage.waitForEvent('download', { timeout: 60000 });
            await exportButton.click();

            expect((await download).suggestedFilename()).toMatch(
                new RegExp(`^${typeName}_contents_\\d{1,2}_\\d{1,2}_\\d{4}\\.csv$`)
            );
        } finally {
            await deleteContentlets(request, contentletIds);
            await deleteContentType(request, contentType.id);
        }
    });
});
