import { NewEditContentFormPage } from '@pages';

import { HostFolderField } from './helpers/host-folder-field';

import { expect, test } from '../../../../fixtures/host-folder.fixture';

/**
 * Journey 1: Default Host/Folder Selection (No Folder Context)
 *
 * Admin creates new content without folder context; the Host/Folder field
 * defaults to a site. The user selects a different site from the tree,
 * saves, and verifies the selection persists after reopen.
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
        await field.expectLabelMatchesPattern(/^\/\/.+/);
    });

    test('select a site, save, and verify persistence @critical', async ({
        adminPage,
        testSuffix
    }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToNew(contentTypeVariable);

        const field = new HostFolderField(adminPage);
        await formPage.fillTextField(`Title Default ${testSuffix}`);

        await field.openDropdown();
        await field.expectAtLeastOneTreeNode();

        const selectedSiteName = await field.selectFirstNode();

        await field.expectPanelClosed();
        await field.expectLabelText(`//${selectedSiteName}`);

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

        await field.expectLabelText(`//${selectedSiteName}`);
    });

    test('tree select shows filterable list of sites @smoke', async ({ adminPage }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToNew(contentTypeVariable);

        const field = new HostFolderField(adminPage);
        await field.openDropdown();

        const siteName = await field.getFirstNodeLabel();
        const partial = siteName.substring(0, Math.min(4, siteName.length));

        await field.filterTree(partial);

        const filteredNodes = field.panel.locator('.p-tree-node');
        await expect(filteredNodes.first()).toBeVisible({ timeout: 5000 });
    });

    test('System Host is hidden when field is required', async ({ adminPage }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToNew(contentTypeVariable);

        const field = new HostFolderField(adminPage);
        await field.openDropdown();
        await field.expectTreeNodeNotVisible('System Host');
    });
});
