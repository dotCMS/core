import { expect, test } from '@playwright/test';
import { createContentlet, deleteContentlets } from '@requests/contentlets';
import { createFakeContentType, deleteContentType } from '@requests/contentType';
import { getLayoutIdForPortlet } from '@requests/menu';
import { getLegacyFrame } from '@utils/iframe';

/**
 * The legacy Query Tool (`/WEB-INF/jsp/lucene/lucene_search.jsp`) builds its result rows in the
 * browser, so no REST-level test reaches that code. These tests run a query in the page itself.
 */
test.describe('Query Tool (Legacy) — result rows', () => {
    test('shows a title with markup as text and opens the contentlet from its row @smoke', async ({
        page,
        request
    }) => {
        const suffix = crypto.randomUUID().slice(0, 8);
        // markup, an entity and both kinds of quote, none of which may be read as HTML
        const title = `<b id="t1">Q&A "double" 'single' ${suffix}</b>`;

        const contentType = await createFakeContentType(request, {
            name: `Query tool encoding ${suffix}`
        });

        try {
            const contentlet = await createContentlet(request, {
                contentType: contentType.variable,
                languageId: 1,
                title
            });

            try {
                const layoutId = await getLayoutIdForPortlet(request, 'graphql');

                // The portal page leaves a window that is not framed, so it is opened in an iframe of
                // the admin shell (like #detailFrame), where its `window.parent` is the shell. The
                // legacy Query Tool is not in the menu, so the shell cannot route to it.
                await page.goto('/dotAdmin/');
                await page.evaluate((src) => {
                    const frame = document.createElement('iframe');
                    frame.id = 'detailFrame';
                    frame.style.cssText =
                        'position:fixed;inset:0;width:100%;height:100%;z-index:9999;background:#fff';
                    frame.src = src;
                    document.body.appendChild(frame);
                }, `/c/portal/layout?p_l_id=${layoutId}&p_p_id=query-tool-legacy`);

                const frame = getLegacyFrame(page);

                await frame.getByLabel('Lucene Query').fill(`+contentType:${contentType.variable}`);
                await frame.getByRole('button', { name: 'Submit' }).click();

                const rows = frame.locator('#luceneResultTable');
                const link = rows.getByRole('link', { name: title, exact: true });

                // the title is shown as typed, and no element was created from it
                await expect(link).toBeVisible();
                await expect(rows.locator('#t1')).toHaveCount(0);
                await expect(rows.locator('b')).toHaveCount(0);

                // the row carries the contentlet and its variant to the editor
                const visited: string[] = [];
                page.on('framenavigated', (navigated) => {
                    if (navigated === page.mainFrame()) visited.push(navigated.url());
                });
                await link.click();

                await expect
                    .poll(() => visited.map((url) => new URL(url).hash))
                    .toContain(`#/c/content/${contentlet.inode}&variantName=DEFAULT`);
            } finally {
                await deleteContentlets(request, [contentlet.identifier]);
            }
        } finally {
            await deleteContentType(request, contentType.id);
        }
    });
});
