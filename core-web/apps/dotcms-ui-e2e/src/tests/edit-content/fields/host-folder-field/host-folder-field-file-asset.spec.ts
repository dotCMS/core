import { NewEditContentFormPage } from '@pages';

import { HostFolderField } from './helpers/host-folder-field';

import { test } from '../../../../fixtures/host-folder.fixture';

test.describe('Folder Context Pre-fill — File Asset Content Type', () => {
    test.describe.configure({ mode: 'serial' });

    let contentTypeId: string;
    let contentTypeVariable: string;
    let siteName: string;
    let folder1Name: string;
    let folder2Name: string;

    test.beforeEach(async ({ apiHelpers, testSuffix }) => {
        const contentType = await apiHelpers.createContentType({
            clazz: 'com.dotcms.contenttype.model.type.ImmutableFileAssetContentType',
            name: `FileAssetTest${testSuffix}`
        });
        contentTypeId = contentType.id;
        contentTypeVariable = contentType.variable;

        const defaultSite = await apiHelpers.getDefaultSite();
        siteName = defaultSite.hostname;
        folder1Name = `folder-1-${testSuffix}`;
        folder2Name = `folder-2-${testSuffix}`;

        await apiHelpers.createFolders(siteName, [`/${folder1Name}/${folder2Name}`]);
    });

    test.afterEach(async ({ apiHelpers }) => {
        await apiHelpers.deleteContentType(contentTypeId);
    });

    test('folderPath query param pre-fills Host/Folder field for file asset type @critical', async ({
        adminPage
    }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToNewFileAssetWithFolderPath(
            contentTypeVariable,
            `${siteName}/${folder1Name}/${folder2Name}/`
        );

        const field = new HostFolderField(adminPage, 'hostFolder');
        await field.expectLabelContains(`${siteName}/${folder1Name}/${folder2Name}`);
    });

    test('shallow folderPath pre-fills single-level folder for file asset type @critical', async ({
        adminPage
    }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToNewFileAssetWithFolderPath(
            contentTypeVariable,
            `${siteName}/${folder1Name}/`
        );

        const field = new HostFolderField(adminPage, 'hostFolder');
        await field.expectLabelContains(`${siteName}/${folder1Name}`);
    });

    test('empty folderPath falls back to default site for file asset type', async ({
        adminPage
    }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToNewFileAssetWithFolderPath(contentTypeVariable, '');

        const field = new HostFolderField(adminPage, 'hostFolder');
        await field.expectLabelMatchesPattern(/^\/\/.+/);
        await field.expectFormFunctional();
    });
});
