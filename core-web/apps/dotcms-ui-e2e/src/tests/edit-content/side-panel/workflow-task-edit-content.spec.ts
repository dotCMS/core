import { expect, type FrameLocator, type Locator, type Page, test } from '@playwright/test';
import { createContentlet } from '@requests/contentlets';
import { type ContentType, createFakeContentType, deleteContentType } from '@requests/contentType';
import { ensurePortletInMenu } from '@requests/menu';
import { getWorkflowTaskId } from '@requests/workflow';
import { getLegacyFrame } from '@utils/iframe';
import { Portlet } from '@utils/portlets';
import { uniqueSuffix } from '@utils/utils';

import { SidePanel } from './helpers/side-panel';

/**
 * #38303, FR-025a and US2 scenario 8: the Workflow Tasks detail's "Edit Content" opens the content
 * in the side panel, above the task dialog, instead of loading the legacy editor inside the
 * dialog's iframe. The panel is written to the task's URL (FR-044). Once the panel saves, the
 * detail reloads and shows the content as saved; Escape closes the panel and leaves the dialog open
 * on the same task.
 *
 * Every fired workflow action creates the content's task, so the content created through the API
 * already has one. The task list shows tasks assigned to the author's role by default; the spec
 * lists every user's tasks, open and resolved, narrowed to the content's title.
 */

/** The Workflow Tasks portlet's id, also the segment after `/c/` in its route. */
const WORKFLOW_PORTLET = 'workflow';

/** The task dialog, which `dot-workflow-task-detail` opens over the task list. */
function taskDialog(page: Page): Locator {
    return page.getByRole('dialog').filter({ hasText: 'Task Detail' });
}

/**
 * The task detail JSP (`view_workflow_task.jsp`) inside the task dialog. Scoped to the dialog:
 * the task list behind it is in an iframe of the same name.
 */
function taskDetailFrame(page: Page): FrameLocator {
    return taskDialog(page).frameLocator('iframe[name="detailFrame"]');
}

test.describe('Workflow Tasks opens task content in the side panel over the task dialog', () => {
    let contentType: ContentType | null = null;
    let title: string;
    let inode: string;
    let identifier: string;

    test.beforeEach(async ({ request }) => {
        await ensurePortletInMenu(request, WORKFLOW_PORTLET);

        const suffix = uniqueSuffix();
        // createFakeContentType gives it a Title text field and the new editor.
        contentType = await createFakeContentType(request, {
            name: `E2ESidePanelTask${suffix}`,
            metadata: { CONTENT_EDITOR2_ENABLED: true }
        });

        title = `Task content ${suffix}`;
        const contentlet = await createContentlet(request, {
            contentType: contentType.variable,
            title
        });
        inode = contentlet.inode;
        identifier = contentlet.identifier;
    });

    test.afterEach(async ({ request }) => {
        // Deleting the content type deletes its contentlet and, with it, its task.
        if (contentType) {
            await deleteContentType(request, contentType.id);
            contentType = null;
        }
    });

    test('edits task content above the task dialog and closes back to it with Escape @critical', async ({
        page,
        request
    }) => {
        const sidePanel = new SidePanel(page);
        const taskId = await getWorkflowTaskId(request, inode);
        const detail = taskDetailFrame(page);
        const detailTitle = detail.getByRole('heading', { level: 1 });
        const typed = `${title} edited from its task`;

        await page.goto(Portlet.WorkflowTasks);

        // The task list's filters are Dojo widgets; each one clicked runs the list's search.
        const taskList = getLegacyFrame(page);
        await taskList.locator('#keywords').fill(title);
        await taskList.locator('#showClosed').click();
        await taskList.locator('#showAllLink').click();
        await taskList.locator('td', { hasText: title }).first().click();

        await expect(page).toHaveURL(new RegExp(`/dotAdmin/#/c/workflow/${taskId}`));
        await expect(taskDialog(page)).toBeVisible({ timeout: 20000 });
        await expect(detailTitle).toContainText(title, { timeout: 20000 });

        // "Edit Content" is in the detail's actions menu, a dijit DropDownButton.
        await detail.locator('#archiveDropDownButton').click();
        await detail.getByRole('menuitem', { name: 'Edit Content' }).click();

        await sidePanel.expectOpen();
        expect(await sidePanel.isOnTop(), 'the side panel is not above the task dialog').toBe(true);
        await expect(taskDialog(page)).toBeVisible();
        await expect(page).toHaveURL(
            new RegExp(`/dotAdmin/#/c/workflow/${taskId}\\?.*editContent=${identifier}`)
        );

        // Keystrokes, not `fill()`: a focus trap left on the dialog would swallow them.
        const titleField = sidePanel.front.getByTestId('title');
        await titleField.fill('');
        await titleField.click();
        await page.keyboard.type(typed);
        await expect(titleField).toHaveValue(typed);

        // Saved so the form is pristine: with unsaved changes Escape asks before closing (US3).
        await sidePanel.form.save();

        // The detail was viewed again: it heads itself with the content's title as saved.
        await expect(detailTitle).toContainText(typed, { timeout: 20000 });

        await page.keyboard.press('Escape');
        await sidePanel.expectClosed();
        await expect(taskDialog(page)).toBeVisible();
        await expect(page).toHaveURL(new RegExp(`/dotAdmin/#/c/workflow/${taskId}$`));
    });

    test('opens the task and its content from the task URL with the side panel on top', async ({
        page,
        request
    }) => {
        const sidePanel = new SidePanel(page);
        const taskId = await getWorkflowTaskId(request, inode);

        await page.goto(
            `${Portlet.WorkflowTasks}/${taskId}?editContent=${identifier}&editContentLang=1`
        );

        // Both open from the URL: the task dialog from the route, the panel from its params.
        await expect(taskDialog(page)).toBeVisible({ timeout: 20000 });
        await expect(taskDetailFrame(page).getByRole('heading', { level: 1 })).toContainText(
            title,
            { timeout: 20000 }
        );
        await sidePanel.expectOpen();
        await sidePanel.expectCount(1);
        await expect(sidePanel.title).toContainText(title);
        expect(await sidePanel.isOnTop(), 'the side panel is not above the task dialog').toBe(true);

        await sidePanel.close();
        await sidePanel.expectClosed();
        await expect(taskDialog(page)).toBeVisible();
        await expect(page).toHaveURL(new RegExp(`/dotAdmin/#/c/workflow/${taskId}$`));
    });
});
