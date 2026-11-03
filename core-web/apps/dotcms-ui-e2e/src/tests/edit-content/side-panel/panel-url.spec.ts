import { ContentDrivePage, NewEditContentFormPage } from '@pages';
import { expect, type Page, test } from '@playwright/test';
import { createContentlet } from '@requests/contentlets';
import { type ContentType, createFakeContentType, deleteContentType } from '@requests/contentType';
import { Portlet } from '@utils/portlets';
import { uniqueSuffix } from '@utils/utils';

import { SidePanel } from './helpers/side-panel';

/**
 * #38303, US7 (FR-044 to FR-050): every surface but the page editor keeps its open side panel in
 * its URL. Opening adds one history entry; Back closes the panel through its unsaved-changes
 * prompt; a closed panel stays closed on Forward; the URL reopens the panel in a new page; the path
 * deep links end on the portlet's URL with the panel params.
 *
 * Run on Content Drive (an Angular portlet) and on the legacy content portlet (`/c/content`, a JSP
 * in an iframe), the two ends of the range of surfaces.
 */

/** The new editor's unsaved-changes prompt (`edit.content.unsaved.changes.keep`). */
const KEEP_EDITING = 'Keep editing';

/** Whether the page's URL names a panel, with any of the four panel params. */
const namesAPanel = (page: Page): boolean =>
    /[?&](editContent|editContentLang|createContent|editCode)=/.test(page.url());

test.describe('The open side panel is in the URL', () => {
    let contentType: ContentType | null = null;
    let typeVariable: string;
    let identifier: string;
    let inode: string;
    let title: string;

    test.beforeEach(async ({ request }) => {
        const suffix = uniqueSuffix();
        // createFakeContentType gives it a Title text field and the new editor.
        contentType = await createFakeContentType(request, {
            name: `E2ESidePanelUrl${suffix}`,
            metadata: { CONTENT_EDITOR2_ENABLED: true }
        });
        typeVariable = contentType.variable;

        title = `Panel URL content ${suffix}`;
        const contentlet = await createContentlet(request, {
            contentType: contentType.variable,
            title
        });
        identifier = contentlet.identifier;
        inode = contentlet.inode;
    });

    test.afterEach(async ({ request }) => {
        // Deleting the content type deletes its contentlet.
        if (contentType) {
            await deleteContentType(request, contentType.id);
            contentType = null;
        }
    });

    test.describe('on Content Drive', () => {
        /** Opens the content from the list, as a double-click does, and waits for its panel. */
        const openFromList = async (page: Page): Promise<SidePanel> => {
            const drive = new ContentDrivePage(page);
            const sidePanel = new SidePanel(page);

            await page.goto(
                `${Portlet.ContentDrive}?filters=${encodeURIComponent(`title:${title}`)}`
            );
            await expect(drive.toolbar).toBeVisible({ timeout: 20000 });
            await drive.listTitles.filter({ hasText: title }).first().dblclick();
            await sidePanel.expectOpen();

            return sidePanel;
        };

        test('names the open panel in the URL, and the URL reopens it in a new page @critical', async ({
            page,
            context
        }) => {
            await openFromList(page);

            await expect(page).toHaveURL(new RegExp(`editContent=${identifier}`));
            await expect(page).toHaveURL(/editContentLang=1/);

            const copy = await context.newPage();
            const copied = new SidePanel(copy);
            await copy.goto(page.url());

            await copied.expectOpen();
            await expect(copied.title).toContainText(title);
        });

        test('closes the panel on Back when nothing changed, and the URL no longer names it @critical', async ({
            page
        }) => {
            const sidePanel = await openFromList(page);

            await page.goBack();

            await sidePanel.expectClosed();
            expect(namesAPanel(page), page.url()).toBe(false);
        });

        test('asks before Back closes a panel with a change, and Keep editing keeps the panel and the URL', async ({
            page
        }) => {
            const sidePanel = await openFromList(page);
            await sidePanel.front.getByTestId('title').fill(`${title} (edited)`);

            await page.goBack();
            await page.getByRole('button', { name: KEEP_EDITING }).click();

            await sidePanel.expectCount(1);
            await expect(page).toHaveURL(new RegExp(`editContent=${identifier}`));
        });

        test('does not reopen a panel closed with its X on Forward', async ({ page }) => {
            const sidePanel = await openFromList(page);

            await sidePanel.close();
            await sidePanel.expectClosed();
            expect(namesAPanel(page), page.url()).toBe(false);

            await page.goBack();
            await page.goForward();

            await sidePanel.expectClosed();
            expect(namesAPanel(page), page.url()).toBe(false);
        });
    });

    test.describe('on the legacy content portlet', () => {
        /** Opens a create from the content listing (a JSP event), and waits for its panel. */
        const openCreateFromList = async (page: Page): Promise<SidePanel> => {
            const formPage = new NewEditContentFormPage(page);
            const sidePanel = new SidePanel(page);

            await formPage.goToNew(typeVariable);
            await sidePanel.expectOpen();

            return sidePanel;
        };

        test('names a create opened from the listing in the URL, and the URL reopens it in a new page', async ({
            page,
            context
        }) => {
            await openCreateFromList(page);

            await expect(page).toHaveURL(
                new RegExp(`/c/content\\?.*createContent=${typeVariable}`)
            );

            const copy = await context.newPage();
            const copied = new SidePanel(copy);
            await copy.goto(page.url());

            await copied.expectOpen();
            await expect(copied.front.getByTestId('title')).toHaveValue('');
        });

        test('closes the panel on Back when nothing changed, and the URL no longer names it', async ({
            page
        }) => {
            const sidePanel = await openCreateFromList(page);

            await page.goBack();

            await sidePanel.expectClosed();
            expect(namesAPanel(page), page.url()).toBe(false);
        });

        test('asks before Back closes a panel with a change, and Keep editing keeps the panel and the URL', async ({
            page
        }) => {
            const sidePanel = await openCreateFromList(page);
            await sidePanel.front.getByTestId('title').fill('Typed, not saved');

            await page.goBack();
            await page.getByRole('button', { name: KEEP_EDITING }).click();

            await sidePanel.expectCount(1);
            await expect(page).toHaveURL(new RegExp(`createContent=${typeVariable}`));
        });

        test('does not reopen a panel closed with its X on Forward', async ({ page }) => {
            const sidePanel = await openCreateFromList(page);

            await sidePanel.close();
            await sidePanel.expectClosed();

            await page.goBack();
            await page.goForward();

            await sidePanel.expectClosed();
            expect(namesAPanel(page), page.url()).toBe(false);
        });

        // Velocity and old bookmarks emit the path form; it ends on the portlet's URL.
        test('ends a content link on the portlet URL with the panel params, the panel open @critical', async ({
            page
        }) => {
            const sidePanel = new SidePanel(page);

            await page.goto(`${Portlet.Content}/${inode}`);

            await sidePanel.expectOpen();
            await expect(sidePanel.title).toContainText(title);
            await expect(page).toHaveURL(
                new RegExp(`/dotAdmin/#/c/content\\?.*editContent=${identifier}`)
            );
            expect(page.url()).not.toContain(inode);
        });
    });
});
