import { test, expect } from '../../fixtures/relationship.fixture';
import { NewEditContentFormPage } from '@pages';
import { RelationshipFieldComponent } from './helpers/relationship-field';
import { SelectExistingContentDialogComponent } from './helpers/select-existing-content-dialog';
import { CARDINALITY, TestContentlet } from '../../fixtures/relationship.fixture';

/**
 * Journey 3: Edit Existing Content - Add More Relations
 * Journey 4: Edit Existing Content - Remove Relations
 *
 * Covers loading existing relationships, adding more relations,
 * deleting relations, and verifying persistence across save/reopen.
 */

// ─── Journey 3: Add More Relations ───────────────────────────────

test.describe('Journey 3 - Edit Existing Content: Add More Relations', () => {
    let blogTypeId: string;
    let authorTypeVariable: string;
    let blogTypeVariable: string;
    let blogContentlet: TestContentlet;
    let authors: TestContentlet[];

    test.beforeEach(async ({ apiHelpers, testSuffix }) => {
        // Create Author content type
        const authorType = await apiHelpers.createContentType(
            apiHelpers.authorPayload(`Edit_${testSuffix}`)
        );
        authorTypeVariable = authorType.variable;

        // Create Blog content type with 1:M relationship
        const blogType = await apiHelpers.createContentType(
            apiHelpers.blogPayload(
                `Edit_${testSuffix}`,
                'E2E_Blog_Edit',
                'E2EBlogEdit',
                authorTypeVariable,
                'authors',
                CARDINALITY.ONE_TO_MANY
            )
        );
        blogTypeId = blogType.id;
        blogTypeVariable = blogType.variable;

        // Create 5 Authors
        authors = [];
        for (let i = 1; i <= 5; i++) {
            const author = await apiHelpers.createContentlet(authorTypeVariable, {
                title: `Author Edit ${i} ${testSuffix}`,
                bio: `Bio for edit author ${i}`
            });
            authors.push(author);
        }

        // Create a Blog Post with 2 pre-related Authors
        blogContentlet = await apiHelpers.createContentletWithRelationship(
            blogTypeVariable,
            { title: `Blog Edit Test ${testSuffix}` },
            { authors: `${authors[0].identifier},${authors[1].identifier}` }
        );
    });

    test.afterEach(async ({ apiHelpers }) => {
        if (blogTypeVariable) {
            await apiHelpers.deleteContentType(blogTypeId);
        }
        if (authorTypeVariable) {
            await apiHelpers.deleteContentType(authorTypeVariable);
        }
    });

    test('P1 - Load existing relationships on edit @critical', async ({
        adminPage
    }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToContent(blogContentlet.inode);
        await adminPage.waitForLoadState('networkidle');

        const relationshipField = new RelationshipFieldComponent(adminPage);
        await relationshipField.expectRowCount(2);
    });

    test('P1 - Add more relations to existing content @critical', async ({
        adminPage
    }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToContent(blogContentlet.inode);
        await adminPage.waitForLoadState('networkidle');

        const relationshipField = new RelationshipFieldComponent(adminPage);
        const dialog = new SelectExistingContentDialogComponent(adminPage);

        // Verify initial state: 2 authors
        await relationshipField.expectRowCount(2);

        // Add 1 more author
        await relationshipField.clickRelateExisting();
        await dialog.waitForVisible();
        await dialog.waitForContentLoaded();

        // Select the first available (non-already-related) item
        await dialog.selectItem(0);
        await dialog.clickApply();
        await dialog.expectClosed();

        // Should now show 3 authors
        await relationshipField.expectRowCount(3);

        // Save and verify persistence
        await formPage.save();
        await adminPage.waitForTimeout(1000);
        await adminPage.reload();
        await adminPage.waitForLoadState('networkidle');

        const reloadedField = new RelationshipFieldComponent(adminPage);
        await reloadedField.expectRowCount(3);
    });

    test('P3 - Thumbnail rendering in relationship table', async ({
        adminPage
    }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToContent(blogContentlet.inode);
        await adminPage.waitForLoadState('networkidle');

        // Thumbnails render via dot-contentlet-thumbnail component
        const thumbnails = adminPage.getByTestId('contentlet-thumbnail');
        const count = await thumbnails.count();
        // At least 2 thumbnails for the 2 related authors
        expect(count).toBeGreaterThanOrEqual(2);
    });
});

// ─── Journey 4: Remove Relations ─────────────────────────────────

