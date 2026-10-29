import { expect, test } from '@playwright/test';
import { createContentlet, lockContentlet } from '@requests/contentlets';
import { type ContentType, createFakeContentType, deleteContentType } from '@requests/contentType';
import { ensurePortletInMenu } from '@requests/menu';
import { Portlet } from '@utils/portlets';
import { uniqueSuffix } from '@utils/utils';

import { SidePanel } from './helpers/side-panel';

/**
 * #38303, US2: a Query Tool result opens in the side panel over the results instead of a new tab,
 * in the legacy editor for a legacy type, and the query reruns when the content is saved.
 */

/** The search the Query Tool runs, and reruns after a save (`DotQueryToolService.search`). */
const SEARCH_ENDPOINT = '/api/v1/content/_search';

/** How long a click is given to open a tab, if it is going to. */
const NEW_TAB_WINDOW_MS = 1500;

const TITLE_FIELD = 'title';

test.describe('Query Tool opens results in the side panel', () => {
    let contentType: ContentType | null = null;
    let identifier: string;
    let title: string;

    test.beforeEach(async ({ request }) => {
        await ensurePortletInMenu(request, 'query-tool');

        const suffix = uniqueSuffix();
        // createFakeContentType gives it a Title text field.
        contentType = await createFakeContentType(request, {
            name: `E2ESidePanelQuery${suffix}`,
            metadata: { CONTENT_EDITOR2_ENABLED: false }
        });

        title = `Query Tool result ${suffix}`;
        const contentlet = await createContentlet(request, {
            contentType: contentType.variable,
            [TITLE_FIELD]: title
        });
        identifier = contentlet.identifier;
        // The legacy editor opens content read-only unless the user holds its lock.
        await lockContentlet(request, identifier);
    });

    test.afterEach(async ({ request }) => {
        // Deleting the content type deletes its contentlet.
        if (contentType) {
            await deleteContentType(request, contentType.id);
            contentType = null;
        }
    });

    test('opens a legacy-editor result in the side panel, not a tab, and reruns the query on save @critical', async ({
        page,
        context
    }) => {
        const sidePanel = new SidePanel(page);
        const query = `+identifier:${identifier}`;

        // A `q` in the address runs the query on load, as a shared Query Tool link does.
        await page.goto(`${Portlet.QueryTool}?q=${encodeURIComponent(query)}`);
        const resultLink = page.getByTestId('query-tool-result-link').filter({ hasText: title });
        await expect(resultLink).toBeVisible({ timeout: 20000 });

        // Armed before the click: a tab opened by it must not slip past.
        const tabOpened = context.waitForEvent('page', { timeout: NEW_TAB_WINDOW_MS }).then(
            () => true,
            () => false
        );
        await resultLink.click();

        await sidePanel.expectOpen();
        expect(await tabOpened, 'the result opened a new tab').toBe(false);

        const rerun = page.waitForRequest(
            (request) => request.url().includes(SEARCH_ENDPOINT) && request.method() === 'POST',
            { timeout: 30000 }
        );
        await sidePanel.saveLegacyTextField(TITLE_FIELD, `${title} edited`);
        await rerun;
    });
});
