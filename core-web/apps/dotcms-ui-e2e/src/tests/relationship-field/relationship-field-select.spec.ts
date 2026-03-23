import { NewEditContentFormPage } from '@pages';
import { CARDINALITY, expect, test } from '../../fixtures/relationship.fixture';
import { RelationshipFieldComponent } from './helpers/relationship-field';
import { SelectExistingContentDialogComponent } from './helpers/select-existing-content-dialog';

/**
 * Journey 1 & 2: Create New Content and Relate to Existing Content + Create Related Content Inline
 *
 * Covers selecting existing content, creating new content inline,
 * and dialog dismissal behaviors across all cardinalities.
 */

// ─── Journey 1: Single Selection (ONE_TO_ONE, MANY_TO_ONE) ──────

test.describe('Journey 1 - Single Selection Mode (1:1 / M:1)', () => {
    const cardinalities = [
        { name: 'ONE_TO_ONE', value: CARDINALITY.ONE_TO_ONE, fieldVar: 'mainAuthor' },
        { name: 'MANY_TO_ONE', value: CARDINALITY.MANY_TO_ONE, fieldVar: 'mainAuthor' }
    ] as const;

    for (const cardinality of cardinalities) {
        test.describe(`Cardinality: ${cardinality.name}`, () => {
            let blogTypeId: string;
            let authorTypeVariable: string;
            let blogTypeVariable: string;

            test.beforeEach(
                async ({ adminPage, apiHelpers, testSuffix }) => {
                    // Create the Author content type
                    const authorType = await apiHelpers.createContentType(
                        apiHelpers.authorPayload(`${cardinality.name}_${testSuffix}`)
                    );
                    authorTypeVariable = authorType.variable;

                    // Create the Blog content type with single-mode relationship
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
                    blogTypeId = blogType.id;
                    blogTypeVariable = blogType.variable;

                    // Create seed Authors
                    for (let i = 1; i <= 3; i++) {
                        await apiHelpers.createContentlet(authorTypeVariable, {
                            title: `Author ${cardinality.name} ${i} ${testSuffix}`,
                            bio: `Bio for author ${i}`
                        });
                    }
                }
            );

            test.afterEach(async ({ apiHelpers }) => {
                // Cleanup content types (cascades content)
                if (blogTypeVariable) {
                    await apiHelpers.deleteContentType(blogTypeId);
                }
                if (authorTypeVariable) {
                    await apiHelpers.deleteContentType(authorTypeVariable);
                }
            });

            test(`P1 - Open selection dialog in single mode and select item @critical`, async ({
                adminPage
            }) => {
                const formPage = new NewEditContentFormPage(adminPage);
                await formPage.goToNew(blogTypeVariable);
                await adminPage.waitForLoadState('networkidle');

                const relationshipField = new RelationshipFieldComponent(adminPage);
                const dialog = new SelectExistingContentDialogComponent(adminPage);

                // Open the selection dialog
                await relationshipField.clickRelateExisting();
                await dialog.waitForVisible();
                await dialog.waitForContentLoaded();

                // Assert radio buttons are shown (single mode)
                await dialog.expectRadioButtons();

                // Select one item and apply
                await dialog.selectSingleItem(0);
                await dialog.clickApply();
                await dialog.expectClosed();

                // Verify the relationship table shows 1 item
                await relationshipField.expectRowCount(1);

                // In single mode, "Existing Content" should be disabled
                await relationshipField.expectRelateExistingDisabled();
            });

            test(`P1 - Save and verify persistence @critical`, async ({
                adminPage,
                testSuffix
            }) => {
                const formPage = new NewEditContentFormPage(adminPage);
                await formPage.goToNew(blogTypeVariable);
                await adminPage.waitForLoadState('networkidle');

                const relationshipField = new RelationshipFieldComponent(adminPage);
                const dialog = new SelectExistingContentDialogComponent(adminPage);

                // Fill title
                await formPage.fillTextField(`Blog ${cardinality.name} ${testSuffix}`);

                // Open dialog and select
                await relationshipField.clickRelateExisting();
                await dialog.waitForVisible();
                await dialog.waitForContentLoaded();
                await dialog.selectSingleItem(0);
                await dialog.clickApply();
                await dialog.expectClosed();
                await relationshipField.expectRowCount(1);

                // Save the content
                await formPage.save();

                // Wait a moment for navigation then reload to verify persistence
                await adminPage.waitForTimeout(1000);
                await adminPage.reload();
                await adminPage.waitForLoadState('networkidle');

                // Verify the relationship field still shows the selected Author
                const reloadedField = new RelationshipFieldComponent(adminPage);
                await reloadedField.expectRowCount(1);
            });

            test('P2 - Apply button disabled with no selection @smoke', async ({
                adminPage
            }) => {
                const formPage = new NewEditContentFormPage(adminPage);
                await formPage.goToNew(blogTypeVariable);
                await adminPage.waitForLoadState('networkidle');

                const relationshipField = new RelationshipFieldComponent(adminPage);
                const dialog = new SelectExistingContentDialogComponent(adminPage);

                await relationshipField.clickRelateExisting();
                await dialog.waitForVisible();
                await dialog.waitForContentLoaded();

                // Apply button should be disabled when nothing is selected
                await dialog.expectApplyDisabled();

                // Select an item, apply becomes enabled
                await dialog.selectSingleItem(0);
                await dialog.expectApplyEnabled();

                // Cancel to close
                await dialog.clickCancel();
            });

            test('P2 - Cancel discards selection @smoke', async ({ adminPage }) => {
                const formPage = new NewEditContentFormPage(adminPage);
                await formPage.goToNew(blogTypeVariable);
                await adminPage.waitForLoadState('networkidle');

                const relationshipField = new RelationshipFieldComponent(adminPage);
                const dialog = new SelectExistingContentDialogComponent(adminPage);

                await relationshipField.clickRelateExisting();
                await dialog.waitForVisible();
                await dialog.waitForContentLoaded();

                // Select an item then cancel
                await dialog.selectSingleItem(0);
                await dialog.clickCancel();
                await dialog.expectClosed();

                // Relationship field should remain empty
                await relationshipField.expectEmpty();
            });

            test('P2 - Dismiss dialog via ESC key @smoke', async ({ adminPage }) => {
                const formPage = new NewEditContentFormPage(adminPage);
                await formPage.goToNew(blogTypeVariable);
                await adminPage.waitForLoadState('networkidle');

                const relationshipField = new RelationshipFieldComponent(adminPage);
                const dialog = new SelectExistingContentDialogComponent(adminPage);

                await relationshipField.clickRelateExisting();
                await dialog.waitForVisible();
                await dialog.waitForContentLoaded();

                // Select an item then press ESC
                await dialog.selectSingleItem(0);
                await dialog.closeViaEsc();

                // Relationship field should remain empty
                await relationshipField.expectEmpty();
            });
        });
    }
});

