import { APIRequestContext, expect, Locator, Page, test } from '@playwright/test';
import { ContentType, createFakeContentType, deleteContentType } from '@requests/contentType';
import { admin1 } from '@utils/credentials';
import { generateBase64Credentials } from '@utils/generateBase64Credential';

/**
 * Regression spec for issue #36617: the sidebar's History and Comments sections must
 * reflect the latest state after a save/publish, with no manual page reload.
 *
 * The bug had two faces, one per host, so both are covered here:
 * - Full-screen (routed): the save navigates, which re-initializes the store and empties
 *   the version list. Without the fix the list stayed empty.
 * - Dialog (overlay): the save does NOT navigate, so nothing clears or re-fetches.
 *   Without the fix the list stayed stale until the dialog was closed and reopened.
 *
 * Notes:
 * - Sidebar tab headers carry no data-testid; they are identified by role/tab index
 *   (0 = Actions, 1 = History, 2 = Comments). The panels themselves DO have testids
 *   (`history`, `activities`), so the assertions anchor on those.
 * - The dialog is only reachable in production from UVE or the relationship field's
 *   "New Content" item. The latter needs no page/template setup, so it is used here.
 */

const HISTORY_TAB_INDEX = 1;
const COMMENTS_TAB_INDEX = 2;

function authHeaders() {
    return { Authorization: generateBase64Credentials(admin1.username, admin1.password) };
}

/** Version rows currently rendered in the History panel of the given root. */
function historyItems(root: Page | Locator): Locator {
    return root.getByTestId('history').getByTestId('history-item');
}

async function openSidebarTab(root: Page | Locator, index: number): Promise<void> {
    // Must be scoped to `sidebar-tabs`: the form area has its own role="tab" elements
    // (Content / SEO / Social / Advanced Properties) that would otherwise match first.
    await root.getByTestId('sidebar-tabs').getByRole('tab').nth(index).click();
}

async function fireSidebarAction(root: Page | Locator, actionName: string): Promise<void> {
    await openSidebarTab(root, 0);
    const button = root.getByTestId('sidebar-workflow-actions').getByRole('button', {
        name: actionName
    });
    await button.waitFor({ state: 'visible', timeout: 10000 });
    await button.click();
}

