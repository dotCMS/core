import { NewEditContentFormPage } from '@pages';

import { HostFolderField } from './helpers/host-folder-field';

import { test } from '../../../../fixtures/host-folder.fixture';

/**
 * Journey 1: Default Host/Folder Selection (No Folder Context)
 */
test.describe('Default Host/Folder Selection', () => {
    test.describe.configure({ mode: 'serial' });

    let contentTypeVariable: string;

    test.beforeEach(async ({ apiHelpers, testSuffix }) => {
        const contentType = await apiHelpers.createContentType(
            apiHelpers.hostFolderPayload(testSuffix)
        );
        contentTypeVariable = contentType.variable;
    });

    test('default field shows a site selection for new content @critical', async ({
        adminPage
    }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToNew(contentTypeVariable);

        const field = new HostFolderField(adminPage);
        await field.expectVisible();
        await field.expectLabelMatchesPattern(/.+\..+/);
    });

    test('selects site root through the popover and updates the trigger label @critical', async ({
        adminPage,
        apiHelpers
    }) => {
        const defaultSite = await apiHelpers.getDefaultSite();
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToNew(contentTypeVariable);

        const field = new HostFolderField(adminPage);
        await field.selectSiteRoot(defaultSite.hostname);

        await field.expectLabelContains(defaultSite.hostname);
    });

    test('selects a folder through the popover and updates the trigger label @critical', async ({
        adminPage,
        apiHelpers,
        testSuffix
    }) => {
        const defaultSite = await apiHelpers.getDefaultSite();
        const folderName = `popover-folder-${testSuffix}`;
        await apiHelpers.createFolders(defaultSite.hostname, [`/${folderName}`]);

        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToNew(contentTypeVariable);

        const field = new HostFolderField(adminPage);
        await field.openOverlay();
        await field.selectSite(defaultSite.hostname);
        const partial = folderName.substring(0, Math.min(6, folderName.length));
        await field.searchFolders(partial);
        await field.selectFolderFlow(folderName);

        await field.expectLabelContains(folderName);
    });
});
