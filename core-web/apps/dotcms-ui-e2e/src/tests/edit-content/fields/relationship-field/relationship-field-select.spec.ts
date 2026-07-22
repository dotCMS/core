import { NewEditContentFormPage } from '@pages';

import { RelationshipField } from './helpers/relationship-field';
import { SelectExistingContentDialog } from './helpers/select-existing-content-dialog';

import { CARDINALITY, expect, test } from '../../../../fixtures/relationship.fixture';

// ─── Single Selection (ONE_TO_ONE, MANY_TO_ONE) ─────────────────

test.describe('Single Selection (1:1 / M:1)', () => {
    const cardinalities = [
        { name: 'ONE_TO_ONE', value: CARDINALITY.ONE_TO_ONE, fieldVar: 'mainAuthor' },
        { name: 'MANY_TO_ONE', value: CARDINALITY.MANY_TO_ONE, fieldVar: 'mainAuthor' }
    ] as const;

    for (const cardinality of cardinalities) {
        test.describe(`${cardinality.name}`, () => {
            // Serial: beforeEach shares mutable `let` vars across tests.
            test.describe.configure({ mode: 'serial' });

            let authorTypeVariable: string;
            let blogTypeVariable: string;

            test.beforeEach(async ({ apiHelpers, testSuffix }) => {
                const authorType = await apiHelpers.createContentType(
                    apiHelpers.authorPayload(`${cardinality.name}_${testSuffix}`)
                );
                authorTypeVariable = authorType.variable;

                const blogType = await apiHelpers.createContentType(
                    apiHelpers.blogPayload(
                        `${cardinality.name}_${testSuffix}`,
                        `E2E_Blog_${cardinality.name}`,
                        `E2EBlog${cardinality.name}`,
                        authorTypeVariable,
                        cardinality.fieldVar,
                        cardinality.value
                    )
                );
                blogTypeVariable = blogType.variable;

                await apiHelpers.createContentlets(
                    authorTypeVariable,
                    Array.from({ length: 3 }, (_, j) => {
                        const i = j + 1;
                        return {
                            title: `Author ${cardinality.name} ${i} ${testSuffix}`,
                            bio: `Bio for author ${i}`
                        };
                    })
                );
            });

            test('select and apply item @critical', async ({ adminPage }) => {
                const formPage = new NewEditContentFormPage(adminPage);
                await formPage.goToNew(blogTypeVariable);

                const relationshipField = new RelationshipField(adminPage);
                const dialog = new SelectExistingContentDialog(adminPage);

                await relationshipField.clickRelateExisting();
                await dialog.waitForVisible();
                await dialog.waitForContentLoaded();

                await dialog.expectRadioButtons();

                await dialog.selectSingleItem(0);
                await dialog.clickApply();
                await dialog.expectClosed();

                await relationshipField.expectRowCount(1);
                await relationshipField.expectRelateExistingDisabled();
            });

            test('save and verify persistence @critical', async ({ adminPage, testSuffix }) => {
                const formPage = new NewEditContentFormPage(adminPage);
                await formPage.goToNew(blogTypeVariable);

                const relationshipField = new RelationshipField(adminPage);
                const dialog = new SelectExistingContentDialog(adminPage);

                await formPage.fillTextField(`Blog ${cardinality.name} ${testSuffix}`);

                await relationshipField.clickRelateExisting();
                await dialog.waitForVisible();
                await dialog.waitForContentLoaded();
                await dialog.selectSingleItem(0);
                await dialog.clickApply();
                await dialog.expectClosed();
                await relationshipField.expectRowCount(1);

                await formPage.save();

                await relationshipField.expectRowCount(1);
            });

            test('apply button disabled with no selection @smoke', async ({ adminPage }) => {
                const formPage = new NewEditContentFormPage(adminPage);
                await formPage.goToNew(blogTypeVariable);

                const relationshipField = new RelationshipField(adminPage);
                const dialog = new SelectExistingContentDialog(adminPage);

                await relationshipField.clickRelateExisting();
                await dialog.waitForVisible();
                await dialog.waitForContentLoaded();

                await dialog.expectApplyDisabled();

                await dialog.selectSingleItem(0);
                await dialog.expectApplyEnabled();

                await dialog.clickCancel();
            });

            test('cancel discards selection @smoke', async ({ adminPage }) => {
                const formPage = new NewEditContentFormPage(adminPage);
                await formPage.goToNew(blogTypeVariable);

                const relationshipField = new RelationshipField(adminPage);
                const dialog = new SelectExistingContentDialog(adminPage);

                await relationshipField.clickRelateExisting();
                await dialog.waitForVisible();
                await dialog.waitForContentLoaded();

                await dialog.selectSingleItem(0);
                await dialog.clickCancel();
                await dialog.expectClosed();

                await relationshipField.expectEmpty();
            });

            test('ESC key dismisses select existing dialog without applying @smoke', async ({
                adminPage
            }) => {
                const formPage = new NewEditContentFormPage(adminPage);
                await formPage.goToNew(blogTypeVariable);

                const relationshipField = new RelationshipField(adminPage);
                const dialog = new SelectExistingContentDialog(adminPage);

                await relationshipField.clickRelateExisting();
                await dialog.waitForVisible();
                await dialog.waitForContentLoaded();

                await dialog.selectSingleItem(0);
                await dialog.closeViaEsc();

                // Dialog has closeOnEscape: true — ESC should close it
                await dialog.expectClosed();
                await relationshipField.expectEmpty();
            });
        });
    }
});

