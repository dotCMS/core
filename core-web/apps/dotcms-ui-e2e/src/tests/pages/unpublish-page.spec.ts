import { faker } from '@faker-js/faker';
import { PagesListPage } from '@pages';
import { expect, test } from '@playwright/test';
import { createPage, actionsPageWorkflow, Page } from '@requests/pages';

let pageContentlet: Page | null = null;

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
});

test.afterEach(async ({ request }) => {
    if (pageContentlet) {
        await actionsPageWorkflow(request, pageContentlet.inode, ['Archive', 'Destroy']);
    }
});

test('unpublish the page @critical', async ({ page }) => {
    const pagesListPage = new PagesListPage(page);
    await pagesListPage.navigateTo();

    const rowLocator = pagesListPage.getRowByTitle(pageContentlet.title);
    await expect(rowLocator).toBeVisible();

    await pagesListPage.doActionOnPage(rowLocator, 'Unpublish');

    const statusIcon = pagesListPage.getStatusIcon(rowLocator);
    await expect(statusIcon).toHaveAttribute('aria-label', 'Draft');
});
