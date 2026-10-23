import { ContentDrivePage } from '@pages';
import { createFakePayloadTextField } from '@utils/dot-content-types.mock';

import { readFile } from 'node:fs/promises';

import { ImportDialog } from './helpers/content-drive-import';

import { expect, test } from '../../fixtures/content-drive.fixture';
import { findContentletsOfType } from '../../requests/contentlets';
import {
    createFakeContentType,
    deleteContentType,
    type ContentType
} from '../../requests/contentType';

/**
 * Journey: Content Drive CSV import (#37896)
 *
 * The unit specs prove the dialog sends the right form and reads the preview right. These prove the
 * parts only a browser and a running server can: that a real file chooser hands the CSV over, that
 * the server's preview reaches the screen, that the import runs as a background job and reports
 * back through the status toast, and that the sample download is a real file.
 *
 * Every test seeds its own content type, with a required Title so a row without one is an error,
 * and deletes it afterwards. The rows land in that type's default site and folder (System Host),
 * not in the folder on screen, so what was written is checked through the API, not the listing.
 */
// An import is a queued job, and the suite runs two workers against one instance.
test.describe.configure({ timeout: 300000 });

const PUBLISH = 'Publish ( System Workflow )';

/** A content type of its own for one test: a required Title and nothing else. */
const seedContentType = (request: Parameters<typeof createFakeContentType>[0], suffix: string) =>
    createFakeContentType(request, {
        name: `CsvImport ${suffix}`,
        variable: `CsvImport${suffix.replace(/-/g, '')}`,
        fields: [
            createFakePayloadTextField({
                name: 'Title',
                variable: 'title',
                sortOrder: 1,
                required: true
            })
        ]
    });

test.describe('Content Drive CSV import', () => {
    let contentType: ContentType | undefined;

    test.afterEach(async ({ request }) => {
        if (contentType) {
            await deleteContentType(request, contentType.id);
            contentType = undefined;
        }
    });

    test('imports a CSV from the Upload menu and reports what it created @critical', async ({
        adminPage,
        request,
        testSuffix
    }) => {
        contentType = await seedContentType(request, testSuffix);
        const fileName = `import-${testSuffix}.csv`;

        const drive = new ContentDrivePage(adminPage);
        await drive.goTo();
        const dialog = new ImportDialog(adminPage);
        await dialog.open();
        await dialog.pickContentType(contentType.name);
        await dialog.pickWorkflowAction(PUBLISH);
        await dialog.chooseCsv(fileName, `title\nFirst ${testSuffix}\nSecond ${testSuffix}\n`);

        await dialog.runPreview();

        // No errors, and nothing written yet: the preview is a dry run. Warnings are expected —
        // like Content Search, the importer always warns when no key field was chosen.
        await expect(dialog.willImport).toHaveText('2');
        await expect(dialog.errorsCount).toHaveCount(0);
        await expect(dialog.errorsBanner).toHaveCount(0);
        expect(await findContentletsOfType(request, contentType.variable)).toHaveLength(0);

        const variable = contentType.variable;
        await dialog.importAndConfirm();

        // The dialog hands the run to the status toast and gets out of the way.
        await expect(dialog.dialog).toBeHidden();
        await drive.expectOutcomeContaining('2 created, 0 updated, 0 failed');

        await expect
            .poll(
                async () =>
                    (await findContentletsOfType(request, variable))
                        .map((contentlet) => contentlet.title)
                        .sort(),
                { timeout: 60000 }
            )
            .toEqual([`First ${testSuffix}`, `Second ${testSuffix}`]);
    });

    test('previews warnings and errors by line before anything is written', async ({
        adminPage,
        request,
        testSuffix
    }) => {
        contentType = await seedContentType(request, testSuffix);
        const fileName = `errors-${testSuffix}.csv`;

        const drive = new ContentDrivePage(adminPage);
        await drive.goTo();
        const dialog = new ImportDialog(adminPage);
        await dialog.open();
        await dialog.pickContentType(contentType.name);
        await dialog.pickWorkflowAction(PUBLISH);
        // Line 3 has no Title, which the type requires; `color` is no field of the type.
        await dialog.chooseCsv(fileName, `title,color\nKept ${testSuffix},red\n,blue\n`);

        await dialog.runPreview();

        await expect(dialog.errorsBanner).toBeVisible();
        await expect(dialog.errorsCount).toHaveText('1');
        await expect(dialog.errorItems.first()).toContainText('Line 3');
        await expect(dialog.warningItems.filter({ hasText: 'color' })).toHaveCount(1);
        await expect(dialog.confirmButton).toContainText('Import Valid Rows');
        expect(await findContentletsOfType(request, contentType.variable)).toHaveLength(0);

        // Going back keeps what the author filled in, so they can fix the file and try again.
        await dialog.backButton.click();
        await expect(dialog.preview).toBeHidden();
        await expect(dialog.contentType).toContainText(contentType.name);
        await expect(dialog.workflowAction).toContainText(PUBLISH);
        await expect(dialog.fileName).toHaveText(fileName);
    });

    test('downloads the sample CSV of the picked content type', async ({
        adminPage,
        request,
        testSuffix
    }) => {
        contentType = await seedContentType(request, testSuffix);

        const drive = new ContentDrivePage(adminPage);
        await drive.goTo();
        const dialog = new ImportDialog(adminPage);
        await dialog.open();

        // The sample needs a type to know its columns, so it is only offered once one is picked.
        await expect(dialog.sampleButton).toHaveCount(0);
        await dialog.pickContentType(contentType.name);

        const download = await dialog.downloadSample();

        expect(download.suggestedFilename()).toBe('CSV_Template.csv');
        const path = await download.path();
        expect(path, 'the sample should have been saved').toBeTruthy();
        const content = await readFile(path as string, 'utf-8');
        expect(content.split('\n')[0]).toBe('title');
    });
});