// ─── Multiple Selection (ONE_TO_MANY, MANY_TO_MANY) ─────────────

test.describe('Multiple Selection (1:M / M:M)', () => {
    const cardinalities = [
        { name: 'ONE_TO_MANY', value: CARDINALITY.ONE_TO_MANY, fieldVar: 'authors' },
        { name: 'MANY_TO_MANY', value: CARDINALITY.MANY_TO_MANY, fieldVar: 'tags' }
    ] as const;

    for (const cardinality of cardinalities) {
        test.describe(`${cardinality.name}`, () => {
            // Serial: beforeEach shares mutable `let` vars across tests.
            test.describe.configure({ mode: 'serial' });

            let authorTypeVariable: string;
            let blogTypeVariable: string;

            test.beforeEach(async ({ apiHelpers, testSuffix }) => {
                const authorType = await apiHelpers.createContentType(
                    apiHelpers.authorPayload(`${cardinality.name}_${testSuffix}`)
                );
                authorTypeVariable = authorType.variable;

                const blogType = await apiHelpers.createContentType(
                    apiHelpers.blogPayload(
                        `${cardinality.name}_${testSuffix}`,
                        `E2E_Blog_${cardinality.name}`,
                        `E2EBlog${cardinality.name}`,
                        authorTypeVariable,
                        cardinality.fieldVar,
                        cardinality.value
                    )
                );
                blogTypeVariable = blogType.variable;

                await apiHelpers.createContentlets(
                    authorTypeVariable,
                    Array.from({ length: 3 }, (_, j) => {
                        const i = j + 1;
                        return {
                            title: `Author ${cardinality.name} ${i} ${testSuffix}`,
                            bio: `Bio for author ${i}`
                        };
                    })
                );
            });

            test('open dialog shows checkboxes @critical', async ({ adminPage }) => {
                const formPage = new NewEditContentFormPage(adminPage);
                await formPage.goToNew(blogTypeVariable);

                const relationshipField = new RelationshipField(adminPage);
                const dialog = new SelectExistingContentDialog(adminPage);

                await relationshipField.clickRelateExisting();
                await dialog.waitForVisible();
                await dialog.waitForContentLoaded();

                await dialog.expectCheckboxes();
                await dialog.expectHeaderCheckbox();

                await dialog.clickCancel();
            });

            test('select multiple items and apply @critical', async ({ adminPage }) => {
                const formPage = new NewEditContentFormPage(adminPage);
                await formPage.goToNew(blogTypeVariable);

                const relationshipField = new RelationshipField(adminPage);
                const dialog = new SelectExistingContentDialog(adminPage);

                await relationshipField.clickRelateExisting();
                await dialog.waitForVisible();
                await dialog.waitForContentLoaded();

                await dialog.selectItems([0, 1, 2]);
                await dialog.clickApply();
                await dialog.expectClosed();

                await relationshipField.expectRowCount(3);
                await relationshipField.expectAddButtonEnabled();
            });

            test('save and verify persistence with multiple relations @critical', async ({
                adminPage,
                testSuffix
            }) => {
                const formPage = new NewEditContentFormPage(adminPage);
                await formPage.goToNew(blogTypeVariable);

                const relationshipField = new RelationshipField(adminPage);
                const dialog = new SelectExistingContentDialog(adminPage);

                await formPage.fillTextField(`Blog ${cardinality.name} Multi ${testSuffix}`);

                await relationshipField.clickRelateExisting();
                await dialog.waitForVisible();
                await dialog.waitForContentLoaded();
                await dialog.selectItems([0, 1, 2]);
                await dialog.clickApply();
                await dialog.expectClosed();
                await relationshipField.expectRowCount(3);

                await formPage.save();

                await relationshipField.expectRowCount(3);
            });

            test('select all with header checkbox @smoke', async ({ adminPage }) => {
                const formPage = new NewEditContentFormPage(adminPage);
                await formPage.goToNew(blogTypeVariable);

                const relationshipField = new RelationshipField(adminPage);
                const dialog = new SelectExistingContentDialog(adminPage);

                await relationshipField.clickRelateExisting();
                await dialog.waitForVisible();
                await dialog.waitForContentLoaded();

                await dialog.toggleSelectAll();
                await dialog.expectApplyEnabled();

                await dialog.clickApply();
                await dialog.expectClosed();

                await relationshipField.expectRowCount(3);
            });
        });
    }
});

