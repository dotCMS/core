import { NewEditContentFormPage } from '@pages';

import { HostFolderField } from './helpers/host-folder-field';

import { expect, test } from '../../../../fixtures/host-folder.fixture';

const ROOT_FOLDER_COUNT = 45;
const NESTED_CHILD_COUNT = 45;

test.describe('Host/Folder Load More Pagination', () => {
    test.describe.configure({ mode: 'serial' });

    let contentTypeVariable: string;
    let siteName: string;

    test.beforeEach(async ({ apiHelpers, testSuffix }) => {
        const contentType = await apiHelpers.createContentType(
            apiHelpers.hostFolderPayload(testSuffix)
        );
        contentTypeVariable = contentType.variable;

        const defaultSite = await apiHelpers.getDefaultSite();
        siteName = defaultSite.hostname;
    });

    test('root load more loads next page of folders @critical', async ({
        adminPage,
        apiHelpers,
        testSuffix
    }) => {
        const prefix = `hfroot-${testSuffix}`;
        const targetFolder = `${prefix}-${ROOT_FOLDER_COUNT}`;
        await apiHelpers.createFolders(
            siteName,
            apiHelpers.buildFolderPaths(prefix, ROOT_FOLDER_COUNT)
        );

        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToNew(contentTypeVariable);

        const field = new HostFolderField(adminPage);
        await formPage.fillTextField(`Title LoadMore Root ${testSuffix}`);

        await field.openOverlay();
        await field.selectSite(siteName);
        await field.expectLoadMoreVisible();

        const countBefore = await field.folderNodeCount();
        await field.clickLoadMore();
        const countAfter = await field.folderNodeCount();
        expect(countAfter).toBeGreaterThan(countBefore);

        await field.searchFolders(prefix);
        await field.selectFolderFlow(targetFolder);
        await field.expectLabelContains(targetFolder);

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
        await field.expectLabelContains(targetFolder);
    });

    test('nested load more loads next page under expanded parent @critical', async ({
        adminPage,
        apiHelpers,
        testSuffix
    }) => {
        const parentName = `aaa-hfparent-${testSuffix}`;
        const prefix = `hfchild-${testSuffix}`;
        const targetFolder = `${prefix}-${NESTED_CHILD_COUNT}`;

        await apiHelpers.createFolders(siteName, [`/${parentName}`]);
        await apiHelpers.createFolders(
            siteName,
            apiHelpers.buildFolderPaths(prefix, NESTED_CHILD_COUNT, `/${parentName}`)
        );

        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToNew(contentTypeVariable);

        const field = new HostFolderField(adminPage);
        await formPage.fillTextField(`Title LoadMore Nested ${testSuffix}`);

        await field.openOverlay();
        await field.selectSite(siteName);
        await field.expectTreeNodeVisible(parentName);
        await field.expandFolder(parentName);
        await field.expectLoadMoreVisible();

        const countBefore = await field.folderNodeCount();
        await field.clickLoadMore();
        expect(await field.folderNodeCount()).toBeGreaterThan(countBefore);

        await field.searchFolders(prefix);
        await field.expectTreeNodeVisible(targetFolder);

        await field.selectFolderFlow(targetFolder);
        await field.expectLabelContains(targetFolder);
        await field.expectLabelContains(parentName);
    });
});
