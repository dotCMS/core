import { LegacyEditContentFormPage } from '@pages';
import { expect, test } from '@playwright/test';
import { Contentlet, createContentlet, deleteContentlets } from '@requests/contentlets';
import { ContentType, createFakeContentType, deleteContentType } from '@requests/contentType';
import { createFakePayloadTextField } from '@utils/dot-content-types.mock';
import { uniqueSuffix } from '@utils/utils';

/**
 * A title with an apostrophe and double quotes: the legacy form writes it into the inline script that fires `edit-contentlet-loaded`, so the event only carries the title if that script parses.
 */
const QUOTED_TITLE = `It's a "test"`;

/** A frame's window, with the `ng-event` details the init script below records in it. */
interface NgEventLog {
    __ngEvents?: unknown[];
}

async function createLegacyContentType(request: Parameters<typeof createFakeContentType>[0]) {
    return createFakeContentType(request, {
        // An apostrophe in the type name: it travels in the same event payload as the title.
        name: `E2E Legacy's ${uniqueSuffix()}`,
        // false -> the legacy Dojo/JSP editor, the one that fires the event.
        metadata: { CONTENT_EDITOR2_ENABLED: false },
        fields: [
            createFakePayloadTextField({
                name: 'Title',
                variable: 'title',
                sortOrder: 1
            })
        ]
    });
}

test.describe('Legacy editor — loaded event', () => {
    let contentType: ContentType | null = null;
    let contentTypeVariable: string;
    let contentTypeName: string;
    let contentlet: Contentlet | null = null;

    test.beforeEach(async ({ request }) => {
        contentType = await createLegacyContentType(request);
        contentTypeVariable = contentType.variable;
        contentTypeName = contentType.name;
        contentlet = await createContentlet(request, {
            contentType: contentType.variable,
            title: QUOTED_TITLE
        });
    });

    test.afterEach(async ({ request }) => {
        if (contentlet) {
            await deleteContentlets(request, [contentlet.identifier]);
            contentlet = null;
        }

        if (contentType) {
            await deleteContentType(request, contentType.id);
            contentType = null;
        }
    });

    /**
     * The legacy form opens in the side panel, which has no use for `edit-contentlet-loaded` and does not change the document title or show a header from it. The spec listens for the event itself instead: the form's frame records every `ng-event` it dispatches, and the recorded event must name the content type and carry the title as the same text, quotes included. With the title written raw into the script, the script never parses and the event never fires.
     */
    test('fires with a title holding quotes and leaves no page error @critical', async ({
        page
    }) => {
        // Only a script that fails to parse matters here; unrelated runtime errors are noise.
        const syntaxErrors: string[] = [];
        page.on('pageerror', (error) => {
            if (error.name === 'SyntaxError') {
                syntaxErrors.push(error.message);
            }
        });

        // In every frame, the legacy form's included: it dispatches the event on its own document.
        await page.addInitScript(() => {
            document.addEventListener('ng-event', (event) => {
                const log = window as unknown as NgEventLog;
                (log.__ngEvents ??= []).push((event as CustomEvent).detail);
            });
        });

        const formPage = new LegacyEditContentFormPage(page);
        await formPage.goToLegacyEdit(contentTypeVariable);

        const legacyFrame = await formPage.getLegacyContentFrame();
        // The form dispatches the event two seconds after it loads.
        await expect
            .poll(
                () =>
                    legacyFrame.evaluate(() => (window as unknown as NgEventLog).__ngEvents ?? []),
                { timeout: 20000 }
            )
            .toContainEqual({
                name: 'edit-contentlet-loaded',
                data: { contentType: contentTypeName, pageTitle: QUOTED_TITLE }
            });

        await expect(legacyFrame.locator('#title')).toHaveValue(QUOTED_TITLE);

        expect(syntaxErrors).toEqual([]);
    });
});
