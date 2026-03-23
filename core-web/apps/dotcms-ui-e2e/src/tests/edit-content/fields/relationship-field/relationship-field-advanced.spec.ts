import { NewEditContentFormPage } from '@pages';
import { CARDINALITY, expect, test, TestContentlet } from '../../../../fixtures/relationship.fixture';
import { RelationshipField } from './helpers/relationship-field';
import { SelectExistingContentDialog } from './helpers/select-existing-content-dialog';

/**
 * Journey 9: Multiple Relationship Fields on the Same Content
 * Journey 10: Custom Columns (showFields)
 * Journey 11: Errors and Edge Cases
 */

// ─── Journey 9: Multiple Relationship Fields ─────────────────────

test.describe('Journey 9 - Multiple Relationship Fields on the Same Content', () => {
    let blogTypeId: string;
    let authorTypeVariable: string;
    let blogTypeVariable: string;

    test.beforeEach(async ({ apiHelpers, testSuffix }) => {
        const authorType = await apiHelpers.createContentType(
            apiHelpers.authorPayload(`Multi_${testSuffix}`)
        );
        authorTypeVariable = authorType.variable;

        // Create a content type with TWO relationship fields
        const blogPayload = {
            clazz: 'com.dotcms.contenttype.model.type.ImmutableSimpleContentType',
            name: `E2E_Blog_MultiRelation_${testSuffix}`,
            variable: `E2EBlogMultiRelation${testSuffix}`,
            host: 'SYSTEM_HOST',
            folder: 'SYSTEM_FOLDER',
            metadata: { CONTENT_EDITOR2_ENABLED: true },
            fields: [
                {
                    clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField',
                    name: 'Title',
                    variable: 'title',
                    sortOrder: 1
                },
                {
                    clazz: 'com.dotcms.contenttype.model.field.ImmutableRelationshipField',
                    name: 'Authors',
                    variable: 'authors',
                    sortOrder: 2,
                    relationships: {
                        velocityVar: `${authorTypeVariable}.authors`,
                        cardinality: CARDINALITY.ONE_TO_MANY
                    }
                },
                {
                    clazz: 'com.dotcms.contenttype.model.field.ImmutableRelationshipField',
                    name: 'Tags',
                    variable: 'tags',
                    sortOrder: 3,
                    relationships: {
                        velocityVar: `${authorTypeVariable}.tags`,
                        cardinality: CARDINALITY.MANY_TO_MANY
                    }
                }
            ]
        };

        const blogType = await apiHelpers.createContentType(blogPayload);
        blogTypeId = blogType.id;
        blogTypeVariable = blogType.variable;

        // Create 5 Authors
        for (let i = 1; i <= 5; i++) {
            await apiHelpers.createContentlet(authorTypeVariable, {
                title: `MultiAuthor ${i} ${testSuffix}`,
                bio: `Bio ${i}`
            });
        }
    });

    test.afterEach(async ({ apiHelpers }) => {
        if (blogTypeVariable) {
            await apiHelpers.deleteContentType(blogTypeId);
        }
        if (authorTypeVariable) {
            await apiHelpers.deleteContentType(authorTypeVariable);
        }
    });

    test('P1 - Coexistence of multiple relationship fields @critical', async ({
        adminPage,
        testSuffix
    }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToNew(blogTypeVariable);
        await adminPage.waitForLoadState('networkidle');

        // There should be two relationship field tables on the page
        const allTables = adminPage.getByTestId('relationship-field-table');
        await expect(allTables).toHaveCount(2);

        // Work with the first relationship field ("authors")
        const authorsField = new RelationshipField(adminPage);

        // Fill title
        await formPage.fillTextField(`Multi Blog ${testSuffix}`);

        // Add 2 Authors to the first field
        const authorsAddBtn = allTables.first().getByTestId('relationship-add-button');
        await authorsAddBtn.click();
        const menu1 = adminPage.locator('.p-menu-overlay, .p-menu').last();
        await expect(menu1).toBeVisible();
        await menu1.locator('.p-menuitem').first().click();

        const dialog1 = new SelectExistingContentDialog(adminPage);
        await dialog1.waitForVisible();
        await dialog1.waitForContentLoaded();
        await dialog1.selectItems([0, 1]);
        await dialog1.clickApply();
        await dialog1.expectClosed();

        // Add 3 items to the second field ("tags")
        const tagsAddBtn = allTables.last().getByTestId('relationship-add-button');
        await tagsAddBtn.click();
        const menu2 = adminPage.locator('.p-menu-overlay, .p-menu').last();
        await expect(menu2).toBeVisible();
        await menu2.locator('.p-menuitem').first().click();

        const dialog2 = new SelectExistingContentDialog(adminPage);
        await dialog2.waitForVisible();
        await dialog2.waitForContentLoaded();
        await dialog2.selectItems([0, 1, 2]);
        await dialog2.clickApply();
        await dialog2.expectClosed();

        // Verify both fields have their items
        const authorsRows = allTables.first().locator('tbody tr:not(:has(.pi-folder-open))');
        await expect(authorsRows).toHaveCount(2);

        const tagsRows = allTables.last().locator('tbody tr:not(:has(.pi-folder-open))');
        await expect(tagsRows).toHaveCount(3);

        // Save
        await formPage.save();

        // Reload and verify persistence
        await adminPage.waitForTimeout(1000);
        await adminPage.reload();
        await adminPage.waitForLoadState('networkidle');

        const reloadedTables = adminPage.getByTestId('relationship-field-table');
        const reloadedAuthorsRows = reloadedTables
            .first()
            .locator('tbody tr:not(:has(.pi-folder-open))');
        const reloadedTagsRows = reloadedTables
            .last()
            .locator('tbody tr:not(:has(.pi-folder-open))');

        await expect(reloadedAuthorsRows).toHaveCount(2);
        await expect(reloadedTagsRows).toHaveCount(3);
    });

    test('P2 - Independence between fields @smoke', async ({
        adminPage,
        testSuffix
    }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToNew(blogTypeVariable);
        await adminPage.waitForLoadState('networkidle');

        const allTables = adminPage.getByTestId('relationship-field-table');

        await formPage.fillTextField(`Independence Blog ${testSuffix}`);

        // Add 2 Authors to the first field
        const authorsAddBtn = allTables.first().getByTestId('relationship-add-button');
        await authorsAddBtn.click();
        const menu1 = adminPage.locator('.p-menu-overlay, .p-menu').last();
        await expect(menu1).toBeVisible();
        await menu1.locator('.p-menuitem').first().click();

        const dialog1 = new SelectExistingContentDialog(adminPage);
        await dialog1.waitForVisible();
        await dialog1.waitForContentLoaded();
        await dialog1.selectItems([0, 1]);
        await dialog1.clickApply();
        await dialog1.expectClosed();

        // Add 2 items to the second field
        const tagsAddBtn = allTables.last().getByTestId('relationship-add-button');
        await tagsAddBtn.click();
        const menu2 = adminPage.locator('.p-menu-overlay, .p-menu').last();
        await expect(menu2).toBeVisible();
        await menu2.locator('.p-menuitem').first().click();

        const dialog2 = new SelectExistingContentDialog(adminPage);
        await dialog2.waitForVisible();
        await dialog2.waitForContentLoaded();
        await dialog2.selectItems([0, 1]);
        await dialog2.clickApply();
        await dialog2.expectClosed();

        // Delete one item from the authors field
        const authorsDeleteBtn = allTables
            .first()
            .getByTestId('relationship-delete-button')
            .first();
        await authorsDeleteBtn.click();

        // Verify: authors has 1, tags still has 2
        const authorsRows = allTables.first().locator('tbody tr:not(:has(.pi-folder-open))');
        await expect(authorsRows).toHaveCount(1);

        const tagsRows = allTables.last().locator('tbody tr:not(:has(.pi-folder-open))');
        await expect(tagsRows).toHaveCount(2);
    });
});

