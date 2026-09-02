import { NewEditContentFormPage } from '@pages';

import { RelationshipField } from './helpers/relationship-field';
import { SelectExistingContentDialog } from './helpers/select-existing-content-dialog';

import {
    CARDINALITY,
    expect,
    test,
    TestContentlet
} from '../../../../fixtures/relationship.fixture';

// ─── Add More Relations ─────────────────────────────────────────

test.describe('Add More Relations', () => {
    // Serial: limits concurrent API calls against one dotCMS instance (not shared UI state).
    test.describe.configure({ mode: 'serial' });

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

    test('load existing relationships on edit @critical', async ({ adminPage }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToContent(blogContentlet.inode);

        const relationshipField = new RelationshipField(adminPage);
        await relationshipField.expectRowCount(2);
    });

    test('add more relations to existing content @critical', async ({ adminPage }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToContent(blogContentlet.inode);

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

        await relationshipField.expectRowCount(3);
    });

    test.fixme('thumbnail rendering in relationship table', async ({ adminPage }) => {
        // TODO: Thumbnails only render for content types with image fields. E2E_Author has text fields only.
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToContent(blogContentlet.inode);

        const thumbnails = adminPage.getByTestId('contentlet-thumbnail');
        // 2 related authors = 2 thumbnails
        await expect(thumbnails).toHaveCount(2);
    });
});

// ─── Remove Relations ───────────────────────────────────────────

test.describe('Remove Relations', () => {
    // Serial: limits concurrent API calls against one dotCMS instance (not shared UI state).
    test.describe.configure({ mode: 'serial' });

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

    test('delete a single relation @critical', async ({ adminPage }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToContent(blogContentlet.inode);

        const relationshipField = new RelationshipField(adminPage);

        await relationshipField.expectRowCount(3);
        await relationshipField.deleteRow(0);
        await relationshipField.expectRowCount(2);
    });

    test('delete and verify persistence @critical', async ({ adminPage }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToContent(blogContentlet.inode);

        const relationshipField = new RelationshipField(adminPage);
        await relationshipField.expectRowCount(3);

        await relationshipField.deleteRow(0);
        await relationshipField.expectRowCount(2);

        await formPage.save();

        await relationshipField.expectRowCount(2);
    });

    test('delete all items @smoke', async ({ adminPage }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToContent(blogContentlet.inode);

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

    test('delete and re-add in single mode', async ({ adminPage }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToContent(blogContentlet.inode);

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
