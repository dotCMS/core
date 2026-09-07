import { NewEditContentFormPage } from '@pages';
import {
    createFakePayloadRelationshipField,
    createFakePayloadTextField,
    IMMUTABLE_SIMPLE_CONTENT_TYPE
} from '@utils/dot-content-types.mock';

import { RelationshipField } from './helpers/relationship-field';
import { SelectExistingContentDialog } from './helpers/select-existing-content-dialog';

import {
    CARDINALITY,
    expect,
    SYSTEM_WORKFLOW_ID,
    test,
    TestContentlet
} from '../../../../fixtures/relationship.fixture';

/**
 * Blog content type with title + `authors` (1:N) and optional extra relationship fields.
 */
function blogTypeWithRelationships(
    name: string,
    variable: string,
    authorTypeVariable: string,
    extraRelationshipFields: {
        name: string;
        variable: string;
        sortOrder: number;
        cardinality: number;
    }[] = []
): Record<string, unknown> {
    return {
        clazz: IMMUTABLE_SIMPLE_CONTENT_TYPE,
        name,
        variable,
        host: 'SYSTEM_HOST',
        folder: 'SYSTEM_FOLDER',
        metadata: { CONTENT_EDITOR2_ENABLED: true },
        workflow: [SYSTEM_WORKFLOW_ID],
        fields: [
            createFakePayloadTextField({
                name: 'Title',
                variable: 'title',
                sortOrder: 1
            }),
            createFakePayloadRelationshipField({
                name: 'Authors',
                variable: 'authors',
                sortOrder: 2,
                relationships: {
                    velocityVar: authorTypeVariable,
                    cardinality: CARDINALITY.ONE_TO_MANY
                }
            }),
            ...extraRelationshipFields.map((f) =>
                createFakePayloadRelationshipField({
                    name: f.name,
                    variable: f.variable,
                    sortOrder: f.sortOrder,
                    relationships: {
                        velocityVar: authorTypeVariable,
                        cardinality: f.cardinality
                    }
                })
            )
        ]
    };
}

// ─── Multiple Relationship Fields ───────────────────────────────

test.describe('Multiple Relationship Fields', () => {
    // Serial: limits concurrent API churn on one dotCMS backend; each test owns its content types.
    test.describe.configure({ mode: 'serial' });

    let authorTypeVariable: string;
    let blogTypeVariable: string;

    test.beforeEach(async ({ apiHelpers, testSuffix }) => {
        const authorType = await apiHelpers.createContentType(
            apiHelpers.authorPayload(`Multi_${testSuffix}`)
        );
        authorTypeVariable = authorType.variable;

        const blogPayload = blogTypeWithRelationships(
            `E2E_Blog_MultiRelation_${testSuffix}`,
            `E2EBlogMultiRelation${testSuffix}`,
            authorTypeVariable,
            [
                {
                    name: 'Tags',
                    variable: 'tags',
                    sortOrder: 3,
                    cardinality: CARDINALITY.MANY_TO_MANY
                }
            ]
        );

        const blogType = await apiHelpers.createContentType(blogPayload);
        blogTypeVariable = blogType.variable;

        await apiHelpers.createContentlets(
            authorTypeVariable,
            Array.from({ length: 5 }, (_, j) => {
                const i = j + 1;
                return {
                    title: `MultiAuthor ${i} ${testSuffix}`,
                    bio: `Bio ${i}`
                };
            })
        );
    });

    test('coexistence of multiple relationship fields @critical', async ({
        adminPage,
        testSuffix
    }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToNew(blogTypeVariable);

        // Use fieldVariable to scope each relationship field
        const authorsField = new RelationshipField(adminPage, 'authors');
        const tagsField = new RelationshipField(adminPage, 'tags');

        await formPage.fillTextField(`Multi Blog ${testSuffix}`);

        // Add 2 Authors to the "authors" field
        await authorsField.clickRelateExisting();
        const selectDialog = new SelectExistingContentDialog(adminPage);
        await selectDialog.waitForVisible();
        await selectDialog.waitForContentLoaded();
        await selectDialog.selectItems([0, 1]);
        await selectDialog.clickApply();
        await selectDialog.expectClosed();

        // Add 3 items to the "tags" field
        await tagsField.clickRelateExisting();
        await selectDialog.waitForVisible();
        await selectDialog.waitForContentLoaded();
        await selectDialog.selectItems([0, 1, 2]);
        await selectDialog.clickApply();
        await selectDialog.expectClosed();

        // Verify both fields
        await authorsField.expectRowCount(2);
        await tagsField.expectRowCount(3);

        // Save
        await formPage.save();

        const reloadedAuthors = new RelationshipField(adminPage, 'authors');
        const reloadedTags = new RelationshipField(adminPage, 'tags');
        await reloadedAuthors.expectRowCount(2);
        await reloadedTags.expectRowCount(3);
    });

    test('independence between fields @smoke', async ({ adminPage, testSuffix }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToNew(blogTypeVariable);

        const authorsField = new RelationshipField(adminPage, 'authors');
        const tagsField = new RelationshipField(adminPage, 'tags');

        await formPage.fillTextField(`Independence Blog ${testSuffix}`);

        // Add 2 Authors
        await authorsField.clickRelateExisting();
        const selectDialog = new SelectExistingContentDialog(adminPage);
        await selectDialog.waitForVisible();
        await selectDialog.waitForContentLoaded();
        await selectDialog.selectItems([0, 1]);
        await selectDialog.clickApply();
        await selectDialog.expectClosed();

        // Add 2 Tags
        await tagsField.clickRelateExisting();
        await selectDialog.waitForVisible();
        await selectDialog.waitForContentLoaded();
        await selectDialog.selectItems([0, 1]);
        await selectDialog.clickApply();
        await selectDialog.expectClosed();

        // Delete one from authors
        await authorsField.deleteRow(0);

        // Verify: authors has 1, tags still has 2
        await authorsField.expectRowCount(1);
        await tagsField.expectRowCount(2);
    });
});