// ─── Journey 10: Custom Columns (showFields) ────────────────────

test.describe('Journey 10 - Custom Columns (showFields)', () => {
    let blogTypeId: string;
    let authorTypeVariable: string;
    let blogTypeVariable: string;
    let blogContentlet: TestContentlet;

    test.beforeEach(async ({ apiHelpers, testSuffix }) => {
        const authorType = await apiHelpers.createContentType(
            apiHelpers.authorPayload(`ShowFields_${testSuffix}`)
        );
        authorTypeVariable = authorType.variable;

        // Create Blog type with showFields configured on the relationship
        const blogPayload = {
            clazz: 'com.dotcms.contenttype.model.type.ImmutableSimpleContentType',
            name: `E2E_Blog_ShowFields_${testSuffix}`,
            variable: `E2EBlogShowFields${testSuffix}`,
            host: 'SYSTEM_HOST',
            folder: 'SYSTEM_FOLDER',
            metadata: { CONTENT_EDITOR2_ENABLED: true },
            fields: [
                {
                    clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField',
                    name: 'Title',
                    variable: 'title',
                    sortOrder: 1
                },
                {
                    clazz: 'com.dotcms.contenttype.model.field.ImmutableRelationshipField',
                    name: 'Authors',
                    variable: 'authors',
                    sortOrder: 2,
                    relationships: {
                        velocityVar: `${authorTypeVariable}.authors`,
                        cardinality: CARDINALITY.ONE_TO_MANY
                    },
                    fieldVariables: [
                        {
                            key: 'showFields',
                            value: 'title,bio'
                        }
                    ]
                }
            ]
        };

        const blogType = await apiHelpers.createContentType(blogPayload);
        blogTypeId = blogType.id;
        blogTypeVariable = blogType.variable;

        // Create authors
        const authors: TestContentlet[] = [];
        for (let i = 1; i <= 2; i++) {
            const author = await apiHelpers.createContentlet(authorTypeVariable, {
                title: `ShowFields Author ${i} ${testSuffix}`,
                bio: `Detailed bio for author ${i}`
            });
            authors.push(author);
        }

        // Create Blog with related authors
        blogContentlet = await apiHelpers.createContentletWithRelationship(
            blogTypeVariable,
            { title: `Blog ShowFields Test ${testSuffix}` },
            { authors: authors.map((a) => a.identifier).join(',') }
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

    test('P3 - Custom columns with showFields configured', async ({ adminPage }) => {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToContent(blogContentlet.inode);
        await adminPage.waitForLoadState('networkidle');

        const relationshipField = new RelationshipField(adminPage);
        await relationshipField.expectRowCount(2);

        // Check the table headers for custom columns
        const table = adminPage.getByTestId('relationship-field-table');
        const headers = table.locator('thead th');

        // With showFields="title,bio", columns should include Title and Bio
        // rather than the default Language/Status columns
        const headerTexts: string[] = [];
        const headerCount = await headers.count();
        for (let i = 0; i < headerCount; i++) {
            const text = await headers.nth(i).textContent();
            if (text?.trim()) {
                headerTexts.push(text.trim().toLowerCase());
            }
        }

        // Should contain 'title' and 'bio' headers
        const hasTitle = headerTexts.some((h) => h.includes('title'));
        const hasBio = headerTexts.some((h) => h.includes('bio'));
        expect(hasTitle).toBe(true);
        expect(hasBio).toBe(true);
    });

    test('P3 - Default columns without showFields', async ({
        adminPage,
        apiHelpers,
        testSuffix
    }) => {
        // Create a blog type WITHOUT showFields
        const defaultBlogPayload = {
            clazz: 'com.dotcms.contenttype.model.type.ImmutableSimpleContentType',
            name: `E2E_Blog_DefaultCols_${testSuffix}`,
            variable: `E2EBlogDefaultCols${testSuffix}`,
            host: 'SYSTEM_HOST',
            folder: 'SYSTEM_FOLDER',
            metadata: { CONTENT_EDITOR2_ENABLED: true },
            fields: [
                {
                    clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField',
                    name: 'Title',
                    variable: 'title',
                    sortOrder: 1
                },
                {
                    clazz: 'com.dotcms.contenttype.model.field.ImmutableRelationshipField',
                    name: 'Authors',
                    variable: 'authors',
                    sortOrder: 2,
                    relationships: {
                        velocityVar: `${authorTypeVariable}.authors`,
                        cardinality: CARDINALITY.ONE_TO_MANY
                    }
                }
            ]
        };

        const defaultBlogType = await apiHelpers.createContentType(defaultBlogPayload);

        try {
            // Create author
            const author = await apiHelpers.createContentlet(authorTypeVariable, {
                title: `DefaultCol Author ${testSuffix}`,
                bio: 'Bio'
            });

            // Create blog with author
            const blog = await apiHelpers.createContentletWithRelationship(
                defaultBlogType.variable,
                { title: `Blog DefaultCols Test ${testSuffix}` },
                { authors: author.identifier }
            );

            const formPage = new NewEditContentFormPage(adminPage);
            await formPage.goToContent(blog.inode);
            await adminPage.waitForLoadState('networkidle');

            // Check for default columns: Title, Language, Status
            const table = adminPage.getByTestId('relationship-field-table');
            const headers = table.locator('thead th');

            const headerTexts: string[] = [];
            const headerCount = await headers.count();
            for (let i = 0; i < headerCount; i++) {
                const text = await headers.nth(i).textContent();
                if (text?.trim()) {
                    headerTexts.push(text.trim().toLowerCase());
                }
            }

            const hasTitle = headerTexts.some((h) => h.includes('title'));
            const hasLanguage = headerTexts.some((h) => h.includes('language'));
            const hasStatus = headerTexts.some((h) => h.includes('status'));

            expect(hasTitle).toBe(true);
            expect(hasLanguage).toBe(true);
            expect(hasStatus).toBe(true);
        } finally {
            await apiHelpers.deleteContentType(defaultBlogType.id);
        }
    });
});

// ─── Journey 11: Errors and Edge Cases ───────────────────────────

test.describe('Journey 11 - Errors and Edge Cases', () => {
    test('P3 - Error loading content in selection dialog', async ({
        adminPage,
        apiHelpers,
        testSuffix
    }) => {
        const authorType = await apiHelpers.createContentType(
            apiHelpers.authorPayload(`Error_${testSuffix}`)
        );

        const blogType = await apiHelpers.createContentType(
            apiHelpers.blogPayload(
                `Error_${testSuffix}`,
                'E2E_Blog_Error',
                'E2EBlogError',
                authorType.variable,
                'authors',
                CARDINALITY.ONE_TO_MANY
            )
        );

        try {
            const formPage = new NewEditContentFormPage(adminPage);
            await formPage.goToNew(blogType.variable);
            await adminPage.waitForLoadState('networkidle');

            // Intercept the content API to force an error
            await adminPage.route('**/api/content/_search**', (route) =>
                route.fulfill({
                    status: 500,
                    contentType: 'application/json',
                    body: JSON.stringify({ message: 'Internal Server Error' })
                })
            );

            const relationshipField = new RelationshipField(adminPage);
            await relationshipField.clickRelateExisting();

            const dialog = new SelectExistingContentDialog(adminPage);
            await dialog.waitForVisible();

            // Should show error message
            await dialog.expectErrorMessage();

            // Clean up route
            await adminPage.unroute('**/api/content/_search**');
            await adminPage.keyboard.press('Escape');
        } finally {
            await apiHelpers.deleteContentType(blogType.id);
            await apiHelpers.deleteContentType(authorType.variable);
        }
    });
});
