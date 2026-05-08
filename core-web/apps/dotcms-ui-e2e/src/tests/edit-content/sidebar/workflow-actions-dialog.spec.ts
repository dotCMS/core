import { expect, test } from '@playwright/test';
import { deleteContentlets } from '@requests/contentlets';
import { ContentType, createFakeContentType, deleteContentType } from '@requests/contentType';
import { WorkflowActionCreated, createWorkflowAction, deleteWorkflowAction, getWorkflowStepId } from '@requests/workflow';
import { admin1 } from '@utils/credentials';
import { generateBase64Credentials } from '@utils/generateBase64Credential';

/**
 * Regression spec for issue #34347: the New Edit Contentlet mode must open the wizard
 * dialog when a workflow action has commentable or assignable inputs.
 *
 * Notes:
 * - The p-dialog host (data-testid="dot-wizard") is always "hidden" in Playwright
 *   because PrimeNG's appendTo="body" moves the visual content to the body portal.
 *   Inner elements (.dot-wizard__view) ARE in the portal and detect wizard visibility.
 * - The wizard action may render as an inline p-button or inside the overflow (...)
 *   popup menu depending on how many actions fit in the toolbar viewport.
 */

const SYSTEM_WORKFLOW_ID = 'd61a59e1-a49c-46f2-a929-db2b4bfa88b2';

function authHeaders() {
    return { Authorization: generateBase64Credentials(admin1.username, admin1.password) };
}

async function navigateToContent(page: import('@playwright/test').Page, inode: string) {
    await page.goto(`/dotAdmin/#/content/${inode}`);
    await page.waitForLoadState('domcontentloaded');
    await page.locator('dot-edit-content-sidebar').waitFor({ state: 'visible', timeout: 15000 });
    await page.getByTestId('title').waitFor({ state: 'visible', timeout: 15000 });
}

async function openWizardDialog(page: import('@playwright/test').Page, actionName: string) {
    await page.getByTestId('workflow-actions').waitFor({ state: 'visible', timeout: 15000 });

    const inlineButton = page.getByRole('button', { name: actionName });

    if ((await inlineButton.count()) > 0) {
        await inlineButton.click();
    } else {
        const overflowButton = page.getByTestId('overflow-button');
        await overflowButton.waitFor({ state: 'visible', timeout: 5000 });
        await overflowButton.click();
        const menuItem = page.getByRole('menuitem', { name: actionName });
        await menuItem.waitFor({ state: 'visible', timeout: 5000 });
        await menuItem.click();
    }

    await page.locator('.dot-wizard__view').waitFor({ state: 'visible', timeout: 10000 });
}

test.describe('Workflow action wizard dialog', () => {
    test.describe.configure({ mode: 'serial' });

    let contentType: ContentType;
    let contentletInode: string;
    let contentletIdentifier: string;
    let stepId: string;

    test.beforeAll(async ({ request }) => {
        const suffix = Date.now();

        contentType = await createFakeContentType(request, {
            name: `WFDialogTest${suffix}`,
            variable: `wfDialogCT${suffix}`,
            fields: [
                {
                    clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField',
                    name: 'Title',
                    variable: 'title',
                    sortOrder: 1
                }
            ]
        });

        const response = await request.put(
            '/api/v1/workflow/actions/default/fire/NEW?indexPolicy=WAIT_FOR',
            {
                data: {
                    contentlet: {
                        contentType: contentType.variable,
                        title: `WF Dialog Regression ${suffix}`
                    }
                },
                headers: authHeaders()
            }
        );

        expect(response.status()).toBe(200);
        const data = await response.json();
        contentletInode = data.entity.inode;
        contentletIdentifier = data.entity.identifier;

        stepId = await getWorkflowStepId(request, contentletInode);
    });

    test.afterAll(async ({ request }) => {
        if (contentletIdentifier) {
            await deleteContentlets(request, [contentletIdentifier]);
        }
        if (contentType?.id) {
            await deleteContentType(request, contentType.id);
        }
    });

    test.describe('assignable + commentable action', () => {
        let action: WorkflowActionCreated;

        test.beforeAll(async ({ request }) => {
            action = await createWorkflowAction(request, {
                schemeId: SYSTEM_WORKFLOW_ID,
                stepId,
                name: `WFBothAction${Date.now()}`,
                assignable: true,
                commentable: true
            });
        });

        test.afterAll(async ({ request }) => {
            if (action?.id) await deleteWorkflowAction(request, action.id);
        });

        test('clicking the action opens the wizard dialog @critical', async ({ page }) => {
            await navigateToContent(page, contentletInode);
            await openWizardDialog(page, action.name);

            await expect(page.locator('.dot-wizard__view')).toBeVisible();
        });

        test('wizard contains the assignee dropdown @critical', async ({ page }) => {
            await navigateToContent(page, contentletInode);
            await openWizardDialog(page, action.name);

            await expect(page.locator('#dotRoles')).toBeVisible();
        });

        test('wizard contains the comments textarea @critical', async ({ page }) => {
            await navigateToContent(page, contentletInode);
            await openWizardDialog(page, action.name);

            await expect(page.locator('#comment')).toBeVisible();
        });

        test('canceling the wizard closes it without firing the action @smoke', async ({
            page
        }) => {
            await navigateToContent(page, contentletInode);
            await openWizardDialog(page, action.name);

            await page.getByTestId('dialog-close-button').click();

            await expect(page.locator('.dot-wizard__view')).toBeHidden();
        });
    });

    test.describe('assignable-only action', () => {
        let action: WorkflowActionCreated;

        test.beforeAll(async ({ request }) => {
            action = await createWorkflowAction(request, {
                schemeId: SYSTEM_WORKFLOW_ID,
                stepId,
                name: `WFAssignAction${Date.now()}`,
                assignable: true,
                commentable: false
            });
        });

        test.afterAll(async ({ request }) => {
            if (action?.id) await deleteWorkflowAction(request, action.id);
        });

        test('clicking the action opens the wizard dialog @critical', async ({ page }) => {
            await navigateToContent(page, contentletInode);
            await openWizardDialog(page, action.name);

            await expect(page.locator('.dot-wizard__view')).toBeVisible();
        });

        test('wizard contains the assignee dropdown @critical', async ({ page }) => {
            await navigateToContent(page, contentletInode);
            await openWizardDialog(page, action.name);

            await expect(page.locator('#dotRoles')).toBeVisible();
        });
    });

    test.describe('commentable-only action', () => {
        let action: WorkflowActionCreated;

        test.beforeAll(async ({ request }) => {
            action = await createWorkflowAction(request, {
                schemeId: SYSTEM_WORKFLOW_ID,
                stepId,
                name: `WFCommentAction${Date.now()}`,
                assignable: false,
                commentable: true
            });
        });

        test.afterAll(async ({ request }) => {
            if (action?.id) await deleteWorkflowAction(request, action.id);
        });

        test('clicking the action opens the wizard dialog @critical', async ({ page }) => {
            await navigateToContent(page, contentletInode);
            await openWizardDialog(page, action.name);

            await expect(page.locator('.dot-wizard__view')).toBeVisible();
        });

        test('wizard contains the comments textarea @critical', async ({ page }) => {
            await navigateToContent(page, contentletInode);
            await openWizardDialog(page, action.name);

            await expect(page.locator('#comment')).toBeVisible();
        });
    });
});