// ─── Custom Columns (showFields) ────────────────────────────────

test.describe('Custom Columns (showFields)', () => {
    // Serial: limits concurrent API churn on one dotCMS backend; each test owns its content types.
    test.describe.configure({ mode: 'serial' });

    let blogTypeId: string;
    let authorTypeVariable: string;
    let blogTypeVariable: string;
    let blogContentlet: TestContentlet;

    test.beforeEach(async ({ apiHelpers, testSuffix }) => {
        const authorType = await apiHelpers.createContentType(
            apiHelpers.authorPayload(`ShowFields_${testSuffix}`)
        );
        authorTypeVariable = authorType.variable;

        const blogPayload = blogTypeWithRelationships(
            `E2E_Blog_ShowFields_${testSuffix}`,
            `E2EBlogShowFields${testSuffix}`,
            authorTypeVariable
        );

        const blogType = await apiHelpers.createContentType(blogPayload);
        blogTypeId = blogType.id;
        blogTypeVariable = blogType.variable;

        // Add showFields variable to the relationship field after creation
        const relationshipFieldDef = blogType.fields.find((f) => f.variable === 'authors');
        if (relationshipFieldDef) {
            await apiHelpers.addFieldVariable(
                blogTypeId,
                relationshipFieldDef.id,
                'showFields',
                'title,bio'
            );
        }

        const authors = await apiHelpers.createContentlets(authorTypeVariable, [
            { title: `ShowFields Author 1 ${testSuffix}`, bio: 'Detailed bio for author 1' },
            { title: `ShowFields Author 2 ${testSuffix}`, bio: 'Detailed bio for author 2' }
        ]);

        blogContentlet = await apiHelpers.createContentletWithRelationship(
            blogTypeVariable,
            { title: `Blog ShowFields Test ${testSuffix}` },
            { authors: authors.map((a) => a.identifier).join(',') }
        );
    });

    test('custom columns with showFields configured', async ({ adminPage }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToContent(blogContentlet.inode);

        const relationshipField = new RelationshipField(adminPage);
        await relationshipField.expectRowCount(2);

        // With showFields="title,bio", headers should include Title and Bio
        const headerTexts = await relationshipField.getHeaderTexts();

        expect(headerTexts.some((h) => h.includes('title'))).toBe(true);
        expect(headerTexts.some((h) => h.includes('bio'))).toBe(true);
    });

    test('default columns without showFields', async ({ adminPage, apiHelpers, testSuffix }) => {
        // Create a blog type WITHOUT showFields
        const defaultBlogPayload = blogTypeWithRelationships(
            `E2E_Blog_DefaultCols_${testSuffix}`,
            `E2EBlogDefaultCols${testSuffix}`,
            authorTypeVariable
        );

        const defaultBlogType = await apiHelpers.createContentType(defaultBlogPayload);

        const author = await apiHelpers.createContentlet(authorTypeVariable, {
            title: `DefaultCol Author ${testSuffix}`,
            bio: 'Bio'
        });

        const blog = await apiHelpers.createContentletWithRelationship(
            defaultBlogType.variable,
            { title: `Blog DefaultCols Test ${testSuffix}` },
            { authors: author.identifier }
        );

        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToContent(blog.inode);

        // Default columns: Title, Locales, Status
        const relationshipField = new RelationshipField(adminPage);
        const headerTexts = await relationshipField.getHeaderTexts();

        expect(headerTexts.some((h) => h.includes('title'))).toBe(true);
        expect(headerTexts.some((h) => h.includes('locales'))).toBe(true);
        expect(headerTexts.some((h) => h.includes('status'))).toBe(true);
    });
});