test.describe('Journey 4 - Edit Existing Content: Remove Relations', () => {
    let blogTypeId: string;
    let authorTypeVariable: string;
    let blogTypeVariable: string;
    let blogContentlet: TestContentlet;
    let authors: TestContentlet[];

    test.beforeEach(async ({ apiHelpers, testSuffix }) => {
        const authorType = await apiHelpers.createContentType(
            apiHelpers.authorPayload(`Delete_${testSuffix}`)
        );
        authorTypeVariable = authorType.variable;

        const blogType = await apiHelpers.createContentType(
            apiHelpers.blogPayload(
                `Delete_${testSuffix}`,
                'E2E_Blog_Delete',
                'E2EBlogDelete',
                authorTypeVariable,
                'authors',
                CARDINALITY.ONE_TO_MANY
            )
        );
        blogTypeId = blogType.id;
        blogTypeVariable = blogType.variable;

        // Create 3 Authors
        authors = [];
        for (let i = 1; i <= 3; i++) {
            const author = await apiHelpers.createContentlet(authorTypeVariable, {
                title: `Author Delete ${i} ${testSuffix}`,
                bio: `Bio for delete author ${i}`
            });
            authors.push(author);
        }

        // Create Blog with 3 related Authors
        blogContentlet = await apiHelpers.createContentletWithRelationship(
            blogTypeVariable,
            { title: `Blog Delete Test ${testSuffix}` },
            {
                authors: authors.map((a) => a.identifier).join(',')
            }
        );
    });

    test.afterEach(async ({ apiHelpers }) => {
        if (blogTypeVariable) {
            await apiHelpers.deleteContentType(blogTypeId);
        }
        if (authorTypeVariable) {
            await apiHelpers.deleteContentType(authorTypeVariable);
        }
    });

    test('P1 - Delete a single relation @critical', async ({ adminPage }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToContent(blogContentlet.inode);
        await adminPage.waitForLoadState('networkidle');

        const relationshipField = new RelationshipFieldComponent(adminPage);

        // Verify initial: 3 authors
        await relationshipField.expectRowCount(3);

        // Delete the first author
        await relationshipField.deleteRow(0);

        // Should now show 2
        await relationshipField.expectRowCount(2);
    });

    test('P1 - Delete and verify persistence @critical', async ({ adminPage }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToContent(blogContentlet.inode);
        await adminPage.waitForLoadState('networkidle');

        const relationshipField = new RelationshipFieldComponent(adminPage);
        await relationshipField.expectRowCount(3);

        // Delete one
        await relationshipField.deleteRow(0);
        await relationshipField.expectRowCount(2);

        // Save
        await formPage.save();
        await adminPage.waitForTimeout(1000);
        await adminPage.reload();
        await adminPage.waitForLoadState('networkidle');

        const reloadedField = new RelationshipFieldComponent(adminPage);
        await reloadedField.expectRowCount(2);
    });

    test('P2 - Delete all items @smoke', async ({ adminPage }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToContent(blogContentlet.inode);
        await adminPage.waitForLoadState('networkidle');

        const relationshipField = new RelationshipFieldComponent(adminPage);
        await relationshipField.expectRowCount(3);

        // Delete all 3 items (always delete the first since list shifts)
        await relationshipField.deleteRow(0);
        await relationshipField.deleteRow(0);
        await relationshipField.deleteRow(0);

        // Table should be empty
        await relationshipField.expectEmpty();

        // Add button should be enabled again
        await relationshipField.expectAddButtonEnabled();
    });
});

// ─── Journey 4: Delete and Re-add in Single Mode ────────────────

test.describe('Journey 4 - Delete in Single Mode and Re-add', () => {
    let blogTypeId: string;
    let authorTypeVariable: string;
    let blogTypeVariable: string;
    let blogContentlet: TestContentlet;
    let authors: TestContentlet[];

    test.beforeEach(async ({ apiHelpers, testSuffix }) => {
        const authorType = await apiHelpers.createContentType(
            apiHelpers.authorPayload(`SingleDel_${testSuffix}`)
        );
        authorTypeVariable = authorType.variable;

        const blogType = await apiHelpers.createContentType(
            apiHelpers.blogPayload(
                `SingleDel_${testSuffix}`,
                'E2E_Blog_SingleDel',
                'E2EBlogSingleDel',
                authorTypeVariable,
                'mainAuthor',
                CARDINALITY.ONE_TO_ONE
            )
        );
        blogTypeId = blogType.id;
        blogTypeVariable = blogType.variable;

        // Create 2 Authors
        authors = [];
        for (let i = 1; i <= 2; i++) {
            const author = await apiHelpers.createContentlet(authorTypeVariable, {
                title: `Author SingleDel ${i} ${testSuffix}`,
                bio: `Bio ${i}`
            });
            authors.push(author);
        }

        // Create Blog with 1 related Author
        blogContentlet = await apiHelpers.createContentletWithRelationship(
            blogTypeVariable,
            { title: `Blog SingleDel Test ${testSuffix}` },
            { mainAuthor: authors[0].identifier }
        );
    });

    test.afterEach(async ({ apiHelpers }) => {
        if (blogTypeVariable) {
            await apiHelpers.deleteContentType(blogTypeId);
        }
        if (authorTypeVariable) {
            await apiHelpers.deleteContentType(authorTypeVariable);
        }
    });

    test('P3 - Delete in single mode and re-add', async ({ adminPage }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToContent(blogContentlet.inode);
        await adminPage.waitForLoadState('networkidle');

        const relationshipField = new RelationshipFieldComponent(adminPage);
        const dialog = new SelectExistingContentDialogComponent(adminPage);

        // Verify 1 author
        await relationshipField.expectRowCount(1);
        await relationshipField.expectAddButtonDisabled();

        // Delete the author
        await relationshipField.deleteRow(0);
        await relationshipField.expectEmpty();
        await relationshipField.expectAddButtonEnabled();

        // Add a different author
        await relationshipField.clickRelateExisting();
        await dialog.waitForVisible();
        await dialog.waitForContentLoaded();
        await dialog.selectSingleItem(0);
        await dialog.clickApply();
        await dialog.expectClosed();

        await relationshipField.expectRowCount(1);
    });
});
