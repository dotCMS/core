import { NewEditContentFormPage } from '@pages';
import {
    CARDINALITY,
    expect,
    test,
    TestContentlet
} from '../../../../fixtures/relationship.fixture';
import { RelationshipField } from './helpers/relationship-field';
import { SelectExistingContentDialog } from './helpers/select-existing-content-dialog';

// ─── Add More Relations ─────────────────────────────────────────

test.describe('Add More Relations', () => {
    // Serial: limits concurrent API calls against one dotCMS instance (not shared UI state).
    test.describe.configure({ mode: 'serial' });

    let blogTypeId: string;
    let authorTypeVariable: string;
    let blogTypeVariable: string;
    let blogContentlet: TestContentlet;
    let authors: TestContentlet[];

    test.beforeEach(async ({ apiHelpers, testSuffix }) => {
        const authorType = await apiHelpers.createContentType(
            apiHelpers.authorPayload(`Edit_${testSuffix}`)
        );
        authorTypeVariable = authorType.variable;

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

        authors = await apiHelpers.createContentlets(
            authorTypeVariable,
            Array.from({ length: 5 }, (_, j) => {
                const i = j + 1;
                return {
                    title: `Author Edit ${i} ${testSuffix}`,
                    bio: `Bio for edit author ${i}`
                };
            })
        );

        blogContentlet = await apiHelpers.createContentletWithRelationship(
            blogTypeVariable,
            { title: `Blog Edit Test ${testSuffix}` },
            { authors: `${authors[0].identifier},${authors[1].identifier}` }
        );
    });

    test.afterEach(async ({ apiHelpers }) => {
        if (blogTypeId) await apiHelpers.deleteContentType(blogTypeId);
        if (authorTypeVariable) await apiHelpers.deleteContentType(authorTypeVariable);
    });

    test('load existing relationships on edit @critical', async ({ adminPage }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToContent(blogContentlet.inode);
        await adminPage.waitForLoadState('networkidle');

        const relationshipField = new RelationshipField(adminPage);
        await relationshipField.expectRowCount(2);
    });

    test('add more relations to existing content @critical', async ({ adminPage }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToContent(blogContentlet.inode);
        await adminPage.waitForLoadState('networkidle');

        const relationshipField = new RelationshipField(adminPage);
        const selectDialog = new SelectExistingContentDialog(adminPage);

        await relationshipField.expectRowCount(2);

        await relationshipField.clickRelateExisting();
        await selectDialog.waitForVisible();
        await selectDialog.waitForContentLoaded();

        await selectDialog.selectItem(0);
        await selectDialog.clickApply();
        await selectDialog.expectClosed();

        await relationshipField.expectRowCount(3);

        await formPage.save();
        await adminPage.waitForLoadState('networkidle');

        await relationshipField.expectRowCount(3);
    });

    test.fixme('thumbnail rendering in relationship table', async ({ adminPage }) => {
        // TODO: Thumbnails only render for content types with image fields. E2E_Author has text fields only.
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToContent(blogContentlet.inode);
        await adminPage.waitForLoadState('networkidle');

        const thumbnails = adminPage.getByTestId('contentlet-thumbnail');
        // 2 related authors = 2 thumbnails
        await expect(thumbnails).toHaveCount(2);
    });
});

// ─── Remove Relations ───────────────────────────────────────────

test.describe('Remove Relations', () => {
    // Serial: limits concurrent API calls against one dotCMS instance (not shared UI state).
    test.describe.configure({ mode: 'serial' });

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

        authors = await apiHelpers.createContentlets(
            authorTypeVariable,
            Array.from({ length: 3 }, (_, j) => {
                const i = j + 1;
                return {
                    title: `Author Delete ${i} ${testSuffix}`,
                    bio: `Bio for delete author ${i}`
                };
            })
        );

        blogContentlet = await apiHelpers.createContentletWithRelationship(
            blogTypeVariable,
            { title: `Blog Delete Test ${testSuffix}` },
            { authors: authors.map((a) => a.identifier).join(',') }
        );
    });

    test.afterEach(async ({ apiHelpers }) => {
        if (blogTypeId) await apiHelpers.deleteContentType(blogTypeId);
        if (authorTypeVariable) await apiHelpers.deleteContentType(authorTypeVariable);
    });

    test('delete a single relation @critical', async ({ adminPage }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToContent(blogContentlet.inode);
        await adminPage.waitForLoadState('networkidle');

        const relationshipField = new RelationshipField(adminPage);

        await relationshipField.expectRowCount(3);
        await relationshipField.deleteRow(0);
        await relationshipField.expectRowCount(2);
    });

    test('delete and verify persistence @critical', async ({ adminPage }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToContent(blogContentlet.inode);
        await adminPage.waitForLoadState('networkidle');

        const relationshipField = new RelationshipField(adminPage);
        await relationshipField.expectRowCount(3);

        await relationshipField.deleteRow(0);
        await relationshipField.expectRowCount(2);

        await formPage.save();
        await adminPage.waitForLoadState('networkidle');

        await relationshipField.expectRowCount(2);
    });

    test('delete all items @smoke', async ({ adminPage }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToContent(blogContentlet.inode);
        await adminPage.waitForLoadState('networkidle');

        const relationshipField = new RelationshipField(adminPage);
        await relationshipField.expectRowCount(3);

        await relationshipField.deleteRow(0);
        await relationshipField.deleteRow(0);
        await relationshipField.deleteRow(0);

        await relationshipField.expectEmpty();
        await relationshipField.expectAddButtonEnabled();
    });
});

// ─── Delete and Re-add in Single Mode ───────────────────────────

test.describe('Delete and Re-add (Single Mode)', () => {
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

        authors = await apiHelpers.createContentlets(authorTypeVariable, [
            { title: `Author SingleDel 1 ${testSuffix}`, bio: 'Bio 1' },
            { title: `Author SingleDel 2 ${testSuffix}`, bio: 'Bio 2' }
        ]);

        blogContentlet = await apiHelpers.createContentletWithRelationship(
            blogTypeVariable,
            { title: `Blog SingleDel Test ${testSuffix}` },
            { mainAuthor: authors[0].identifier }
        );
    });

    test.afterEach(async ({ apiHelpers }) => {
        if (blogTypeId) await apiHelpers.deleteContentType(blogTypeId);
        if (authorTypeVariable) await apiHelpers.deleteContentType(authorTypeVariable);
    });

    test('delete and re-add in single mode', async ({ adminPage }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToContent(blogContentlet.inode);
        await adminPage.waitForLoadState('networkidle');

        const relationshipField = new RelationshipField(adminPage);
        const selectDialog = new SelectExistingContentDialog(adminPage);

        await relationshipField.expectRowCount(1);
        // In single mode, menu options are disabled when item exists
        await relationshipField.expectRelateExistingDisabled();

        await relationshipField.deleteRow(0);
        await relationshipField.expectEmpty();
        await relationshipField.expectAddButtonEnabled();

        await relationshipField.clickRelateExisting();
        await selectDialog.waitForVisible();
        await selectDialog.waitForContentLoaded();
        await selectDialog.selectSingleItem(0);
        await selectDialog.clickApply();
        await selectDialog.expectClosed();

        await relationshipField.expectRowCount(1);
    });
});
