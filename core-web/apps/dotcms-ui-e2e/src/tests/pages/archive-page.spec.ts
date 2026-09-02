import { faker } from '@faker-js/faker';
import { PagesListPage } from '@pages';
import { expect, test } from '@playwright/test';
import { actionsPageWorkflow, createPage, Page } from '@requests/pages';

let pageContentlet: Page;

test.beforeEach(async ({ request }) => {
    const title = faker.lorem.words(3);
    const url = title.split(' ').join('-');

    pageContentlet = await createPage(request, {
        title,
        url,
        friendlyName: title,
        template: 'SYSTEM_TEMPLATE',
        contentType: 'htmlpageasset',
        cachettl: 0
    });

    // Unpublish via API so the page starts in Draft state before the UI test
    await actionsPageWorkflow(request, pageContentlet.inode, ['Unpublish']);
});

test.afterEach(async ({ request }) => {
    if (pageContentlet) {
        await actionsPageWorkflow(request, pageContentlet.inode, ['Destroy']);
    }
});

test('archive the page @critical', async ({ page }) => {
    const pagesListPage = new PagesListPage(page);
    await pagesListPage.navigateTo();

    const rowLocator = pagesListPage.getRowByTitle(pageContentlet.title);
    await expect(rowLocator).toBeVisible();

    const statusIcon = await pagesListPage.getStatusChipText(rowLocator);
    expect(statusIcon).toContain('Draft');

    await pagesListPage.doActionOnPage(rowLocator, 'Archive');
    await pagesListPage.activeArchivedFilter();

    const archivedRowLocator = pagesListPage.getRowByTitle(pageContentlet.title);
    await expect(archivedRowLocator).toBeVisible();
});
