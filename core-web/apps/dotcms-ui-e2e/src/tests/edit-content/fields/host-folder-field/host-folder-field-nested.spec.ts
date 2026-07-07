import { NewEditContentFormPage } from '@pages';

import { HostFolderField } from './helpers/host-folder-field';

import { expect, test } from '../../../../fixtures/host-folder.fixture';

/**
 * Journey 3: Nested Folder Context Pre-fill
 */
test.describe('Nested Folder Pre-fill', () => {
    test.describe.configure({ mode: 'serial' });

    let contentTypeVariable: string;
    let siteName: string;
    let folder1Name: string;
    let folder2Name: string;

    test.beforeEach(async ({ apiHelpers, testSuffix }) => {
        const contentType = await apiHelpers.createContentType(
            apiHelpers.hostFolderPayload(testSuffix)
        );
        contentTypeVariable = contentType.variable;

        const defaultSite = await apiHelpers.getDefaultSite();
        siteName = defaultSite.hostname;
        folder1Name = `folder-1-${testSuffix}`;
        folder2Name = `folder-2-${testSuffix}`;

        await apiHelpers.createFolders(siteName, [`/${folder1Name}/${folder2Name}`]);
    });

    test('nested folderPath pre-fills with full depth @critical', async ({ adminPage }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        const folderPath = `${siteName}/${folder1Name}/${folder2Name}/`;
        await formPage.goToNewWithFolderPath(contentTypeVariable, folderPath);

        const field = new HostFolderField(adminPage);
        await field.expectLabelContains(folder1Name);
        await field.expectLabelContains(folder2Name);
    });

    test('save nested folder selection and verify persistence @critical', async ({
        adminPage,
        testSuffix
    }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        const folderPath = `${siteName}/${folder1Name}/${folder2Name}/`;
        await formPage.goToNewWithFolderPath(contentTypeVariable, folderPath);

        const field = new HostFolderField(adminPage);

        await field.expectLabelContains(folder2Name);
        await formPage.fillTextField(`Title Nested ${testSuffix}`);

        const responsePromise = adminPage.waitForResponse(
            (r) => r.url().includes('/api/v1/workflow/actions/') && r.status() === 200
        );
        await adminPage.getByRole('button', { name: 'Save' }).click();
        await responsePromise;

        await adminPage.waitForURL(/\/content\/([a-f0-9-]+)/);
        const url = adminPage.url();
        const [, savedContentIdentifier] = url.match(/\/content\/([a-f0-9-]+)/) as RegExpMatchArray;
        expect(savedContentIdentifier).toBeTruthy();

        await formPage.goToContent(savedContentIdentifier);
        await field.expectLabelContains(folder2Name);
    });

    test('expanding tree shows the nested folder hierarchy @smoke', async ({ adminPage }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        const folderPath = `${siteName}/${folder1Name}/${folder2Name}/`;
        await formPage.goToNewWithFolderPath(contentTypeVariable, folderPath);

        const field = new HostFolderField(adminPage);
        await field.expectLabelContains(folder2Name);

        await field.openOverlay();
        await field.expectTreeNodeVisible(folder1Name);
        await field.expectTreeNodeVisible(folder2Name);
        await field.expectTreeNodeSelected(folder2Name);
    });

    test('invalid nested folderPath falls back gracefully', async ({ adminPage }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToNewWithFolderPath(
            contentTypeVariable,
            `${siteName}/nonexistent-folder/`
        );

        const field = new HostFolderField(adminPage);
        await field.expectFormFunctional();

        const labelText = await field.getLabelText();
        expect(labelText).toBeTruthy();
        expect(labelText).not.toBe('');
    });
});
