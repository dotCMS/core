import { NewEditContentFormPage } from '@pages';
import { expect, test } from '@playwright/test';
import { Category, createCategory, deleteCategories } from '@requests/categories';
import { Contentlet, createContentlet, deleteContentlets } from '@requests/contentlets';
import { ContentType, createFakeContentType, deleteContentType } from '@requests/contentType';
import { admin1 } from '@utils/credentials';
import {
    createFakePayloadCategoryField,
    createFakePayloadTextField
} from '@utils/dot-content-types.mock';
import { generateBase64Credentials } from '@utils/generateBase64Credential';

/**
 * Category field round-trip (issue #37670).
 *
 * `categories` is required on `ContentTypeCategoryField` and was optional on the flat interface.
 * The API stores a category field as an array of `{ key: name }` objects; the form control holds
 * the keys the category resolver reshapes them into. Both tests seed a contentlet through the API
 * with a category already stored — a new contentlet has nothing to reshape.
 *
 * The category component does not normally keep the resolver's value: its store re-derives the
 * selection from the contentlet, fetches the hierarchy, and overwrites the control with inodes.
 * The resolver's keys are what gets saved only when that fetch fails — the component then keeps
 * what `writeValue` received rather than blank the field. So the first test covers the ordinary
 * round trip, and the second forces the fetch to fail, which is the path that reaches the
 * reshape. Breaking the reshape (returning names instead of keys) fails the second test and not
 * the first.
 */
let parentCategory: Category | null = null;
let childCategory: Category | null = null;
let contentType: ContentType | null = null;
let contentlet: Contentlet | null = null;

test.beforeEach(async ({ request }) => {
    const suffix = Date.now();

    parentCategory = await createCategory(request, {
        name: `E2E Category Parent ${suffix}`,
        key: `e2eCategoryParent${suffix}`
    });
    childCategory = await createCategory(request, {
        name: `E2E Category Child ${suffix}`,
        key: `e2eCategoryChild${suffix}`,
        parent: parentCategory.inode
    });

    contentType = await createFakeContentType(request, {
        name: `E2ECategoryField${suffix}`,
        fields: [
            createFakePayloadTextField({ name: 'Title', variable: 'title', sortOrder: 1 }),
            createFakePayloadCategoryField({
                name: 'Category',
                variable: 'categoryField',
                sortOrder: 2,
                values: parentCategory.inode
            })
        ]
    });

    contentlet = await createContentlet(request, {
        contentType: contentType.variable,
        title: 'Category round trip',
        categoryField: [childCategory.inode]
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

    const categoryInodes = [childCategory?.inode, parentCategory?.inode].filter(
        (inode): inode is string => !!inode
    );
    if (categoryInodes.length) {
        await deleteCategories(request, categoryInodes);
    }
    childCategory = null;
    parentCategory = null;
});

test('a stored category is shown, saved back and still stored after reopen @critical', async ({
    page,
    request
}) => {
    const formPage = new NewEditContentFormPage(page);
    const { identifier, inode } = contentlet as Contentlet;
    const child = childCategory as Category;

    const selectedChip = page
        .getByTestId('field-categoryField')
        .getByTestId('category-list')
        .locator('.p-chip', { hasText: child.categoryName });

    // Opening: the component's store resolves the stored category into a chip.
    await formPage.goToContent(inode);
    await expect(selectedChip).toBeVisible({ timeout: 20000 });

    // Saving: editing the title makes the save a real one rather than a no-op on a pristine form.
    await page.getByTestId('title').fill('Category round trip (saved)');
    await formPage.save();

    const response = await request.get(`/api/v1/content/${identifier}`, {
        headers: {
            Authorization: generateBase64Credentials(admin1.username, admin1.password)
        }
    });
    expect(response.status()).toBe(200);
    const stored = (await response.json()).entity;

    expect(stored.title).toBe('Category round trip (saved)');
    expect(stored.categoryField).toEqual([{ [child.key]: child.categoryName }]);

    // Reopening: the save made a new version, so this opens the inode the API now reports, not
    // the seeded one.
    await formPage.goToContent(stored.inode);
    await expect(selectedChip).toBeVisible({ timeout: 20000 });
});

test('with the hierarchy fetch failing, the reshaped keys are what gets saved @critical', async ({
    page,
    request
}) => {
    const formPage = new NewEditContentFormPage(page);
    const { identifier, inode } = contentlet as Contentlet;
    const child = childCategory as Category;

    // Leaves the component in ERROR, where it keeps the value `writeValue` received — the keys
    // the resolver produced from the stored `[{ key: name }]` — instead of overwriting it.
    await page.route('**/api/v1/categories/hierarchy', (route) =>
        route.fulfill({ status: 500, body: '{}' })
    );

    await formPage.goToContent(inode);

    // The failed fetch is reported in a modal error dialog, which blocks the workflow actions
    // until it is dismissed.
    await page.getByRole('button', { name: 'Accept' }).click();

    await page.getByTestId('title').fill('Category round trip (fetch failed)');
    await formPage.save();

    const response = await request.get(`/api/v1/content/${identifier}`, {
        headers: {
            Authorization: generateBase64Credentials(admin1.username, admin1.password)
        }
    });
    expect(response.status()).toBe(200);
    const stored = (await response.json()).entity;

    expect(stored.title).toBe('Category round trip (fetch failed)');
    expect(stored.categoryField).toEqual([{ [child.key]: child.categoryName }]);
});
