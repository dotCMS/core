import { NewEditContentFormPage } from '@pages';

import { HostFolderField } from './helpers/host-folder-field';

import { expect, test } from '../../../../fixtures/host-folder.fixture';

/**
 * Journey 1: Default Host/Folder Selection (No Folder Context)
 */
test.describe('Default Host/Folder Selection', () => {
    test('default field shows a site selection for new content @critical', async ({
        adminPage,
        apiHelpers,
        testSuffix
    }) => {
        const contentType = await apiHelpers.createContentType(
            apiHelpers.hostFolderPayload(testSuffix)
        );
        try {
            const defaultSite = await apiHelpers.getDefaultSite();
            const formPage = new NewEditContentFormPage(adminPage);
            await formPage.goToNew(contentType.variable);

            const field = new HostFolderField(adminPage);
            await field.expectVisible();
            await field.expectLabelContains(defaultSite.hostname);
        } finally {
            await apiHelpers.deleteContentType(contentType.id);
        }
    });

    test('selects site root through the popover and updates the trigger label @critical', async ({
        adminPage,
        apiHelpers,
        testSuffix
    }) => {
        const contentType = await apiHelpers.createContentType(
            apiHelpers.hostFolderPayload(testSuffix)
        );
        try {
            const defaultSite = await apiHelpers.getDefaultSite();
            const formPage = new NewEditContentFormPage(adminPage);
            await formPage.goToNew(contentType.variable);

            const field = new HostFolderField(adminPage);
            await field.selectSiteRoot(defaultSite.hostname);

            await field.expectLabelContains(defaultSite.hostname);
        } finally {
            await apiHelpers.deleteContentType(contentType.id);
        }
    });

    test('selects a folder through the popover and updates the trigger label @critical', async ({
        adminPage,
        apiHelpers,
        testSuffix
    }) => {
        const contentType = await apiHelpers.createContentType(
            apiHelpers.hostFolderPayload(testSuffix)
        );
        try {
            const defaultSite = await apiHelpers.getDefaultSite();
            const folderName = `popover-folder-${testSuffix}`;
            await apiHelpers.createFolders(defaultSite.hostname, [`/${folderName}`]);

            const formPage = new NewEditContentFormPage(adminPage);
            await formPage.goToNew(contentType.variable);

            const field = new HostFolderField(adminPage);
            await field.openOverlay();
            await field.selectSite(defaultSite.hostname);
            const partial = folderName.substring(0, Math.min(6, folderName.length));
            await field.searchFolders(partial);
            await field.selectFolderFlow(folderName);

            await field.expectLabelContains(folderName);
        } finally {
            await apiHelpers.deleteContentType(contentType.id);
        }
    });

    test('site selection survives a save and reload @critical', async ({
        adminPage,
        apiHelpers,
        testSuffix
    }) => {
        const contentType = await apiHelpers.createContentType(
            apiHelpers.hostFolderPayload(testSuffix)
        );
        try {
            const formPage = new NewEditContentFormPage(adminPage);
            await formPage.goToNew(contentType.variable);

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
            const [, savedContentIdentifier] = url.match(
                /\/content\/([a-f0-9-]+)/
            ) as RegExpMatchArray;
            expect(savedContentIdentifier).toBeTruthy();

            await formPage.goToContent(savedContentIdentifier);
            await field.expectLabelContains(selectedSiteName);
        } finally {
            await apiHelpers.deleteContentType(contentType.id);
        }
    });
});
