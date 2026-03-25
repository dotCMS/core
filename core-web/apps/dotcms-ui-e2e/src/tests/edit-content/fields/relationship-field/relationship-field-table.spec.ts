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

        const authors = await apiHelpers.createContentlets(authorTypeVariable, [
            { title: `Reorder Author 1 ${testSuffix}`, bio: 'Bio 1' },
            { title: `Reorder Author 2 ${testSuffix}`, bio: 'Bio 2' },
            { title: `Reorder Author 3 ${testSuffix}`, bio: 'Bio 3' }
        ]);

        blogContentlet = await apiHelpers.createContentletWithRelationship(
            blogTypeVariable,
            { title: `Blog Reorder Test ${testSuffix}` },
            { authors: authors.map((a) => a.identifier).join(',') }
        );
    });

    test('drag handles visible @smoke', async ({ adminPage }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToContent(blogContentlet.inode);

        const relationshipField = new RelationshipField(adminPage);
        await relationshipField.expectRowCount(3);
        await relationshipField.expectDragHandlesVisible();
    });

    test('reorder persists after save @smoke', async ({ adminPage }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToContent(blogContentlet.inode);

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

        await relationshipField.expectRowCount(3);
        const persistedFirst = await relationshipField.getRowTitle(0);
        expect(persistedFirst).toBe(originalThird);
    });
});

// ─── Search and Filter in Selection Dialog ──────────────────────

test.describe('Search and Filter', () => {
    // Serial: limits concurrent API churn on one dotCMS backend; each test creates its own content types.
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
        await apiHelpers.createContentlets(
            authorTypeVariable,
            names.map((name) => ({
                title: `${name} ${testSuffix}`,
                bio: `Bio for ${name}`
            }))
        );
    });

    test('global search filters results @smoke', async ({ adminPage }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToNew(blogTypeVariable);

        const relationshipField = new RelationshipField(adminPage);
        const selectDialog = new SelectExistingContentDialog(adminPage);

        await relationshipField.clickRelateExisting();
        await selectDialog.waitForVisible();
        await selectDialog.waitForContentLoaded();

        await selectDialog.expectRowCountAtLeast(5);

        await selectDialog.search('John');

        // At least John Smith + John Doe; Alice Johnson may or may not match depending on Solr tokenization
        await selectDialog.expectRowCountAtLeast(2);

        await selectDialog.clickCancel();
    });

    test('clear search resets results @smoke', async ({ adminPage }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToNew(blogTypeVariable);

        const relationshipField = new RelationshipField(adminPage);
        const selectDialog = new SelectExistingContentDialog(adminPage);

        await relationshipField.clickRelateExisting();
        await selectDialog.waitForVisible();
        await selectDialog.waitForContentLoaded();

        const initialCount = await selectDialog.waitForRowCountAtLeast(5);

        // Debounced search — fires automatically after 300ms
        await selectDialog.search('John');

        // Verify the filter actually reduced results (not just "at least 2")
        await expect
            .poll(() => selectDialog.rows.count(), { timeout: 10000 })
            .toBeLessThan(initialCount);
        await selectDialog.expectRowCountAtLeast(2);

        // Clear via the filter popover and wait for the table to reload
        await selectDialog.openFilters();
        await selectDialog.clearSearch();
        await selectDialog.waitForContentLoaded();

        await selectDialog.expectRowCountAtLeast(initialCount);

        await selectDialog.clickCancel();
    });

    test('toggle show selected items', async ({ adminPage }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToNew(blogTypeVariable);

        const relationshipField = new RelationshipField(adminPage);
        const selectDialog = new SelectExistingContentDialog(adminPage);

        await relationshipField.clickRelateExisting();
        await selectDialog.waitForVisible();
        await selectDialog.waitForContentLoaded();

        await selectDialog.selectItems([0, 1]);

        await selectDialog.toggleShowSelected();
        await selectDialog.expectRowCount(2);

        await selectDialog.toggleShowSelected();
        await selectDialog.expectRowCountAtLeast(5);

        await selectDialog.clickCancel();
    });
});

// ─── Dialog Lists All Items ─────────────────────────────────────

test.describe('Dialog Content Listing', () => {
    test.slow(); // bulk-creates 15 contentlets — needs extra time on CI

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

        await apiHelpers.createContentlets(
            authorTypeVariable,
            Array.from({ length: 15 }, (_, j) => {
                const i = j + 1;
                return {
                    title: `ListAuthor ${String(i).padStart(2, '0')} ${testSuffix}`,
                    bio: `Bio ${i}`
                };
            })
        );
    });

    test('dialog shows all 15 items @smoke', async ({ adminPage }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToNew(blogTypeVariable);

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

    test.fixme('paginates at 10 items per page @smoke', async ({
        adminPage,
        apiHelpers,
        testSuffix
    }) => {
        // BUG: pagination shows all items instead of 10 per page.
        // Remove fixme once the bug is fixed.
        const authors = await apiHelpers.createContentlets(
            authorTypeVariable,
            Array.from({ length: 12 }, (_, j) => {
                const i = j + 1;
                return {
                    title: `TablePag Author ${i} ${testSuffix}`,
                    bio: `Bio ${i}`
                };
            })
        );

        const blog = await apiHelpers.createContentletWithRelationship(
            blogTypeVariable,
            { title: `Blog TablePag Test ${testSuffix}` },
            { authors: authors.map((a) => a.identifier).join(',') }
        );

        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToContent(blog.inode);

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
        const authors = await apiHelpers.createContentlets(
            authorTypeVariable,
            Array.from({ length: 9 }, (_, j) => {
                const i = j + 1;
                return {
                    title: `NoPag Author ${i} ${testSuffix}`,
                    bio: `Bio ${i}`
                };
            })
        );

        const blog = await apiHelpers.createContentletWithRelationship(
            blogTypeVariable,
            { title: `Blog NoPag Test ${testSuffix}` },
            { authors: authors.map((a) => a.identifier).join(',') }
        );

        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToContent(blog.inode);

        const relationshipField = new RelationshipField(adminPage);

        await relationshipField.expectRowCount(9);
        await relationshipField.expectPaginationHidden();
    });
});
