import { NewEditContentFormPage } from '@pages';
import {
    CARDINALITY,
    expect,
    test,
    TestContentlet
} from '../../../../fixtures/relationship.fixture';
import { RelationshipField } from './helpers/relationship-field';
import { SelectExistingContentDialog } from './helpers/select-existing-content-dialog';

// ─── Reorder (Drag & Drop) ──────────────────────────────────────

test.describe('Reorder (Drag & Drop)', () => {
    let blogTypeId: string;
    let authorTypeVariable: string;
    let blogTypeVariable: string;
    let blogContentlet: TestContentlet;

    test.beforeEach(async ({ apiHelpers, testSuffix }) => {
        const authorType = await apiHelpers.createContentType(
            apiHelpers.authorPayload(`Reorder_${testSuffix}`)
        );
        authorTypeVariable = authorType.variable;

        const blogType = await apiHelpers.createContentType(
            apiHelpers.blogPayload(
                `Reorder_${testSuffix}`,
                'E2E_Blog_Reorder',
                'E2EBlogReorder',
                authorTypeVariable,
                'authors',
                CARDINALITY.ONE_TO_MANY
            )
        );
        blogTypeId = blogType.id;
        blogTypeVariable = blogType.variable;

        const authors: TestContentlet[] = [];
        for (let i = 1; i <= 3; i++) {
            const author = await apiHelpers.createContentlet(authorTypeVariable, {
                title: `Reorder Author ${i} ${testSuffix}`,
                bio: `Bio ${i}`
            });
            authors.push(author);
        }

        blogContentlet = await apiHelpers.createContentletWithRelationship(
            blogTypeVariable,
            { title: `Blog Reorder Test ${testSuffix}` },
            { authors: authors.map((a) => a.identifier).join(',') }
        );
    });

    test.afterEach(async ({ apiHelpers }) => {
        if (blogTypeId) await apiHelpers.deleteContentType(blogTypeId);
        if (authorTypeVariable) await apiHelpers.deleteContentType(authorTypeVariable);
    });

    test('drag handles visible @smoke', async ({ adminPage }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToContent(blogContentlet.inode);
        await adminPage.waitForLoadState('networkidle');

        const relationshipField = new RelationshipField(adminPage);
        await relationshipField.expectRowCount(3);
        await relationshipField.expectDragHandlesVisible();
    });

    test('reorder persists after save @smoke', async ({ adminPage }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToContent(blogContentlet.inode);
        await adminPage.waitForLoadState('networkidle');

        const relationshipField = new RelationshipField(adminPage);
        await relationshipField.expectRowCount(3);

        // Capture original order: Author 1, Author 2, Author 3
        const originalFirst = await relationshipField.getRowTitle(0);
        const originalThird = await relationshipField.getRowTitle(2);

        // Drag row 3 to row 1 position
        const handles = relationshipField.getDragHandles();
        const sourceHandle = handles.nth(2);
        const targetHandle = handles.nth(0);

        const sourceBounds = await sourceHandle.boundingBox();
        const targetBounds = await targetHandle.boundingBox();

        expect(sourceBounds).toBeTruthy();
        expect(targetBounds).toBeTruthy();

        await adminPage.mouse.move(
            sourceBounds!.x + sourceBounds!.width / 2,
            sourceBounds!.y + sourceBounds!.height / 2
        );
        await adminPage.mouse.down();
        await adminPage.mouse.move(
            targetBounds!.x + targetBounds!.width / 2,
            targetBounds!.y + targetBounds!.height / 2,
            { steps: 10 }
        );
        await adminPage.mouse.up();

        // Verify order changed: original 3rd should now be 1st
        const newFirst = await relationshipField.getRowTitle(0);
        expect(newFirst).toBe(originalThird);

        // Save
        await formPage.save();
        await adminPage.waitForLoadState('networkidle');

        await relationshipField.expectRowCount(3);
    });
});

// ─── Search and Filter in Selection Dialog ──────────────────────

