import { LegacyEditContentFormPage } from '@pages';
import { expect, test } from '@playwright/test';
import { Contentlet, createContentlet, deleteContentlets } from '@requests/contentlets';
import { ContentType, createFakeContentType, deleteContentType } from '@requests/contentType';
import {
    createFakePayloadBinaryField,
    createFakePayloadTextField
} from '@utils/dot-content-types.mock';
import { uniqueSuffix } from '@utils/utils';

import { LegacyBinaryField } from './helpers/legacy-binary-field';

const BINARY_FIELD_VARIABLE = 'binaryField';

/**
 * Matches the contentlet lookup ONLY when it carries no inode — the request the legacy editor
 * used to issue on create. Deliberately anchored so `/api/v1/content/<inode>`, which the edit
 * path still makes legitimately, does not match.
 */
const INODELESS_CONTENT_REQUEST = /\/api\/v1\/content\/(\?.*)?$/;

async function createBinaryFieldContentType(request: Parameters<typeof createFakeContentType>[0]) {
    return createFakeContentType(request, {
        name: `E2EBinaryLegacy${uniqueSuffix()}`,
        // false -> the legacy Dojo/JSP editor, which is where the defect lives.
        metadata: { CONTENT_EDITOR2_ENABLED: false },
        fields: [
            createFakePayloadTextField({
                name: 'Title',
                variable: 'title',
                sortOrder: 1
            }),
            createFakePayloadBinaryField({
                name: 'Binary Field',
                variable: BINARY_FIELD_VARIABLE,
                sortOrder: 2
            })
        ]
    });
}

test.describe('Binary field — legacy editor', () => {
    let contentType: ContentType | null = null;
    let contentTypeVariable: string;
    let contentlet: Contentlet | null = null;

    test.beforeEach(async ({ request }) => {
        contentType = await createBinaryFieldContentType(request);
        contentTypeVariable = contentType.variable;
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
     * Regression guard for the infinite-spinner defect (#37603).
     *
     * On create there is no inode yet, so the editor used to fetch `/api/v1/content/` — a path
     * that never matches the JAX-RS route and always 404s. Whether that broke the field depended
     * on what the environment's error page returned: a bare `404` body parses as valid JSON and
     * the component renders anyway, while an empty or HTML body throws and left the spinner
     * running forever.
     *
     * This asserts the REQUEST IS NEVER MADE rather than that the field renders. Field visibility
     * alone is not a usable signal here: an equivalent assertion already exists in
     * binary-field-image-editor.spec.ts and passed green while the defect was live, because CI
     * runs in the masking configuration. Asserting the absence of the request is deterministic
     * and holds in any environment, whatever the error page returns.
     */
    test('does not request the contentlet when there is no inode @critical', async ({ page }) => {
        const inodelessRequests: string[] = [];

        await page.route(INODELESS_CONTENT_REQUEST, (route) => {
            inodelessRequests.push(route.request().url());
            return route.continue();
        });

        const formPage = new LegacyEditContentFormPage(page);
        await formPage.goToLegacyNew(contentTypeVariable);

        const legacyFrame = await formPage.getLegacyContentFrame();
        const field = new LegacyBinaryField(legacyFrame, page, BINARY_FIELD_VARIABLE);
        await field.expectVisible();

        expect(
            inodelessRequests,
            'the legacy editor must not look up a contentlet that does not exist yet'
        ).toHaveLength(0);
    });

    /**
     * Counterpart to the guard above: the edit path must keep hydrating from the API.
     *
     * The create fix skips the lookup entirely, and the same change added a `response.ok` check
     * that now throws on any non-2xx. Both narrow the path that edit still depends on, and no
     * other spec covers the legacy editor in edit mode — the binary-field specs that open
     * existing content run against the new Angular editor, which never loads edit_field.jsp.
     *
     * Asserts the field renders AND that the `.catch` fallback did not fire, since that fallback
     * only became visible once the `innerHTMl` typo was fixed.
     */
    test('hydrates the binary field when editing existing content @critical', async ({
        page,
        request
    }) => {
        contentlet = await createContentlet(request, {
            contentType: contentTypeVariable,
            title: `E2E Binary Legacy Edit ${uniqueSuffix()}`
        });

        const formPage = new LegacyEditContentFormPage(page);
        await formPage.goToLegacyEdit(contentTypeVariable);

        const legacyFrame = await formPage.getLegacyContentFrame();
        const field = new LegacyBinaryField(legacyFrame, page, BINARY_FIELD_VARIABLE);

        await field.expectVisible();
        await field.expectNoLoadError();
    });
});
