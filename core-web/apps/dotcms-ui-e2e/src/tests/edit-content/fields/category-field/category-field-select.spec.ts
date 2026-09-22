import { NewEditContentFormPage } from '@pages';
import { expect, test } from '@playwright/test';
import { ContentType, createFakeContentType, deleteContentType } from '@requests/contentType';
import {
    createFakePayloadCategoryField,
    createFakePayloadTextField
} from '@utils/dot-content-types.mock';

/**
 * Category field round-trip (issue #37670).
 *
 * `categories` is required on `ContentTypeCategoryField` and was optional on the flat interface,
 * and the committed mock for it never carried the property at all — this proves the field still
 * builds, renders and survives a save with the tightened type in place.
 *
 * **It does not cover the reshape.** The category resolver turns an array of stored objects into
 * an array of their keys, and reaching that branch needs a seeded category tree plus a selection,
 * which this spec does not set up: `createFakePayloadCategoryField` points `values` at a random
 * uuid, so there is no tree to pick from and the saved value stays empty. The reshape is pinned
 * through the full two-stage path in
 * `dot-edit-content-form-resolutions.characterization.spec.ts` ("reshapes a stored category array
 * into the keys the control holds"). Seeding a tree here — via `/api/v1/categories` and a
 * `values` pointing at the created inode — would let this spec cover it end to end, and is worth
 * its own issue.
 */
let contentType: ContentType | null = null;
let contentTypeVariable: string;

test.beforeEach(async ({ request }) => {
    contentType = await createFakeContentType(request, {
        name: `E2ECategoryField${Date.now()}`,
        fields: [
            createFakePayloadTextField({ name: 'Title', variable: 'title', sortOrder: 1 }),
            createFakePayloadCategoryField({
                name: 'Category',
                variable: 'categoryField',
                sortOrder: 2
            })
        ]
    });
    contentTypeVariable = contentType.variable;
});

test.afterEach(async ({ request }) => {
    if (contentType) {
        await deleteContentType(request, contentType.id);
        contentType = null;
    }
});

test('the category field renders and the form saves with it present @critical', async ({
    page
}) => {
    const formPage = new NewEditContentFormPage(page);
    await formPage.goToNew(contentTypeVariable);

    await page.getByTestId('title').fill('Category round trip');

    const categoryField = page.getByTestId('field-categoryField');
    await expect(categoryField).toBeVisible({ timeout: 20000 });

    await formPage.save();
    await page.reload();

    // The field still renders after the round trip, on a contentlet saved with no category
    // selected. That is the empty-value path through the resolver, not the reshape — see the
    // note at the top of this file.
    await expect(categoryField).toBeVisible({ timeout: 20000 });
    await expect(page.getByTestId('title')).toHaveValue('Category round trip');
});
