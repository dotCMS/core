import { NewEditContentFormPage } from '@pages';

import { HostFolderField } from './helpers/host-folder-field';

import { expect, test } from '../../../../fixtures/host-folder.fixture';

test.describe('Host/Folder Popover UX', () => {
    test.describe.configure({ mode: 'serial' });

    let contentTypeVariable: string;
    let siteName: string;
    let folderA: string;
    let folderB: string;

    test.beforeEach(async ({ apiHelpers, testSuffix }) => {
        const contentType = await apiHelpers.createContentType(
            apiHelpers.hostFolderPayload(testSuffix)
        );
        contentTypeVariable = contentType.variable;

        const defaultSite = await apiHelpers.getDefaultSite();
        siteName = defaultSite.hostname;
        folderA = `popover-a-${testSuffix}`;
        folderB = `popover-b-${testSuffix}`;

        await apiHelpers.createFolders(siteName, [`/${folderA}`, `/${folderB}`]);
    });

    test('closing overlay without Select discards staged selection @critical', async ({
        adminPage
    }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToNewWithFolderPath(contentTypeVariable, `${siteName}/${folderA}/`);

        const field = new HostFolderField(adminPage);
        await field.expectLabelContains(folderA);
        const labelBefore = await field.getLabelText();
        expect(labelBefore).toContain(folderA);

        await field.openOverlay();
        await field.searchFolders(folderB.substring(0, Math.min(8, folderB.length)));
        await field.clickFolder(folderB);
        await field.closeOverlay();

        await field.expectLabelContains(folderA);
        await expect(field.label).not.toContainText(folderB);
    });

    test('copy path button shows check icon after click @smoke', async ({ adminPage }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToNewWithFolderPath(contentTypeVariable, `${siteName}/${folderA}/`);

        const field = new HostFolderField(adminPage);
        await field.expectLabelContains(folderA);

        await field.copyPath();
        await field.expectCopyIconCheck();
    });

    test('sites panel lists available sites @smoke', async ({ adminPage }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToNew(contentTypeVariable);

        const field = new HostFolderField(adminPage);
        await field.openOverlay();

        const sites = field.sitesPanel.getByTestId('host-folder-site-item');
        await expect(sites.first()).toBeVisible({ timeout: 10000 });
        expect(await sites.count()).toBeGreaterThan(0);
    });
});