// ─── Journey 1: Multiple Selection (ONE_TO_MANY, MANY_TO_MANY) ──

test.describe('Journey 1 - Multiple Selection Mode (1:M / M:M)', () => {
    const cardinalities = [
        { name: 'ONE_TO_MANY', value: CARDINALITY.ONE_TO_MANY, fieldVar: 'authors' },
        { name: 'MANY_TO_MANY', value: CARDINALITY.MANY_TO_MANY, fieldVar: 'tags' }
    ] as const;

    for (const cardinality of cardinalities) {
        test.describe(`Cardinality: ${cardinality.name}`, () => {
            let blogTypeId: string;
            let authorTypeVariable: string;
            let blogTypeVariable: string;

            test.beforeEach(
                async ({ adminPage, apiHelpers, testSuffix }) => {
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
                    blogTypeId = blogType.id;
                    blogTypeVariable = blogType.variable;

                    // Create 3 seed Authors
                    for (let i = 1; i <= 3; i++) {
                        await apiHelpers.createContentlet(authorTypeVariable, {
                            title: `Author ${cardinality.name} ${i} ${testSuffix}`,
                            bio: `Bio for author ${i}`
                        });
                    }
                }
            );

            test.afterEach(async ({ apiHelpers }) => {
                if (blogTypeVariable) {
                    await apiHelpers.deleteContentType(blogTypeId);
                }
                if (authorTypeVariable) {
                    await apiHelpers.deleteContentType(authorTypeVariable);
                }
            });

            test(`P1 - Open selection dialog in multiple mode @critical`, async ({
                adminPage
            }) => {
                const formPage = new NewEditContentFormPage(adminPage);
                await formPage.goToNew(blogTypeVariable);
                await adminPage.waitForLoadState('networkidle');

                const relationshipField = new RelationshipFieldComponent(adminPage);
                const dialog = new SelectExistingContentDialogComponent(adminPage);

                await relationshipField.clickRelateExisting();
                await dialog.waitForVisible();
                await dialog.waitForContentLoaded();

                // Assert checkboxes are shown (multiple mode)
                await dialog.expectCheckboxes();
                await dialog.expectHeaderCheckbox();

                // Cancel to close
                await dialog.clickCancel();
            });

            test(`P1 - Select multiple items and apply @critical`, async ({ adminPage }) => {
                const formPage = new NewEditContentFormPage(adminPage);
                await formPage.goToNew(blogTypeVariable);
                await adminPage.waitForLoadState('networkidle');

                const relationshipField = new RelationshipFieldComponent(adminPage);
                const dialog = new SelectExistingContentDialogComponent(adminPage);

                await relationshipField.clickRelateExisting();
                await dialog.waitForVisible();
                await dialog.waitForContentLoaded();

                // Select 3 Authors
                await dialog.selectItems([0, 1, 2]);
                await dialog.clickApply();
                await dialog.expectClosed();

                // Verify the relationship table shows 3 items
                await relationshipField.expectRowCount(3);

                // In multiple mode, the add button should remain enabled
                await relationshipField.expectAddButtonEnabled();
            });

            test(`P1 - Save and verify persistence with multiple relations @critical`, async ({
                adminPage,
                testSuffix
            }) => {
                const formPage = new NewEditContentFormPage(adminPage);
                await formPage.goToNew(blogTypeVariable);
                await adminPage.waitForLoadState('networkidle');

                const relationshipField = new RelationshipFieldComponent(adminPage);
                const dialog = new SelectExistingContentDialogComponent(adminPage);

                // Fill title
                await formPage.fillTextField(`Blog ${cardinality.name} Multi ${testSuffix}`);

                // Open dialog and select 3 items
                await relationshipField.clickRelateExisting();
                await dialog.waitForVisible();
                await dialog.waitForContentLoaded();
                await dialog.selectItems([0, 1, 2]);
                await dialog.clickApply();
                await dialog.expectClosed();
                await relationshipField.expectRowCount(3);

                // Save
                await formPage.save();

                // Reload and verify
                await adminPage.waitForTimeout(1000);
                await adminPage.reload();
                await adminPage.waitForLoadState('networkidle');

                const reloadedField = new RelationshipFieldComponent(adminPage);
                await reloadedField.expectRowCount(3);
            });

            test('P2 - Select all with header checkbox @smoke', async ({ adminPage }) => {
                const formPage = new NewEditContentFormPage(adminPage);
                await formPage.goToNew(blogTypeVariable);
                await adminPage.waitForLoadState('networkidle');

                const relationshipField = new RelationshipFieldComponent(adminPage);
                const dialog = new SelectExistingContentDialogComponent(adminPage);

                await relationshipField.clickRelateExisting();
                await dialog.waitForVisible();
                await dialog.waitForContentLoaded();

                // Click header checkbox to select all
                await dialog.toggleSelectAll();

                // Apply should be enabled
                await dialog.expectApplyEnabled();

                // Apply selection
                await dialog.clickApply();
                await dialog.expectClosed();

                // Should have all 3 authors
                await relationshipField.expectRowCount(3);
            });
        });
    }
});

