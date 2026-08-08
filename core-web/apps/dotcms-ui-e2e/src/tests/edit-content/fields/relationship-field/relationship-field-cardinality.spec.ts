import { NewEditContentFormPage } from '@pages';

import { RelationshipField } from './helpers/relationship-field';
import { SelectExistingContentDialog } from './helpers/select-existing-content-dialog';

import {
    CARDINALITY,
    test,
    type TestContentType,
    type TestContentlet
} from '../../../../fixtures/relationship.fixture';

/**
 * Verifies that the "Select Existing Content" dialog disables items
 * already related to another parent (ONE_TO_MANY cardinality).
 *
 * Setup:
 *   - Comments type (title + comment field) → 2 contentlets
 *   - Post type (title + relationship 1:M to Comments)
 *   - Post A relates to Comment 1
 *   - Post B has no relationships → edit this one and open dialog
 *   - Comment 1 should be disabled, Comment 2 selectable
 */
test.describe('Cardinality Constraints', () => {
    test.describe('ONE_TO_MANY', () => {
        let commentsType: TestContentType;
        let postType: TestContentType;
        let comments: TestContentlet[];
        let postB: TestContentlet;

        test.beforeEach(async ({ apiHelpers, testSuffix }) => {
            commentsType = await apiHelpers.createContentType({
                clazz: 'com.dotcms.contenttype.model.type.ImmutableSimpleContentType',
                name: `E2E_Comments_${testSuffix}`,
                variable: `E2EComments${testSuffix}`,
                host: 'SYSTEM_HOST',
                folder: 'SYSTEM_FOLDER',
                metadata: { CONTENT_EDITOR2_ENABLED: true },
                workflow: ['d61a59e1-a49c-46f2-a929-db2b4bfa88b2'],
                fields: [
                    {
                        clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField',
                        name: 'Title',
                        variable: 'title',
                        sortOrder: 1
                    },
                    {
                        clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField',
                        name: 'Comment',
                        variable: 'comment',
                        sortOrder: 2
                    }
                ]
            });

            postType = await apiHelpers.createContentType({
                clazz: 'com.dotcms.contenttype.model.type.ImmutableSimpleContentType',
                name: `E2E_Post_${testSuffix}`,
                variable: `E2EPost${testSuffix}`,
                host: 'SYSTEM_HOST',
                folder: 'SYSTEM_FOLDER',
                metadata: { CONTENT_EDITOR2_ENABLED: true },
                workflow: ['d61a59e1-a49c-46f2-a929-db2b4bfa88b2'],
                fields: [
                    {
                        clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField',
                        name: 'Title',
                        variable: 'title',
                        sortOrder: 1
                    },
                    {
                        clazz: 'com.dotcms.contenttype.model.field.ImmutableRelationshipField',
                        name: 'Comments',
                        variable: 'comments',
                        sortOrder: 2,
                        relationships: {
                            velocityVar: commentsType.variable,
                            cardinality: CARDINALITY.ONE_TO_MANY
                        }
                    }
                ]
            });

            comments = await apiHelpers.createContentlets(commentsType.variable, [
                { title: 'Comment 1', comment: 'First comment' },
                { title: 'Comment 2', comment: 'Second comment' }
            ]);

            await apiHelpers.createContentletWithRelationship(
                postType.variable,
                { title: 'Post A' },
                { comments: [comments[0].identifier].join(',') }
            );

            postB = await apiHelpers.createContentlet(postType.variable, { title: 'Post B' });
        });

        test('comment already related to another post appears disabled @critical', async ({
            adminPage
        }) => {
            const formPage = new NewEditContentFormPage(adminPage);
            const relField = new RelationshipField(adminPage, 'comments');
            const dialog = new SelectExistingContentDialog(adminPage);

            await formPage.goToContent(postB.inode);

            await relField.clickRelateExisting();
            await dialog.waitForVisible();
            await dialog.waitForContentLoaded();

            await dialog.expectRowCount(2);

            await dialog.expectRowConstrainedByText('Comment 1');
            await dialog.expectRowSelectableByText('Comment 2');
        });
    });

    /**
     * ONE_TO_ONE cardinality: each child can only belong to one parent.
     *
     * Setup:
     *   - ChildType (title) → 2 contentlets (Child 1, Child 2)
     *   - ParentType (title + relationship 1:1 to ChildType)
     *   - Parent A relates to Child 1
     *   - Edit existing Parent B (no relationships) → Child 1 disabled, Child 2 selectable
     *   - Create NEW Parent → Child 1 disabled, Child 2 selectable
     */
    test.describe('ONE_TO_ONE', () => {
        let childType: TestContentType;
        let parentType: TestContentType;
        let children: TestContentlet[];
        let parentB: TestContentlet;

        test.beforeEach(async ({ apiHelpers, testSuffix }) => {
            childType = await apiHelpers.createContentType({
                clazz: 'com.dotcms.contenttype.model.type.ImmutableSimpleContentType',
                name: `E2E_Child_${testSuffix}`,
                variable: `E2EChild${testSuffix}`,
                host: 'SYSTEM_HOST',
                folder: 'SYSTEM_FOLDER',
                metadata: { CONTENT_EDITOR2_ENABLED: true },
                workflow: ['d61a59e1-a49c-46f2-a929-db2b4bfa88b2'],
                fields: [
                    {
                        clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField',
                        name: 'Title',
                        variable: 'title',
                        sortOrder: 1
                    }
                ]
            });

            parentType = await apiHelpers.createContentType({
                clazz: 'com.dotcms.contenttype.model.type.ImmutableSimpleContentType',
                name: `E2E_Parent_${testSuffix}`,
                variable: `E2EParent${testSuffix}`,
                host: 'SYSTEM_HOST',
                folder: 'SYSTEM_FOLDER',
                metadata: { CONTENT_EDITOR2_ENABLED: true },
                workflow: ['d61a59e1-a49c-46f2-a929-db2b4bfa88b2'],
                fields: [
                    {
                        clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField',
                        name: 'Title',
                        variable: 'title',
                        sortOrder: 1
                    },
                    {
                        clazz: 'com.dotcms.contenttype.model.field.ImmutableRelationshipField',
                        name: 'Rel',
                        variable: 'rel',
                        sortOrder: 2,
                        relationships: {
                            velocityVar: childType.variable,
                            cardinality: CARDINALITY.ONE_TO_ONE
                        }
                    }
                ]
            });

            children = await apiHelpers.createContentlets(childType.variable, [
                { title: 'Child 1' },
                { title: 'Child 2' }
            ]);

            // Parent A takes Child 1
            await apiHelpers.createContentletWithRelationship(
                parentType.variable,
                { title: 'Parent A' },
                { rel: children[0].identifier }
            );

            // Parent B — no relationships (for edit test)
            parentB = await apiHelpers.createContentlet(parentType.variable, {
                title: 'Parent B'
            });
        });

        test('child already related to another parent appears disabled (edit existing) @critical', async ({
            adminPage
        }) => {
            const formPage = new NewEditContentFormPage(adminPage);
            const relField = new RelationshipField(adminPage, 'rel');
            const dialog = new SelectExistingContentDialog(adminPage);

            await formPage.goToContent(parentB.inode);

            await relField.clickRelateExisting();
            await dialog.waitForVisible();
            await dialog.waitForContentLoaded();

            await dialog.expectRowCount(2);

            await dialog.expectRowConstrainedByText('Child 1');
            await dialog.expectRowSelectableByText('Child 2');
        });

        test('child already related appears disabled when creating new parent @critical', async ({
            adminPage
        }) => {
            const formPage = new NewEditContentFormPage(adminPage);
            const relField = new RelationshipField(adminPage, 'rel');
            const dialog = new SelectExistingContentDialog(adminPage);

            // Navigate to create a NEW parent contentlet
            await formPage.goToNew(parentType.variable);

            await relField.clickRelateExisting();
            await dialog.waitForVisible();
            await dialog.waitForContentLoaded();

            await dialog.expectRowCount(2);

            await dialog.expectRowConstrainedByText('Child 1');
            await dialog.expectRowSelectableByText('Child 2');
        });
    });
});
