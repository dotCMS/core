import { HostFolderField } from './helpers/host-folder-field';

import { test } from '../../../../fixtures/host-folder.fixture';

test.describe('Folder Context Pre-fill — File Asset Content Type', () => {
    test.describe.configure({ mode: 'serial' });

    let contentTypeVariable: string;
    let siteName: string;
    let folder1Name: string;
    let folder2Name: string;

    test.beforeEach(async ({ apiHelpers, testSuffix }) => {
        const contentType = await apiHelpers.createContentType({
            clazz: 'com.dotcms.contenttype.model.type.ImmutableFileAssetContentType',
            name: `FileAssetTest${testSuffix}`
        });
        contentTypeVariable = contentType.variable;

        const defaultSite = await apiHelpers.getDefaultSite();
        siteName = defaultSite.hostname;
        folder1Name = `folder-1-${testSuffix}`;
        folder2Name = `folder-2-${testSuffix}`;

        await apiHelpers.createFolders(siteName, [`/${folder1Name}/${folder2Name}`]);
    });

    async function navigateToNewFileAsset(
        adminPage: import('@playwright/test').Page,
        contentType: string,
        folderPath: string
    ) {
        await adminPage.goto(`/dotAdmin/#/content/new/${contentType}?folderPath=${folderPath}`);
        await adminPage.waitForLoadState('domcontentloaded');
        await adminPage
            .getByTestId('field-hostFolder')
            .waitFor({ state: 'visible', timeout: 15000 });
    }

    test('folderPath query param pre-fills Host/Folder field for file asset type @critical', async ({
        adminPage
    }) => {
        await navigateToNewFileAsset(
            adminPage,
            contentTypeVariable,
            `${siteName}/${folder1Name}/${folder2Name}/`
        );

        const field = new HostFolderField(adminPage, 'hostFolder');
        await field.expectLabelContains(`${siteName}/${folder1Name}/${folder2Name}`);
    });

    test('shallow folderPath pre-fills single-level folder for file asset type @critical', async ({
        adminPage
    }) => {
        await navigateToNewFileAsset(adminPage, contentTypeVariable, `${siteName}/${folder1Name}/`);

        const field = new HostFolderField(adminPage, 'hostFolder');
        await field.expectLabelContains(`${siteName}/${folder1Name}`);
    });

    test('empty folderPath falls back to default site for file asset type', async ({
        adminPage
    }) => {
        await navigateToNewFileAsset(adminPage, contentTypeVariable, '');

        const field = new HostFolderField(adminPage, 'hostFolder');
        await field.expectLabelMatchesPattern(/^\/\/.+/);
        await field.expectFormFunctional();
    });
});
