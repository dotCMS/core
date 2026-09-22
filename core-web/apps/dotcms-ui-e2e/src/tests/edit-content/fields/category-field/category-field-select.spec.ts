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
 * The category resolver is the one that reshapes rather than casts: the contentlet stores an
 * array of objects and the form control wants an array of their keys. It is also the field the
 * issue cites as the motivating example for per-type typing — `categories` is required on
 * `ContentTypeCategoryField` and was optional on the flat interface, and the committed mock for
 * it never carried the property at all.
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

    // The field still renders after the round trip. Selecting a value needs a category tree
    // seeded in the instance, which this spec deliberately does not assume — what it protects
    // is that the resolver returns a shape the control accepts rather than throwing, which is
    // where the reshape from stored objects to keys would fail.
    await expect(categoryField).toBeVisible({ timeout: 20000 });
    await expect(page.getByTestId('title')).toHaveValue('Category round trip');
});