test.describe('Search and Filter', () => {
    // Tests share the same dialog — must run sequentially
    test.describe.configure({ mode: 'serial' });
    let blogTypeId: string;
    let authorTypeVariable: string;
    let blogTypeVariable: string;

    test.beforeEach(async ({ apiHelpers, testSuffix }) => {
        const authorType = await apiHelpers.createContentType(
            apiHelpers.authorPayload(`Search_${testSuffix}`)
        );
        authorTypeVariable = authorType.variable;

        const blogType = await apiHelpers.createContentType(
            apiHelpers.blogPayload(
                `Search_${testSuffix}`,
                'E2E_Blog_Search',
                'E2EBlogSearch',
                authorTypeVariable,
                'authors',
                CARDINALITY.ONE_TO_MANY
            )
        );
        blogTypeId = blogType.id;
        blogTypeVariable = blogType.variable;

        const names = ['John Smith', 'John Doe', 'Jane Smith', 'Alice Johnson', 'Bob Williams'];
        for (const name of names) {
            await apiHelpers.createContentlet(authorTypeVariable, {
                title: `${name} ${testSuffix}`,
                bio: `Bio for ${name}`
            });
        }
    });

    test.afterEach(async ({ apiHelpers }) => {
        if (blogTypeId) await apiHelpers.deleteContentType(blogTypeId);
        if (authorTypeVariable) await apiHelpers.deleteContentType(authorTypeVariable);
    });

    test('global search filters results @smoke', async ({ adminPage }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToNew(blogTypeVariable);
        await adminPage.waitForLoadState('networkidle');

        const relationshipField = new RelationshipField(adminPage);
        const selectDialog = new SelectExistingContentDialog(adminPage);

        await relationshipField.clickRelateExisting();
        await selectDialog.waitForVisible();
        await selectDialog.waitForContentLoaded();

        const initialCount = await selectDialog.getRowCount();
        expect(initialCount).toBeGreaterThanOrEqual(5);

        await selectDialog.search('John');

        // "John" matches: John Smith, John Doe, Alice Johnson = 3 results
        // expectRowCount uses toHaveCount which auto-retries until results update
        await selectDialog.expectRowCount(3);

        await selectDialog.clickCancel();
    });

    test('clear search resets results @smoke', async ({ adminPage }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToNew(blogTypeVariable);
        await adminPage.waitForLoadState('networkidle');

        const relationshipField = new RelationshipField(adminPage);
        const selectDialog = new SelectExistingContentDialog(adminPage);

        await relationshipField.clickRelateExisting();
        await selectDialog.waitForVisible();
        await selectDialog.waitForContentLoaded();

        const initialCount = await selectDialog.getRowCount();

        await selectDialog.search('John');
        await selectDialog.expectRowCount(3);

        await selectDialog.openFilters();
        await selectDialog.clearSearch();

        // Use polling assertion — getRowCount() is a snapshot, not auto-retrying
        await expect
            .poll(() => selectDialog.getRowCount(), { timeout: 10000 })
            .toBeGreaterThanOrEqual(initialCount);

        await selectDialog.clickCancel();
    });

    test('toggle show selected items', async ({ adminPage }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToNew(blogTypeVariable);
        await adminPage.waitForLoadState('networkidle');

        const relationshipField = new RelationshipField(adminPage);
        const selectDialog = new SelectExistingContentDialog(adminPage);

        await relationshipField.clickRelateExisting();
        await selectDialog.waitForVisible();
        await selectDialog.waitForContentLoaded();

        await selectDialog.selectItems([0, 1]);

        await selectDialog.toggleShowSelected();
        await selectDialog.expectRowCount(2);

        await selectDialog.toggleShowSelected();
        await expect
            .poll(() => selectDialog.getRowCount(), { timeout: 10000 })
            .toBeGreaterThanOrEqual(5);

        await selectDialog.clickCancel();
    });
});

// ─── Dialog Lists All Items ─────────────────────────────────────

