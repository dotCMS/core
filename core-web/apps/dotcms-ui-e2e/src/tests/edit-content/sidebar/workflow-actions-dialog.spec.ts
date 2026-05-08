import { expect, test } from '@playwright/test';
import { deleteContentlets } from '@requests/contentlets';
import { ContentType, createFakeContentType, deleteContentType } from '@requests/contentType';
import {
    WorkflowActionCreated,
    createCommentableWorkflowAction,
    deleteWorkflowAction,
    getWorkflowStepId
} from '@requests/workflow';
import { admin1 } from '@utils/credentials';
import { generateBase64Credentials } from '@utils/generateBase64Credential';

/**
 * Regression spec for issue #34347: the New Edit Contentlet mode must open the wizard
 * dialog when a workflow action has commentable or assignable inputs.
 *
 * Setup strategy:
 * - Creates a fresh content type associated with the System Workflow.
 * - Creates a contentlet via fire/NEW so it starts in the initial step.
 * - Creates a workflow action (commentable + assignable) in the System Workflow at
 *   the contentlet's current step — mirrors the unit-test mock configuration.
 * - Cleans up the action (and contentlet / content type) in afterAll.
 *
 * Notes:
 * - Uses PUT (not POST) — PUT returns the contentlet directly in entity; POST
 *   returns a bulk-format response with entity.results[0][key].
 * - The p-dialog host (data-testid="dot-wizard") is always "hidden" in Playwright
 *   because PrimeNG's appendTo="body" moves the visual content to the body portal.
 *   The portal does NOT inherit the host's data-testid.
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
    // Wait for the workflow actions toolbar to be rendered
    await page.getByTestId('workflow-actions').waitFor({ state: 'visible', timeout: 15000 });

    // The action may be an inline p-button or in the overflow (...) popup menu.
    // count() never throws — returns 0 when not found.
    const inlineButton = page.getByRole('button', { name: actionName });

    if ((await inlineButton.count()) > 0) {
        await inlineButton.click();
    } else {
        // Action is beyond the inline cap — open the overflow menu
        const overflowButton = page.getByTestId('overflow-button');
        await overflowButton.waitFor({ state: 'visible', timeout: 5000 });
        await overflowButton.click();
        const menuItem = page.getByRole('menuitem', { name: actionName });
        await menuItem.waitFor({ state: 'visible', timeout: 5000 });
        await menuItem.click();
    }

    // The p-dialog host (data-testid="dot-wizard") is always "hidden" because
    // PrimeNG moves portal content to body. Use an inner element in the portal.
    await page.locator('.dot-wizard__view').waitFor({ state: 'visible', timeout: 10000 });
}

test.describe('Workflow action commentable and assignable dialog', () => {
    test.describe.configure({ mode: 'serial' });

    let contentType: ContentType;
    let contentletInode: string;
    let contentletIdentifier: string;
    let wizardAction: WorkflowActionCreated;

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

        // fire/NEW saves without publishing — PUT returns entity directly.
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

        // Discover the step the contentlet is currently in, then create a
        // commentable + assignable action at that step so it shows in the editor.
        const stepId = await getWorkflowStepId(request, contentletInode);

        wizardAction = await createCommentableWorkflowAction(request, {
            schemeId: SYSTEM_WORKFLOW_ID,
            stepId,
            name: `WFDialogAction${suffix}`
        });
    });

    test.afterAll(async ({ request }) => {
        if (wizardAction?.id) {
            await deleteWorkflowAction(request, wizardAction.id);
        }
        if (contentletIdentifier) {
            await deleteContentlets(request, [contentletIdentifier]);
        }
        if (contentType?.id) {
            await deleteContentType(request, contentType.id);
        }
    });

    test('clicking a commentable/assignable action opens the wizard dialog @critical', async ({
        page
    }) => {
        await navigateToContent(page, contentletInode);
        await openWizardDialog(page, wizardAction.name);

        await expect(page.locator('.dot-wizard__view')).toBeVisible();
    });

    test('wizard dialog contains the assignee dropdown @critical', async ({ page }) => {
        await navigateToContent(page, contentletInode);
        await openWizardDialog(page, wizardAction.name);

        await expect(page.locator('#dotRoles')).toBeVisible();
    });

    test('wizard dialog contains the comments textarea @critical', async ({ page }) => {
        await navigateToContent(page, contentletInode);
        await openWizardDialog(page, wizardAction.name);

        await expect(page.locator('#comment')).toBeVisible();
    });

    test('canceling the wizard dialog closes it without firing the action @smoke', async ({
        page
    }) => {
        await navigateToContent(page, contentletInode);
        await openWizardDialog(page, wizardAction.name);

        await page.getByTestId('dialog-close-button').click();

        await expect(page.locator('.dot-wizard__view')).toBeHidden();
    });
});