test.describe('Sidebar History/Comments refresh after save (#36617)', () => {
    test.describe.configure({ mode: 'serial' });

    let contentType: ContentType;
    let contentletInode: string;

    test.beforeAll(async ({ request }) => {
        const suffix = Date.now();

        contentType = await createFakeContentType(request, {
            name: `HistoryRefresh${suffix}`,
            variable: `historyRefreshCT${suffix}`,
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
            '/api/v1/workflow/actions/default/fire/PUBLISH?indexPolicy=WAIT_FOR',
            {
                data: {
                    contentlet: {
                        contentType: contentType.variable,
                        title: `History Refresh ${suffix}`
                    }
                },
                headers: authHeaders()
            }
        );

        expect(response.status()).toBe(200);
        contentletInode = (await response.json()).entity.inode;
    });

    test.afterAll(async ({ request }) => {
        // deleteContentType cascades contentlets
        if (contentType?.id) {
            await deleteContentType(request, contentType.id);
        }
    });

    test.describe('full-screen host', () => {
        test.beforeEach(async ({ page }) => {
            await page.goto(`/dotAdmin/#/content/${contentletInode}`);
            await page.waitForLoadState('domcontentloaded');
            await page
                .locator('dot-edit-content-sidebar')
                .waitFor({ state: 'visible', timeout: 15000 });
            await page.getByTestId('title').waitFor({ state: 'visible', timeout: 15000 });
        });

        test('adds the new version to History after publishing, with no reload', async ({
            page
        }) => {
            await openSidebarTab(page, HISTORY_TAB_INDEX);
            await expect(historyItems(page).first()).toBeVisible({ timeout: 15000 });
            const before = await historyItems(page).count();

            await fireSidebarAction(page, 'Publish');

            // Back to History: the list must grow on its own. The bug rendered it empty
            // here (the routed save empties the list and nothing re-fetched it).
            await openSidebarTab(page, HISTORY_TAB_INDEX);
            await expect(historyItems(page)).toHaveCount(before + 1, { timeout: 20000 });
        });

        test('adds the new comment to Comments after posting, with no reload', async ({ page }) => {
            await openSidebarTab(page, COMMENTS_TAB_INDEX);

            const comment = `e2e comment ${Date.now()}`;
            await page.getByTestId('activities-input').fill(comment);
            await page.getByTestId('activities-submit').click();

            await expect(
                page.getByTestId('activities').getByText(comment, { exact: false })
            ).toBeVisible({ timeout: 15000 });
        });
    });

    test.describe('dialog host', () => {
        // Tracked at describe scope so teardown can reach them: they are created inside
        // the helper below, which runs per test. Assigned as soon as each create resolves
        // so a failure halfway through setup still leaves the earlier type deletable.
        let dialogTargetTypeId: string | undefined;
        let dialogParentTypeId: string | undefined;

        test.afterAll(async ({ request }) => {
            // Parent first: it holds the relationship field pointing at the target type,
            // so removing the target while that reference exists is not safe.
            // deleteContentType cascades the contentlets of each type.
            if (dialogParentTypeId) {
                await deleteContentType(request, dialogParentTypeId);
            }

            if (dialogTargetTypeId) {
                await deleteContentType(request, dialogTargetTypeId);
            }
        });

        /**
         * Opens the editor through the relationship field's "New Content" item, saves once
         * so the contentlet gains a version, and returns the editor root.
         *
         * `FEATURE_FLAG_EDIT_CONTENT_SIDE_PANEL` defaults to `true`
         * (dotmarketing-config.properties), so "New Content" opens the slide-in side panel,
         * not the centered dialog — hence `edit-content-side-panel`, not `edit-content-dialog`.
         */
        async function openDialogWithSavedContent(
            page: Page,
            request: APIRequestContext
        ): Promise<Locator> {
            const suffix = Date.now();

            const targetType = await createFakeContentType(request, {
                name: `DialogTarget${suffix}`,
                variable: `dialogTargetCT${suffix}`,
                fields: [
                    {
                        clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField',
                        name: 'Title',
                        variable: 'title',
                        sortOrder: 1
                    }
                ]
            });
            dialogTargetTypeId = targetType.id;

            const parentType = await createFakeContentType(request, {
                name: `DialogParent${suffix}`,
                variable: `dialogParentCT${suffix}`,
                fields: [
                    {
                        clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField',
                        name: 'Title',
                        variable: 'title',
                        sortOrder: 1
                    },
                    {
                        clazz: 'com.dotcms.contenttype.model.field.ImmutableRelationshipField',
                        name: 'Rel',
                        variable: 'rel',
                        sortOrder: 2,
                        relationships: {
                            cardinality: 0,
                            velocityVar: targetType.variable
                        }
                    }
                ]
            });
            dialogParentTypeId = parentType.id;

            const created = await request.put(
                '/api/v1/workflow/actions/default/fire/PUBLISH?indexPolicy=WAIT_FOR',
                {
                    data: {
                        contentlet: {
                            contentType: parentType.variable,
                            title: `Dialog Parent ${suffix}`
                        }
                    },
                    headers: authHeaders()
                }
            );
            expect(created.status()).toBe(200);
            const parentInode = (await created.json()).entity.inode;

            await page.goto(`/dotAdmin/#/content/${parentInode}`);
            await page.waitForLoadState('domcontentloaded');
            await page.getByTestId('relationship-add-button').click();
            await page.getByRole('menuitem', { name: 'New Content' }).click();

            const dialog = page.getByTestId('edit-content-side-panel');
            await dialog.waitFor({ state: 'visible', timeout: 15000 });

            // Save once inside the dialog so there is a version to compare against.
            await dialog.getByTestId('title').fill(`Dialog Child ${suffix}`);
            await fireSidebarAction(dialog, 'Publish');
            await openSidebarTab(dialog, HISTORY_TAB_INDEX);
            await expect(historyItems(dialog).first()).toBeVisible({ timeout: 20000 });

            return dialog;
        }

        test('adds the new version to History after publishing inside the dialog', async ({
            page,
            request
        }) => {
            const dialog = await openDialogWithSavedContent(page, request);
            const before = await historyItems(dialog).count();

            await fireSidebarAction(dialog, 'Publish');

            // The dialog never navigates, so nothing clears or re-fetches on its own.
            // The bug left this list stale until the dialog was closed and reopened.
            await openSidebarTab(dialog, HISTORY_TAB_INDEX);
            await expect(historyItems(dialog)).toHaveCount(before + 1, { timeout: 20000 });
        });

        /**
         * Comments are deliberately NOT covered in the dialog host. The comment form is
         * hidden whenever the editor was opened for new content
         * (`$hideForm = $initialContentletState() === 'new'` in
         * dot-edit-content-sidebar-activities.component.ts), and that flag keeps its
         * value after the first save. The relationship field — the only dialog entry
         * point that needs no page/template setup — always opens with `mode: 'new'`,
         * so the form is never available there.
         *
         * Covering it requires the UVE pencil flow (`mode: 'edit'`), which needs a page
         * with a contentlet on it. Worth adding when a page fixture exists; the
         * full-screen comment test above already covers the refresh logic itself, and
         * the dialog History test covers the dialog-specific half of the bug.
         */
    });
});
