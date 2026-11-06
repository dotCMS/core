import { LegacyEditContentFormPage } from '@pages';
import { expect, test } from '@playwright/test';
import { Contentlet, createContentlet, deleteContentlets } from '@requests/contentlets';
import { ContentType, createFakeContentType, deleteContentType } from '@requests/contentType';
import { createFakePayloadTextField } from '@utils/dot-content-types.mock';
import { uniqueSuffix } from '@utils/utils';

/**
 * A title with an apostrophe and double quotes: the legacy form writes it into the inline script
 * that fires `edit-contentlet-loaded`, so the shell only learns the title if that script parses.
 */
const QUOTED_TITLE = `It's a "test"`;

function quoteForRegExp(text: string): string {
    return text.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

async function createLegacyContentType(request: Parameters<typeof createFakeContentType>[0]) {
    return createFakeContentType(request, {
        name: `E2ELegacyLoaded${uniqueSuffix()}`,
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
    let contentlet: Contentlet | null = null;

    test.beforeEach(async ({ request }) => {
        contentType = await createLegacyContentType(request);
        contentTypeVariable = contentType.variable;
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
     * The shell handles `edit-contentlet-loaded` by setting the document title to
     * `<pageTitle> - ...` (dot-contentlet-wrapper.component.ts). Asserting the document title
     * therefore proves the event fired AND that the title arrived as the same text, quotes
     * included. With the title written raw into the script, the script never parses, the event
     * never fires and the document title keeps the listing's.
     */
    test('fires with a title holding quotes and leaves no page error @critical', async ({
        page
    }) => {
        const pageErrors: string[] = [];
        page.on('pageerror', (error) => pageErrors.push(error.message));

        const formPage = new LegacyEditContentFormPage(page);
        await formPage.goToLegacyEdit(contentTypeVariable);

        await expect(page).toHaveTitle(new RegExp(`^${quoteForRegExp(QUOTED_TITLE)} - `), {
            timeout: 20000
        });

        const legacyFrame = await formPage.getLegacyContentFrame();
        await expect(legacyFrame.locator('#title')).toHaveValue(QUOTED_TITLE);

        expect(pageErrors).toEqual([]);
    });
});