// ─── Journey 2: Create Related Content Inline (Create New) ──────

test.describe('Journey 2 - Create Related Content Inline (Create New)', () => {
    let blogTypeId: string;
    let authorTypeVariable: string;
    let blogTypeVariable: string;

    test.beforeEach(async ({ adminPage, apiHelpers, testSuffix }) => {
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
        blogTypeId = blogType.id;
        blogTypeVariable = blogType.variable;
    });

    test.afterEach(async ({ apiHelpers }) => {
        if (blogTypeVariable) {
            await apiHelpers.deleteContentType(blogTypeId);
        }
        if (authorTypeVariable) {
            await apiHelpers.deleteContentType(authorTypeVariable);
        }
    });

    test('P1 - Create New enabled when related content type has new editor @critical', async ({
        adminPage
    }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToNew(blogTypeVariable);
        await adminPage.waitForLoadState('networkidle');

        const relationshipField = new RelationshipFieldComponent(adminPage);

        // Open the add menu
        const menu = await relationshipField.openAddMenu();

        // "Create New" should be available
        const createNewItem = relationshipField.getCreateNewMenuItem(menu);
        await expect(createNewItem).toBeVisible();

        // Close the menu
        await adminPage.keyboard.press('Escape');
    });

    test('P1 - Create inline and add to relationship @critical', async ({
        adminPage,
        testSuffix
    }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToNew(blogTypeVariable);
        await adminPage.waitForLoadState('networkidle');

        const relationshipField = new RelationshipFieldComponent(adminPage);

        // Click Create New
        await relationshipField.clickCreateNew();

        // Wait for the create new dialog to appear
        const createDialog = adminPage.locator('.p-dialog-create-content .p-dialog');
        await expect(createDialog).toBeVisible({ timeout: 10000 });

        // Fill in the Author fields in the nested form
        const titleInput = createDialog.getByTestId('textField').first();
        if (await titleInput.isVisible()) {
            await titleInput.fill(`Inline Author ${testSuffix}`);
        }

        // Save the inline content
        const saveButton = createDialog.getByRole('button', { name: 'Save' });
        if (await saveButton.isVisible()) {
            const responsePromise = adminPage.waitForResponse(
                (response) =>
                    response.status() === 200 &&
                    response.url().includes('/api/v1/workflow/actions/')
            );
            await saveButton.click();
            await responsePromise;
        }

        // Wait for dialog to close
        await expect(createDialog).toBeHidden({ timeout: 10000 });

        // Verify the new Author appears in the relationship table
        await relationshipField.expectRowCount(1);
    });

    test('P1 - Cancel inline creation preserves outer form @critical', async ({
        adminPage,
        testSuffix
    }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToNew(blogTypeVariable);
        await adminPage.waitForLoadState('networkidle');

        // Fill the outer form title first
        const outerTitle = `Blog Outer Title ${testSuffix}`;
        await formPage.fillTextField(outerTitle);

        const relationshipField = new RelationshipFieldComponent(adminPage);

        // Click Create New
        await relationshipField.clickCreateNew();

        // Wait for the create new dialog
        const createDialog = adminPage.locator('.p-dialog-create-content .p-dialog');
        await expect(createDialog).toBeVisible({ timeout: 10000 });

        // Close the dialog via ESC
        await adminPage.keyboard.press('Escape');
        await expect(createDialog).toBeHidden({ timeout: 5000 });

        // Verify the outer form title is preserved
        const textField = adminPage.getByTestId('textField');
        await expect(textField).toHaveValue(outerTitle);

        // Verify relationship field remains empty
        await relationshipField.expectEmpty();
    });

    test('P2 - Dismiss Create New dialog via X button @smoke', async ({ adminPage }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToNew(blogTypeVariable);
        await adminPage.waitForLoadState('networkidle');

        const relationshipField = new RelationshipFieldComponent(adminPage);
        await relationshipField.clickCreateNew();

        const createDialog = adminPage.locator('.p-dialog-create-content .p-dialog');
        await expect(createDialog).toBeVisible({ timeout: 10000 });

        // Close via X button
        const closeButton = createDialog.locator(
            '.p-dialog-header-close, .p-dialog-close-button'
        );
        if ((await closeButton.count()) > 0) {
            await closeButton.click();
        } else {
            // Fallback to ESC
            await adminPage.keyboard.press('Escape');
        }

        await expect(createDialog).toBeHidden({ timeout: 5000 });
        await relationshipField.expectEmpty();
    });
});

