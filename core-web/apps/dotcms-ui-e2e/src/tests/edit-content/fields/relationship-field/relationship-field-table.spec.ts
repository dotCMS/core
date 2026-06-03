import { NewEditContentFormPage } from '@pages';

import { RelationshipField } from './helpers/relationship-field';
import { SelectExistingContentDialog } from './helpers/select-existing-content-dialog';

import {
    CARDINALITY,
    expect,
    test,
    TestContentlet
} from '../../../../fixtures/relationship.fixture';

// ─── Reorder (Drag & Drop) ──────────────────────────────────────

test.describe('Reorder (Drag & Drop)', () => {
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

        // Capture original order to verify reorder
        const originalThird = await relationshipField.getRowTitle(2);

        // Drag row 3 to row 1 position
        const handles = relationshipField.getDragHandles();
        const sourceHandle = handles.nth(2);
        const targetHandle = handles.nth(0);

        const sourceBounds = await sourceHandle.boundingBox();
        const targetBounds = await targetHandle.boundingBox();

        if (!sourceBounds || !targetBounds) {
            throw new Error('Could not get bounding boxes for drag handles');
        }

        await adminPage.mouse.move(
            sourceBounds.x + sourceBounds.width / 2,
            sourceBounds.y + sourceBounds.height / 2
        );
        await adminPage.mouse.down();
        await adminPage.mouse.move(
            targetBounds.x + targetBounds.width / 2,
            targetBounds.y + targetBounds.height / 2,
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
        blogTypeVariable = blogType.variable;
    });

    test('paginates at 6 items per page @smoke', async ({ adminPage, apiHelpers, testSuffix }) => {
        const authors = await apiHelpers.createContentlets(
            authorTypeVariable,
            Array.from({ length: 8 }, (_, j) => {
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

        await relationshipField.expectRowCount(6);
        await relationshipField.expectPaginationVisible();

        // Capture page 1 titles
        const page1Titles: string[] = [];
        for (let i = 0; i < 6; i++) {
            page1Titles.push(await relationshipField.getRowTitle(i));
        }

        await relationshipField.clickNextPage();
        await relationshipField.expectRowCount(2);

        // Verify page 2 shows different items than page 1
        const page2Titles: string[] = [];
        for (let i = 0; i < 2; i++) {
            page2Titles.push(await relationshipField.getRowTitle(i));
        }

        for (const title of page2Titles) {
            expect(page1Titles).not.toContain(title);
        }
    });

    test('no pagination with 6 or fewer items', async ({ adminPage, apiHelpers, testSuffix }) => {
        const authors = await apiHelpers.createContentlets(
            authorTypeVariable,
            Array.from({ length: 5 }, (_, j) => {
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

        await relationshipField.expectRowCount(5);
        await relationshipField.expectPaginationHidden();
    });
});
