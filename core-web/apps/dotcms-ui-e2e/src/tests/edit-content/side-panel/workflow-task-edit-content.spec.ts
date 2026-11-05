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

/**
 * Waits until Dojo's parse has made a widget of a legacy page's markup. Until then a click on it is
 * lost: the parse rebuilds the widget from its markup, unchecked, and its handler is not bound yet.
 * The widget's node carries `widgetid` from then on.
 *
 * @param frame - The legacy page
 * @param id - The widget's id
 */
async function waitForDojoWidget(frame: FrameLocator, id: string): Promise<void> {
    await expect(frame.locator(`[widgetid="${id}"]`), `the ${id} widget is ready`).toBeAttached({
        timeout: 20000
    });
}

/**
 * Waits for the task detail's `load`. The app hears the detail's `ng-event`s (the "Edit Content"
 * that opens the side panel) through a listener it adds on that load, while the detail's heading,
 * and its menu once parsed, are there before it.
 *
 * @param page - The page with the task dialog open
 */
async function waitForTaskDetailLoad(page: Page): Promise<void> {
    const iframe = await taskDialog(page).locator('iframe[name="detailFrame"]').elementHandle();
    const frame = await iframe?.contentFrame();

    expect(frame, 'the task dialog shows its detail').toBeTruthy();
    await frame?.waitForLoadState('load');
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

        // The task list's filters are Dojo widgets; each one clicked runs the list's search. `check()`
        // rather than `click()`: "All" is a radio whose handler toggles, so a second click would
        // turn it off again, and both confirm the state they leave.
        const taskList = getLegacyFrame(page);
        await waitForDojoWidget(taskList, 'showClosed');
        await taskList.locator('#keywords').fill(title);
        await taskList.locator('#showClosed').check();
        await taskList.locator('#showAllLink').check();
        const taskRow = taskList.locator('td', { hasText: title }).first();
        await expect(taskRow, 'the filtered task list shows the task').toBeVisible({
            timeout: 20000
        });
        await taskRow.click();

        await expect(page).toHaveURL(new RegExp(`/dotAdmin/#/c/workflow/${taskId}`));
        await expect(taskDialog(page)).toBeVisible({ timeout: 20000 });
        await expect(detailTitle).toContainText(title, { timeout: 20000 });
        await waitForTaskDetailLoad(page);

        // "Edit Content" is in the detail's actions menu, a dijit DropDownButton: chosen once the
        // menu is open.
        await waitForDojoWidget(detail, 'archiveDropDownButton');
        await detail.locator('#archiveDropDownButton').click();
        const editContent = detail.getByRole('menuitem', { name: 'Edit Content' });
        await expect(editContent, 'the actions menu is open').toBeVisible();
        await editContent.click();

        await sidePanel.expectOpen();
        await sidePanel.expectOnTop('the side panel is not above the task dialog');
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
        await sidePanel.expectOnTop('the side panel is not above the task dialog');

        await sidePanel.close();
        await sidePanel.expectClosed();
        await expect(taskDialog(page)).toBeVisible();
        await expect(page).toHaveURL(new RegExp(`/dotAdmin/#/c/workflow/${taskId}$`));
    });
});
