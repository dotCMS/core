import { NewEditContentFormPage } from '@pages';

import { HostFolderField } from './helpers/host-folder-field';

import { expect, test } from '../../../../fixtures/host-folder.fixture';

/**
 * Journey 2: Folder Context Pre-fill from Query Params (Issue #34588 Regression)
 */
test.describe('Folder Context Pre-fill', () => {
    test('folderPath query param pre-fills the Host/Folder field @critical', async ({
        adminPage,
        apiHelpers,
        testSuffix
    }) => {
        const contentType = await apiHelpers.createContentType(
            apiHelpers.hostFolderPayload(testSuffix)
        );
        try {
            const defaultSite = await apiHelpers.getDefaultSite();
            const siteName = defaultSite.hostname;
            const folderName = `folder-1-${testSuffix}`;
            await apiHelpers.createFolders(siteName, [`/${folderName}`]);

            const formPage = new NewEditContentFormPage(adminPage);
            await formPage.goToNewWithFolderPath(
                contentType.variable,
                `${siteName}/${folderName}/`
            );

            const field = new HostFolderField(adminPage);
            await field.expectLabelContains(siteName);
            await field.expectLabelContains(folderName);
        } finally {
            await apiHelpers.deleteContentType(contentType.id);
        }
    });

    test('save content with pre-filled folder and verify persistence @critical', async ({
        adminPage,
        apiHelpers,
        testSuffix
    }) => {
        const contentType = await apiHelpers.createContentType(
            apiHelpers.hostFolderPayload(testSuffix)
        );
        try {
            const defaultSite = await apiHelpers.getDefaultSite();
            const siteName = defaultSite.hostname;
            const folderName = `folder-1-${testSuffix}`;
            await apiHelpers.createFolders(siteName, [`/${folderName}`]);

            const formPage = new NewEditContentFormPage(adminPage);
            await formPage.goToNewWithFolderPath(
                contentType.variable,
                `${siteName}/${folderName}/`
            );

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
            const [, savedContentIdentifier] = url.match(
                /\/content\/([a-f0-9-]+)/
            ) as RegExpMatchArray;
            expect(savedContentIdentifier).toBeTruthy();

            await formPage.goToContent(savedContentIdentifier);
            await field.expectLabelContains(folderName);
        } finally {
            await apiHelpers.deleteContentType(contentType.id);
        }
    });

    test('user can override the pre-filled folder value @smoke', async ({
        adminPage,
        apiHelpers,
        testSuffix
    }) => {
        const contentType = await apiHelpers.createContentType(
            apiHelpers.hostFolderPayload(testSuffix)
        );
        try {
            const defaultSite = await apiHelpers.getDefaultSite();
            const siteName = defaultSite.hostname;
            const folderName = `folder-1-${testSuffix}`;
            const alternateFolderName = `folder-2-${testSuffix}`;
            await apiHelpers.createFolders(siteName, [`/${folderName}`, `/${alternateFolderName}`]);

            const formPage = new NewEditContentFormPage(adminPage);
            await formPage.goToNewWithFolderPath(
                contentType.variable,
                `${siteName}/${folderName}/`
            );

            const field = new HostFolderField(adminPage);
            await field.expectLabelContains(folderName);

            await field.openOverlay();
            await field.searchFolders(alternateFolderName);
            await field.selectFolderFlow(alternateFolderName);

            await field.expectLabelContains(alternateFolderName);
            await expect(field.label).not.toContainText(folderName, { ignoreCase: true });
        } finally {
            await apiHelpers.deleteContentType(contentType.id);
        }
    });

    test('empty folderPath query param falls back to default', async ({
        adminPage,
        apiHelpers,
        testSuffix
    }) => {
        const contentType = await apiHelpers.createContentType(
            apiHelpers.hostFolderPayload(testSuffix)
        );
        try {
            const currentSite = await apiHelpers.getCurrentSite();
            const formPage = new NewEditContentFormPage(adminPage);
            await formPage.goToNewWithFolderPath(contentType.variable, '');

            const field = new HostFolderField(adminPage);
            await field.expectLabelContains(currentSite.hostname);
            await field.expectFormFunctional();
        } finally {
            await apiHelpers.deleteContentType(contentType.id);
        }
    });
});