// ─── Create New Inline ──────────────────────────────────────────

test.describe('Create New Inline', () => {
    // Serial: beforeEach shares mutable `let` vars across tests.
    test.describe.configure({ mode: 'serial' });

    let authorTypeVariable: string;
    let blogTypeVariable: string;

    test.beforeEach(async ({ apiHelpers, testSuffix }) => {
        const authorType = await apiHelpers.createContentType(
            apiHelpers.authorPayload(`CreateNew_${testSuffix}`)
        );
        authorTypeVariable = authorType.variable;

        const blogType = await apiHelpers.createContentType(
            apiHelpers.blogPayload(
                `CreateNew_${testSuffix}`,
                'E2E_Blog_CreateNew',
                'E2EBlogCreateNew',
                authorTypeVariable,
                'authors',
                CARDINALITY.ONE_TO_MANY
            )
        );
        blogTypeVariable = blogType.variable;
    });

    test('new content option is visible when editor enabled @critical', async ({ adminPage }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToNew(blogTypeVariable);

        const relationshipField = new RelationshipField(adminPage);
        const menu = await relationshipField.openAddMenu();
        const createNewItem = relationshipField.getCreateNewMenuItem(menu);
        await expect(createNewItem).toBeVisible();

        await adminPage.keyboard.press('Escape');
    });

    test('create inline and add to relationship @critical', async ({ adminPage, testSuffix }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToNew(blogTypeVariable);

        const relationshipField = new RelationshipField(adminPage);
        await relationshipField.clickCreateNew();

        const createDialog = adminPage.locator('.p-dialog-create-content .p-dialog');
        await expect(createDialog).toBeVisible({ timeout: 10000 });

        const titleInput = createDialog.getByTestId('title').first();
        await titleInput.waitFor({ state: 'visible', timeout: 10000 });
        await titleInput.fill(`Inline Author ${testSuffix}`);

        const responsePromise = adminPage.waitForResponse((response) =>
            response.url().includes('/api/v1/workflow/actions/')
        );
        const saveButton = createDialog.getByRole('button', { name: /Save/ });
        await saveButton.waitFor({ state: 'visible', timeout: 5000 });
        await saveButton.click();
        await responsePromise;

        // Dialog stays open after save — close via X button
        const closeButton = createDialog.locator(
            '.p-dialog-header-close, button[aria-label="Close"]'
        );
        await closeButton.waitFor({ state: 'visible', timeout: 5000 });
        await closeButton.click();
        await expect(createDialog).toBeHidden({ timeout: 10000 });

        await relationshipField.expectRowCount(1);
    });

    test('cancel inline creation preserves outer form @critical', async ({
        adminPage,
        testSuffix
    }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToNew(blogTypeVariable);

        const outerTitle = `Blog Outer Title ${testSuffix}`;
        await formPage.fillTextField(outerTitle);

        const relationshipField = new RelationshipField(adminPage);
        await relationshipField.clickCreateNew();

        const createDialog = adminPage.locator('.p-dialog-create-content .p-dialog');
        await expect(createDialog).toBeVisible({ timeout: 10000 });

        await adminPage.keyboard.press('Escape');
        await expect(createDialog).toBeHidden({ timeout: 5000 });

        const textField = adminPage.getByTestId('title');
        await expect(textField).toHaveValue(outerTitle);
        await relationshipField.expectEmpty();
    });

    test('dismiss create dialog via X button @smoke', async ({ adminPage }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToNew(blogTypeVariable);

        const relationshipField = new RelationshipField(adminPage);
        await relationshipField.clickCreateNew();

        const createDialog = adminPage.locator('.p-dialog-create-content .p-dialog');
        await expect(createDialog).toBeVisible({ timeout: 10000 });

        const closeButton = createDialog.locator(
            '.p-dialog-header-close, button[aria-label="Close"]'
        );
        await expect(closeButton).toBeVisible({ timeout: 5000 });
        await closeButton.click();

        await expect(createDialog).toBeHidden({ timeout: 5000 });
        await relationshipField.expectEmpty();
    });
});

