import { NewEditContentFormPage } from '@pages';

import { RelationshipField } from './helpers/relationship-field';
import { SelectExistingContentDialog } from './helpers/select-existing-content-dialog';

import {
    CARDINALITY,
    expect,
    test,
    type TestContentType,
    type TestContentlet
} from '../../../../fixtures/relationship.fixture';

/**
 * Verifies that the "Select Existing Content" dialog disables items
 * already related to another parent (ONE_TO_MANY cardinality).
 *
 * Setup:
 *   - Comments type (title + comment field) → 3 contentlets
 *   - Post type (title + relationship 1:M to Comments)
 *   - Post A relates to Comment 1
 *   - Post B has no relationships → edit this one and open dialog
 *   - Comment 1 should be disabled, Comment 2 and 3 selectable
 */
test.describe('Cardinality Constraints', () => {
    let commentsType: TestContentType;
    let postType: TestContentType;
    let comments: TestContentlet[];
    let postB: TestContentlet;

    test.beforeEach(async ({ apiHelpers, testSuffix }) => {
        // 1. Comments content type with title + comment fields
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

        // 2. Post content type with title + relationship (1:M) to Comments
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

        // 3. Create 2 comments
        comments = await apiHelpers.createContentlets(commentsType.variable, [
            { title: 'Comment 1', comment: 'First comment' },
            { title: 'Comment 2', comment: 'Second comment' }
        ]);

        // 4. Create Post A and relate it to Comment 1
        await apiHelpers.createContentletWithRelationship(
            postType.variable,
            { title: 'Post A' },
            { comments: [comments[0].identifier].join(',') }
        );

        // 5. Create Post B (no relationships) — this is the one we'll edit
        postB = await apiHelpers.createContentlet(postType.variable, { title: 'Post B' });
    });

    test('comment already related to another post appears disabled @critical', async ({
        adminPage
    }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        const relField = new RelationshipField(adminPage, 'comments');
        const dialog = new SelectExistingContentDialog(adminPage);

        // Navigate to edit Post B
        await formPage.goToContent(postB.inode);

        // Open "Select Existing Content" dialog
        await relField.clickRelateExisting();
        await dialog.waitForVisible();
        await dialog.waitForContentLoaded();

        // Should see both comments
        await dialog.expectRowCount(2);

        // Find which row is Comment 1 (taken by Post A) and which is Comment 2 (free)
        let constrainedIdx = -1;
        let freeIdx = -1;

        for (let i = 0; i < 2; i++) {
            const rowText = await dialog.rows.nth(i).textContent();
            if (rowText?.includes('Comment 1')) {
                constrainedIdx = i;
            } else {
                freeIdx = i;
            }
        }

        expect(constrainedIdx, 'Comment 1 should exist in dialog').toBeGreaterThanOrEqual(0);

        // Comment 1 disabled — already related to Post A
        await dialog.expectRowConstrained(constrainedIdx);

        // Comment 2 selectable — free
        await dialog.expectRowSelectable(freeIdx);
    });
});