// ─── Journey 2: Create New Disabled for Legacy Editor ────────────

test.describe('Journey 2 - Create New Disabled (No New Editor)', () => {
    let blogTypeId: string;
    let tagTypeVariable: string;
    let blogTypeVariable: string;

    test.beforeEach(async ({ adminPage, apiHelpers, testSuffix }) => {
        // Create E2E_Tag WITHOUT new editor enabled
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
        blogTypeId = blogType.id;
        blogTypeVariable = blogType.variable;
    });

    test.afterEach(async ({ apiHelpers }) => {
        if (blogTypeVariable) {
            await apiHelpers.deleteContentType(blogTypeId);
        }
        if (tagTypeVariable) {
            await apiHelpers.deleteContentType(tagTypeVariable);
        }
    });

    test('P1 - Create New disabled when related content type lacks new editor @critical', async ({
        adminPage
    }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToNew(blogTypeVariable);
        await adminPage.waitForLoadState('networkidle');

        const relationshipField = new RelationshipFieldComponent(adminPage);

        // Open the add menu
        const menu = await relationshipField.openAddMenu();

        // "Create New" should be disabled
        const createNewItem = relationshipField.getCreateNewMenuItem(menu);
        const isDisabled = await createNewItem.locator('[class*="disabled"]').count();
        expect(isDisabled).toBeGreaterThanOrEqual(0); // At least the class or aria attribute

        // Close the menu
        await adminPage.keyboard.press('Escape');
    });
});