// ─── New Content Disabled (No New Editor) ───────────────────────

test.describe('New Content Disabled (No New Editor)', () => {
    let tagTypeVariable: string;
    let blogTypeVariable: string;

    test.beforeEach(async ({ apiHelpers, testSuffix }) => {
        const tagType = await apiHelpers.createContentType(
            apiHelpers.tagPayload(`NoEditor_${testSuffix}`)
        );
        tagTypeVariable = tagType.variable;

        const blogType = await apiHelpers.createContentType(
            apiHelpers.blogPayload(
                `NoEditor_${testSuffix}`,
                'E2E_Blog_NoEditor',
                'E2EBlogNoEditor',
                tagTypeVariable,
                'tags',
                CARDINALITY.ONE_TO_MANY
            )
        );
        blogTypeVariable = blogType.variable;
    });

    test('new content disabled when related type lacks new editor @critical', async ({
        adminPage
    }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToNew(blogTypeVariable);

        const relationshipField = new RelationshipField(adminPage);
        await relationshipField.expectNewContentDisabled();
    });
});

// ─── Menu Disabled in Single Mode (Item Already Exists) ─────────

test.describe('Menu Disabled When Single Item Exists', () => {
    // Single-test describe: describe-level lets are safe (worker runs tests sequentially).
    // If a second test is added, move setup into each test with try/finally cleanup.
    let authorTypeId: string | undefined;
    let authorTypeVariable: string;
    let blogTypeId: string | undefined;
    let blogTypeVariable: string;

    test.beforeEach(async ({ apiHelpers, testSuffix }) => {
        const authorType = await apiHelpers.createContentType(
            apiHelpers.authorPayload(`SingleFull_${testSuffix}`)
        );
        authorTypeId = authorType.id;
        authorTypeVariable = authorType.variable;

        const blogType = await apiHelpers.createContentType(
            apiHelpers.blogPayload(
                `SingleFull_${testSuffix}`,
                'E2E_Blog_SingleFull',
                'E2EBlogSingleFull',
                authorTypeVariable,
                'mainAuthor',
                CARDINALITY.ONE_TO_ONE
            )
        );
        blogTypeId = blogType.id;
        blogTypeVariable = blogType.variable;

        await apiHelpers.createContentlet(authorTypeVariable, {
            title: `Author SingleFull ${testSuffix}`,
            bio: 'Bio'
        });
    });

    test.afterEach(async ({ apiHelpers }) => {
        if (blogTypeId) {
            await apiHelpers.deleteContentType(blogTypeId);
        }
        if (authorTypeId) {
            await apiHelpers.deleteContentType(authorTypeId);
        }
    });

    test('existing content disabled after selecting item @smoke', async ({ adminPage }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToNew(blogTypeVariable);

        const relationshipField = new RelationshipField(adminPage);
        const dialog = new SelectExistingContentDialog(adminPage);

        await relationshipField.clickRelateExisting();
        await dialog.waitForVisible();
        await dialog.waitForContentLoaded();
        await dialog.selectSingleItem(0);
        await dialog.clickApply();
        await dialog.expectClosed();

        await relationshipField.expectRelateExistingDisabled();
    });
});
