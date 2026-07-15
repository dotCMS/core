import { NewEditContentFormPage } from '@pages';

import { HostFolderField } from './helpers/host-folder-field';

import { expect, test } from '../../../../fixtures/host-folder.fixture';

/**
 * Journey 2: Folder Context Pre-fill from Query Params (Issue #34588 Regression)
 */
test.describe('Folder Context Pre-fill', () => {
    test.describe.configure({ mode: 'serial' });

    let contentTypeVariable: string;
    let siteName: string;
    let folderName: string;
    let alternateFolderName: string;

    test.beforeEach(async ({ apiHelpers, testSuffix }) => {
        const contentType = await apiHelpers.createContentType(
            apiHelpers.hostFolderPayload(testSuffix)
        );
        contentTypeVariable = contentType.variable;

        const defaultSite = await apiHelpers.getDefaultSite();
        siteName = defaultSite.hostname;
        folderName = `folder-1-${testSuffix}`;
        alternateFolderName = `folder-2-${testSuffix}`;

        await apiHelpers.createFolders(siteName, [`/${folderName}`, `/${alternateFolderName}`]);
    });

    test('folderPath query param pre-fills the Host/Folder field @critical', async ({
        adminPage
    }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToNewWithFolderPath(contentTypeVariable, `${siteName}/${folderName}/`);

        const field = new HostFolderField(adminPage);
        await field.expectLabelContains(siteName);
        await field.expectLabelContains(folderName);
    });

    test('save content with pre-filled folder and verify persistence @critical', async ({
        adminPage,
        testSuffix
    }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToNewWithFolderPath(contentTypeVariable, `${siteName}/${folderName}/`);

        const field = new HostFolderField(adminPage);

        await field.expectLabelContains(folderName);
        await formPage.fillTextField(`Title Prefill ${testSuffix}`);

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
        await field.expectLabelContains(folderName);
    });

    test('user can override the pre-filled folder value @smoke', async ({ adminPage }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToNewWithFolderPath(contentTypeVariable, `${siteName}/${folderName}/`);

        const field = new HostFolderField(adminPage);
        await field.expectLabelContains(folderName);

        await field.openOverlay();
        await field.searchFolders(alternateFolderName);
        await field.selectFolderFlow(alternateFolderName);

        await field.expectLabelContains(alternateFolderName);
        await expect(field.label).not.toContainText(folderName, { ignoreCase: true });
    });

    test('empty folderPath query param falls back to default', async ({
        adminPage,
        apiHelpers
    }) => {
        const currentSite = await apiHelpers.getCurrentSite();
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToNewWithFolderPath(contentTypeVariable, '');

        const field = new HostFolderField(adminPage);
        await field.expectLabelContains(currentSite.hostname);
        await field.expectFormFunctional();
    });
});