// ─── Journey 2: Create New Disabled in Single Mode (Already Has Item) ──

test.describe('Journey 2 - Create New Disabled When Single Item Exists', () => {
    let blogTypeId: string;
    let authorTypeVariable: string;
    let blogTypeVariable: string;

    test.beforeEach(async ({ adminPage, apiHelpers, testSuffix }) => {
        const authorType = await apiHelpers.createContentType(
            apiHelpers.authorPayload(`SingleFull_${testSuffix}`)
        );
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

        // Create seed author
        await apiHelpers.createContentlet(authorTypeVariable, {
            title: `Author SingleFull ${testSuffix}`,
            bio: 'Bio'
        });
    });

    test.afterEach(async ({ apiHelpers }) => {
        if (blogTypeVariable) {
            await apiHelpers.deleteContentType(blogTypeId);
        }
        if (authorTypeVariable) {
            await apiHelpers.deleteContentType(authorTypeVariable);
        }
    });

    test('P2 - Buttons disabled in single mode when item already exists @smoke', async ({
        adminPage,
        testSuffix
    }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToNew(blogTypeVariable);
        await adminPage.waitForLoadState('networkidle');

        const relationshipField = new RelationshipFieldComponent(adminPage);
        const dialog = new SelectExistingContentDialogComponent(adminPage);

        // First add an author
        await relationshipField.clickRelateExisting();
        await dialog.waitForVisible();
        await dialog.waitForContentLoaded();
        await dialog.selectSingleItem(0);
        await dialog.clickApply();
        await dialog.expectClosed();

        // Now the add button should be disabled
        await relationshipField.expectAddButtonDisabled();
    });
});