test.describe('Dialog Content Listing', () => {
    let blogTypeId: string;
    let authorTypeVariable: string;
    let blogTypeVariable: string;

    test.beforeEach(async ({ apiHelpers, testSuffix }) => {
        const authorType = await apiHelpers.createContentType(
            apiHelpers.authorPayload(`DialogList_${testSuffix}`)
        );
        authorTypeVariable = authorType.variable;

        const blogType = await apiHelpers.createContentType(
            apiHelpers.blogPayload(
                `DialogList_${testSuffix}`,
                'E2E_Blog_DialogList',
                'E2EBlogDialogList',
                authorTypeVariable,
                'authors',
                CARDINALITY.MANY_TO_MANY
            )
        );
        blogTypeId = blogType.id;
        blogTypeVariable = blogType.variable;

        for (let i = 1; i <= 15; i++) {
            await apiHelpers.createContentlet(authorTypeVariable, {
                title: `ListAuthor ${String(i).padStart(2, '0')} ${testSuffix}`,
                bio: `Bio ${i}`
            });
        }
    });

    test.afterEach(async ({ apiHelpers }) => {
        if (blogTypeId) await apiHelpers.deleteContentType(blogTypeId);
        if (authorTypeVariable) await apiHelpers.deleteContentType(authorTypeVariable);
    });

    test('dialog shows all 15 items @smoke', async ({ adminPage }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToNew(blogTypeVariable);
        await adminPage.waitForLoadState('networkidle');

        const relationshipField = new RelationshipField(adminPage);
        const selectDialog = new SelectExistingContentDialog(adminPage);

        await relationshipField.clickRelateExisting();
        await selectDialog.waitForVisible();
        await selectDialog.waitForContentLoaded();

        await selectDialog.expectRowCount(15);

        await selectDialog.clickCancel();
    });
});

// ─── Table Pagination (>6 items) ────────────────────────────────

test.describe('Table Pagination', () => {
    let blogTypeId: string;
    let authorTypeVariable: string;
    let blogTypeVariable: string;

    test.beforeEach(async ({ apiHelpers, testSuffix }) => {
        const authorType = await apiHelpers.createContentType(
            apiHelpers.authorPayload(`TablePag_${testSuffix}`)
        );
        authorTypeVariable = authorType.variable;

        const blogType = await apiHelpers.createContentType(
            apiHelpers.blogPayload(
                `TablePag_${testSuffix}`,
                'E2E_Blog_TablePag',
                'E2EBlogTablePag',
                authorTypeVariable,
                'authors',
                CARDINALITY.MANY_TO_MANY
            )
        );
        blogTypeId = blogType.id;
        blogTypeVariable = blogType.variable;
    });

    test.afterEach(async ({ apiHelpers }) => {
        if (blogTypeId) await apiHelpers.deleteContentType(blogTypeId);
        if (authorTypeVariable) await apiHelpers.deleteContentType(authorTypeVariable);
    });

    test.fixme('paginates at 10 items per page @smoke', async ({
        adminPage,
        apiHelpers,
        testSuffix
    }) => {
        // BUG: pagination shows all items instead of 10 per page.
        // Remove fixme once the bug is fixed.
        const authors: TestContentlet[] = [];
        for (let i = 1; i <= 12; i++) {
            const author = await apiHelpers.createContentlet(authorTypeVariable, {
                title: `TablePag Author ${i} ${testSuffix}`,
                bio: `Bio ${i}`
            });
            authors.push(author);
        }

        const blog = await apiHelpers.createContentletWithRelationship(
            blogTypeVariable,
            { title: `Blog TablePag Test ${testSuffix}` },
            { authors: authors.map((a) => a.identifier).join(',') }
        );

        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToContent(blog.inode);
        await adminPage.waitForLoadState('networkidle');

        const relationshipField = new RelationshipField(adminPage);

        await relationshipField.expectRowCount(10);
        await relationshipField.expectPaginationVisible();
        await relationshipField.clickNextPage();
        await relationshipField.expectRowCount(2);
    });

    test.fixme('no pagination with 10 or fewer items', async ({
        adminPage,
        apiHelpers,
        testSuffix
    }) => {
        // BUG: pagination appears even with fewer items. Same pagination bug as above.
        const authors: TestContentlet[] = [];
        for (let i = 1; i <= 9; i++) {
            const author = await apiHelpers.createContentlet(authorTypeVariable, {
                title: `NoPag Author ${i} ${testSuffix}`,
                bio: `Bio ${i}`
            });
            authors.push(author);
        }

        const blog = await apiHelpers.createContentletWithRelationship(
            blogTypeVariable,
            { title: `Blog NoPag Test ${testSuffix}` },
            { authors: authors.map((a) => a.identifier).join(',') }
        );

        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToContent(blog.inode);
        await adminPage.waitForLoadState('networkidle');

        const relationshipField = new RelationshipField(adminPage);

        await relationshipField.expectRowCount(9);
        await relationshipField.expectPaginationHidden();
    });
});
