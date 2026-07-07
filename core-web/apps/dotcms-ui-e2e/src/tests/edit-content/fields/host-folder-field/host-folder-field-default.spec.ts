import { NewEditContentFormPage } from '@pages';

import { HostFolderField } from './helpers/host-folder-field';

import { expect, test } from '../../../../fixtures/host-folder.fixture';

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

    test('select a site, save, and verify persistence @critical', async ({
        adminPage,
        testSuffix
    }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToNew(contentTypeVariable);

        const field = new HostFolderField(adminPage);
        await formPage.fillTextField(`Title Default ${testSuffix}`);

        const selectedSiteName = await field.selectSiteRoot();

        await field.expectLabelContains(selectedSiteName);

        const responsePromise = adminPage.waitForResponse(
            (r) => r.url().includes('/api/v1/workflow/actions/') && r.status() === 200
        );
        await adminPage.getByRole('button', { name: 'Save' }).click();
        await responsePromise;

        await adminPage.waitForURL(/\/content\/([a-f0-9-]+)/);
        const url = adminPage.url();
        const [, savedContentIdentifier] = url.match(/\/content\/([a-f0-9-]+)/) as RegExpMatchArray;
        expect(savedContentIdentifier).toBeTruthy();

        await adminPage.goto(`/dotAdmin/#/content/${savedContentIdentifier}`);
        await adminPage.waitForLoadState('domcontentloaded');
        await adminPage.getByTestId('title').waitFor({ state: 'visible', timeout: 15000 });

        await field.expectLabelContains(selectedSiteName);
    });

    test('search filters folder list @smoke', async ({ adminPage, apiHelpers, testSuffix }) => {
        const currentSite = await apiHelpers.getCurrentSite();
        const folderName = `search-${testSuffix}`;
        await apiHelpers.createFolders(currentSite.hostname, [`/${folderName}`]);

        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToNew(contentTypeVariable);

        const field = new HostFolderField(adminPage);
        await field.openOverlay();
        await field.expectAtLeastOneFolderNode();

        const partial = folderName.substring(0, Math.min(6, folderName.length));
        await field.searchFolders(partial);

        await field.expectTreeNodeVisible(folderName);
    });

    test('System Host is hidden when field is required', async ({ adminPage }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToNew(contentTypeVariable);

        const field = new HostFolderField(adminPage);
        await field.openOverlay();
        await field.expectSiteNotVisible('System Host');
    });
});
