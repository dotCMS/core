import { expect, test } from '@playwright/test';
import { deleteContentlets } from '@requests/contentlets';
import { ContentType, deleteContentType } from '@requests/contentType';
import { createPage, Page as DotPage } from '@requests/pages';
import { admin1 } from '@utils/credentials';
import { generateBase64Credentials } from '@utils/generateBase64Credential';

/**
 * SystemWorkflow ID — standard across all dotCMS instances.
 */
const SYSTEM_WORKFLOW_ID = 'd61a59e1-a49c-46f2-a929-db2b4bfa88b2';

type ContentTypeWithVariable = ContentType & { variable: string };

/**
 * Navigate to edit content for a given inode and click the Settings tab (tab index 3).
 * Uses direct navigation instead of goToContent() because HTML pages don't have
 * the data-testid="title" field that goToContent() waits for.
 */
async function navigateToSettingsTab(page: import('@playwright/test').Page, inode: string) {
    await page.goto(`/dotAdmin/#/content/${inode}`);
    await page.waitForLoadState('domcontentloaded');
    // Wait for the edit content sidebar to confirm the editor has loaded
    await page.locator('dot-edit-content-sidebar').waitFor({ state: 'visible', timeout: 15000 });

    // Settings tab is the p-tab containing the cog icon (tab index 3)
    const settingsTab = page.locator('p-tab:has(.pi-cog)');
    await settingsTab.waitFor({ state: 'visible', timeout: 10000 });
    await settingsTab.click();
}

test.describe('Rules Dialog', () => {
    // Serial mode ensures beforeAll runs once and all tests share the same page instance
    test.describe.configure({ mode: 'serial' });

    let htmlPage: DotPage;

    test.beforeAll(async ({ request }) => {
        htmlPage = await createPage(request, {
            contentType: 'htmlpageasset',
            title: `E2E Rules Test Page ${Date.now()}`,
            url: `e2e-rules-test-${Date.now()}`,
            template: 'SYSTEM_TEMPLATE',
            friendlyName: 'E2E Rules Test Page',
            cachettl: 0
        });
    });

    test.afterAll(async ({ request }) => {
        if (htmlPage?.identifier) {
            await deleteContentlets(request, [htmlPage.identifier]);
        }
    });

    test.describe('rules card visibility', () => {
        test('rules card is visible in settings tab for saved HTML Page @critical', async ({
            page
        }) => {
            await navigateToSettingsTab(page, htmlPage.inode);

            await expect(page.getByTestId('rules')).toBeAttached();
            await expect(page.getByTestId('rules-card')).toBeVisible();
        });
    });

    test.describe('open rules dialog', () => {
        test('clicking the rules card opens the rules dialog @critical', async ({ page }) => {
            await navigateToSettingsTab(page, htmlPage.inode);

            await expect(page.getByTestId('rules-card')).toBeVisible();
            await page.getByTestId('rules-card').click();

            await expect(page.locator('.p-dialog')).toBeVisible({ timeout: 10000 });
            await expect(page.getByTestId('rules-container')).toBeVisible({ timeout: 10000 });
            await expect(page.getByTestId('rules-empty')).not.toBeAttached();
        });
    });

    test.describe('close rules dialog', () => {
        test('close button dismisses the rules dialog @critical', async ({ page }) => {
            await navigateToSettingsTab(page, htmlPage.inode);
            await page.getByTestId('rules-card').click();

            await expect(page.locator('.p-dialog')).toBeVisible({ timeout: 10000 });
            await page.locator('.p-dialog-header-close, .p-dialog-close-button').click();

            await expect(page.locator('.p-dialog')).toBeHidden();
            await expect(page.getByTestId('rules-card')).toBeVisible();
        });

        test('escape key does NOT close the rules dialog @smoke', async ({ page }) => {
            await navigateToSettingsTab(page, htmlPage.inode);
            await page.getByTestId('rules-card').click();

            await expect(page.locator('.p-dialog')).toBeVisible({ timeout: 10000 });
            await page.keyboard.press('Escape');

            // Dialog must remain open — closeOnEscape: false by design
            await expect(page.locator('.p-dialog')).toBeVisible();
            await expect(page.getByTestId('rules-container')).toBeVisible();
        });
    });

    test.describe('prevent duplicate dialogs', () => {
        test('rapid repeated clicks open only one dialog @critical', async ({ page }) => {
            await navigateToSettingsTab(page, htmlPage.inode);

            const rulesCard = page.getByTestId('rules-card');
            await expect(rulesCard).toBeVisible();

            // After the first click, the dialog mask intercepts real pointer events on the card.
            // Programmatic HTMLElement.click() still fires on the card (stress test for duplicate opens).
            for (let i = 0; i < 3; i++) {
                await rulesCard.evaluate((el) => (el as HTMLElement).click());
            }

            await expect(page.locator('.p-dialog')).toHaveCount(1, { timeout: 10000 });
        });
    });

    test.describe('rules card not visible for non-Page content type', () => {
        test.describe.configure({ mode: 'serial' });

        let contentType: ContentTypeWithVariable;
        let contentletInode: string;
        let contentletIdentifier: string;

        test.beforeEach(async ({ request }) => {
            const suffix = Date.now();

            // Create a simple (non-Page) content type with the new editor enabled.
            // Avoids createFakeContentType which calls /api/v1/schemas (404).
            const ctResponse = await request.post('/api/v1/contenttype', {
                data: {
                    clazz: 'com.dotcms.contenttype.model.type.ImmutableSimpleContentType',
                    name: `RulesTestType${suffix}`,
                    variable: `rulesCT${suffix}`,
                    host: 'SYSTEM_HOST',
                    folder: 'SYSTEM_FOLDER',
                    metadata: { CONTENT_EDITOR2_ENABLED: true },
                    workflow: [SYSTEM_WORKFLOW_ID],
                    fields: [
                        {
                            clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField',
                            name: 'Title',
                            variable: 'title',
                            sortOrder: 1
                        }
                    ]
                },
                headers: {
                    Authorization: generateBase64Credentials(admin1.username, admin1.password)
                }
            });
            const ctData = await ctResponse.json();
            contentType = (ctData.entity[0] ?? ctData.entity) as ContentTypeWithVariable;

            const contentletResponse = await request.post(
                '/api/v1/workflow/actions/default/fire/PUBLISH?indexPolicy=WAIT_FOR',
                {
                    data: {
                        contentlet: {
                            contentType: contentType.variable,
                            title: `Rules Non-Page Test ${suffix}`
                        }
                    },
                    headers: {
                        Authorization: generateBase64Credentials(admin1.username, admin1.password)
                    }
                }
            );
            const contentletData = await contentletResponse.json();
            const key = Object.keys(contentletData.entity.results[0])[0];
            const contentlet = contentletData.entity.results[0][key];
            contentletInode = contentlet.inode;
            contentletIdentifier = contentlet.identifier;
        });

        test.afterEach(async ({ request }) => {
            if (contentletIdentifier) {
                await deleteContentlets(request, [contentletIdentifier]);
            }
            if (contentType?.id) {
                await deleteContentType(request, contentType.id);
            }
        });

        test('rules card is absent in settings tab for non-Page contentlet @critical', async ({
            page
        }) => {
            await navigateToSettingsTab(page, contentletInode);

            await expect(page.getByTestId('rules')).not.toBeAttached();
            await expect(page.getByTestId('rules-card')).not.toBeAttached();
        });
    });
});
