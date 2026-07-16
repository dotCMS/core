import { NewEditContentFormPage } from '@pages';

import { HostFolderField } from './helpers/host-folder-field';

import { test } from '../../../../fixtures/host-folder.fixture';

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
});
